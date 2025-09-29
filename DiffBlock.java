package com.example.diffplugin.model;

import java.util.List;

/**
 * Represents a block of differences between two versions of a file
 */
public class DiffBlock {
    public enum Type {
        ADDED,    // New lines (green)
        DELETED,  // Removed lines (red)
        MODIFIED  // Changed lines (combination of red/green)
    }
    
    private final Type type;
    private final int startLine;
    private final int endLine;
    private final List<String> oldContent;
    private final List<String> newContent;
    private final String blockId;
    
    public DiffBlock(Type type, int startLine, int endLine, 
                     List<String> oldContent, List<String> newContent, String blockId) {
        this.type = type;
        this.startLine = startLine;
        this.endLine = endLine;
        this.oldContent = oldContent;
        this.newContent = newContent;
        this.blockId = blockId;
    }
    
    public Type getType() {
        return type;
    }
    
    public int getStartLine() {
        return startLine;
    }
    
    public int getEndLine() {
        return endLine;
    }
    
    public List<String> getOldContent() {
        return oldContent;
    }
    
    public List<String> getNewContent() {
        return newContent;
    }
    
    public String getBlockId() {
        return blockId;
    }
    
    public boolean isAddition() {
        return type == Type.ADDED;
    }
    
    public boolean isDeletion() {
        return type == Type.DELETED;
    }
    
    public boolean isModification() {
        return type == Type.MODIFIED;
    }
}
