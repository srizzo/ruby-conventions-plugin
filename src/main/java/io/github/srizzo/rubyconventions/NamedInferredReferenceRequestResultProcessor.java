package io.github.srizzo.rubyconventions;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.RequestResultProcessor;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.ruby.ruby.lang.psi.variables.RIdentifier;

public class NamedInferredReferenceRequestResultProcessor extends RequestResultProcessor {
    protected final PsiElement myReferredElement;

    public NamedInferredReferenceRequestResultProcessor(@NotNull PsiElement referredElement) {
        super(referredElement);
        this.myReferredElement = referredElement;
    }

    public boolean processTextOccurrence(@NotNull PsiElement candidate, int offsetInElement, @NotNull Processor<? super PsiReference> processor) {
        if (!(candidate.isValid() && candidate instanceof RIdentifier)) return true;

        for (PsiReference reference : candidate.getReferences()) {
            if (reference instanceof NameInferredReference && reference.isReferenceTo(candidate))
                return processor.process(reference);
        }

        return true;
    }
}
