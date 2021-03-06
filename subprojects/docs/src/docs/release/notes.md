## New and noteworthy

Here are the new features introduced in this Gradle release.

<!--
IMPORTANT: if this is a patch release, ensure that a prominent link is included in the foreword to all releases of the same minor stream.
Add-->

### Public type for representing lazily evaluated properties

Because Gradle's build lifecycle clearly distinguishes between configuration phase and execution phase the evaluation of property
 values has to be deferred under certain conditions to properly capture end user input. A typical use case is the mapping of
 extension properties to custom task properties as part of a plugin implementation. In the past, many plugin developers were forced to solve evaluation order problems by using the concept of convention mapping, an internal API in Gradle subject to change.

This release of Gradle introduces a mutable type to the public API representing a property with state. The relevant interface is called [`PropertyState`](javadoc/org/gradle/api/provider/PropertyState.html). An instance of this type can be created through the method [`Project.property(Class)`](javadoc/org/gradle/api/Project.html#property-java.lang.Class-).

The following example demonstrates how to use the property state API to map an extension property to a custom task property without
running into evaluation ordering issues:

    apply plugin: GreetingPlugin

    greeting {
        message = 'Hi from Gradle'
        outputFiles = files('a.txt', 'b.txt')
    }

    class GreetingPlugin implements Plugin<Project> {
        void apply(Project project) {
            // Add the 'greeting' extension object
            def extension = project.extensions.create('greeting', GreetingPluginExtension, project)
            // Add a task that uses the configuration
            project.tasks.create('hello', Greeting) {
                message = extension.messageProvider
                outputFiles = extension.outputFiles
            }
        }
    }

    class GreetingPluginExtension {
        final PropertyState<String> message
        final ConfigurableFileCollection outputFiles

        GreetingPluginExtension(Project project) {
            message = project.property(String)
            setMessage('Hello from GreetingPlugin')
            outputFiles = project.files()
        }

        String getMessage() {
            message.get()
        }

        Provider<String> getMessageProvider() {
            message
        }

        void setMessage(String message) {
            this.message.set(message)
        }

        FileCollection getOutputFiles() {
            outputFiles
        }

        void setOutputFiles(FileCollection outputFiles) {
            this.outputFiles.setFrom(outputFiles)
        }
    }

    class Greeting extends DefaultTask {
        final PropertyState<String> message = project.property(String)
        final ConfigurableFileCollection outputFiles = project.files()

        @Input
        String getMessage() {
            message.get()
        }

        void setMessage(String message) {
            this.message.set(message)
        }

        void setMessage(Provider<String> message) {
            this.message.set(message)
        }

        FileCollection getOutputFiles() {
            outputFiles
        }

        void setOutputFiles(FileCollection outputFiles) {
            this.outputFiles.setFrom(outputFiles)
        }

        @TaskAction
        void printMessage() {
            getOutputFiles().each {
                it.text = getMessage()
            }
        }
    }

### Build Cache Improvements

#### Remote build cache honors `--offline` 

When running with `--offline`, Gradle will disable the remote build cache.

#### Detecting overlapping task outputs

When two tasks write into the same directory, Gradle will now disable task output caching for the second task to execute. This prevents issues where task outputs for a different task are captured for the wrong build cache key. On subsequent builds, if overlapping outputs are detected, Gradle will also prevent you from loading task outputs from the cache if it would remove existing outputs from another task.

You can diagnose overlapping task output issues by running Gradle at the `--info` log level. If you are using [Gradle Build Scans](https://gradle.com/scans/get-started), the same detailed reason for disabling task output caching will be included in the build timeline. 

#### Stricter validation of task properties

When a plugin is built with the [Java Gradle Plugin Development Plugin](userguide/javaGradle_plugin.html), custom task types declared in the plugin will go through validation. In Gradle 4.0, additional problems are now detected. 

A warning is shown when:
* a task has a property without an input or output annotation (this might indicate a forgotten input or output),
* a task has `@Input` on a `File` property (instead of using `@InputFile` of `@InputDirectory`),
* a task declares conflicting types for a property (say, both `@InputFile` and `@InputDirectory`),
* a cacheable task declares a property without specifying `@PathSensitive`. In such a case, we default to `ABSOLUTE` path sensitivity, which will prevent the task's outputs from being shared across different users via a shared cache.

For more info on using task property annotations, see the [user guide chapter](userguide/more_about_tasks.html#sec:task_input_output_annotations).

#### Cache-safe mixed JVM language compilation

In Gradle 3.5, projects that used both Java and another JVM language (like Groovy or Scala) would encounter problems when using the build cache. The class files created by multiple compilation tasks were all placed into the same output directory, which made determining the set of outputs to cache for each task difficult.

Gradle now uses separate output directories for each JVM language. TODO: We should expand this since this is not specifically a build cache improvement.

#### Automatic clean-up of local build cache

By default, Gradle limits the size of the local build cache to 5GB. In Gradle 3.5, the local build cache was allowed to grow indefinitely.

You can increase or decrease the size of the local build cache by configuring your local cache:

    buildCache {
        local {
            // Set target size to 10GB
            targetSizeInMB = 10240
        }
    }

This is a _target_ size for the build cache. Gradle will periodically check if the local build cache has grown too large and trim it to below the target size. The oldest build cache entries will be deleted first.

### Parallel download of dependencies

Gradle will now download dependencies from remote repositories in parallel. It will also make sure that if you build multiple projects in parallel (with `--parallel`) and that 2 projects try to download the same dependency at the same time, that dependency wouldn't be downloaded twice.

### Default Zinc compiler upgraded from 0.3.7 to 0.3.13

This will take advantage of performance optimizations in the latest [Zinc](https://github.com/typesafehub/zinc) releases.

### Ivy plugin repositories support patterns and layouts

Ivy plugin repositories now support the same API for patterns and layouts that Ivy artifact repositories support.

<!--
### Example new and noteworthy
-->

## Promoted features

Promoted features are features that were incubating in previous versions of Gradle but are now supported and subject to backwards compatibility.
See the User guide section on the “[Feature Lifecycle](userguide/feature_lifecycle.html)” for more information.

The following are the features that have been promoted in this Gradle release.

<!--
### Example promoted
-->

## Fixed issues

## Deprecations

Features that have become superseded or irrelevant due to the natural evolution of Gradle become *deprecated*, and scheduled to be removed
in the next major Gradle version (Gradle 4.0). See the User guide section on the “[Feature Lifecycle](userguide/feature_lifecycle.html)” for more information.

The following are the newly deprecated items in this Gradle release. If you have concerns about a deprecation, please raise it via the [Gradle Forums](https://discuss.gradle.org).

<!--
### Example deprecation
-->

### Setting the compiler executable is no longer deprecated

In Gradle 3.5 `ForkOptions.executable` has been deprecated. In Gradle 4.0 it is not deprecated anymore, but using it will disable task output caching for the compiler task.

## Potential breaking changes

### Multiple class directories

In projects that use multiple JVM languages (Java and Scala, Groovy and other languages), Gradle now uses separate output directories for each language. 

To return to the old behavior, explicitly set the classes directory:

   // Change the output directory for the main source set back to the old path
   sourceSets.main.output.classesDir = new File(buildDir, "classes/main")

Please be aware that this will interfere with the effectiveness of the build cache when using multiple JVM languages in the same source set.

### Location of classes in the build directory

The default location of classes when using the `java`, `groovy` or `scala` plugin has changed from:

   Java: build/classes/main
   Groovy: build/classes/main
   Scala: build/classes/main
   Generically: build/classes/${sourceSet.name}

to

   Java: build/classes/java/main
   Groovy: build/classes/groovy/main
   Scala: build/classes/scala/main
   Generically: build/classes/${sourceDirectorySet.name}/${sourceSet.name}

Plugins, tasks or builds that hardcoded these paths may fail.  You can access the specific output directory for a particular language via [`SourceDirectorySet#outputDir`](dsl/org.gradle.api.file.SourceDirectorySet.html#org.gradle.api.file.SourceDirectorySet:outputDir). 

### maven-publish and ivy-publish mirror multi-project behavior

When using the `java` plugin, all `compile` and `runtime` dependencies will now be mapped to the `compile` scope, i.e. "leaked" into the consumer's compile classpath. This is in line with how
 these legacy configurations work in multi-project builds. We strongly encourage you to use the `api`(java-library plugin only), `implementation` and `runtimeOnly` configurations instead. These
 are mapped as expected, with `api` being exposed to the consumer's compile classpath and `implementation` and `runtimeOnly` only available on the consumer's runtime classpath.

### Memory settings and forking not tracked as inputs for JVM compilation

Previously Gradle would treat JVM compilation tasks as out-of-date whenever their memory settings changed compared to the previous execution. The same would happen if a forking compilation task was changed to non-forking, or vice versa. Since Gradle 4.0, these parameters are not treated as inputs anymore, and thus the compilation tasks will stay up-to-date when they are changed.

<!--
### Example breaking change
-->

### Changes to previously deprecated APIs

- The `JacocoPluginExtension` methods `getLogger()`, `setLogger(Logger)` are removed.
- The `JacocoTaskExtension` methods `getClassDumpFile()`, `setClassDumpFile(File)`, `getAgent()` and `setAgent(JacocoAgentJar)` are removed.
- Removed constructor `AccessRule(Object, Object)`.
- Removed constructor `ProjectDependency(String, String)` and the methods `getGradlePath()`, `setGradlePath(String)`.
- Removed constructor `WbDependentModule(Object)`.
- Removed constructor `WbProperty(Object)`.
- Removed constructor `WbResource(Object)`.
- Removed constructor `JarDirectory(Object, Object)`.
- Removed constructor `Jdk(Object, Object, Object, Object)`.
- Removed constructor `ModuleDependency(Object, Object)`.
- Moved classes `RhinoWorkerHandleFactory` and `RhinoWorkerUtils` into internal package.
- Removed `RhinoWorker`.
- Removed constructor `EarPluginConvention(Instantiator)`.
- Removed the method `registerWatchPoints(FileSystemSubset.Builder)` from `FileCollectionDependency`.
- Removed the method `getConfiguration()` from `ModuleDependency`.
- Removed the method `getProjectConfiguration()` from `ProjectDependency`.
- Removed class `BuildCache`.
- Removed class `MapBasedBuildCache`.
- Removed class `ActionBroadcast`.
- Removed the [Gradle GUI](https://docs.gradle.org/3.5/userguide/tutorial_gradle_gui.html). All classes for this feature have been removed as well as all leftovers supporting class from the Open API partly removed due to deprecation in Gradle 2.0.
- Removed the annotation `@OrderSensitive` and the method `TaskInputFilePropertyBuilder.orderSensitive`.
- Removed `dependencyCacheDir` getter and setters in java plugin, `JavaCompileSpec`, and `CompileOptions`
- Removed Ant <depend> related classes `AntDepend`, `AntDependsStaleClassCleaner`, and `DependOptions`
- Removed `Javadoc#setOptions`
- Removed `Manifest.writeTo(Writer)`. Please use `Manifest.writeTo(Object)`
- Removed `TaskInputs.source()` and `sourceDir()`. Please use `TaskInputs.file().skipWhenEmpty()`, `files().skipWhenEmpty()` and `dir().skipWhenEmpty()`.
- Chaining calls to `TaskInputs.file()`, `files()`, `dir()` and `TaskOutputs.file()`, `files()` and `dir()` are not supported anymore.
- Removed `TaskOutputs.doNotCacheIf(Spec)`, use `doNotCacheIf(String, Spec)` instead.

The deprecated `jetty` plugin has been removed. We recommend using the [Gretty plugin](https://github.com/akhikhl/gretty) for developing Java web applications.
The deprecated `pluginRepositories` block for declaring custom plugin repositories has been removed in favor of `pluginManagement.repositories`.

### Adding copy specs is not allowed during task execution of a `AbstractCopyTask` task

You can no longer add copy specs to a copy (like `Copy` and `Sync`) or archive task (like `Zip` and `Tar`) when the task is executing. Tasks that used this behavior could produce incorrect results and not honor task dependencies. 

Starting with Gradle 4.0, builds that rely on this behavior will fail.  Previously, Gradle only failed if the task was cacheable and emitted a warning otherwise. 

```groovy
// This task adds a copy spec during the execution phase.
task copy(type: Copy) {
    from ("some-dir")
    into ("build/output")

    doFirst {
        // Adding copy specs during runtime is not allowed anymore
        // The build will fail with 4.0
        from ("some-other-dir") {
            exclude "non-existent-file"
        }
    }
}
```

## External contributions

We would like to thank the following community members for making contributions to this release of Gradle.

- [Ion Alberdi](https://github.com/yetanotherion) - Fix lazy evaluation of parent/grand-parent pom's properties ([gradle/gradle#1192](https://github.com/gradle/gradle/pull/1192))
- [Guillaume Delente](https://github.com/GuillaumeDelente) - Fix typo in user guide ([gradle/gradle#1562](https://github.com/gradle/gradle/pull/1562))
- [Guillaume Le Floch](https://github.com/glefloch) - Support of compileOnly scope in buildInit plugin ([gradle/gradle#1536](https://github.com/gradle/gradle/pull/1536))
- [Eitan Adler](https://github.com/grimreaper) - Remove some some duplicated words from documentation ([gradle/gradle#1513](https://github.com/gradle/gradle/pull/1513))
- [Eitan Adler](https://github.com/grimreaper) - Remove extraneous letter in documentation ([gradle/gradle#1459](https://github.com/gradle/gradle/pull/1459))
- [Pierre Noel](https://github.com/petersg83) - Add missing comma in `FileReferenceFactory.toString()` ([gradle/gradle#1440](https://github.com/gradle/gradle/pull/1440))
- [Hugo Bijmans](https://github.com/HugooB) - Fixed some typos and spelling in the JavaPlugin user guide ([gradle/gradle#1514](https://github.com/gradle/gradle/pull/1514))
- [Andy Wilkinson](https://github.com/wilkinsona) - Copy resolution listeners when a configuration is copied ([gradle/gradle#1603](https://github.com/gradle/gradle/pull/1603))
- [Tim Hunt](https://github.com/mitnuh) - Allow the use of single quote characters in Javadoc task options header and footer ([gradle/gradle#1288](https://github.com/gradle/gradle/pull/1288))
- [Jenn Strater](https://github.com/jlstrater) - Add groovy-application project init type ([gradle/gradle#1480](https://github.com/gradle/gradle/pull/1480))
- [Jacob Ilsoe](https://github.com/jacobilsoe) - Update Zinc to 0.3.13 ([gradle/gradle#1463](https://github.com/gradle/gradle/issues/1463))
- [Shintaro Katafuchi](https://github.com/hotchemi) - Issue: #952 Make project.sync() a public API ([gradle/gradle#1137](https://github.com/gradle/gradle/pull/1137))
- [Lari Hotari](https://github.com/lhtorai) - Issue: #1730 Memory leak in Gradle daemon
 ([gradle/gradle#1736](https://github.com/gradle/gradle/pull/1736))
- [Andy Bell](https://github.com/andyrbell) - Prevent NullPointerException for JUnit Categories for test description with null test class([gradle/gradle#1511](https://github.com/gradle/gradle/pull/1511))
- [Piotr Kubowicz](https://github.com/pkubowicz) - Default to compileClasspath configuration in DependencyInsightReportTask ([gradle/gradle#1376](https://github.com/gradle/gradle/pull/1395))
- [Chris Gavin](https://github.com/chrisgavin) - Clean up Sign task API ([gradle/gradle#1679](https://github.com/gradle/gradle/pull/1679))
- [Szczepan Faber](https://github.com/szczepiq) - Issue: #1857 Could not copy MANIFEST.MF / Multiple entries with same key
- [Bo Zhang](https://github.com/blindpirate) - Use Enum.getDeclaringClass() to avoid NPE in comparing enums ([gradle/gradle#1862](https://github.com/gradle/gradle/pull/1862))
- [Danny Thomas](https://github.com/DanielThomas) - Improve performance of version parsing ([gradle/gradle#1659](https://github.com/gradle/gradle/pull/1659))
- [Ethan Hall](https://github.com/ethankhall) - Pattern and layout support for Ivy plugin repositories ([gradle/gradle#1813](https://github.com/gradle/gradle/pull/1813))

We love getting contributions from the Gradle community. For information on contributing, please see [gradle.org/contribute](https://gradle.org/contribute).

## Known issues

Known issues are problems that were discovered post release that are directly related to changes made in this release.
