
package io.github.srizzo.rubyconventions;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
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
import org.jetbrains.plugins.ruby.ruby.lang.psi.variables.RIdentifier;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TypeProvider extends AbstractRubyTypeProvider {
    private static final Logger LOG = Logger.getInstance(TypeProvider.class.getName());

    private static final String SCRIPT = "./symbolic_type_inference";
    //    public static final String SCRIPT = "./ruby_type_provider";

    public RType createTypeBySymbol(@NotNull Symbol symbol) {
        return null;
    }

    public RType createTypeByRExpression(@NotNull RExpression expression) {

        if (!(expression instanceof RIdentifier)) return null;
        RIdentifier identifier = (RIdentifier) expression;
        VirtualFile contentRoot1 = ModuleRootManager.getInstance(ModuleManager.getInstance(identifier.getProject()).getModules()[0]).getContentRoots()[0];
        Path contentRoot = contentRoot1.toNioPath();

//        ModuleRootManager.getInstance(ModuleManager.getInstance(project).getModules()[0]).getSourceRoots()

//        if (identifier.getContainingFile().getVirtualFile() == null) {
//            return null;
//        }


//        Path contentRoot = FileUtil.getContentRootPath(identifier);

//        Path contentRoot = Paths.get("/Users/ralphus/Development/intellij/rubytest");

//        if (true) return null;

        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("RCP_TEXT", identifier.getText());

        try {
            return ProcessUtil.execIfExists(contentRoot, SCRIPT, env).map((line) ->
                    (RClass) RubyFQNUtil.findContainerByFQN(identifier.getProject(),
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
