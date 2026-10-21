# Copilot instructions

This is a Java 17 IntelliJ Platform plugin for detecting and previewing SVG data URIs. For the project map and workflow, read the repository-root [`instruction.md`](../instruction.md).

- Treat `build.gradle.kts` and `src/main/resources/META-INF/plugin.xml` as authoritative for build/platform settings and extension registration; other documentation may be stale.
- Follow existing architecture in `com.plugin.svg`. The active gutter path is `SvgIconLineMarkerProvider` → decode/render/cache → `SvgVirtualFile` → `SvgFileEditorProvider` / `SvgFileEditor`.
- `OpenSvgWithToolkitAction` provides the Project View context-menu action for selected `.svg` files. Keep visibility tied to an actual SVG selection; Project View selection access belongs on the EDT, and PSI resolution belongs in read actions.
- Preserve IntelliJ threading requirements: access PSI in read actions, update Swing on the EDT, and keep expensive rendering off the UI thread.
- Custom `FileEditor.getState()` implementations must return a non-null `FileEditorState`; IntelliJ requests editor state during tab selection and document-history updates.
- For every release, synchronize the version and release information in `CHANGELOG.md`, `README.md`, and `build.gradle.kts`, and review `src/main/resources/META-INF/plugin.xml`, updating its description to reflect changed or added user-facing features.
- Preserve support for text-based file types and the URI encodings currently handled. Add or update focused JUnit 4 tests for behavior changes.
- Run `./gradlew test` for targeted unit-test validation and `./gradlew build` for broader plugin/build validation.
- Keep changes focused; do not edit generated files under `build/`, `.gradle/`, or IDE sandbox output.
