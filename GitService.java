package com.example.diffplugin.services;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitLineHandler;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public final class GitService {
    private static final Logger LOG = Logger.getInstance(GitService.class);
    
    private final Project project;
    
    public GitService(Project project) {
        this.project = project;
    }
    
    /**
     * Gets the content of a file from the last commit (HEAD)
     */
    public Optional<String> getLastCommitContent(VirtualFile file) {
        try {
            GitRepository repository = getRepository(file);
            if (repository == null) {
                LOG.warn("No git repository found for file: " + file.getPath());
                return Optional.empty();
            }
            
            String relativePath = repository.getRoot().toNioPath().relativize(file.toNioPath()).toString();
            if (relativePath == null) {
                LOG.warn("Could not get relative path for file: " + file.getPath());
                return Optional.empty();
            }
            
            GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.SHOW);
            handler.addParameters("HEAD:" + relativePath);
            handler.setSilent(true);
            
            String content = Git.getInstance().runCommand(handler).getOutputOrThrow();
            return Optional.of(content);
            
        } catch (Exception e) {
            LOG.warn("Failed to get last commit content for file: " + file.getPath(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Gets the current content of a file
     */
    public Optional<String> getCurrentContent(VirtualFile file) {
        try {
            byte[] content = file.contentsToByteArray();
            return Optional.of(new String(content, StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOG.warn("Failed to read current content of file: " + file.getPath(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Checks if a file is under git version control
     */
    public boolean isUnderGit(VirtualFile file) {
        return getRepository(file) != null;
    }
    
    private GitRepository getRepository(VirtualFile file) {
        GitRepositoryManager manager = GitRepositoryManager.getInstance(project);
        return manager.getRepositoryForFile(file);
    }
}
