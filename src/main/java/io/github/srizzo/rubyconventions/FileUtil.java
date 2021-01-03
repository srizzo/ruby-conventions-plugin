package io.github.srizzo.rubyconventions;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.PsiElement;

import java.nio.file.Path;

public class FileUtil {
    public static Path getContentRootPath(Module module) {
        if (module == null) return null;
        return ModuleRootManager.getInstance(module).getContentRoots()[0].toNioPath();
    }

    public static Path getContentRootPath(PsiElement element) {
        return getContentRootPath(ProjectRootManager.getInstance(element.getProject())
                .getFileIndex().getModuleForFile(element.getContainingFile().getVirtualFile()));
    }
}
