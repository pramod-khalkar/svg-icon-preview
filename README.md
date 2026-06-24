# SVG Icon Preview

A lightweight IntelliJ IDEA plugin that displays inline previews of SVG images encoded as base64 data URIs in your code.

## Features

- 🖼️ **Gutter Icon Preview** - Detects `data:image/svg+xml;base64,...` strings and shows a preview icon in the editor gutter
- 👁️ **Popup Preview** - Click the gutter icon to open a larger preview of the SVG image
- ⚡ **Zero Configuration** - Works out of the box, no setup required
- 🔄 **Multi-Version Support** - Compatible with IntelliJ IDEA 2023.1 and all newer versions

## Requirements

- **IntelliJ IDEA** 2023.1 or later (Community or Ultimate edition)
- **Java** 11 or later (JDK)
- **Gradle** 9.5+ (included via gradle wrapper)

## Installation

1. Download the latest `svg-icon-preview-1.0.0.zip` from the `build/distributions/` directory
2. In IntelliJ IDEA: `Preferences` → `Plugins` → `⚙️` → `Install plugin from disk...`
3. Select the ZIP file and restart IntelliJ IDEA

## Building from Source

### Prerequisites
- JDK 11 or higher installed
- Git

### Build Steps

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd svg-icon-preview
   ```

2. **Build the plugin**
   ```bash
   ./gradlew clean buildPlugin
   ```

3. **Output**
   The compiled plugin ZIP will be generated at:
   ```
   build/distributions/svg-icon-preview-1.0.0.zip
   ```

### Build Configuration

The build uses:
- **IntelliJ Platform**: 2023.1 (minimum supported version)
- **Java Compatibility**: Java 11+ (backward compatible with Java 17, 21, etc.)
- **Build Tool**: Gradle 9.5 with IntelliJ Platform Gradle Plugin 2.0.1

## Testing

### Running in Development Mode

1. **Run the plugin in a sandbox IDE**
   ```bash
   ./gradlew runIde
   ```
   This launches a new IntelliJ instance with the plugin pre-installed.

2. **Create a test file**
   - Create or open a Java/Kotlin file
   - Add a field with a base64-encoded SVG data URI:
     ```java
     String svgIcon = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0OCIgaGVpZ2h0PSI0OCI+PC9zdmc+";
     ```

3. **Verify the preview**
   - Look at the gutter (left margin) next to the line
   - You should see a small icon preview
   - Click the icon to see an enlarged preview in a popup

### Unit Testing

Currently, no automated tests are configured. To add tests:

1. Create test files in `src/test/java/`
2. Run tests with:
   ```bash
   ./gradlew test
   ```

## Project Structure

```
svg-icon-preview/
├── build.gradle.kts              # Gradle build configuration
├── settings.gradle.kts           # Gradle settings with plugin management
├── src/
│   ├── main/
│   │   ├── java/com/plugin/svg/  # Plugin source code
│   │   │   └── SvgIconLineMarkerProvider.java
│   │   └── resources/
│   │       └── META-INF/plugin.xml  # Plugin configuration
│   └── test/                     # Test files (if added)
├── gradle/wrapper/               # Gradle wrapper files
└── build/                        # Build output (generated)
    └── distributions/
        └── svg-icon-preview-1.0.0.zip
```

## Dependencies

- **Apache Batik** (v1.17) - For SVG transcoding and rendering
  - `batik-transcoder` - SVG to raster image conversion
  - `batik-codec` - Image codec support
  - `batik-rasterizer` - SVG rasterization

## Configuration

The plugin automatically detects SVG data URIs in the following formats:
```
data:image/svg+xml;base64,<base64-encoded-svg>
```

No configuration file or IDE settings are required.

## Compatibility

| Version | Support |
|---------|---------|
| IntelliJ IDEA 2023.1+ | ✅ Full support |
| IntelliJ IDEA 2024.1+ | ✅ Full support |
| IntelliJ IDEA 2025+   | ✅ Full support |
| Community Edition | ✅ Supported |
| Ultimate Edition | ✅ Supported |

## Development Notes

- **Plugin ID**: `com.plugin.svg-icon-preview`
- **Version**: 1.0.0
- **Author**: Pramod Khalkar
- **License**: See LICENSE file

## Troubleshooting

### Plugin not showing icons
- Ensure the SVG data URI follows the format: `data:image/svg+xml;base64,...`
- Verify the base64-encoded content is a valid SVG
- Restart IntelliJ IDEA after installation

### Build fails
- Ensure you have Java 11+ installed: `java -version`
- Clear cache and rebuild: `./gradlew clean buildPlugin`
- Check that Gradle wrapper is executable: `chmod +x gradlew`

## License

See the LICENSE file for licensing information.

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.
