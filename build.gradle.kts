import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.0.1"
}

group = "com.plugin.svg"
version = "1.1.2"

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
            <h3>Version 1.1.2</h3>
            <ul>
                <li>SVG detection and rendering now works in ALL file types (Java, JSON, HTML, JavaScript, TypeScript, XML, etc.)</li>
                <li>Editor tab interface with split view: Formatted SVG source | SVG preview</li>
                <li>Enhanced SVG rendering reliability with improved DOCTYPE handling</li>
                <li>User-configurable MAX_INLINE_SIZE setting (10 KB–10 MB, default 200 KB)</li>
                <li>LRU image cache for ~50x faster duplicate loading</li>
                <li>Zoom controls (10%-400%) with keyboard shortcuts (Ctrl+±, Ctrl+0)</li>
                <li>Copy as PNG and Export to file capabilities</li>
            </ul>
            """.trimIndent()
        )
    }

    buildPlugin {
        archiveFileName.set("svg-icon-preview-${version}.zip")
    }
}
