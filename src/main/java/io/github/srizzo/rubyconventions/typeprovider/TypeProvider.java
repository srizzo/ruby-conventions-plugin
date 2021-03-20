package io.github.srizzo.rubyconventions.typeprovider;

import com.intellij.openapi.module.Module;
import io.github.srizzo.rubyconventions.RubyConventions;
import io.github.srizzo.rubyconventions.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.ruby.ruby.codeInsight.AbstractRubyTypeProvider;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.Symbol;
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.RType;
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.RTypeUtil;
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.classes.RClass;
import org.jetbrains.plugins.ruby.ruby.lang.psi.expressions.RExpression;
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.RStubBasedPsiElementBase;
import org.jetbrains.plugins.ruby.ruby.lang.psi.methodCall.RCall;
import org.jetbrains.plugins.ruby.ruby.lang.psi.variables.RIdentifier;

public class TypeProvider extends AbstractRubyTypeProvider {
    public RType createTypeBySymbol(@NotNull Symbol symbol) {
        return null;
    }

    public RType createTypeByRExpression(@NotNull RExpression expression) {
        if (!(expression instanceof RIdentifier) && !(expression instanceof RCall)) return null;

        Module module = FileUtil.getModule(expression);
        if (module == null) return null;

        RClass found = RubyConventions.processTypeProvider(module, expression.getName());
        if (found != null) return RTypeUtil.getTypeByClass(found);

        return null;
    }
}
