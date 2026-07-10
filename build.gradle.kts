import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.0.1"
}

group = "com.plugin.svg"
version = "1.1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create("IC-2023.1")
        instrumentationTools()
        pluginVerifier()
    }

    implementation("org.apache.xmlgraphics:batik-transcoder:1.17") {
        exclude(group = "xml-apis", module = "xml-apis")
    }

    implementation("org.apache.xmlgraphics:batik-codec:1.17") {
        exclude(group = "xml-apis", module = "xml-apis")
    }

    implementation("org.apache.xmlgraphics:batik-rasterizer:1.17") {
        exclude(group = "xml-apis", module = "xml-apis")
    }

    implementation("xml-apis:xml-apis-ext:1.3.04") {
        exclude(group = "xml-apis", module = "xml-apis")
    }

    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    buildSearchableOptions = false
    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
    patchPluginXml {
//        sinceBuild.set("231")
        untilBuild.set("299.*")

        changeNotes.set(
            """
            <h3>Version 1.1.0</h3>
            <ul>
                <li>Initial public release of SVG Toolkit.</li>
                <li>Smart detection of Base64-encoded SVG images.</li>
                <li>Gutter icon with instant SVG preview.</li>
                <li>Show decoded SVG file.</li>
                <li>Export SVG to PNG.</li>
                <li>Copy rendered SVG as PNG to the clipboard.</li>
                <li>Configurable SVG size limit in Settings.</li>
            </ul>
            """.trimIndent()
        )
    }

    buildPlugin {
        archiveFileName.set("svg-icon-preview-${version}.zip")
    }
}
