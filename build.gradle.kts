plugins {
    id("java")
}

group = "io.github.spiky"
version = "0.1.0"

repositories {
    mavenCentral()
}

val jfxVersion = "latest.release"

dependencies {
    //Implementing javafx libraries for Windows to be able to read the class methods
    implementation("org.openjfx:javafx-base:$jfxVersion:win")
    implementation("org.openjfx:javafx-controls:$jfxVersion:win")
    implementation("org.openjfx:javafx-graphics:$jfxVersion:win")
    implementation("org.openjfx:javafx-fxml:$jfxVersion:win")
    implementation("org.openjfx:javafx-media:$jfxVersion:win")
    implementation("org.openjfx:javafx-web:$jfxVersion:win")
    implementation("org.openjfx:javafx-swing:$jfxVersion:win")
    //Implementing Jackson for JSON logic
    implementation("tools.jackson.core:jackson-databind:3.1.3")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}