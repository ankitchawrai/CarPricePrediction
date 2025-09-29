package com.example.diffplugin.startup;

import com.example.diffplugin.listeners.DocumentChangeListener;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class DiffPluginStartupActivity implements StartupActivity {
    
    @Override
    public void runActivity(@NotNull Project project) {
        // Register the document change listener when the project starts
        DocumentChangeListener listener = new DocumentChangeListener();
        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(listener, project);
    }
}
