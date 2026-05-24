#!/usr/bin/env kotlin

// This should be included in your build.gradle.kts
tasks.register<JavaExec>("generateFxmlReflection") {
description = "Automated reflection export task."
group = "automation"
mainClass.set("com.spiky.FXMLReflectionExporter.Main")
classpath = files("tools/FXML-Reflection-exporter-v0.1.0.jar") + configurations["runtimeClasspath"]
jvmArgs = listOf("--enable-native-access=ALL-UNNAMED") //Just included to mute console warnings, it's optional
doFirst {
var compilerArg = "-d"
if (project.plugins.hasPlugin("org.graalvm.buildtools.native")){compilerArg = "graalvm"}
if (project.plugins.hasPlugin("com.gluonhq.client-gradle-plugin")){compilerArg = "gluon"}
args = listOf("-d", compilerArg)
println("[FxmlExporter] Detected native framework: $compilerArg")
}
}
tasks.named("processResources") {
dependsOn("generateFxmlReflection")
}