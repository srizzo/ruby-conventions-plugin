package io.github.srizzo.rubyconventions;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.diagnostic.Logger;
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class NamedInferredReferencesSearch extends QueryExecutorBase<PsiReference, SearchParameters> {
    private static final Logger LOG = Logger.getInstance(NamedInferredReferencesSearch.class.getName());
    private static final String SCRIPT = "./referenced_as";

    public NamedInferredReferencesSearch() {
        super(true);
    }

    public void processQuery(@NotNull SearchParameters params, @NotNull Processor<? super PsiReference> consumer) {
        try {
            PsiElement elementToSearch = params.getElementToSearch();
            if (!(elementToSearch instanceof RClass)) return;

            Path contentRoot = FileUtil.getContentRootPath(elementToSearch);
            if (contentRoot == null) return;

            Map<String, String> env = new HashMap<>(System.getenv());
            env.put("RCP_TEXT", ((RClass) elementToSearch).getFQNWithNesting().toString());

            SearchRequestCollector optimizer = params.getOptimizer();
            SearchScope searchScope = RubyPsiUtil.restrictScopeToRubyFiles(params.getEffectiveSearchScope());

            ProcessUtil.execIfExists(contentRoot, SCRIPT, env)
                    .forEach((line) ->
                            optimizer.searchWord(line, searchScope, UsageSearchContext.IN_CODE, true, elementToSearch, new NamedInferredReferenceRequestResultProcessor(elementToSearch))
                    );

        } catch (IOException | InterruptedException e) {
            LOG.error(e);
        }
    }
}
