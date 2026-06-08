import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
    id("org.jetbrains.compose") version "1.7.3"
}

group = "org.photocollection"
version = "1.0.0"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("com.drewnoakes:metadata-extractor:2.19.0")
    testImplementation(kotlin("test"))
    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    testImplementation(compose.uiTest)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

compose.desktop {
    application {
        mainClass = "org.photocollection.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "MacPhotoCollection"
            packageVersion = "1.0.0"
            macOS {
                iconFile.set(project.file("src/main/resources/icons/app-icon.icns"))
            }
        }
    }
}
