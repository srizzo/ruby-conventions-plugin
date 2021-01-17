package io.github.srizzo.rubyconventions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbolicExecution.SymbolicExecutionContext;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbolicExecution.SymbolicExpressionProvider;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbolicExecution.TypeInferenceComponent;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbolicExecution.instance.TypeInferenceInstance;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbolicExecution.symbolicExpression.SymbolicCall;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbolicExecution.symbolicExpression.SymbolicExpression;
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.RType;
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.RTypeUtil;
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.classes.RClass;

public class SymbolicTypeInferenceProvider implements org.jetbrains.plugins.ruby.ruby.codeInsight.symbolicExecution.SymbolicTypeInferenceProvider {
    @Override
    public @Nullable
    SymbolicExpression evaluateSymbolicCall(@NotNull SymbolicCall symbolicCall,
                                            @NotNull SymbolicExecutionContext symbolicExecutionContext,
                                            @NotNull TypeInferenceInstance.CallContext callContext,
                                            @NotNull SymbolicExpressionProvider symbolicExpressionProvider,
                                            @NotNull TypeInferenceComponent typeInferenceComponent) {

        RClass found = RubyConventions.processSymbolicTypeInference(callContext.getAnchor().getModule(), symbolicCall.getName());
        if (found == null) return null;

        RType type = RTypeUtil.getTypeByClass(found);
        if (RTypeUtil.isNullOrEmpty(type)) return null;

        SymbolicExpression expression = symbolicExpressionProvider.createSymbolicVariable();
        typeInferenceComponent.updateSymbolicExpressionType(expression, type);
        return expression;
    }
}
