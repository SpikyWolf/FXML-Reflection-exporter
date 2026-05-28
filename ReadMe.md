<h1>FXML Reflection Exporter</h1>
This is a util designed to parse through any fxml files in your JavaFX project and export any relevant methods into a JSON file.<br>
The JSON file it generates will be written to the path expected by either GraalVM or Gluon depending on which of these are used as a plugin in your project.
If neither are present it will default to the GraalVM path to allow the util to be used in any project.

This util is provided both as a Gradle plugin as well as a standalone .Jar to allow for command line use.<br>
In case you use it as a command line util it expects the following arguments:
* Argument 1, The path to your FXML file directory, it accepts "-d" as a default which will direct it to src/main/resources. This is also the directory the plugin version will check.
* Argument 2, Your native compiler's name. Currently, the util only recognises GraalVM and Gluon, this argument also accepts "-d" which will default to GraalVM's expected config path.

<h2>Kotlin DSL</h2>
<pre>
plugins{
    id("io.github.spiky.fxmlreflectionexporter") version "1.0.0"
}
</pre>

<h2>Groovy DSL</h2>
<pre>
plugins{
    id 'io.github.spiky.fxmlreflectionexporter' version '1.0.0'
}
</pre>
