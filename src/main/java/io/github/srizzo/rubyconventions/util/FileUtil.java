package io.github.srizzo.rubyconventions.util;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

public class FileUtil {
    @Nullable
    public static VirtualFile getContentRootPath(Module module) {
        if (module == null) return null;
        return ModuleRootManager.getInstance(module).getContentRoots()[0];
    }

    @Nullable
    public static Module getModule(PsiElement element) {
        return ModuleUtil.findModuleForPsiElement(element);
    }
}
