package io.github.srizzo.rubyconventions;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

public class ProcessUtil {
    private static final Logger LOG = Logger.getInstance(ProcessUtil.class.getName());

    @NotNull
    public static Stream<String> execIfExists(Path contentRoot, String cmd, Map<String, String> env) throws IOException, InterruptedException {
        Path pluginScriptsFolder = contentRoot.resolve(".rubyconventions");

        if (!Files.exists(pluginScriptsFolder.resolve(cmd))) return Stream.empty();

        Process process = Runtime.getRuntime().exec(cmd, toEnvArray(env), pluginScriptsFolder.toFile());
        process.waitFor();

        try (BufferedReader processIn = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

            if (process.exitValue() != 0) {
                LOG.error("Process exited with value: " + process.exitValue());
            }
            error.lines().forEach(LOG::error);

            return Arrays.stream(processIn.lines().toArray(String[]::new));
        }
    }

    private static String[] toEnvArray(Map<String, String> env) {
        return env.entrySet().stream().map((entry) -> entry.getKey() + "=" + entry.getValue()).toArray(String[]::new);
    }
}
