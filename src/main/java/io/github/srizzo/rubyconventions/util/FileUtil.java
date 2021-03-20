package io.github.srizzo.rubyconventions.util;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;

public class FileUtil {
    public static VirtualFile getContentRootPath(Module module) {
        if (module == null) return null;
        return ModuleRootManager.getInstance(module).getContentRoots()[0];
    }

    public static Module getModule(PsiElement element) {
        return ModuleUtilCore.findModuleForPsiElement(element);
    }
}
