package com.example.diffplugin.actions;

import com.example.diffplugin.services.GitService;
import com.example.diffplugin.services.DiffCalculationService;
import com.example.diffplugin.ui.DiffViewPanel;
import com.example.diffplugin.model.DiffBlock;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class ShowUnifiedDiffAction extends AnAction {
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file == null) return;
        
        GitService gitService = project.getService(GitService.class);
        DiffCalculationService diffService = project.getService(DiffCalculationService.class);
        
        if (!gitService.isUnderGit(file)) {
            // Show notification that file is not under git
            return;
        }
        
        Optional<String> lastCommitContent = gitService.getLastCommitContent(file);
        Optional<String> currentContent = gitService.getCurrentContent(file);
        
        if (lastCommitContent.isEmpty() || currentContent.isEmpty()) {
            // Show notification that content could not be retrieved
            return;
        }
        
        List<DiffBlock> diffBlocks = diffService.calculateDiff(
            lastCommitContent.get(), 
            currentContent.get()
        );
        
        showDiffInToolWindow(project, file, diffBlocks, currentContent.get());
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        
        boolean enabled = project != null && file != null && editor != null;
        
        if (enabled) {
            GitService gitService = project.getService(GitService.class);
            enabled = gitService.isUnderGit(file);
        }
        
        e.getPresentation().setEnabledAndVisible(enabled);
    }
    
    private void showDiffInToolWindow(Project project, VirtualFile file, 
                                    List<DiffBlock> diffBlocks, String currentContent) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow("UnifiedDiff");
        
        if (toolWindow != null) {
            DiffViewPanel diffPanel = new DiffViewPanel(project, file, diffBlocks, currentContent);
            
            ContentFactory contentFactory = ContentFactory.getInstance();
            Content content = contentFactory.createContent(
                diffPanel, 
                file.getName() + " - Unified Diff", 
                false
            );
            
            toolWindow.getContentManager().removeAllContents(true);
            toolWindow.getContentManager().addContent(content);
            toolWindow.activate(null);
        }
    }
}
