package io.github.srizzo.rubyconventions;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import io.github.srizzo.rubyconventions.util.FileUtil;
import io.github.srizzo.rubyconventions.util.ProcessUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.ruby.codeInsight.RubyFQNUtil;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.Type;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.TypeSet;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.fqn.FQN;
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.classes.RClass;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class RubyConventions {
    private static final Logger LOG = Logger.getInstance(RubyConventions.class.getName());

    private static final TypeSet CLASS_TYPE_SET = new TypeSet(Type.CLASS);
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final RClass[] EMPTY_RCLASS_ARRAY = new RClass[0];
    private static final int TYPES_FROM_TEXT_MIN_LENGTH = 2;

    private static final String REFERENCED_AS_SCRIPT = "referenced_as";
    private static final String GO_TO_RELATED_SCRIPT = "go_to_related";
    private static final String TYPE_PROVIDER_SCRIPT = "type_provider";
    private static final String SYMBOLIC_TYPE_INFERENCE_SCRIPT = "symbolic_type_inference";
    private static final String REFERENCES_SCRIPT = "references";

    private static final Key<Map<String, CachedValue<String[]>>> REFERENCED_AS_CACHE = Key.create("RubyConventions.REFERENCED_AS_CACHE");
    private static final Key<Map<String, CachedValue<RClass[]>>> GO_TO_RELATED_CACHE = Key.create("RubyConventions.GO_TO_RELATED_CACHE");
    private static final Key<Map<String, CachedValue<RClass[]>>> TYPE_PROVIDER_CACHE = Key.create("RubyConventions.TYPE_PROVIDER_CACHE");
    private static final Key<Map<String, CachedValue<RClass[]>>> SYMBOLIC_TYPE_INFERENCE_CACHE = Key.create("RubyConventions.SYMBOLIC_TYPE_INFERENCE_CACHE");
    private static final Key<Map<String, CachedValue<RClass[]>>> REFERENCES_CACHE = Key.create("RubyConventions.REFERENCES_CACHE");

    public static Collection<String> processReferencesSearch(Module module, String className) {
        return Arrays.asList(getCachedTextResultsOrProcess(className, module, REFERENCED_AS_CACHE, REFERENCED_AS_SCRIPT));
    }

    public static RClass processTypeProvider(Module module, String text) {
        return firstOrNull(getCachedTypeResultsOrProcess(module, TYPE_PROVIDER_SCRIPT, text, TYPE_PROVIDER_CACHE));
    }

    public static RClass[] processReferences(Module module, String text) {
        return getCachedTypeResultsOrProcess(module, REFERENCES_SCRIPT, text, REFERENCES_CACHE);
    }


    public static RClass processSymbolicTypeInference(Module module, String text) {
        return firstOrNull(getCachedTypeResultsOrProcess(module, SYMBOLIC_TYPE_INFERENCE_SCRIPT, text, SYMBOLIC_TYPE_INFERENCE_CACHE));
    }


    public static RClass[] processGoToRelated(Module module, String text) {
        return getCachedTypeResultsOrProcess(module, GO_TO_RELATED_SCRIPT, text, GO_TO_RELATED_CACHE);
    }

    private static RClass[] getCachedTypeResultsOrProcess(Module module, String scriptName, String text, Key<Map<String, CachedValue<RClass[]>>> cacheStorageKey) {
        if (text == null || text.length() < TYPES_FROM_TEXT_MIN_LENGTH) return EMPTY_RCLASS_ARRAY;
        VirtualFile scriptFile = getScriptFile(module, scriptName);
        return cached(text, (String value) -> lookupTypes(module.getProject(), process(scriptFile, text)), module, cacheStorageKey, scriptFile);
    }

    private static String[] getCachedTextResultsOrProcess(String text, Module module, Key<Map<String, CachedValue<String[]>>> cacheStorageKey, String scriptName) {
        VirtualFile scriptFile = getScriptFile(module, scriptName);
        return cached(text, (String value) -> process(scriptFile, value), module, cacheStorageKey, scriptFile);
    }

    @Nullable
    private static VirtualFile getScriptFile(Module module, String script) {
        VirtualFile contentRoot = FileUtil.getContentRootPath(module);
        if (contentRoot == null) return null;
        return contentRoot.findFileByRelativePath(".rubyconventions/" + script);
    }

    @NotNull
    private static <T> T[] cached(String lookupKey, Function<String, T[]> cacheFunction, Module module, Key<Map<String, CachedValue<T[]>>> cacheStorageKey, VirtualFile cacheDependency) {
        return getCacheStorage(cacheStorageKey, module)
                .computeIfAbsent(lookupKey, key -> {
                    var cachedValue = CachedValuesManager.getManager(module.getProject())
                            .createCachedValue(() ->
                                    CachedValueProvider.Result.create(
                                            cacheFunction.apply(key), cacheDependency
                                    )
                            );
                    cachedValue.getValue(); // forces first execution
                    return cachedValue;
                }).getValue();
    }

    @NotNull
    private static <T> Map<String, CachedValue<T>> getCacheStorage(Key<Map<String, CachedValue<T>>> cacheStorageKey, Module module) {
        Map<String, CachedValue<T>> cacheStorage = module.getUserData(cacheStorageKey);
        if (cacheStorage == null) {
            cacheStorage = new ConcurrentHashMap<>();
            module.putUserData(cacheStorageKey, cacheStorage);
        }
        return cacheStorage;
    }

    @NotNull
    private static String[] process(VirtualFile scriptPath, String text) {
        if (scriptPath == null || !scriptPath.exists() || !scriptPath.isValid()) return EMPTY_STRING_ARRAY;

        try {
            Map<String, String> env = new HashMap<>(System.getenv());
            env.put("RCP_TEXT", text);
            return ProcessUtil.execScript(scriptPath, env);
        } catch (IOException | InterruptedException e) {
            LOG.error(e);
            return new String[0];
        }
    }

    @NotNull
    private static RClass[] lookupTypes(Project project, String[] results) {
        return Arrays.stream(results)
                .map((line) ->
                        (RClass) RubyFQNUtil.findContainerByFQN(project,
                                CLASS_TYPE_SET,
                                FQN.Builder.fromString(line),
                                null))
                .filter(Objects::nonNull)
                .toArray(RClass[]::new);
    }

    @Nullable
    private static RClass firstOrNull(RClass[] array) {
        if (array.length == 0) return null;
        return array[0];
    }
}
