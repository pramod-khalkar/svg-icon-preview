package com.plugin.svg.browser;

import com.intellij.testFramework.LightVirtualFile;
import com.plugin.svg.data.SvgDataUriUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SvgBatchPngExporterTest {
    private static final String RECTANGULAR_SVG =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"40\" height=\"20\" viewBox=\"0 0 40 20\">"
                    + "<rect width=\"40\" height=\"20\" fill=\"#18A999\"/></svg>";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void parsesAndDeduplicatesCustomSizes() {
        assertEquals(List.of(16, 24, 128),
                SvgBatchPngExporter.parseCustomSizes("24, 16, 24, 128"));
        assertTrue(SvgBatchPngExporter.parseCustomSizes("  ").isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidCustomSize() {
        SvgBatchPngExporter.parseCustomSizes("24, invalid");
    }

    @Test
    public void explainsCommaSeparatedFormatWhenValuesAreNotSeparated() {
        try {
            SvgBatchPngExporter.parseCustomSizes("256 512");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().contains("comma-separated"));
            return;
        }
        fail("Expected values without a comma to be rejected.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsSizesOutsideSupportedRange() {
        SvgBatchPngExporter.parseCustomSizes("4097");
    }

    @Test
    public void createsOneOutputPerDistinctSize() throws Exception {
        Path outputDirectory = temporaryFolder.newFolder("planned-output").toPath();
        List<SvgBatchPngExporter.ExportJob> jobs = SvgBatchPngExporter.createPlan(
                List.of(new LightVirtualFile("icon.svg", RECTANGULAR_SVG)),
                List.of(24, 64, 24), null, outputDirectory);

        assertEquals(2, jobs.size());
        assertEquals("icon-24px.png", jobs.get(0).getDestination().getFileName().toString());
        assertEquals("icon-64px.png", jobs.get(1).getDestination().getFileName().toString());
        assertEquals(outputDirectory.toAbsolutePath().normalize(),
                jobs.get(0).getDestination().getParent());
    }

    @Test
    public void disambiguatesSameNamedFilesFromDifferentFolders() {
        List<String> names = SvgBatchPngExporter.createOutputNames(
                List.of(Path.of("toolbar", "search.svg"), Path.of("navigation", "search.svg")), 64);

        assertEquals(List.of("toolbar-search-64px.png", "navigation-search-64px.png"), names);
    }

    @Test
    public void addsNumericSuffixWhenFlattenedPathsStillCollide() {
        List<String> names = SvgBatchPngExporter.createOutputNames(
                List.of(Path.of("a-b", "x", "icon.svg"), Path.of("a", "b-x", "icon.svg")), 64);

        assertEquals(List.of("a-b-x-icon-64px.png", "a-b-x-icon-2-64px.png"), names);
    }

    @Test
    public void plansEmbeddedMatchesDirectlyInOutputDirectoryWithStableNames() throws Exception {
        String firstUri = SvgDataUriUtil.encodeBase64DataUri("<svg><path/></svg>");
        String secondUri = SvgDataUriUtil.encodeBase64DataUri("<svg><circle/></svg>");
        List<SvgEmbeddedSearch.Match> matches = SvgEmbeddedSearch.findMatches(
                "/project/src/icons.js", firstUri + " " + secondUri);
        Path outputDirectory = temporaryFolder.newFolder("embedded-output").toPath();

        List<SvgBatchPngExporter.ExportJob> jobs = SvgBatchPngExporter.createEmbeddedPlan(
                matches.subList(1, 2), List.of(64), "/project", outputDirectory);

        assertEquals(1, jobs.size());
        assertEquals("icons-line1-match2-64px.png",
                jobs.get(0).getDestination().getFileName().toString());
        assertEquals(outputDirectory.toAbsolutePath().normalize(),
                jobs.get(0).getDestination().getParent());
    }

    @Test
    public void rendersPngAtMaximumDimensionWithoutDistortingAspectRatio() throws Exception {
        Path output = temporaryFolder.getRoot().toPath().resolve("icon-64px.png");

        SvgBatchPngExporter.renderAndWrite(RECTANGULAR_SVG, 64, output,
                SvgBatchPngExporter.ExistingFilesPolicy.SKIP);

        BufferedImage image = ImageIO.read(output.toFile());
        assertEquals(64, image.getWidth());
        assertEquals(32, image.getHeight());
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void doesNotOverwriteExistingPngWhenSkipPolicyIsSelected() throws Exception {
        Path output = temporaryFolder.getRoot().toPath().resolve("existing.png");
        Files.writeString(output, "keep this file");

        SvgBatchPngExporter.writePng(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB),
                output, SvgBatchPngExporter.ExistingFilesPolicy.SKIP);
    }

    @Test
    public void replacesExistingPngOnlyWhenReplacePolicyIsSelected() throws Exception {
        Path output = temporaryFolder.getRoot().toPath().resolve("existing.png");
        Files.writeString(output, "replace this file");

        SvgBatchPngExporter.writePng(new BufferedImage(4, 2, BufferedImage.TYPE_INT_ARGB),
                output, SvgBatchPngExporter.ExistingFilesPolicy.REPLACE);

        BufferedImage image = ImageIO.read(output.toFile());
        assertEquals(4, image.getWidth());
        assertEquals(2, image.getHeight());
    }
}
