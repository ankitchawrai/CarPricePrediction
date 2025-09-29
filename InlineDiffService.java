package com.example.diffplugin.services;

import com.example.diffplugin.model.DiffBlock;
import com.example.diffplugin.ui.InlineDiffRenderer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public final class InlineDiffService {
    private static final Logger LOG = Logger.getInstance(InlineDiffService.class);
    private final Project project;
    private final Map<Editor, List<RangeHighlighter>> editorHighlighters = new ConcurrentHashMap<>();
    
    public InlineDiffService(Project project) {
        this.project = project;
    }
    
    public void updateInlineDiff(Editor editor, VirtualFile file) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                clearExistingHighlighters(editor);
                
                GitService gitService = project.getService(GitService.class);
                if (!gitService.isUnderGit(file)) {
                    return;
                }
                
                Optional<String> lastCommitContent = gitService.getLastCommitContent(file);
                Optional<String> currentContent = gitService.getCurrentContent(file);
                
                if (lastCommitContent.isEmpty() || currentContent.isEmpty()) {
                    return;
                }
                
                // Skip if contents are identical
                if (lastCommitContent.get().equals(currentContent.get())) {
                    return;
                }
                
                DiffCalculationService diffService = project.getService(DiffCalculationService.class);
                List<DiffBlock> diffBlocks = diffService.calculateDiff(
                    lastCommitContent.get(), 
                    currentContent.get()
                );
                
                if (!diffBlocks.isEmpty()) {
                    renderInlineDiffs(editor, file, diffBlocks);
                }
                
            } catch (Exception e) {
                LOG.warn("Failed to update inline diff for file: " + file.getPath(), e);
            }
        });
    }
    
    private void clearExistingHighlighters(Editor editor) {
        List<RangeHighlighter> highlighters = editorHighlighters.get(editor);
        if (highlighters != null) {
            MarkupModel markupModel = editor.getMarkupModel();
            for (RangeHighlighter highlighter : highlighters) {
                markupModel.removeHighlighter(highlighter);
            }
            highlighters.clear();
        }
    }
    
    private void renderInlineDiffs(Editor editor, VirtualFile file, List<DiffBlock> diffBlocks) {
        List<RangeHighlighter> newHighlighters = new ArrayList<>();
        MarkupModel markupModel = editor.getMarkupModel();
        
        for (DiffBlock diffBlock : diffBlocks) {
            try {
                InlineDiffRenderer renderer = new InlineDiffRenderer(project, editor, file, diffBlock);
                
                // Calculate the line range for this diff block
                int startOffset = getOffsetForLine(editor, diffBlock.getStartLine());
                int endOffset = getOffsetForLine(editor, diffBlock.getEndLine() + 1);
                
                if (startOffset >= 0 && endOffset >= startOffset) {
                    RangeHighlighter highlighter = markupModel.addRangeHighlighter(
                        startOffset,
                        endOffset,
                        HighlighterLayer.LAST + 1,
                        null,
                        com.intellij.openapi.editor.markup.HighlighterTargetArea.LINES_IN_RANGE
                    );
                    
                    highlighter.setGutterIconRenderer(renderer);
                    newHighlighters.add(highlighter);
                }
                
            } catch (Exception e) {
                LOG.warn("Failed to render diff block: " + diffBlock.getBlockId(), e);
            }
        }
        
        editorHighlighters.put(editor, newHighlighters);
    }
    
    private int getOffsetForLine(Editor editor, int line) {
        try {
            if (line < 0 || line >= editor.getDocument().getLineCount()) {
                return -1;
            }
            return editor.getDocument().getLineStartOffset(line);
        } catch (Exception e) {
            return -1;
        }
    }
    
    public void clearAllDiffs(Editor editor) {
        clearExistingHighlighters(editor);
        editorHighlighters.remove(editor);
    }
}
