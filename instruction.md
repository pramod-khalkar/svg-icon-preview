# Repository instructions for Copilot

## Project overview

This repository contains an IntelliJ Platform plugin named **SVG Toolkit**. It detects SVG data URIs in text-based files, adds gutter previews, provides an editor with live SVG source/preview and save/export actions, and includes a tool window for searching standalone project SVG files. The implementation is Java, built with Gradle Kotlin DSL.

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
- `SvgFileEditorProvider` accepts the synthetic `svg-preview-` files and creates `SvgFileEditor`. `SvgFileEditor` displays source and preview panes and groups save/export/copy actions in the preview toolbar's File menu.
- `SvgVirtualFile` carries the original standalone source file and its original content for Save and external-change confirmation. Data-URI previews do not have a source file and only offer Save As/export/copy actions.
- `SvgProjectBrowserToolWindowFactory` registers the **SVG Toolkit** tool window. `SvgProjectBrowserPanel` searches standalone `.svg` files in project content, filters names/paths, shows thumbnails, dimensions, size, and project-relative paths, and opens selected files through `OpenSvgWithToolkitAction.openPreview`.
- `OpenSvgWithToolkitAction` adds the Project View context-menu action for selected `.svg` files. Keep its update logic limited to actual SVG selections, use the EDT for Project View selection access, and use read actions when resolving PSI.
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

## Release workflow

For every release, keep release information synchronized across all user-facing and build metadata:

- Update `CHANGELOG.md` with the released version and its notable changes.
- Update the version references and release notes in `README.md`.
- Update the plugin version and change notes in `build.gradle.kts`.
- Review `src/main/resources/META-INF/plugin.xml` for every release and update its description to reflect any changed or added user-facing features; keep it accurate if no description change is needed.

## Change guidance

- Preserve IntelliJ Platform threading rules: PSI/document access belongs in read actions; Swing UI updates belong on the event dispatch thread; expensive SVG decoding/rendering should not block the UI.
- `FileEditor.getState()` must return a non-null `FileEditorState`; IntelliJ may request it during tab selection and document-history updates.
- Keep SVG parsing, decoding, rendering, caching, and UI responsibilities in their existing classes unless a change clearly warrants a small shared abstraction.
- Preserve behavior for non-Java text files and for supported URI encodings. Add focused tests for decoder or detection changes, including malformed and boundary inputs where relevant.
- Keep cache behavior bounded and avoid retaining project/PSI objects in long-lived static state.
- Distinguish released functionality from current working-tree features in user-facing documentation. Update `CHANGELOG.md` only when preparing an actual release; do not describe unreleased work as part of the current published version.
- After every code or user-facing behavior change, update the directly relevant documentation in the same change (for example README usage, roadmap status, plugin description, and release notes when preparing a release). Do not leave documentation updates for a later task.
- Follow nearby Java style and existing IntelliJ Platform APIs. Update `plugin.xml` when adding/removing platform extensions or services.
- Do not assume README feature descriptions, file names, roadmap entries, or changelog entries exactly match current code. Verify behavior from active registrations and call sites.
- Make focused changes; do not modify generated Gradle output, `.gradle/`, `build/`, or IDE sandbox files as source.
- When user-facing behavior or configuration changes, update the directly related documentation and tests.
