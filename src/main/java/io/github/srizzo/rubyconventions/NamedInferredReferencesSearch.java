package io.github.srizzo.rubyconventions;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.SearchRequestCollector;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.ReferencesSearch.SearchParameters;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.ruby.ruby.lang.psi.RubyPsiUtil;
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.classes.RClass;

public final class NamedInferredReferencesSearch extends QueryExecutorBase<PsiReference, SearchParameters> {
    public NamedInferredReferencesSearch() {
        super(true);
    }

    public void processQuery(@NotNull SearchParameters params, @NotNull Processor<? super PsiReference> consumer) {
        PsiElement elementToSearch = params.getElementToSearch();
        if (!(elementToSearch instanceof RClass)) return;

        Module module = FileUtil.getModule(elementToSearch);
        if (module == null) return;

        SearchRequestCollector optimizer = params.getOptimizer();
        SearchScope searchScope = RubyPsiUtil.restrictScopeToRubyFiles(params.getEffectiveSearchScope());

        RubyConventions.processReferencesSearch(module, ((RClass) elementToSearch).getFQNWithNesting().toString())
                .forEach((line) ->
                        optimizer.searchWord(line, searchScope, UsageSearchContext.IN_CODE, true, elementToSearch, new NamedInferredReferenceRequestResultProcessor(elementToSearch))
                );
    }
}
