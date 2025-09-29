package com.example.diffplugin.actions;

import com.example.diffplugin.services.InlineDiffService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class ToggleInlineDiffAction extends AnAction {
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        
        if (project == null || editor == null || file == null) {
            return;
        }
        
        InlineDiffService diffService = project.getService(InlineDiffService.class);
        diffService.updateInlineDiff(editor, file);
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        
        boolean enabled = project != null && file != null && editor != null;
        e.getPresentation().setEnabledAndVisible(enabled);
    }
}
