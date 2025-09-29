package com.example.diffplugin;

import com.example.diffplugin.model.DiffBlock;
import com.example.diffplugin.services.DiffCalculationService;
import com.example.diffplugin.services.GitService;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment;
import com.intellij.psi.PsiElement;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class DiffGutterIconProvider implements LineMarkerProvider {
    
    @Override
    public @Nullable LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        // We'll add line markers for elements that have changes
        PsiFile psiFile = element.getContainingFile();
        if (psiFile == null) return null;
        
        VirtualFile virtualFile = psiFile.getVirtualFile();
        if (virtualFile == null) return null;
        
        Project project = element.getProject();
        GitService gitService = project.getService(GitService.class);
        
        if (!gitService.isUnderGit(virtualFile)) {
            return null;
        }
        
        // Check if this line has changes
        int lineNumber = getLineNumber(element);
        if (lineNumber < 0) return null;
        
        if (hasChangesAtLine(project, virtualFile, lineNumber)) {
            return new LineMarkerInfo<>(
                element,
                element.getTextRange(),
                com.intellij.icons.AllIcons.Vcs.Vendors.Github,
                null,
                new DiffNavigationHandler(project, virtualFile),
                Alignment.LEFT,
                () -> "Show unified diff for this file"
            );
        }
        
        return null;
    }
    
    private int getLineNumber(PsiElement element) {
        // Get the line number for the element
        // This is a simplified implementation
        return element.getTextOffset();
    }
    
    private boolean hasChangesAtLine(Project project, VirtualFile file, int lineNumber) {
        GitService gitService = project.getService(GitService.class);
        DiffCalculationService diffService = project.getService(DiffCalculationService.class);
        
        Optional<String> lastCommitContent = gitService.getLastCommitContent(file);
        Optional<String> currentContent = gitService.getCurrentContent(file);
        
        if (lastCommitContent.isEmpty() || currentContent.isEmpty()) {
            return false;
        }
        
        List<DiffBlock> diffBlocks = diffService.calculateDiff(
            lastCommitContent.get(), 
            currentContent.get()
        );
        
        // Check if any diff block contains this line
        for (DiffBlock block : diffBlocks) {
            if (lineNumber >= block.getStartLine() && lineNumber <= block.getEndLine()) {
                return true;
            }
        }
        
        return false;
    }
    
    private static class DiffNavigationHandler implements GutterIconNavigationHandler<PsiElement> {
        private final Project project;
        private final VirtualFile file;
        
        public DiffNavigationHandler(Project project, VirtualFile file) {
            this.project = project;
            this.file = file;
        }
        
        @Override
        public void navigate(MouseEvent e, PsiElement elt) {
            // Trigger the show unified diff action
            // This would ideally invoke the ShowUnifiedDiffAction
            JOptionPane.showMessageDialog(null, 
                "Click detected! This would show the unified diff view for: " + file.getName());
        }
    }
}
