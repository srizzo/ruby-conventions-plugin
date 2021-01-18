package io.github.srizzo.rubyconventions;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
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
import java.util.stream.Collectors;

public class RubyConventions {
    private static final String TYPE_PROVIDER_SCRIPT = "type_provider";
    private static final String SYMBOLIC_TYPE_INFERENCE_SCRIPT = "symbolic_type_inference";
    private static final String REFERENCES_SCRIPT = "references";
    private static final String GO_TO_RELATED_SCRIPT = "go_to_related";
    private static final String REFERENCED_AS_SCRIPT = "referenced_as";

    private static final Key<Map<String, CachedValue<String[]>>> TYPE_PROVIDER_CACHE = Key.create("RubyConventions.TYPE_PROVIDER_CACHE");
    private static final Key<Map<String, CachedValue<String[]>>> SYMBOLIC_TYPE_INFERENCE_CACHE = Key.create("RubyConventions.SYMBOLIC_TYPE_INFERENCE_CACHE");
    private static final Key<Map<String, CachedValue<String[]>>> REFERENCES_CACHE = Key.create("RubyConventions.REFERENCES_CACHE");
    private static final Key<Map<String, CachedValue<String[]>>> GO_TO_RELATED_CACHE = Key.create("RubyConventions.GO_TO_RELATED_CACHE");
    private static final Key<Map<String, CachedValue<String[]>>> REFERENCED_AS_CACHE = Key.create("RubyConventions.REFERENCED_AS_CACHE");

    private static final int TYPES_FROM_TEXT_MIN_LENGTH = 2;

    private static final Logger LOG = Logger.getInstance(RubyConventions.class.getName());

    public static RClass processTypeProvider(Module module, String text) {
        return processTypesFromText(module, TYPE_PROVIDER_SCRIPT, text, TYPE_PROVIDER_CACHE).stream().findFirst().orElse(null);
    }

    public static Collection<RClass> processReferences(Module module, String text) {
        return processTypesFromText(module, REFERENCES_SCRIPT, text, REFERENCES_CACHE);
    }

    public static Collection<RClass> processGoToRelated(Module module, String text) {
        return processTypesFromText(module, GO_TO_RELATED_SCRIPT, text, GO_TO_RELATED_CACHE);
    }

    public static RClass processSymbolicTypeInference(Module module, String text) {
        return processTypesFromText(module, SYMBOLIC_TYPE_INFERENCE_SCRIPT, text, SYMBOLIC_TYPE_INFERENCE_CACHE).stream().findFirst().orElse(null);
    }

    public static Collection<String> processReferencesSearch(Module module, String className) {
        VirtualFile scriptFile = getScriptFile(module, REFERENCED_AS_SCRIPT);
        if (scriptFile == null) return Collections.emptyList();

        return Arrays.asList(getCachedOrProcess(REFERENCED_AS_CACHE, module, scriptFile, className));
    }

    private static Collection<RClass> processTypesFromText(Module module, String script, String text, Key<Map<String, CachedValue<String[]>>> cacheKey) {
        if (text.length() < TYPES_FROM_TEXT_MIN_LENGTH) return Collections.emptyList();

        VirtualFile scriptFile = getScriptFile(module, script);
        if (scriptFile == null) return Collections.emptyList();

        String[] results = getCachedOrProcess(cacheKey, module, scriptFile, text);

        return Arrays.stream(results)
                .map((line) ->
                        (RClass) RubyFQNUtil.findContainerByFQN(module.getProject(),
                                new TypeSet(Type.CLASS),
                                FQN.Builder.fromString(line),
                                null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static String[] getCachedOrProcess(Key<Map<String, CachedValue<String[]>>> cacheKey, Module module, VirtualFile scriptFile, String text) {
        return getCache(cacheKey, module).
                computeIfAbsent(text, key -> {
                    CachedValue<String[]> cachedValue = CachedValuesManager.getManager(module.getProject())
                            .createCachedValue(() ->
                                    CachedValueProvider.Result.create(
                                            process(scriptFile, key), scriptFile
                                    )
                            );
                    cachedValue.getValue(); // forces first execution
                    return cachedValue;
                }).getValue();
    }

    @Nullable
    private static VirtualFile getScriptFile(Module module, String script) {
        VirtualFile contentRoot = FileUtil.getContentRootPath(module);
        if (contentRoot == null) return null;
        return contentRoot.findFileByRelativePath(".rubyconventions/" + script);
    }

    @NotNull
    private static Map<String, CachedValue<String[]>> getCache(Key<Map<String, CachedValue<String[]>>> cacheKey, Module module) {
        synchronized (cacheKey) {
            Map<String, CachedValue<String[]>> cache = module.getUserData(cacheKey);
            if (cache == null) {
                cache = new ConcurrentHashMap<>();
                module.putUserData(cacheKey, cache);
            }
            return cache;
        }
    }

    @NotNull
    private static String[] process(VirtualFile scriptPath, String text) {
        try {
            Map<String, String> env = new HashMap<>(System.getenv());
            env.put("RCP_TEXT", text);
            return ProcessUtil.execScript(scriptPath, env);
        } catch (IOException | InterruptedException e) {
            LOG.error(e);
            return new String[0];
        }
    }
}
