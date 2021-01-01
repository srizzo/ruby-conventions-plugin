package io.github.srizzo.rubyconventions;

import com.intellij.navigation.GotoRelatedItem;
import com.intellij.navigation.GotoRelatedProvider;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.NlsContexts.ListItem;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.actions.BaseRefactoringAction;
import com.intellij.util.containers.ContainerUtil;
import icons.RubyIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.ruby.codeInsight.resolve.scope.RElementWithFQN;
import org.jetbrains.plugins.ruby.ruby.lang.psi.RFile;
import org.jetbrains.plugins.ruby.ruby.lang.psi.RubyProjectAndLibrariesScope;
import org.jetbrains.plugins.ruby.ruby.lang.psi.indexes.RubyClassModuleNameIndex;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class RelatedTypeDefinitionsProvider extends GotoRelatedProvider {
    private static final Logger LOG = Logger.getInstance(RelatedTypeDefinitionsProvider.class.getName());

    @NotNull
    public List<? extends GotoRelatedItem> getItems(@NotNull DataContext dataContext) {
        PsiElement elementAtCaret = getElementAtCaret(dataContext);
        if (elementAtCaret == null) return Collections.emptyList();

        List<RElementWithFQN> results = new ArrayList<>();
        try {
            ContainerUtil.addAll(results, getTypeDefinitionRelatedItem(elementAtCaret).iterator());
        } catch (IOException | InterruptedException e) {
            LOG.error(e);
        }

        return results
                .stream()
                .map((result) -> new TypeDefinitionItem(result, result.getFQNWithNesting().toString()))
                .collect(Collectors.toList());
    }

    @NotNull
    private String[] getProcessEnv(PsiElement elementAtCaret) {
        Path contentRoot = ProjectRootManager.getInstance(elementAtCaret.getProject()).getFileIndex()
                .getContentRootForFile(elementAtCaret.getContainingFile().getVirtualFile()).toNioPath();
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("RCP_FILE_PATH", elementAtCaret.getContainingFile().getVirtualFile().toNioPath().relativize(contentRoot).toFile().toString());
        env.put("RCP_TEXT", elementAtCaret.getText());
        env.put("RCP_PSI_PATH", psiPath(elementAtCaret));
        env.put("RCP_PSI_CLASS", elementAtCaret.getClass().getCanonicalName());
        env.put("RCP_PSI_TYPE", elementAtCaret.toString());
        return env.entrySet().stream().map((entry) -> entry.getKey() + "=" + entry.getValue()).toArray(String[]::new);
    }

    @Nullable
    private Collection<RElementWithFQN> getTypeDefinitionRelatedItem(@NotNull PsiElement elementAtCaret) throws IOException, InterruptedException {
        Path contentRoot = ProjectRootManager.getInstance(elementAtCaret.getProject())
                .getFileIndex().getContentRootForFile(elementAtCaret.getContainingFile().getVirtualFile()).toNioPath();
        GlobalSearchScope scope = new RubyProjectAndLibrariesScope(elementAtCaret.getProject());

        String script = "./go_to_related";

        Path pluginScriptsFolder = contentRoot.resolve(".rubyconventions");

        if (!Files.exists(pluginScriptsFolder.resolve(script))) {
            return Collections.emptyList();
        }

        String[] envArray = getProcessEnv(elementAtCaret);

        Process process = Runtime.getRuntime().exec(script, envArray, pluginScriptsFolder.toFile());
        process.waitFor();

        List<RElementWithFQN> results = new ArrayList<>();

        try (BufferedReader processIn = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = processIn.readLine()) != null) {
                results.addAll(RubyClassModuleNameIndex.find(elementAtCaret.getProject(), line, scope));
            }

            if (process.exitValue() != 0) {
                LOG.error("Process exited with value: " + process.exitValue());
            }
            error.lines().forEach(LOG::error);
        }

        return results;
    }

    private String psiPath(PsiElement elementAtCaret) {
        List<? extends PsiElement> parents =
                new ArrayList<>(PsiTreeUtil.collectParents(elementAtCaret, PsiElement.class, true, x -> x instanceof RFile));
        Collections.reverse(parents);
        return parents.stream().map(Object::toString).collect(Collectors.joining("#"));
    }

    private PsiElement getElementAtCaret(@NotNull DataContext dataContext) {
        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        PsiFile psiFile = PsiUtilCore.getTemplateLanguageFile(CommonDataKeys.PSI_FILE.getData(dataContext));
        if (editor == null || psiFile == null) return null;

        return BaseRefactoringAction.getElementAtCaret(editor, psiFile);
    }

    public static final class TypeDefinitionItem extends GotoRelatedItem {
        @ListItem
        private final String name;

        public TypeDefinitionItem(@NotNull PsiElement element,
                                  @NotNull @ListItem String name) {
            super(element, "Type Definition");
            this.name = name;
        }

        @NotNull
        public String getCustomName() {
            return this.name;
        }

        @Nullable
        public String getCustomContainerName() {
            return null;
        }


        @Nullable
        public Icon getCustomIcon() {
            return RubyIcons.Ruby.Ruby;
        }
    }
}
