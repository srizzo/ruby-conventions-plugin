package io.github.srizzo.rubyconventions.relatedtypedefinitionsprovider;

import com.intellij.navigation.GotoRelatedItem;
import com.intellij.navigation.GotoRelatedProvider;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.NlsContexts.ListItem;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.actions.BaseRefactoringAction;
import com.intellij.util.containers.ContainerUtil;
import icons.RubyIcons;
import io.github.srizzo.rubyconventions.RubyConventions;
import io.github.srizzo.rubyconventions.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.classes.RClass;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RelatedTypeDefinitionsProvider extends GotoRelatedProvider {

    @NotNull
    public List<? extends GotoRelatedItem> getItems(@NotNull DataContext dataContext) {
        PsiElement elementAtCaret = getElementAtCaret(dataContext);
        if (elementAtCaret == null) return Collections.emptyList();

        List<RClass> results = new ArrayList<>();

        ContainerUtil.addAll(results, RubyConventions.processGoToRelated(
                FileUtil.getModule(elementAtCaret),
                elementAtCaret.getText()));

        return results
                .stream()
                .map(TypeDefinitionItem::new)
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

        public TypeDefinitionItem(@NotNull RClass result) {
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
