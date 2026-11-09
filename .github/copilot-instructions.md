# Copilot instructions

This is a Java 17 IntelliJ Platform plugin for detecting and previewing SVG data URIs. For the project map and workflow, read the repository-root [`instruction.md`](../instruction.md).

- Treat `build.gradle.kts` and `src/main/resources/META-INF/plugin.xml` as authoritative for build/platform settings and extension registration; other documentation may be stale.
- Follow existing architecture in `com.plugin.svg`. The active gutter path is `SvgIconLineMarkerProvider` → decode/render/cache → `SvgVirtualFile` → `SvgFileEditorProvider` / `SvgFileEditor`.
- The other primary entry point is the SVG Toolkit tool window: `SvgProjectBrowserToolWindowFactory` creates `SvgProjectBrowserPanel`, which delegates project traversal to `ProjectSvgFileScanner` (standalone files) and `EmbeddedSvgScanner` / `SvgEmbeddedSearch` (data URIs). Browser previews also use `SvgVirtualFile` and the registered file-editor provider; source navigation opens the matching project file and line.
- `plugin.xml` is the authoritative map of active IntelliJ registrations. Older preview UI classes remain in the source tree, so check their registrations and call sites before changing or removing them.
- Keep new production classes in the feature package that owns their responsibility: `action`, `browser`, `data`, `editor`, `render`, or `settings` under `com.plugin.svg`. Do not add new classes directly to the root package; create another focused subpackage only when a distinct responsibility genuinely needs one.
- Keep classes cohesive and small: separate UI, parsing/data handling, project scanning, rendering, and caching when that makes responsibilities clearer. Reuse existing components and avoid duplicate helpers, speculative abstractions, and unrelated refactors.
- Apply SOLID principles pragmatically: give each class one clear responsibility; extend behavior without destabilizing existing flows; keep implementations substitutable for their abstractions; keep interfaces and dependencies focused; and depend on suitable abstractions where they improve testability or reduce coupling. Do not introduce interfaces, layers, or design patterns without a concrete need.
- Put tests in the matching package under `src/test/java/com/plugin/svg/` (for example, `browser` tests alongside browser code). Add focused JUnit 4 regression tests for new behavior and edge cases, and keep tests for moved classes aligned with the production package structure.
- `OpenSvgWithToolkitAction` provides the Project View context-menu action for selected `.svg` files. Keep visibility tied to an actual SVG selection; Project View selection access belongs on the EDT, and PSI resolution belongs in read actions.
- Preserve IntelliJ threading requirements: access PSI in read actions, update Swing on the EDT, and keep expensive rendering off the UI thread.
- Custom `FileEditor.getState()` implementations must return a non-null `FileEditorState`; IntelliJ requests editor state during tab selection and document-history updates.
- For every release, synchronize the version and release information in `CHANGELOG.md`, `README.md`, and `build.gradle.kts`, and review `src/main/resources/META-INF/plugin.xml`, updating its description to reflect changed or added user-facing features.
- After each code or user-facing behavior change, update the relevant documentation in the same change (such as README usage, `docs/roadmap.md`, `instruction.md`, or plugin feature descriptions). Do not defer these documentation updates.
- Preserve support for text-based file types and the URI encodings currently handled. Add or update focused JUnit 4 tests for behavior changes.
- Run `./gradlew test` for the full JUnit 4 suite, or select a test class/method with Gradle's `--tests` filter:
  - `./gradlew test --tests 'com.plugin.svg.data.SvgDataUriUtilTest'`
  - `./gradlew test --tests 'com.plugin.svg.data.SvgDataUriUtilTest.testDecodeStandardBase64'`
- Run `./gradlew build` for broader plugin/build validation and `./gradlew runIde` to launch a sandbox IDE.
- No dedicated lint or formatting task is configured in `build.gradle.kts`.
- Keep changes focused; do not edit generated files under `build/`, `.gradle/`, or IDE sandbox output.
