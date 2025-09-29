package com.example.diffplugin.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class DiffToolWindowFactory implements ToolWindowFactory {
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JLabel placeholderLabel = new JLabel("Select a file and use 'Show Unified Diff' to view changes", SwingConstants.CENTER);
        placeholderLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(placeholderLabel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
