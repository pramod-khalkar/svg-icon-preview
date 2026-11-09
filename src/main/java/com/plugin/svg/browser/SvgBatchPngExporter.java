package com.plugin.svg.browser;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.VirtualFile;
import com.plugin.svg.data.SvgDataUriUtil;
import com.plugin.svg.editor.SvgDimensions;
import com.plugin.svg.render.SvgRenderer;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class SvgBatchPngExporter {
    public static final int MAX_DIMENSION = 4096;

    private SvgBatchPngExporter() {
    }

    public static List<Integer> parseCustomSizes(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        Set<Integer> sizes = new TreeSet<>();
        for (String token : value.split(",", -1)) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException(
                        "Enter comma-separated pixel sizes, for example 256, 512.");
            }
            int size;
            try {
                size = Integer.parseInt(trimmed);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "Invalid pixel size: " + trimmed + ". Use comma-separated values, for example 256, 512.",
                        exception);
            }
            if (size < 1 || size > MAX_DIMENSION) {
                throw new IllegalArgumentException(
                        "Sizes must be between 1 and " + MAX_DIMENSION + " pixels.");
            }
            sizes.add(size);
        }
        return List.copyOf(sizes);
    }

    public static List<ExportJob> createPlan(List<VirtualFile> files, List<Integer> maxDimensions,
                                             String projectBasePath, Path outputDirectory) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Select at least one SVG file.");
        }
        if (maxDimensions == null || maxDimensions.isEmpty()) {
            throw new IllegalArgumentException("Choose at least one output size.");
        }
        if (outputDirectory == null) {
            throw new IllegalArgumentException("Choose an output directory.");
        }

        Set<Integer> dimensions = new TreeSet<>();
        for (Integer maxDimension : maxDimensions) {
            if (maxDimension == null || maxDimension < 1 || maxDimension > MAX_DIMENSION) {
                throw new IllegalArgumentException(
                        "Sizes must be between 1 and " + MAX_DIMENSION + " pixels.");
            }
            dimensions.add(maxDimension);
        }

        Path normalizedOutputDirectory = outputDirectory.toAbsolutePath().normalize();
        List<Path> relativePaths = new ArrayList<>();
        for (VirtualFile file : files) {
            if (file == null || !ProjectSvgFileScanner.isSvgFile(file)) {
                throw new IllegalArgumentException("Only standalone SVG files can be exported.");
            }
            relativePaths.add(getProjectRelativePath(file, projectBasePath));
        }

        Map<Integer, List<String>> outputNamesByDimension = new HashMap<>();
        for (Integer maxDimension : dimensions) {
            outputNamesByDimension.put(maxDimension, createOutputNames(relativePaths, maxDimension));
        }

        List<ExportJob> jobs = new ArrayList<>();
        for (int fileIndex = 0; fileIndex < files.size(); fileIndex++) {
            VirtualFile file = files.get(fileIndex);
            for (Integer maxDimension : dimensions) {
                String outputName = outputNamesByDimension.get(maxDimension).get(fileIndex);
                Path destination = normalizedOutputDirectory.resolve(outputName).normalize();
                if (!destination.startsWith(normalizedOutputDirectory)) {
                    throw new IllegalArgumentException("An output file is outside the selected directory.");
                }
                jobs.add(new ExportJob(file, maxDimension, destination));
            }
        }
        return List.copyOf(jobs);
    }

    static List<String> createOutputNames(List<Path> relativePaths, int maxDimension) {
        Map<String, Integer> baseNameCounts = new HashMap<>();
        for (Path relativePath : relativePaths) {
            String baseName = getBaseName(relativePath.getFileName().toString());
            baseNameCounts.merge(baseName.toLowerCase(Locale.ROOT), 1, Integer::sum);
        }

        Set<String> usedNames = new HashSet<>();
        List<String> outputNames = new ArrayList<>();
        for (Path relativePath : relativePaths) {
            String fileName = relativePath.getFileName().toString();
            String baseName = getBaseName(fileName);
            String nameKey = baseName.toLowerCase(Locale.ROOT);
            String outputStem = baseNameCounts.get(nameKey) > 1
                    ? getFlattenedRelativeStem(relativePath) : baseName;
            String sizeSuffix = "-" + maxDimension + "px.png";
            String outputName = outputStem + sizeSuffix;
            int duplicateIndex = 2;
            while (!usedNames.add(outputName.toLowerCase(Locale.ROOT))) {
                outputName = outputStem + "-" + duplicateIndex++ + sizeSuffix;
            }
            outputNames.add(outputName);
        }
        return List.copyOf(outputNames);
    }

    private static String getFlattenedRelativeStem(Path relativePath) {
        List<String> pathParts = new ArrayList<>();
        for (int index = 0; index < relativePath.getNameCount(); index++) {
            String part = relativePath.getName(index).toString();
            if (index == relativePath.getNameCount() - 1) {
                part = getBaseName(part);
            }
            pathParts.add(part);
        }
        return String.join("-", pathParts);
    }

    private static String getBaseName(String fileName) {
        int extensionStart = fileName.lastIndexOf('.');
        return extensionStart > 0 ? fileName.substring(0, extensionStart) : fileName;
    }

    public static List<Path> findExistingOutputs(List<ExportJob> jobs) {
        List<Path> existing = new ArrayList<>();
        for (ExportJob job : jobs) {
            if (Files.exists(job.getDestination())) {
                existing.add(job.getDestination());
            }
        }
        return existing;
    }

    public static ExportResult export(List<ExportJob> jobs,
                                      Map<Path, ExistingFilesPolicy> existingFilesPolicies,
                                      ProgressIndicator indicator) {
        int exported = 0;
        int skipped = 0;
        List<String> failures = new ArrayList<>();
        for (int index = 0; index < jobs.size(); index++) {
            indicator.checkCanceled();
            ExportJob job = jobs.get(index);
            indicator.setText2(job.getSource().getPath());
            indicator.setFraction((double) index / jobs.size());
            if (!job.getSource().isValid()) {
                failures.add(job.getSource().getName() + ": source file is no longer available.");
                continue;
            }
            ExistingFilesPolicy policy = existingFilesPolicies.getOrDefault(
                    job.getDestination(), ExistingFilesPolicy.SKIP);
            if (policy == ExistingFilesPolicy.SKIP && Files.exists(job.getDestination())) {
                skipped++;
                continue;
            }
            try {
                String rawSource = readSource(job.getSource());
                String svg = SvgDataUriUtil.decodeSvgContent(rawSource);
                if (svg == null) {
                    failures.add(failure(job, "Invalid or unsupported SVG content."));
                    continue;
                }
                renderAndWrite(svg, job.getMaxDimension(), job.getDestination(), policy);
                exported++;
            } catch (ProcessCanceledException exception) {
                throw exception;
            } catch (FileAlreadyExistsException exception) {
                skipped++;
            } catch (IOException | RuntimeException exception) {
                failures.add(failure(job, exception.getMessage() == null
                        ? exception.getClass().getSimpleName() : exception.getMessage()));
            }
        }
        indicator.setFraction(1.0);
        return new ExportResult(exported, skipped, failures);
    }

    private static Path getProjectRelativePath(VirtualFile file, String projectBasePath) {
        if (projectBasePath == null || projectBasePath.isBlank()) {
            return Path.of(file.getName());
        }
        String path = SvgProjectBrowserPanel.projectRelativePath(projectBasePath, file.getPath());
        Path relativePath;
        try {
            relativePath = Path.of(path).normalize();
        } catch (RuntimeException exception) {
            relativePath = Path.of(file.getName());
        }
        if (relativePath.isAbsolute() || relativePath.startsWith("..") || relativePath.getFileName() == null) {
            return Path.of(file.getName());
        }
        return relativePath;
    }

    private static String readSource(VirtualFile file) throws IOException {
        return ApplicationManager.getApplication().runReadAction(
                (ThrowableComputable<String, IOException>) () ->
                        new String(file.contentsToByteArray(), file.getCharset()));
    }

    static void renderAndWrite(String svg, int maxDimension, Path destination,
                               ExistingFilesPolicy existingFilesPolicy) throws IOException {
        Dimension dimensions = SvgDimensions.getRasterDimensions(svg, maxDimension);
        BufferedImage image = SvgRenderer.render(svg, dimensions.width, dimensions.height);
        if (image == null) {
            throw new IOException("SVG could not be rendered.");
        }
        writePng(image, destination, existingFilesPolicy);
    }

    static void writePng(BufferedImage image, Path destination,
                         ExistingFilesPolicy existingFilesPolicy) throws IOException {
        Path parent = destination.getParent();
        Files.createDirectories(parent);
        Path temporaryFile = Files.createTempFile(parent, ".svg-toolkit-", ".png");
        try {
            if (!ImageIO.write(image, "PNG", temporaryFile.toFile())) {
                throw new IOException("No PNG writer is available.");
            }
            if (existingFilesPolicy == ExistingFilesPolicy.REPLACE) {
                try {
                    Files.move(temporaryFile, destination,
                            StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException exception) {
                    Files.move(temporaryFile, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                Files.move(temporaryFile, destination);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static String failure(ExportJob job, String message) {
        return job.getSource().getName() + " (" + job.getMaxDimension() + "px): " + message;
    }

    public enum ExistingFilesPolicy {
        REPLACE,
        SKIP
    }

    public static final class ExportJob {
        private final VirtualFile source;
        private final int maxDimension;
        private final Path destination;

        private ExportJob(VirtualFile source, int maxDimension, Path destination) {
            this.source = source;
            this.maxDimension = maxDimension;
            this.destination = destination;
        }

        public VirtualFile getSource() {
            return source;
        }

        public int getMaxDimension() {
            return maxDimension;
        }

        public Path getDestination() {
            return destination;
        }
    }

    public static final class ExportResult {
        private final int exportedCount;
        private final int skippedCount;
        private final List<String> failures;

        private ExportResult(int exportedCount, int skippedCount, List<String> failures) {
            this.exportedCount = exportedCount;
            this.skippedCount = skippedCount;
            this.failures = List.copyOf(failures);
        }

        public int getExportedCount() {
            return exportedCount;
        }

        public int getSkippedCount() {
            return skippedCount;
        }

        public List<String> getFailures() {
            return failures;
        }
    }
}
