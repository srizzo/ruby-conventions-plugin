package io.github.srizzo.rubyconventions;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.ruby.ruby.lang.psi.variables.RIdentifier;

public class NameInferredReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(RIdentifier.class),
                new PsiReferenceProvider() {
                    @NotNull
                    @Override
                    public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                                 @NotNull ProcessingContext context) {
                        if (!(element instanceof RIdentifier)) return PsiReference.EMPTY_ARRAY;

                        return RubyConventions.processReferences(FileUtil.getModule(element), ((RIdentifier) element).getName())
                                .stream()
                                .map(found -> new NameInferredReference((RIdentifier) element, found))
                                .toArray(PsiReference[]::new);
                    }
                });
    }

}
