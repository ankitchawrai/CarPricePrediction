package com.example.diffplugin.services;

import com.example.diffplugin.model.DiffBlock;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.diff.comparison.ComparisonManager;
import com.intellij.diff.comparison.ComparisonPolicy;
import com.intellij.diff.fragments.LineFragment;
import com.intellij.openapi.progress.DumbProgressIndicator;
import com.intellij.openapi.util.text.StringUtil;

import java.util.*;

@Service
public final class DiffCalculationService {
    private static final Logger LOG = Logger.getInstance(DiffCalculationService.class);
    
    /**
     * Calculates diff blocks between old and new content
     */
    public List<DiffBlock> calculateDiff(String oldContent, String newContent) {
        if (oldContent == null || newContent == null) {
            return Collections.emptyList();
        }
        
        try {
            List<String> oldLines = StringUtil.split(oldContent, "\n");
            List<String> newLines = StringUtil.split(newContent, "\n");
            
            ComparisonManager comparisonManager = ComparisonManager.getInstance();
            List<LineFragment> fragments = comparisonManager.compareLines(
                oldContent, newContent, ComparisonPolicy.DEFAULT, DumbProgressIndicator.INSTANCE
            );
            
            List<DiffBlock> diffBlocks = new ArrayList<>();
            int blockCounter = 0;
            
            for (LineFragment fragment : fragments) {
                DiffBlock.Type type = determineDiffType(fragment);
                int startLine = fragment.getStartLine2(); // Line in new content
                int endLine = fragment.getEndLine2();
                
                List<String> oldFragmentLines = getLines(oldLines, fragment.getStartLine1(), fragment.getEndLine1());
                List<String> newFragmentLines = getLines(newLines, fragment.getStartLine2(), fragment.getEndLine2());
                
                String blockId = "diff_block_" + (++blockCounter);
                
                DiffBlock diffBlock = new DiffBlock(
                    type, startLine, endLine, 
                    oldFragmentLines, newFragmentLines, blockId
                );
                
                diffBlocks.add(diffBlock);
            }
            
            return diffBlocks;
            
        } catch (Exception e) {
            LOG.error("Failed to calculate diff", e);
            return Collections.emptyList();
        }
    }
    
    private DiffBlock.Type determineDiffType(LineFragment fragment) {
        boolean hasOldContent = fragment.getStartLine1() != fragment.getEndLine1();
        boolean hasNewContent = fragment.getStartLine2() != fragment.getEndLine2();
        
        if (hasOldContent && hasNewContent) {
            return DiffBlock.Type.MODIFIED;
        } else if (hasNewContent) {
            return DiffBlock.Type.ADDED;
        } else {
            return DiffBlock.Type.DELETED;
        }
    }
    
    private List<String> getLines(List<String> allLines, int startLine, int endLine) {
        if (startLine >= allLines.size() || startLine < 0) {
            return Collections.emptyList();
        }
        
        int actualEndLine = Math.min(endLine, allLines.size());
        return allLines.subList(startLine, actualEndLine);
    }
}
