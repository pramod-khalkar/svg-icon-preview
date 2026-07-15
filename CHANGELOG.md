## Changelog

All notable changes to SVG Toolkit will be documented in this file.

### [1.1.2] - 2026-07-10

**Feature: Support for All File Types**
- SVG detection and rendering now works in all file types (Java, JSON, HTML, JavaScript, TypeScript, XML, etc.)

### [1.1.0] - 2026-07-10

**Major Release: Editor Tab Interface & Performance Improvements**

#### Added
- **Editor Tab Interface** - SVG previews now open in editor tabs (like Java files) instead of side tool window
  - Split view: Formatted SVG source (left) | SVG preview (right)
  - Toolbar above editor with Copy as PNG, Export PNG, and image details label
  - Zoom-enabled preview panel (mouse wheel + keyboard shortcuts: Ctrl+±, Ctrl+0)
  - Proper SVG/XML file icons in editor tabs (replaces question mark)
  - Clean tab titles: `svg-preview-<timestamp>` (no file extension)
- **Enhanced SVG Rendering Reliability** - Improved DOCTYPE handling and disabled DTD validation to prevent external entity loading issues
- **Improved Error Reporting** - Editor tab shows actionable messages directing users to check idea.log for rendering failures
- **LRU Image Cache** - Caches up to 100 rendered images (50 MB max), ~50x faster for duplicate SVGs
- **Dynamic Tooltips** - Tooltips show SVG size and rendering hints for large files
- **User-Configurable Settings** - Settings UI for MAX_INLINE_SIZE (10 KB–10 MB, default 200 KB)
- **Comprehensive Logging** - Structured logging with payload IDs for debugging
- **Background Rendering** - Smart lazy rendering: ≤200 KB immediate, >200 KB on-demand
- **Robust SVG Decoding** - Supports multiple formats: standard base64, URL-safe, URL-encoded, plain text

#### Improved
- **Detection Regex** - Handles line breaks, charset declarations, optional base64 marker
- **Error Handling** - Graceful fallbacks for malformed SVG URIs
- **Memory Safety** - Bounded cache size, placeholder icons prevent UI lag
- **Performance** - Non-blocking background threads for all rendering
- **XML Formatting** - Decoded SVG is now pretty-printed with 2-space indentation in editor tab
- **Settings Accessibility** - SVG Toolkit settings available under Settings → Tools → SVG Toolkit

#### Fixed
- Plugin descriptor validation (IntelliJ Marketplace compliance)
- Line marker registration for Java and JSON files
- Application service lifecycle management
- SVG rendering failures due to DOCTYPE/network issues
- Tab title showing `.svg-preview` extension
- Missing SVG/XML file type icon in editor tabs
- **Threading violation** - Read access from background thread when accessing PSI elements in line marker provider (fixes "Read access is allowed from inside read-action only" error)
- Plugin compatibility with IntelliJ Platform 2023.1+ (updated to Java 17)

#### Technical Details
- New files: `SvgFileEditorProvider.java`, `SvgFileEditor.java`
- Updated: `SvgIconLineMarkerProvider.java` (removed tool window integration, added editor tab implementation, fixed threading issue with ReadAction), `SvgRenderer.java` (enhanced reliability), `SvgVirtualFile.java` (added FileType implementation for proper icons), `plugin.xml` (replaced toolWindow with fileEditorProvider.java` (enhanced reliability), `SvgVirtualFile.java` (added FileType implementation for proper icons), `plugin.xml` (replaced toolWindow with fileEditorProvider)
- Test coverage: 5 decoder unit tests (base64, URL-safe, padding, URL-encoded, plain SVG)

---

### [1.0.0] - 2026-06-24

#### Added (initial release)
- Initial release of SVG Toolkit plugin
- SVG data URI detection in code
- Gutter icon preview (16×16 px)
- Full-size preview dialog on click
- Support for IntelliJ IDEA 2023.1+
- Compatible with all IntelliJ editions (Community, Ultimate)
- Zero configuration required

#### Supported Formats
- `data:image/svg+xml;base64,<encoded-svg>`