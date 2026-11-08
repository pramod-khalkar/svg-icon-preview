import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.0.1"
}

group = "com.plugin.svg"
version = "1.2.0"

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
        sinceBuild.set("231")
        untilBuild.set("299.*")

        changeNotes.set(
            """
            <h3>Version 1.2.0</h3>
            <ul>
                <li>Added SVG Files and SVG Data URIs browser tabs, including project-wide data URI search with thumbnails, source snippets, line numbers, preview, and source navigation</li>
                <li>Added standalone SVG thumbnails, dimensions, file sizes, and project-relative paths</li>
                <li>Added Save, Save As SVG, Save As PNG, Copy as PNG, and Copy as Base64 data URI actions in the preview File menu</li>
                <li>Added saving edited standalone SVGs with confirmation when the original file changed</li>
                <li>Reopen an existing preview tab for the same SVG file or data URI, and show the source filename on standalone SVG preview tabs</li>
                <li>Prompt for an IDE restart after plugin installation, update, or removal</li>
                <li>Show a one-time What's New notification with release highlights after installation or an update</li>
                <li>Fix a read-access exception when saving edited SVG content back to its original file</li>
                <li>Added IntelliJ-style light and dark icons for the Project View action and SVG Toolkit tool window</li>
            </ul>
            """.trimIndent()
        )
    }

    buildPlugin {
        archiveFileName.set("svg-icon-preview-${version}.zip")
    }
}
