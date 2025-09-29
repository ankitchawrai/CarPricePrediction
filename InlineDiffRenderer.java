package com.example.diffplugin.ui;

import com.example.diffplugin.model.DiffBlock;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Arrays;

public class InlineDiffRenderer extends GutterIconRenderer {
    private final Project project;
    private final Editor editor;
    private final VirtualFile file;
    private final DiffBlock diffBlock;
    private final Icon diffIcon;
    
    public InlineDiffRenderer(Project project, Editor editor, VirtualFile file, DiffBlock diffBlock) {
        this.project = project;
        this.editor = editor;
        this.file = file;
        this.diffBlock = diffBlock;
        this.diffIcon = createDiffIcon();
    }
    
    @Override
    public @NotNull Icon getIcon() {
        return diffIcon;
    }
    
    @Override
    public boolean isNavigateAction() {
        return true;
    }
    
    @Override
    public @Nullable String getTooltipText() {
        String typeText = getTypeDescription();
        return String.format("%s - Click to accept/reject changes", typeText);
    }
    
    @Override
    public @Nullable AnAction getClickAction() {
        return new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                showDiffPopup(e);
            }
        };
    }
    
    private void showDiffPopup(AnActionEvent e) {
        JPanel popupContent = createPopupContent();
        
        JBPopupFactory.getInstance()
            .createComponentPopupBuilder(popupContent, null)
            .setTitle(getTypeDescription())
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .createPopup()
            .show(new RelativePoint((MouseEvent) e.getInputEvent()));
    }
    
    private JPanel createPopupContent() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        panel.setPreferredSize(new Dimension(500, 300));
        
        // Header with diff type and line info
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel headerLabel = new JLabel(getTypeDescription());
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
        headerLabel.setForeground(getTypeColor());
        headerPanel.add(headerLabel);
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Content panel showing old vs new
        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        
        if (diffBlock.isDeletion() || diffBlock.isModification()) {
            JPanel oldPanel = createContentPanel("Original (will be removed):", 
                diffBlock.getOldContent(), new Color(255, 240, 240));
            contentPanel.add(oldPanel);
        }
        
        if (diffBlock.isAddition() || diffBlock.isModification()) {
            JPanel newPanel = createContentPanel("New (current):", 
                diffBlock.getNewContent(), new Color(240, 255, 240));
            contentPanel.add(newPanel);
        }
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton acceptButton = new JButton("Accept Changes");
        acceptButton.setBackground(new Color(76, 175, 80));
        acceptButton.setForeground(Color.WHITE);
        acceptButton.addActionListener(e -> {
            acceptChanges();
            closePopup();
        });
        
        JButton rejectButton = new JButton("Reject Changes");
        rejectButton.setBackground(new Color(244, 67, 54));
        rejectButton.setForeground(Color.WHITE);
        rejectButton.addActionListener(e -> {
            rejectChanges();
            closePopup();
        });
        
        buttonPanel.add(acceptButton);
        buttonPanel.add(rejectButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createContentPanel(String title, java.util.List<String> lines, Color backgroundColor) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(backgroundColor.darker(), 2),
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
        textArea.setBackground(backgroundColor);
        textArea.setLineWrap(false);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(200, 150));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void acceptChanges() {
        // Current content is already in place, so we just need to mark as accepted
        ApplicationManager.getApplication().invokeLater(() -> {
            // Remove the diff highlight since changes are accepted
            removeDiffHighlight();
            showNotification("Changes accepted for " + getTypeDescription().toLowerCase());
        });
    }
    
    private void rejectChanges() {
        ApplicationManager.getApplication().invokeLater(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                try {
                    Document document = editor.getDocument();
                    
                    // Calculate line offsets
                    int startOffset = document.getLineStartOffset(diffBlock.getStartLine());
                    int endOffset = diffBlock.getEndLine() < document.getLineCount() - 1 
                        ? document.getLineStartOffset(diffBlock.getEndLine() + 1)
                        : document.getTextLength();
                    
                    // Replace with original content
                    StringBuilder originalContent = new StringBuilder();
                    for (String line : diffBlock.getOldContent()) {
                        originalContent.append(line).append("\n");
                    }
                    
                    document.replaceString(startOffset, endOffset, originalContent.toString());
                    
                    // Remove the diff highlight
                    removeDiffHighlight();
                    showNotification("Changes rejected for " + getTypeDescription().toLowerCase());
                    
                } catch (Exception ex) {
                    showNotification("Failed to reject changes: " + ex.getMessage());
                }
            });
        });
    }
    
    private void removeDiffHighlight() {
        // This would be implemented to remove the specific highlighter
        // For now, we'll trigger a refresh of the entire diff view
        project.getService(com.example.diffplugin.services.InlineDiffService.class)
            .updateInlineDiff(editor, file);
    }
    
    private void closePopup() {
        // The popup will close automatically when buttons are clicked
    }
    
    private void showNotification(String message) {
        com.intellij.notification.Notifications.Bus.notify(
            new com.intellij.notification.Notification(
                "DiffPlugin",
                "Diff Plugin",
                message,
                com.intellij.notification.NotificationType.INFORMATION
            ),
            project
        );
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
            case ADDED: return new Color(76, 175, 80);
            case DELETED: return new Color(244, 67, 54);
            case MODIFIED: return new Color(255, 152, 0);
            default: return Color.BLACK;
        }
    }
    
    private Icon createDiffIcon() {
        // Create a simple colored square icon based on diff type
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                g2.setColor(getTypeColor());
                g2.fillRoundRect(x + 2, y + 2, getIconWidth() - 4, getIconHeight() - 4, 4, 4);
                
                g2.setColor(Color.WHITE);
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 8));
                String symbol = getTypeSymbol();
                FontMetrics fm = g2.getFontMetrics();
                int symbolX = x + (getIconWidth() - fm.stringWidth(symbol)) / 2;
                int symbolY = y + (getIconHeight() + fm.getAscent()) / 2 - 1;
                g2.drawString(symbol, symbolX, symbolY);
                
                g2.dispose();
            }
            
            @Override
            public int getIconWidth() {
                return 16;
            }
            
            @Override
            public int getIconHeight() {
                return 16;
            }
        };
    }
    
    private String getTypeSymbol() {
        switch (diffBlock.getType()) {
            case ADDED: return "+";
            case DELETED: return "-";
            case MODIFIED: return "~";
            default: return "?";
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        InlineDiffRenderer that = (InlineDiffRenderer) obj;
        return diffBlock.getBlockId().equals(that.diffBlock.getBlockId());
    }
    
    @Override
    public int hashCode() {
        return diffBlock.getBlockId().hashCode();
    }
}
