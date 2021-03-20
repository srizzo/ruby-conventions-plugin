package io.github.srizzo.rubyconventions.references;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import io.github.srizzo.rubyconventions.RubyConventions;
import io.github.srizzo.rubyconventions.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.ruby.ruby.lang.psi.variables.RIdentifier;

import java.util.Arrays;

public class NamingConventionReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(RIdentifier.class),
                new PsiReferenceProvider() {
                    @NotNull
                    @Override
                    public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                                 @NotNull ProcessingContext context) {
                        if (!(element instanceof RIdentifier)) return PsiReference.EMPTY_ARRAY;

                        return Arrays.stream(RubyConventions.processReferences(FileUtil.getModule(element), ((RIdentifier) element).getName()))
                                .map(found -> new NamingConventionClassReference((RIdentifier) element, found))
                                .toArray(PsiReference[]::new);
                    }
                });
    }

}
