import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.0.1"
}

group = "com.plugin.svg"
version = "1.1.4"

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
            <h3>Version 1.1.4</h3>
            <ul>
                <li>Updated Project View selection handling for the SVG Toolkit context-menu action</li>
                <li>Fixed an intermittent editor-state error when opening or selecting preview tabs</li>
                <li>Corrected displayed SVG dimensions and source-size units in the preview</li>
            </ul>
            """.trimIndent()
        )
    }

    buildPlugin {
        archiveFileName.set("svg-icon-preview-${version}.zip")
    }
}
