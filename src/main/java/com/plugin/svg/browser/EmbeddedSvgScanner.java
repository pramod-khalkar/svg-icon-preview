package com.plugin.svg.browser;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.plugin.svg.data.SvgDataUriUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class EmbeddedSvgScanner {
    private EmbeddedSvgScanner() {
    }

    static Result scan(Project project, ProgressIndicator indicator, AtomicLong generation, long expectedGeneration,
                       Consumer<String> progressUpdater, BiConsumer<String, IOException> unreadableFileLogger) {
        List<VirtualFile> files = ReadAction.compute(() -> {
            List<VirtualFile> contentFiles = new ArrayList<>();
            ProjectFileIndex.getInstance(project).iterateContent(file -> {
                checkCurrent(indicator, generation, expectedGeneration);
                if (!file.isDirectory() && !file.getFileType().isBinary()) {
                    contentFiles.add(file);
                }
                return true;
            });
            return contentFiles;
        });
        List<SvgEmbeddedSearch.Match> matches = new ArrayList<>();
        int unreadableFiles = 0;
        for (VirtualFile file : files) {
            checkCurrent(indicator, generation, expectedGeneration);
            if (!file.isValid()) {
                continue;
            }
            progressUpdater.accept(file.getPath());
            try {
                String source = ReadAction.compute(() ->
                        new String(file.contentsToByteArray(), file.getCharset()));
                if (source.toLowerCase(Locale.ROOT).contains("data:image/svg")) {
                    for (SvgEmbeddedSearch.Match match : SvgEmbeddedSearch.findMatches(file.getPath(), source)) {
                        matches.add(match.withSourceFile(file));
                    }
                }
            } catch (IOException exception) {
                unreadableFiles++;
                unreadableFileLogger.accept(file.getPath(), exception);
            }
        }
        matches.sort(Comparator.comparing(SvgEmbeddedSearch.Match::getFilePath,
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(SvgEmbeddedSearch.Match::getLineNumber));
        return new Result(matches, unreadableFiles);
    }

    private static void checkCurrent(ProgressIndicator indicator, AtomicLong generation, long expectedGeneration) {
        indicator.checkCanceled();
        if (generation.get() != expectedGeneration) {
            indicator.cancel();
            indicator.checkCanceled();
        }
    }

    static final class Result {
        private final List<SvgEmbeddedSearch.Match> matches;
        private final int unreadableFiles;

        private Result(List<SvgEmbeddedSearch.Match> matches, int unreadableFiles) {
            this.matches = matches;
            this.unreadableFiles = unreadableFiles;
        }

        List<SvgEmbeddedSearch.Match> getMatches() {
            return matches;
        }

        int getUnreadableFiles() {
            return unreadableFiles;
        }
    }
}
