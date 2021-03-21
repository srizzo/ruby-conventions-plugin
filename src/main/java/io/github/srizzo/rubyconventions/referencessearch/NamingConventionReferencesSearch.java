package io.github.srizzo.rubyconventions.referencessearch;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.RequestResultProcessor;
import com.intellij.psi.search.SearchRequestCollector;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.ReferencesSearch.SearchParameters;
import com.intellij.util.Processor;
import io.github.srizzo.rubyconventions.RubyConventions;
import io.github.srizzo.rubyconventions.references.NamingConventionClassReference;
import io.github.srizzo.rubyconventions.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.ruby.lang.psi.RubyPsiUtil;
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.classes.RClass;
import org.jetbrains.plugins.ruby.ruby.lang.psi.variables.RIdentifier;

public final class NamingConventionReferencesSearch extends QueryExecutorBase<PsiReference, SearchParameters> {
    public static final boolean NOT_PROCESSABLE = true;

    public NamingConventionReferencesSearch() {
        super(true);
    }

    public void processQuery(@NotNull SearchParameters params, @NotNull Processor<? super PsiReference> consumer) {
        @NotNull PsiElement target = params.getElementToSearch();
        if (!(target instanceof RClass)) return;

        @Nullable Module module = FileUtil.getModule(target);
        if (module == null) return;

        String fullClassName = ((RClass) target).getFQNWithNesting().toString();
        SearchScope searchScope = RubyPsiUtil.restrictScopeToRubyFiles(params.getEffectiveSearchScope());
        SearchRequestCollector optimizer = params.getOptimizer();

        RubyConventions.processReferencesSearch(module, fullClassName)
                .forEach((toSearch) ->
                        optimizer.searchWord(toSearch, searchScope, UsageSearchContext.IN_CODE, true, target,
                                new RequestResultProcessor(target) {

                                    @Override
                                    public boolean processTextOccurrence(@NotNull PsiElement candidate,
                                                                         int offsetInElement,
                                                                         @NotNull Processor<? super PsiReference> processor) {
                                        if (!(candidate.isValid() && candidate instanceof RIdentifier))
                                            return NOT_PROCESSABLE;

                                        // TODO fix this
                                        for (PsiReference reference : candidate.getReferences()) {
                                            if (reference instanceof NamingConventionClassReference && reference.isReferenceTo(candidate))
                                                processor.process(reference);
                                        }

                                        return NOT_PROCESSABLE;
                                    }
                                })
                );
    }
}
