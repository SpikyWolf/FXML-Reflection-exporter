package io.github.spiky.FXMLReflectionExporter;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.artifacts.Configuration;
import java.io.File;
import java.util.List;

public class FXMLReflectionPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getTasks().register("generateFxmlReflection", JavaExec.class, task -> {
           task.setGroup("automation");
           task.setDescription("Automated reflection export task.");
           task.getMainClass().set("io.github.spiky.FXMLReflectionExporter.Main");
           try {
               File pluginJar = new File(io.github.spiky.FXMLReflectionExporter.Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
               Configuration hostClasspath  = project.getConfigurations().getByName("runtimeClasspath");
               Configuration detachedCfg = project.getConfigurations().detachedConfiguration(project.getDependencyFactory().create("tools.jackson.core:jackson-databind:3.1.3"));
               task.setClasspath(project.files(pluginJar, hostClasspath, detachedCfg));

           } catch (Exception e){
               throw new RuntimeException("[FxmlExporter] Failed to read and parse project dependencies!", e);
           }
          var compilerArg = "-d";
          if (project.getPluginManager().hasPlugin("org.graalvm.buildtools.native")){compilerArg = "graalvm";}
          if (project.getPluginManager().hasPlugin("com.gluonhq.client-gradle-plugin")){compilerArg = "gluon";}
          task.setArgs(List.of("-d", compilerArg));
          System.out.println("[FxmlExporter] Detected native framework: "+compilerArg);
        });
        project.getTasks().named("processResources").configure(t -> t.dependsOn("generateFxmlReflection"));
    }
}