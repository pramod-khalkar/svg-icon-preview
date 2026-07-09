# SVG Toolkit

A comprehensive IntelliJ IDEA plugin for working with SVG images embedded as data URIs. Displays inline previews, provides powerful tools for inspection, export, and caching.

**Version**: 1.1.0  
**Compatibility**: IntelliJ IDEA 2023.1+ (all editions)

## ✨ Features

### Detection & Preview
- 🖼️ **Smart SVG Detection** - Detects `data:image/svg+xml;base64,...` strings in code (handles line breaks, charset, optional encoding)
- 👁️ **Gutter Icon Preview** - Shows 16×16px icon in editor margin
- 📄 **Editor Tab Preview** - Click gutter icon to open SVG in editor tab with split view (source | preview)

### Robust Decoding
- ✅ **Multiple Format Support** - Standard base64, URL-safe base64, plain text SVG URIs
- ✅ **Smart Fallback Chain** - Auto-fixes missing padding, handles URL encoding variants
- ✅ **Validation** - Ensures decoded content is valid SVG

### Performance & Memory
- ⚡ **Background Rendering** - SVGs ≤200KB render immediately; larger ones on-demand
- 💾 **LRU Image Cache** - Caches up to 100 rendered images (50 MB max), ~50x faster for duplicates
- 📊 **Lazy Loading** - Placeholders for large SVGs prevent UI lag
- 🧵 **Non-Blocking Threads** - All rendering on background threads

### Smart UI/UX
- 🎯 **Dynamic Tooltips** - Shows file size for large SVGs ("Click to load preview")
- 🔍 **Zoom Controls** - 10%-400% zoom with keyboard shortcuts (Ctrl+±, Ctrl+0)
- 🖥️ **High-Quality Rendering** - Bicubic interpolation for smooth scaling
- 🎛️ **Toolbar Controls** - Zoom in/out, reset, copy/export buttons

### Export Capabilities
- 📥 **Copy as PNG** - One-click clipboard copy (paste anywhere: Slack, email, design tools)
- 💾 **Export to File** - Save rendered SVG as PNG with file chooser

### Configuration
- ⚙️ **User Settings** - Configurable MAX_INLINE_SIZE (10 KB–10 MB, default 200 KB)
- 💾 **Persistent Settings** - Settings stored in workspace config
- 🎯 **Zero Configuration** - Works out of the box with sensible defaults

### Logging & Debugging
- 📝 **Comprehensive Logging** - DEBUG/WARN/ERROR logs with payload IDs
- 🔍 **Traceable** - Searchable in `idea.log` for diagnostics
- 🛡️ **Privacy-Safe** - Uses hash IDs instead of full payloads

## Installation

### From JetBrains Marketplace (Recommended)
1. Open IntelliJ IDEA
2. Go to: **Preferences** → **Plugins** → **Marketplace**
3. Search for: **SVG Toolkit**
4. Click **Install** and restart IDE

### From Disk
1. Download `svg-icon-preview-1.1.0.zip` from releases
2. Open IntelliJ IDEA: **Preferences** → **Plugins** → ⚙️ → **Install plugin from disk...**
3. Select the ZIP file and restart

## Requirements

- **IntelliJ IDEA** 2023.1 or later (Community or Ultimate)
- **Java** 11+ (JDK)

## Quick Start

1. **Open a code file** with SVG data URIs:
   ```java
   String icon = "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQi...";
   ```

2. **Hover over line** → See small icon in gutter

3. **Click gutter icon** → SVG opens in editor tab with:
   - Left pane: Formatted, decoded SVG source (non-editable)
   - Right pane: SVG preview with zoom controls
   - Toolbar above: Copy as PNG, Export PNG, image details (e.g., "400 × 400 px ~.5KB")

4. **Use toolbar:**
   - **Zoom**: `-` / `+` buttons or `Ctrl+±`
   - **Reset**: `Ctrl+0`
   - **Copy as PNG**: Click "Copy as PNG" button
   - **Export to file**: Click "Export PNG" button

## Configuration

### User Settings
Go to: **Preferences** → **SVG Toolkit**

**Settings available:**
- **Maximum inline SVG size (KB)** - Default: 200 KB
  - SVGs ≤ this size render immediately
  - SVGs > this size show placeholder, render on-demand (saves memory)

## Performance

| Scenario | Before | After | Improvement |
|----------|--------|-------|-------------|
| 50 identical SVGs in file | Parse + render 50x | Parse + render 1x, cache hit 49x | ~50x faster |
| Large SVG >200KB | Blocks UI | Lazy render on click | Instant |
| Copy SVG to clipboard | Manual export | One click | 30s+ saved |
| Inspect small details | No zoom | 10%-400% zoom | Better UX |

