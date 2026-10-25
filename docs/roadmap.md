# SVG Toolkit Roadmap

This roadmap reflects the shipped features documented in `CHANGELOG.md` through
version 1.1.4, plus the next planned feature implemented in the current
work-in-progress. Future versions are tentative priorities, not release
commitments; adjust them based on user feedback.

## Completed

### 1.0.0 — SVG data URI previews
- Detect SVG data URIs in code and show gutter previews.
- Open a larger preview from the gutter.

### 1.1.0 — Editor preview and reliability
- Open previews in editor tabs with formatted SVG source and rendered preview.
- Add zoom controls, background rendering, caching, configurable size limits,
  robust decoding, and improved error reporting.

### 1.1.2 — Text-file coverage
- Detect SVG data URIs across supported text-based file types.

### 1.1.3 — Standalone SVG editing and export
- Open standalone `.svg` files in SVG Toolkit from the Project view.
- Edit formatted source with a live preview; edits remain local to the preview.
- Switch between split, source-only, and preview-only layouts.
- Copy the edited SVG as a data URI, copy the preview as PNG, or export it to PNG.

### 1.1.4 — Stability and preview details
- Improve Project View selection handling and editor-state reliability.
- Correct displayed SVG dimensions and decoded source-size units.

## Implemented, pending release

### 1.2.0 — Save edited SVGs
- Save edits back to the opened standalone SVG, with a confirmation if the
  source changed since the preview was opened.
- Save edited source as a new `.svg` file.
- Save, export, and copy actions are grouped in the preview toolbar's File menu.
- Data-URI previews offer Save As actions and never overwrite the source code.

## Implemented, pending release

### 1.3.0 — Project SVG browser
- Search standalone project `.svg` files by filename or folder.
- Show thumbnails and file size, with refresh to rescan project content.
- Open a selected asset in the existing SVG Toolkit editor.

## Proposed future releases

### 1.3.1 — Batch raster export
Support workflows that need raster assets at multiple sizes.
- Export selected SVG files as PNG at one or more chosen dimensions.
- Let users choose an output directory and avoid silently overwriting files.

**Acceptance:** Each selected SVG produces the expected PNG sizes and existing
files are handled with an explicit user choice.

### 1.4.0 — SVG diagnostics
Help users understand why an SVG may render or behave unexpectedly.
- Report actionable parse/render errors in the preview.
- Flag selected compatibility concerns, such as missing dimensions or
  externally referenced resources, without treating every valid SVG as invalid.

**Acceptance:** Diagnostics identify the relevant issue, avoid blocking valid
SVGs, and do not make external network requests to inspect resources.

### 1.5.0 — Compare and theme preview
Make visual review of assets easier.
- Compare two SVGs or before/after versions side by side or with an overlay.
- Preview an SVG against common light, dark, and custom background colors.

**Acceptance:** Users can switch comparison inputs and backgrounds without
modifying either SVG.

## Ideas to validate before scheduling

- Click an element in the rendered SVG to locate and inspect its source markup.
- Color replacement preview with an explicit apply/cancel step.
- SVG optimization with a before/after size comparison.

Prioritize these only after validating demand; avoid duplicating general-purpose
SVG generation or code-snippet generation already available through AI assistants
and IDE tooling.

## Release process

- Treat proposed version numbers and scopes as provisional until a release is
  planned.
- For each release, synchronize `CHANGELOG.md`, `README.md`, `build.gradle.kts`,
  and the user-facing description in `src/main/resources/META-INF/plugin.xml`.
- Keep changes focused and add tests for behavior changes.
