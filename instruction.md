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

- `action/SvgIconLineMarkerProvider` scans leaf PSI elements for `data:image/svg+xml` URIs, avoids duplicate markers per line, decodes payloads, and opens the preview on gutter-icon click. It is registered through `plugin.xml`.
- `data/SvgDataUriUtil` handles payload normalization and standard/URL-safe Base64, URL-encoded, and plain SVG decoding; `data/SvgXmlFormatter` formats SVG XML.
- `render/SvgRenderer` rasterizes SVG to `BufferedImage` using Apache Batik. `render/SvgImageCache` is the bounded LRU cache used for rendered gutter images, and `render/SvgDynamicIcon` updates gutter icons after background rendering.
- `render/SvgDiagnostics` analyzes SVG XML without resolving external resources; the active editor shows parse/render errors and non-blocking sizing/resource warnings in its diagnostics panel.
- `editor/SvgVirtualFile` and `editor/SvgVirtualFileSystem` represent an in-memory preview file. `editor/SvgFileEditorProvider` accepts synthetic preview files and creates `SvgFileEditor`, which displays source and preview panes, dimensions, and save/export/copy actions.
- `SvgVirtualFile` carries the original standalone source file and its original content for Save and external-change confirmation. Data-URI previews do not have a source file and only offer Save As/export/copy actions. `editor/SvgDimensions` parses and formats SVG dimensions for the editor.
- `browser/SvgProjectBrowserToolWindowFactory` registers the **SVG Toolkit** tool window. `browser/SvgProjectBrowserPanel` provides tabs for standalone `.svg` browsing and SVG data URI search, with asynchronously rendered/cached thumbnails. `browser/ProjectSvgFileScanner` and `browser/EmbeddedSvgScanner` isolate project traversal; the latter uses `data/SvgEmbeddedSearch` to find valid SVG URIs in non-binary files under project content, then supports opening the SVG preview or navigating to its source location. `browser/SvgThumbnailCache` bounds thumbnail and dimension metadata. `browser/SvgBatchPngExporter` exports selected standalone SVGs and embedded data URI matches directly into the chosen directory at selected maximum-edge sizes, preserves aspect ratio, disambiguates names, and supports per-file or batch-wide conflict choices.
- `action/OpenSvgWithToolkitAction` adds the Project View context-menu action for selected `.svg` files. Keep its update logic limited to actual SVG selections, use the EDT for Project View selection access, and use read actions when resolving PSI.
- `settings/SvgToolkitSettings`, `settings/SvgToolkitSettingsConfigurable`, and `settings/SvgToolkitSettingsPanel` implement the configurable inline-size threshold.
- `settings/SvgToolkitWhatsNewActivity` checks the installed plugin version on project startup and shows a one-time What's New notification; `SvgToolkitWhatsNewDialog` displays its release highlights. The last-shown version is persisted with `SvgToolkitSettings`; update the dialog content when preparing release notes for a new version.
- `editor/SvgPreviewEditorTab`, `editor/SvgPreviewDialog`, and `editor/SvgPreviewToolWindow` are additional/older UI implementations. Trace registrations and call sites before changing or removing them; the registered editor-provider flow currently creates `SvgFileEditor`.

## Structure for new code

- Place production classes in the existing feature package that owns the responsibility: `action`, `browser`, `data`, `editor`, `render`, or `settings` under `com.plugin.svg`. Do not add new classes directly to the root `com.plugin.svg` package. Add a new subpackage only for a clearly distinct responsibility that does not fit the existing boundaries.
- Keep responsibilities cohesive. Separate UI, project scanning, SVG data handling, rendering, and caching when doing so makes ownership clearer; reuse existing implementations rather than duplicating logic. Prefer a small focused class over growing an unrelated class, but avoid speculative abstractions or splitting code without a concrete benefit.
- Apply SOLID principles pragmatically: each class should have one clear responsibility; additions should extend behavior without destabilizing existing flows; implementations should remain substitutable for their abstractions; interfaces and dependencies should stay focused; and abstractions should be introduced when they improve testability or reduce coupling. Do not add interfaces, layers, or patterns without a concrete need.
- Place tests under the matching package in `src/test/java/com/plugin/svg/` (for example, `browser` production code has tests in `browser`). Keep test package structure aligned when moving production classes.
- Before adding an extension, service, or configurable, check existing implementations and registrations; update `plugin.xml` and tests as appropriate. Preserve the established IntelliJ threading and lifecycle rules for any new code.

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
