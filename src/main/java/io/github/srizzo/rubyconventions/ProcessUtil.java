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
    public static String[] execScript(VirtualFile scriptPath, Map<String, String> env) throws IOException, InterruptedException {
        VirtualFile workingDir = scriptPath.getParent();
        Process process = Runtime.getRuntime().exec(scriptPath.toNioPath().toString(), toEnvArray(env), workingDir.toNioPath().toFile());
        process.waitFor();

        try (BufferedReader processIn = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

            if (process.exitValue() != 0) {
                LOG.error("Process exited with value: " + process.exitValue());
            }
            error.lines().forEach(LOG::error);

            return processIn.lines().toArray(String[]::new);
        }
    }

    private static String[] toEnvArray(Map<String, String> env) {
        return env.entrySet().stream().map((entry) -> entry.getKey() + "=" + entry.getValue()).toArray(String[]::new);
    }
}
