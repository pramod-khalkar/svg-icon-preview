## Changelog

All notable changes to SVG Toolkit will be documented in this file.

### [1.1.0] - 2026-07-04

**Major Release: Performance & UX Improvements**

#### Added
- **LRU Image Cache** - Caches up to 100 rendered images (50 MB max), ~50x faster for duplicate SVGs
- **Enhanced Preview Dialog** - Full-featured preview with toolbar
  - Zoom controls (10%-400%, keyboard shortcuts: Ctrl+±, Ctrl+0)
  - High-quality bicubic interpolation for smooth scaling
  - Copy as PNG button (one-click clipboard copy)
  - Export to PNG file with file chooser
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

#### Fixed
- Plugin descriptor validation (IntelliJ Marketplace compliance)
- Line marker registration for Java and JSON files
- Application service lifecycle management

#### Technical Details
- New files: `SvgImageCache.java`, `SvgPreviewDialog.java`, `SvgToolkitSettings.java`, `SvgToolkitSettingsConfigurable.java`, `SvgToolkitSettingsPanel.java`
- Updated: `SvgIconLineMarkerProvider.java`, `plugin.xml`
- Test coverage: 5 decoder unit tests (base64, URL-safe, padding, URL-encoded, plain SVG)

---

### [1.0.0] - Initial Release

#### Added
- Initial release of SVG Toolkit plugin
- SVG data URI detection in code
- Gutter icon preview (16×16 px)
- Full-size preview dialog on click
- Support for IntelliJ IDEA 2023.1+
- Compatible with all IntelliJ editions (Community, Ultimate)
- Zero configuration required

#### Supported Formats
- `data:image/svg+xml;base64,<encoded-svg>`

---

## Roadmap

**v1.2.0** (Planned) - Export Features
- Export SVG to file
- Copy as SVG (text)
- Batch export all SVGs
- Sprite sheet generation

**v1.3.0** (Planned) - SVG Library
- User-defined SVG collections
- Search and quick insert
- Library sync across projects
- Analytics and statistics

**v1.4.0** (Planned) - Testing & Quality
- Comprehensive unit tests
- Integration tests
- Performance benchmarks

**v1.5.0** (Planned) - Advanced Features
- SVG editing tools
- Optimization and compression
- Batch operations
- Design system integration