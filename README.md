# SVG Toolkit

SVG Toolkit helps you find, preview, inspect, edit, and export SVG graphics in
IntelliJ-based IDEs. It previews SVG data URIs directly from code and provides
an editor and project browser for standalone SVG files.

**Current version:** 1.2.0
**IDE compatibility:** IntelliJ Platform 2023.1 and later
**Runtime:** Java 17+

## Features

### Preview SVG data URIs in code

- Detects `data:image/svg+xml` URIs and displays a small gutter preview.
- Click the gutter icon to open a dedicated SVG editor tab.
- Supports text-based file types, including Java, JSON, JavaScript, TypeScript,
  HTML, XML, YAML, and plain text.
- Decodes standard and URL-safe Base64, missing Base64 padding, URL-encoded
  payloads, and plain SVG data URIs.
- Handles optional charset and Base64 markers and validates decoded SVG content.

### Browse and open project SVG files

- Open **View → Tool Windows → SVG Toolkit** to search standalone `.svg` files
  in the current project.
- Filter by filename or folder path; use the refresh button to rescan.
- Browse thumbnails, file sizes, and intrinsic image dimensions when available.
  The displayed path is relative to the project root; hover over it for the
  absolute path.
- Double-click a result or select it and press Enter to open it in the SVG
  Toolkit editor.
- Alternatively, right-click an `.svg` file in the Project view and choose
  **Open with SVG Toolkit**.

The browser indexes standalone `.svg` files only; it does not search for
embedded SVG data URIs.

### Edit and preview

- View formatted SVG source beside the live preview, or switch to source-only
  and preview-only layouts.
- Edit source and see the preview update after a short debounce.
- Edits in a data-URI preview remain local to the preview; the original source
  code is not modified automatically.
- For standalone SVG files, use **File → Save** to write edits to the original.
  If the original changed after the preview was opened, the plugin asks before
  overwriting it.
- Use **File → Save As SVG…** to save the edited source as a separate file.

### Preview controls and output

- Zoom from 10% to 400%, fit the image, or view it at actual size.
- Zoom shortcuts: `Ctrl` + `+` / `=`, `Ctrl` + `-`, and `Ctrl` + `0`.
- Toggle checkerboard transparency and grid overlays.
- The **File** menu provides **Save**, **Save As PNG…**, **Save As SVG…**,
  **Copy as PNG**, and **Copy as Base64 data URI**. Save is enabled only for
  changed standalone SVG previews.

### Performance and settings

- SVG decoding and rendering are performed off the UI thread.
- Gutter previews use a bounded LRU image cache (up to 100 images / 50 MB).
- Large inline SVGs can be rendered on demand.
- Configure the maximum inline SVG size under **Settings → Tools → SVG Toolkit**
  (10 KB–10 MB; default 200 KB).

## Installation

In the IDE, open **Settings/Preferences → Plugins → Marketplace**, search for
**SVG Toolkit**, install it, and restart the IDE.

To install a release ZIP, use **Settings/Preferences → Plugins → Install Plugin
from Disk…**.

## How to use SVG Toolkit

### Preview an SVG data URI from code

1. Open a text-based source file containing an SVG data URI. For example:

   ```java
   String icon = "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQi...";
   ```

2. Find the SVG gutter icon beside the line and click it. SVG Toolkit opens a
   preview editor tab. The plugin accepts standard or URL-safe Base64,
   URL-encoded, and plain SVG data URIs, including supported optional charset
   declarations.
3. The source pane displays formatted SVG markup rather than the encoded URI.
   Edit the markup and wait briefly for the preview to refresh.
4. For a data-URI preview, edits stay local to the preview tab; the plugin does
   not rewrite the source code automatically.

### Open a standalone SVG

Choose either way to open an existing `.svg` file:

- In the Project view, right-click the file and select **Open with SVG Toolkit**.
- Open **View → Tool Windows → SVG Toolkit**, search by filename or folder,
  then double-click a result or select it and press Enter.

