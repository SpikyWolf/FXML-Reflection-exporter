I am working on getting this util published as a plugin, in the meantime you can use the Kotlin script below to add it to your projects.

<h2>Gradle Kotlin DSL</h2>
// This should be included in your build.gradle.kts<br>
tasks.register<JavaExec>("generateFxmlReflection") {<br>
description = "Automated reflection export task."<br>
group = "automation"<br>
mainClass.set("io.github.spiky.FXMLReflectionExporter.Main")<br>
classpath = files("tools/FXML-Reflection-exporter-v0.1.0.jar") + configurations["runtimeClasspath"] +<br>
project.configurations.detachedConfiguration(project.dependencies.create("tools.jackson.core:jackson-databind:3.1.3"))<br>
jvmArgs = listOf("--enable-native-access=ALL-UNNAMED") //Just included to mute console warnings, it's optional<br>
doFirst {<br>
var compilerArg = "-d"<br>
if (project.plugins.hasPlugin("org.graalvm.buildtools.native")){compilerArg = "graalvm"}<br>
if (project.plugins.hasPlugin("com.gluonhq.client-gradle-plugin")){compilerArg = "gluon"}<br>
args = listOf("-d", compilerArg)<br>
println("[FxmlExporter] Detected native framework: $compilerArg")<br>
}<br>
}<br>
tasks.named("processResources") {<br>
dependsOn("generateFxmlReflection")<br>
}