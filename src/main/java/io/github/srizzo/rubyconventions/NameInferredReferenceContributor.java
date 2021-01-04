package io.github.srizzo.rubyconventions;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.ruby.ruby.codeInsight.RubyFQNUtil;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.Type;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.TypeSet;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.fqn.FQN;
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.classes.RClass;
import org.jetbrains.plugins.ruby.ruby.lang.psi.variables.RIdentifier;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class NameInferredReferenceContributor extends PsiReferenceContributor {
    private static final Logger LOG = Logger.getInstance(NameInferredReferenceContributor.class.getName());
    private static final String SCRIPT = "./references";

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(RIdentifier.class),
                new PsiReferenceProvider() {
                    @NotNull
                    @Override
                    public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                                 @NotNull ProcessingContext context) {
                        try {
                            if (!(element instanceof RIdentifier)) return PsiReference.EMPTY_ARRAY;

                            Path contentRoot = FileUtil.getContentRootPath(element);
                            if (contentRoot == null) return null;

                            Map<String, String> env = new HashMap<>(System.getenv());
                            env.put("RCP_TEXT", ((RIdentifier) element).getName());

                            return ProcessUtil.execIfExists(contentRoot, SCRIPT, env).map((line) ->
                                    (RClass) RubyFQNUtil.findContainerByFQN(element.getProject(),
                                            new TypeSet(Type.CLASS),
                                            FQN.Builder.fromString(line),
                                            null))
                                    .filter(Objects::nonNull)
                                    .map(found -> new NameInferredReference((RIdentifier) element, found))
                                    .toArray(PsiReference[]::new);


                        } catch (IOException | InterruptedException e) {
                            LOG.error(e);
                        }

                        return PsiReference.EMPTY_ARRAY;
                    }
                });
    }

}
