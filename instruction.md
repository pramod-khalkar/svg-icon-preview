# Repository instructions for Copilot

## Project overview

This repository contains an IntelliJ Platform plugin named **SVG Toolkit**. It detects SVG data URIs in text-based files, adds gutter previews, and lets users open a rendered preview in a synthetic editor tab. The implementation is Java, built with Gradle Kotlin DSL.

## Project facts

- Main package: `com.plugin.svg`
- Java source/target: 17
- IntelliJ Platform baseline: Community Edition `IC-2023.1`; plugin compatibility is configured in `build.gradle.kts`.
- Plugin descriptor used by the build: `src/main/resources/META-INF/plugin.xml`
- Main implementation: `src/main/java/com/plugin/svg/`
- Tests: `src/test/java/com/plugin/svg/` (JUnit 4)
- Keep `build.gradle.kts` and the plugin descriptor authoritative for versions and platform configuration. README and historical docs can lag behind the implementation.

## Architecture and important files

- `SvgIconLineMarkerProvider` scans leaf PSI elements for `data:image/svg+xml` URIs, avoids duplicate markers per line, decodes payloads, and opens the preview on gutter-icon click. It is registered through `plugin.xml`.
- `SvgDataUriUtil` handles payload normalization and standard/URL-safe Base64, URL-encoded, and plain SVG decoding.
- `SvgRenderer` rasterizes SVG to `BufferedImage` using Apache Batik.
- `SvgImageCache` is the bounded LRU cache used for rendered gutter images.
- `SvgDynamicIcon` displays a placeholder and can update the gutter icon after background rendering.
- `SvgVirtualFile` and `SvgVirtualFileSystem` represent an in-memory preview file.
- `SvgFileEditorProvider` accepts the synthetic `svg-preview-` files and creates `SvgFileEditor`. `SvgFileEditor` displays source and preview panes and provides PNG copy/export actions.
- `SvgToolkitSettings`, `SvgToolkitSettingsConfigurable`, and `SvgToolkitSettingsPanel` implement the configurable inline-size threshold.
- `SvgPreviewEditorTab`, `SvgPreviewDialog`, and `SvgPreviewToolWindow` are additional/older UI implementations. Trace registrations and call sites before changing or removing them; the registered editor-provider flow currently creates `SvgFileEditor`.

## Development workflow

Run commands from the repository root using the Gradle wrapper:

```bash
./gradlew test
./gradlew build
./gradlew runIde
```

Use `./gradlew test` for changes to logic covered by unit tests; use `./gradlew build` when validating plugin packaging or configuration. `runIde` launches a sandbox IDE and is not needed for ordinary unit-test changes.

## Change guidance

- Preserve IntelliJ Platform threading rules: PSI/document access belongs in read actions; Swing UI updates belong on the event dispatch thread; expensive SVG decoding/rendering should not block the UI.
- Keep SVG parsing, decoding, rendering, caching, and UI responsibilities in their existing classes unless a change clearly warrants a small shared abstraction.
- Preserve behavior for non-Java text files and for supported URI encodings. Add focused tests for decoder or detection changes, including malformed and boundary inputs where relevant.
- Keep cache behavior bounded and avoid retaining project/PSI objects in long-lived static state.
- Follow nearby Java style and existing IntelliJ Platform APIs. Update `plugin.xml` when adding/removing platform extensions or services.
- Do not assume README feature descriptions, file names, roadmap entries, or changelog entries exactly match current code. Verify behavior from active registrations and call sites.
- Make focused changes; do not modify generated Gradle output, `.gradle/`, `build/`, or IDE sandbox files as source.
- When user-facing behavior or configuration changes, update the directly related documentation and tests.

