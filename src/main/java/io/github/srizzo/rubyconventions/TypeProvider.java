
package io.github.srizzo.rubyconventions;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.ruby.ruby.codeInsight.AbstractRubyTypeProvider;
import org.jetbrains.plugins.ruby.ruby.codeInsight.RubyFQNUtil;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.Type;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.TypeSet;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.fqn.FQN.Builder;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.Symbol;
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.RType;
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.RTypeUtil;
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.classes.RClass;
import org.jetbrains.plugins.ruby.ruby.lang.psi.expressions.RExpression;
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.RStubBasedPsiElementBase;
import org.jetbrains.plugins.ruby.ruby.lang.psi.methodCall.RCall;
import org.jetbrains.plugins.ruby.ruby.lang.psi.variables.RIdentifier;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TypeProvider extends AbstractRubyTypeProvider {
    private static final Logger LOG = Logger.getInstance(TypeProvider.class.getName());

    public static final String SCRIPT = "./type_provider";

    public RType createTypeBySymbol(@NotNull Symbol symbol) {
            return null;
    }

    public RType createTypeByRExpression(@NotNull RExpression expression) {
        if (!(expression instanceof RIdentifier) && !(expression instanceof RCall)) return null;

        try {
            Path contentRoot = FileUtil.getContentRootPath(expression);
            if (contentRoot == null) return null;

            Map<String, String> env = new HashMap<>(System.getenv());
            env.put("RCP_TYPE", ((RStubBasedPsiElementBase) expression).getElementType().toString());
            env.put("RCP_TEXT", expression.getName());

            return ProcessUtil.execIfExists(contentRoot, SCRIPT, env).map((line) ->
                    (RClass) RubyFQNUtil.findContainerByFQN(expression.getProject(),
                            new TypeSet(Type.CLASS),
                            Builder.fromString(line),
                            null))
                    .filter(Objects::nonNull)
                    .map(RTypeUtil::getTypeByClass)
                    .findFirst()
                    .orElse(null);

        } catch (IOException | InterruptedException e) {
            LOG.error(e);
        }
        return null;
    }
}
