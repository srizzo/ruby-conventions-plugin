package io.github.srizzo.rubyconventions;

import com.intellij.navigation.GotoRelatedItem;
import com.intellij.navigation.GotoRelatedProvider;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.NlsContexts.ListItem;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.actions.BaseRefactoringAction;
import com.intellij.util.containers.ContainerUtil;
import icons.RubyIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.ruby.codeInsight.RubyFQNUtil;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.Types;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.fqn.FQN;
import org.jetbrains.plugins.ruby.ruby.lang.psi.holders.RContainer;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class RelatedTypeDefinitionsProvider extends GotoRelatedProvider {
    private static final Logger LOG = Logger.getInstance(RelatedTypeDefinitionsProvider.class.getName());
    public static final String SCRIPT = "./go_to_related";

    @NotNull
    public List<? extends GotoRelatedItem> getItems(@NotNull DataContext dataContext) {
        PsiElement elementAtCaret = getElementAtCaret(dataContext);
        if (elementAtCaret == null) return Collections.emptyList();

        List<RContainer> results = new ArrayList<>();

        try {
            ContainerUtil.addAll(results, getTypeDefinitionRelatedItem(elementAtCaret).iterator());
        } catch (IOException | InterruptedException e) {
            LOG.error(e);
        }

        return results
                .stream()
                .map(TypeDefinitionItem::new)
                .collect(Collectors.toList());
    }

    @Nullable
    private Collection<RContainer> getTypeDefinitionRelatedItem(@NotNull PsiElement elementAtCaret) throws IOException, InterruptedException {
        Path contentRoot = FileUtil.getContentRootPath(elementAtCaret);
        if (contentRoot == null) return Collections.emptyList();

        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("RCP_TEXT", elementAtCaret.getText());

        return ProcessUtil.execIfExists(contentRoot, SCRIPT, env).flatMap((line) ->
                RubyFQNUtil.findContainersByFQN(elementAtCaret.getProject(),
                        Types.MODULE_OR_CLASS_OR_CONSTANT,
                        FQN.Builder.fromString(line),
                        null)
                        .stream())
                .collect(Collectors.toList());
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

        public TypeDefinitionItem(@NotNull RContainer result) {
            super(result.getOriginalElement(), "Type Definition");
            this.name = result.getFQNWithNesting().toString();
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
