package com.example.diffplugin.listeners;

import com.example.diffplugin.services.InlineDiffService;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class DocumentChangeListener implements DocumentListener {
    private final Alarm alarm = new Alarm();
    private final Map<Document, Runnable> pendingUpdates = new ConcurrentHashMap<>();
    private static final int DELAY_MS = 500; // Delay to avoid too frequent updates
    
    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        Document document = event.getDocument();
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        
        if (file == null || !file.isValid()) {
            return;
        }
        
        // Cancel any pending update for this document
        Runnable pendingUpdate = pendingUpdates.get(document);
        if (pendingUpdate != null) {
            alarm.cancelRequest(pendingUpdate);
        }
        
        // Schedule a new update with delay to avoid too frequent updates
        Runnable updateTask = () -> {
            pendingUpdates.remove(document);
            updateInlineDiff(document, file);
        };
        
        pendingUpdates.put(document, updateTask);
        alarm.addRequest(updateTask, DELAY_MS);
    }
    
    private void updateInlineDiff(Document document, VirtualFile file) {
        // Find the project for this file
        Project project = findProjectForFile(file);
        if (project == null) {
            return;
        }
        
        // Find all editors for this document
        Editor[] editors = EditorFactory.getInstance().getEditors(document, project);
        for (Editor editor : editors) {
            InlineDiffService diffService = project.getService(InlineDiffService.class);
            diffService.updateInlineDiff(editor, file);
        }
    }
    
    private Project findProjectForFile(VirtualFile file) {
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : openProjects) {
            if (project.getProjectFile() != null && 
                file.getPath().startsWith(project.getBasePath())) {
                return project;
            }
        }
        return openProjects.length > 0 ? openProjects[0] : null;
    }
}