The browser lists standalone project `.svg` files, with a thumbnail, file size,
dimensions when readable, and a project-relative path. Hover over the path to
see the absolute path. Use Refresh after adding or moving files. The browser
does not index SVG data URIs embedded in source.

### Edit, save, export, and copy

In the preview editor:

- Edit the source pane to update the rendered preview.
- For a standalone SVG, choose **File → Save** to write edits to the original.
  If the file changed since opening the preview, confirm before overwriting.
- Choose **File → Save As SVG…** to create a separate `.svg` file. For
  data-URI previews, this is the way to save the edited SVG to disk.
- Choose **File → Save As PNG…** to export the rendered preview.
- Choose **File → Copy as PNG** to put the rendered image on the clipboard.
- Choose **File → Copy as Base64 data URI** to copy the edited SVG as a URI for
  manual use in code.

### Adjust the preview

- Use the layout buttons above the editor to show source and preview together,
  source only, or preview only. Drag the divider in split view to resize panes.
- Use Zoom In/Out, **Actual size (1:1)**, or **Fit image to preview**.
- Hold `Ctrl` while scrolling over the preview to zoom. Keyboard shortcuts are
  `Ctrl` + `+` / `=`, `Ctrl` + `-`, and `Ctrl` + `0`.
- Toggle the checkerboard to inspect transparency or the grid to help assess
  alignment. When checkerboard is off, the preview uses the IDE theme background.

### Configure large-inline-SVG behavior

Open **Settings/Preferences → Tools → SVG Toolkit** and set the maximum inline
SVG size (10 KB–10 MB; default 200 KB). Larger inline SVGs are loaded on demand
to help keep the editor responsive.

## Requirements

- IntelliJ Platform 2023.1 or later (Community or Ultimate)
- Java 17 or later

The plugin is built against IntelliJ IDEA Community 2023.1 and declares platform
compatibility in `build.gradle.kts`.

## Build from source

```bash
./gradlew test
./gradlew build
```

To launch a sandbox IDE with the plugin:

```bash
./gradlew runIde
```

## Project structure

```text
src/main/java/com/plugin/svg/
  SvgIconLineMarkerProvider.java       Detects embedded SVG data URIs
  SvgDataUriUtil.java                  Decodes and encodes SVG data URIs
  SvgRenderer.java                     Renders SVG with Apache Batik
  SvgImageCache.java                   Bounded cache for rendered gutter icons
  SvgFileEditor.java                   Source editor, preview, and file actions
  SvgVirtualFile.java                  In-memory file used for preview tabs
  OpenSvgWithToolkitAction.java        Project View context-menu action
  SvgProjectBrowserPanel.java          Searchable project SVG browser
  SvgProjectBrowserToolWindowFactory.java
                                      Registers the project browser tool window

src/main/resources/
  META-INF/plugin.xml                  Plugin metadata and extension registration
  icons/                               IDE action and tool-window icons
```

## Troubleshooting

**No gutter preview appears**
- Confirm the URI starts with `data:image/svg+xml`.
- Check that the payload contains valid SVG markup.
- The gutter provider scans text-based files and may skip SVGs that cannot be
  decoded.

**The project browser does not show a file**
- Confirm it is a standalone `.svg` file inside the current project content.
- Use the browser refresh button after adding or moving assets.
- Embedded data URIs are not included in this browser.

**A large SVG is slow to preview**
- Adjust the maximum inline SVG size under **Settings → Tools → SVG Toolkit**.
- Large inline SVGs are rendered on demand to keep the editor responsive.

For plugin logs, use **Help → Show Log in Finder/Explorer** and inspect
`idea.log`.

## Plugin information

- **Plugin ID:** `com.plugin.svg-icon-preview`
- **Version:** 1.2.0
- **Author:** Pramod Khalkar
- **License:** See `LICENSE`
