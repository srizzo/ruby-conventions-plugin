package io.github.srizzo.rubyconventions;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

public class ProcessUtil {
    private static final Logger LOG = Logger.getInstance(ProcessUtil.class.getName());

    @NotNull
    public static String[] execScript(VirtualFile scriptFile, Map<String, String> env) throws IOException, InterruptedException {
        VirtualFile workingDir = scriptFile.getParent();
        String scriptPath = scriptFile.toNioPath().toString();
        Process process = Runtime.getRuntime().exec(scriptPath, toEnvArray(env), workingDir.toNioPath().toFile());
        process.waitFor();

        try (BufferedReader processIn = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

            if (process.exitValue() != 0) {
                LOG.error(scriptPath + " exited with value: " + process.exitValue());
            }

            String[] stderr = error.lines().toArray(String[]::new);
            if (stderr.length > 0) LOG.error(String.join("\n", stderr));

            return processIn.lines().toArray(String[]::new);
        }
    }

    private static String[] toEnvArray(Map<String, String> env) {
        return env.entrySet().stream().map((entry) -> entry.getKey() + "=" + entry.getValue()).toArray(String[]::new);
    }
}
