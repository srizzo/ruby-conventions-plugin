package io.github.srizzo.rubyconventions.references;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.ruby.lang.psi.variables.RIdentifier;

public class NamingConventionClassReference extends PsiReferenceBase<RIdentifier> implements PsiReference {
    private final PsiElement myReferredElement;

    public NamingConventionClassReference(@NotNull RIdentifier referringElement, PsiElement referredElement) {
        super(referringElement, new TextRange(0, referringElement.getTextLength()), true);
        this.myReferredElement = referredElement;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        return myReferredElement;
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        return myReferredElement.isEquivalentTo(element);

//        element1.getManager().areElementsEquivalent(LetRCallPsiUtil.getEnclosingLetRCall(element1), element2);

//        // TODO
//        for (PsiReference reference : element.getReferences()) {
//            if (reference instanceof NamingConventionClassReference) {
//                return ((NamingConventionClassReference) reference).myReferredElement.isEquivalentTo(this.myReferredElement);
//            }
//        }
//        return false;

    }
}
