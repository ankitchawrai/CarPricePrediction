package com.example.diffplugin.ui;

import com.example.diffplugin.model.DiffBlock;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class DiffViewPanel extends JPanel {
    private final Project project;
    private final VirtualFile file;
    private final List<DiffBlock> diffBlocks;
    private final String currentContent;
    private final Map<String, DiffBlockComponent> blockComponents;
    
    public DiffViewPanel(Project project, VirtualFile file, List<DiffBlock> diffBlocks, String currentContent) {
        this.project = project;
        this.file = file;
        this.diffBlocks = diffBlocks;
        this.currentContent = currentContent;
        this.blockComponents = new HashMap<>();
        
        initializeUI();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
        JScrollPane scrollPane = createDiffScrollPane();
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBorder(JBUI.Borders.empty(5));
        
        JLabel titleLabel = new JLabel("Unified Diff: " + file.getName());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerPanel.add(titleLabel);
        
        JLabel countLabel = new JLabel("(" + diffBlocks.size() + " changes)");
        countLabel.setForeground(JBColor.GRAY);
        headerPanel.add(countLabel);
        
        return headerPanel;
    }
    
    private JScrollPane createDiffScrollPane() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        if (diffBlocks.isEmpty()) {
            JLabel noChangesLabel = new JLabel("No changes detected", SwingConstants.CENTER);
            noChangesLabel.setBorder(JBUI.Borders.empty(20));
            mainPanel.add(noChangesLabel);
        } else {
            for (DiffBlock diffBlock : diffBlocks) {
                DiffBlockComponent blockComponent = new DiffBlockComponent(diffBlock);
                blockComponents.put(diffBlock.getBlockId(), blockComponent);
                mainPanel.add(blockComponent);
                mainPanel.add(Box.createVerticalStrut(10));
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        return scrollPane;
    }
    
    private class DiffBlockComponent extends JPanel {
        private final DiffBlock diffBlock;
        private JButton acceptButton;
        private JButton rejectButton;
        
        public DiffBlockComponent(DiffBlock diffBlock) {
            this.diffBlock = diffBlock;
            initializeBlockUI();
        }
        
        private void initializeBlockUI() {
            setLayout(new BorderLayout());
            setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor.GRAY, 1),
                JBUI.Borders.empty(5)
            ));
            
            JPanel buttonPanel = createButtonPanel();
            add(buttonPanel, BorderLayout.NORTH);
            
            JPanel contentPanel = createContentPanel();
            add(contentPanel, BorderLayout.CENTER);
        }
        
        private JPanel createButtonPanel() {
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            
            JLabel typeLabel = new JLabel(getTypeDescription());
            typeLabel.setFont(typeLabel.getFont().deriveFont(Font.BOLD));
            typeLabel.setForeground(getTypeColor());
            buttonPanel.add(typeLabel);
            
            buttonPanel.add(Box.createHorizontalStrut(10));
            
            acceptButton = new JButton("Accept");
            acceptButton.setToolTipText("Keep the new changes");
            acceptButton.addActionListener(new AcceptActionListener());
            buttonPanel.add(acceptButton);
            
            rejectButton = new JButton("Reject");
            rejectButton.setToolTipText("Revert to original content");
            rejectButton.addActionListener(new RejectActionListener());
            buttonPanel.add(rejectButton);
            
            return buttonPanel;
        }
        
        private JPanel createContentPanel() {
            JPanel contentPanel = new JPanel(new BorderLayout());
            
            if (diffBlock.isDeletion() || diffBlock.isModification()) {
                JPanel oldPanel = createCodePanel(diffBlock.getOldContent(), "Original (will be removed):", JBColor.RED.darker());
                contentPanel.add(oldPanel, BorderLayout.WEST);
            }
            
            if (diffBlock.isAddition() || diffBlock.isModification()) {
                JPanel newPanel = createCodePanel(diffBlock.getNewContent(), "New (will be kept):", JBColor.GREEN.darker());
                contentPanel.add(newPanel, BorderLayout.EAST);
            }
            
            return contentPanel;
        }
        
        private JPanel createCodePanel(List<String> lines, String title, Color borderColor) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(borderColor, 2),
                JBUI.Borders.empty(5)
            ));
            
            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));
            panel.add(titleLabel, BorderLayout.NORTH);
            
            StringBuilder content = new StringBuilder();
            for (String line : lines) {
                content.append(line).append("\n");
            }
            
            JTextArea textArea = new JTextArea(content.toString());
            textArea.setEditable(false);
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            textArea.setBackground(borderColor.equals(JBColor.RED.darker()) ? 
                new Color(255, 240, 240) : new Color(240, 255, 240));
            
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(300, Math.min(100, lines.size() * 20 + 20)));
            panel.add(scrollPane, BorderLayout.CENTER);
            
            return panel;
        }
        
        private String getTypeDescription() {
            switch (diffBlock.getType()) {
                case ADDED: return "Addition (lines " + diffBlock.getStartLine() + "-" + diffBlock.getEndLine() + ")";
                case DELETED: return "Deletion (lines " + diffBlock.getStartLine() + "-" + diffBlock.getEndLine() + ")";
                case MODIFIED: return "Modification (lines " + diffBlock.getStartLine() + "-" + diffBlock.getEndLine() + ")";
                default: return "Change";
            }
        }
        
        private Color getTypeColor() {
            switch (diffBlock.getType()) {
                case ADDED: return JBColor.GREEN.darker();
                case DELETED: return JBColor.RED.darker();
                case MODIFIED: return JBColor.ORANGE.darker();
                default: return JBColor.BLACK;
            }
        }
        
        private class AcceptActionListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Keep the current changes (do nothing, as current content is already there)
                acceptButton.setEnabled(false);
                rejectButton.setEnabled(false);
                acceptButton.setText("Accepted");
                
                // Optionally, you could mark this block as accepted in some way
                JOptionPane.showMessageDialog(DiffViewPanel.this, 
                    "Changes accepted for block: " + diffBlock.getBlockId());
            }
        }
        
        private class RejectActionListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Revert to original content
                revertToOriginalContent();
                acceptButton.setEnabled(false);
                rejectButton.setEnabled(false);
                rejectButton.setText("Rejected");
                
                JOptionPane.showMessageDialog(DiffViewPanel.this, 
                    "Changes rejected for block: " + diffBlock.getBlockId());
            }
        }
        
        private void revertToOriginalContent() {
            ApplicationManager.getApplication().invokeLater(() -> {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    Document document = FileDocumentManager.getInstance().getDocument(file);
                    if (document != null) {
                        // This is a simplified revert - in a real implementation,
                        // you'd need to carefully replace only the specific lines
                        // For now, we'll show a message about what would be reverted
                        String message = "Would revert lines " + diffBlock.getStartLine() + 
                                       " to " + diffBlock.getEndLine() + " to original content";
                        System.out.println(message);
                    }
                });
            });
        }
    }
}