## Building from Source

### Prerequisites
- JDK 11+ installed
- Git

### Build Steps

```bash
# Clone
git clone <repository-url>
cd svg-icon-preview

# Build
./gradlew clean build

# Output
# Plugin: build/libs/svg-icon-preview-1.1.0.jar
# Sandbox: build/idea-sandbox/IC-2023.1/plugins/svg-icon-preview/
```

### Run in Development Mode

```bash
./gradlew runIde
```

This opens a sandbox IntelliJ instance with the plugin pre-installed.

## Project Structure

```
svg-icon-preview/
├── build.gradle.kts                          # Build config
├── src/main/java/com/plugin/svg/
│   ├── SvgIconLineMarkerProvider.java        # Main plugin (detection + rendering)
│   ├── SvgDataUriUtil.java                   # Robust decoder
│   ├── SvgDynamicIcon.java                   # Placeholder icon
│   ├── SvgImageCache.java                    # LRU image cache (50 MB, 100 entries)
│   ├── SvgFileEditorProvider.java            # File editor provider for SVG previews
│   ├── SvgFileEditor.java                    # Main editor showing split view
│   ├── SvgRenderer.java                      # SVG→image conversion (Batik)
│   ├── SvgToolkitSettings.java               # Persistent user settings
│   ├── SvgToolkitSettingsConfigurable.java   # Settings UI entry point
│   ├── SvgToolkitSettingsPanel.java          # Settings panel UI
│   └── SvgVirtualFile.java                   # Virtual file for editor tabs
├── src/test/java/com/plugin/svg/
│   └── SvgDataUriUtilTest.java               # Unit tests (5 decoder variants)
└── src/main/resources/META-INF/plugin.xml    # Plugin descriptor

Docs:
├── docs/roadmap.md                           # Feature roadmap (v1.0.1→v1.5.0)
└── docs/sample-icons.json                    # Example SVG collection
```

## Dependencies

- **Apache Batik 1.17** - SVG transcoding
  - `batik-transcoder` - SVG to raster
  - `batik-codec` - Image codecs
  - `batik-rasterizer` - SVG rendering
- **IntelliJ Platform 2023.1** - IDE framework
- **JUnit 4** - Unit testing

## Compatibility

| IDE Version | Support | Notes |
|-------------|---------|-------|
| IntelliJ 2023.1+ | ✅ Full | Minimum supported |
| IntelliJ 2024.1+ | ✅ Full | Current stable |
| IntelliJ 2025+ | ✅ Full | Future-compatible |
| Community Edition | ✅ Yes | All features |
| Ultimate Edition | ✅ Yes | All features |

## Troubleshooting

### Plugin shows no icons
- Ensure SVG data URI format: `data:image/svg+xml;base64,...`
- Verify base64 is valid SVG (starts with `<svg`)
- Check `Help` → `Show Log in Explorer` and search for payload ID

### Plugin is slow
- Reduce **MAX_INLINE_SIZE** in Preferences → SVG Toolkit (if many large SVGs)
- Check cache stats in logs: "Cache: X entries, YMB / 50MB"
- Restart IDE to clear cache

### Build fails
- Ensure Java 11+: `java -version`
- Clear: `./gradlew clean build`
- Make gradlew executable: `chmod +x gradlew`

## Keyboard Shortcuts

| Action | Shortcut |
|--------|----------|
| Zoom in (preview) | `Ctrl+Plus` or `Ctrl+=` |
| Zoom out (preview) | `Ctrl+Minus` |
| Reset zoom (preview) | `Ctrl+0` |

## Plugin Info

- **Plugin ID**: `com.plugin.svg_icon_preview`
- **Version**: 1.1.0
- **Author**: Pramod Khalkar
- **License**: See LICENSE file

## Roadmap

**v1.2.0** (Export Features) - Export SVG files, batch export, sprite sheets  
**v1.3.0** (SVG Library) - User collections, search & insert, sync across projects  
**v1.4.0** (Testing & Quality) - Comprehensive tests, performance benchmarks  
**v1.5.0** (Advanced) - Analytics, batch operations, optimization

See `docs/roadmap.md` for detailed feature breakdown.

## Contributing

Contributions welcome! Please submit issues and pull requests.

## License

See LICENSE file for licensing details.

---

**Questions or issues?** Open an issue on GitHub or check the troubleshooting section above.
