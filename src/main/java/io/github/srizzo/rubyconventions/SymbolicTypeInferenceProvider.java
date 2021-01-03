
package io.github.srizzo.rubyconventions;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.ruby.codeInsight.RubyFQNUtil;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbolicExecution.SymbolicExecutionContext;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbolicExecution.SymbolicExpressionProvider;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbolicExecution.TypeInferenceComponent;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbolicExecution.instance.TypeInferenceInstance;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbolicExecution.symbolicExpression.SymbolicCall;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbolicExecution.symbolicExpression.SymbolicExpression;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.Type;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.TypeSet;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.fqn.FQN.Builder;
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.RType;
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.RTypeUtil;
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.classes.RClass;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SymbolicTypeInferenceProvider implements org.jetbrains.plugins.ruby.ruby.codeInsight.symbolicExecution.SymbolicTypeInferenceProvider {
    private static final Logger LOG = Logger.getInstance(SymbolicTypeInferenceProvider.class.getName());
    private static final String SCRIPT = "./symbolic_type_inference";

    @Override
    public @Nullable
    SymbolicExpression evaluateSymbolicCall(@NotNull SymbolicCall symbolicCall,
                                            @NotNull SymbolicExecutionContext symbolicExecutionContext,
                                            @NotNull TypeInferenceInstance.CallContext callContext,
                                            @NotNull SymbolicExpressionProvider symbolicExpressionProvider,
                                            @NotNull TypeInferenceComponent typeInferenceComponent) {
        try {
            Path contentRoot = FileUtil.getContentRootPath(callContext.getAnchor().getModule());
            if (contentRoot == null) return null;

            Map<String, String> env = new HashMap<>(System.getenv());
            env.put("RCP_TEXT", symbolicCall.getName());

            RType found = ProcessUtil.execIfExists(contentRoot, SCRIPT, env).map((line) ->
                    (RClass) RubyFQNUtil.findContainerByFQN(callContext.getAnchor().getProject(),
                            new TypeSet(Type.CLASS),
                            Builder.fromString(line),
                            null))
                    .filter(Objects::nonNull)
                    .map(RTypeUtil::getTypeByClass)
                    .findFirst()
                    .orElse(null);

            if (RTypeUtil.isNotEmpty(found)) {
                SymbolicExpression expression = symbolicExpressionProvider.createSymbolicVariable();
                typeInferenceComponent.updateSymbolicExpressionType(expression, found);
                return expression;
            }
        } catch (IOException | InterruptedException e) {
            LOG.error(e);
        }
        return null;
    }
}
