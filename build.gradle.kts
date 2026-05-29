import org.gradle.plugin.compatibility.compatibility

plugins {
    id("java")
    id("com.gradle.plugin-publish") version "2.1.1"
    id("com.gradleup.shadow") version "9.4.2"
}

group = "io.github.spiky"
version = "1.0.0"

repositories {
    mavenCentral()
}

val jfxVersion = "latest.release"
val jfxArch = "win"

dependencies {
    //Implementing javafx libraries for Windows to be able to read the class methods
    compileOnly("org.openjfx:javafx-base:$jfxVersion:$jfxArch")
    compileOnly("org.openjfx:javafx-controls:$jfxVersion:$jfxArch")
    compileOnly("org.openjfx:javafx-graphics:$jfxVersion:$jfxArch")
    compileOnly("org.openjfx:javafx-fxml:$jfxVersion:$jfxArch")
    compileOnly("org.openjfx:javafx-media:$jfxVersion:$jfxArch")
    compileOnly("org.openjfx:javafx-web:$jfxVersion:$jfxArch")
    compileOnly("org.openjfx:javafx-swing:$jfxVersion:$jfxArch")
    implementation("tools.jackson.core:jackson-databind:3.1.3")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    website = "https://github.com/SpikyWolf/FXML-Reflection-exporter"
    vcsUrl = "https://github.com/SpikyWolf/FXML-Reflection-exporter.git"
    plugins {
        register("fxmlReflectionExporterPlugin") {
            id = "io.github.spiky.fxmlreflectionexporter"
            implementationClass = "io.github.spiky.FXMLReflectionExporter.FXMLReflectionPlugin"
            displayName = "FXML parser for JavaFX reflection config generation"
            description = "A plugin allowing easy reflection config json file generation for native compilers"
            tags = listOf("fxml", "json", "reflection", "config", "javafx", "automation", "graalvm", "gluon", "native compile", "compile")
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}

tasks.shadowJar {
    archiveClassifier = ""
}

tasks.test {
    useJUnitPlatform()
}