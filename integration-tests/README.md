# Integration test suite

## TL;DR:
Make sure you have both [qcc](https://github.com/quarkuscc/qcc/) and [qcc-class-library](https://github.com/quarkuscc/qcc-class-library) installed.

```
$ mvn verify -pl integration-tests
```

or simply

```
$ mvn test
```

The command compiles and runs `examples/helloworld`, `integration-tests/apps/branches` programs.
It also recursively scans `integration-tests/snippets/` directory for all `.java` classes and their `.pattern` expected outputs.

All those classes are compiled, executables run and their output is matched with the corresponding regexp pattern.

Build logs, run logs and resulting binaries alongside their `.ll` and `.dot` files are archived in `integration-tests/target/archived-logs/` directory.

All logs are scanned for error and warning messages. 
Messages that were not whitelisted are treated as test errors. 

## Testing

The test suite aims at two main use cases:
1. Providing an easy, dead simple way to test snippets of code exploiting particular language features.
2. Keeping example apps and test apps working. Enabling us to play with packages, multiple classes, specific compilation flags etc.

### 1. Snippets

#### Adding a snippet
 * Create a `.java` file correctly named so as its name is the same as the class within it containing the entrypoint `main`. E.g. `MyCodeFeature.java`.
 * Create a `.pattern` file of the same name. The string within the file will be compiled as `java.util.regex.Pattern` and used to test the program output. Nothing else is needed, the test suite will pick the file up automatically.

Snippets can be organized in subdirectories. The semantic is just organization, not package names. All snippets are `default` package.

#### Running just snippets

```
mvn verify -pl integration-tests -Dtest=SnippetsTest
```

#### How does it work?
See `BuildAndRunCmd` in [SnippetsTest.java](./src/it/java/cc/quarkus/qcc/tests/integration/SnippetsTest.java). 
Each snippet is compiled and ran separately. No jar is built. 

### 2. Example/Test apps

#### Adding a test app

 * Create a directory in [apps](./apps) with all files the application needs to be built.
 * Add a record to [App.java](./src/it/java/cc/quarkus/qcc/tests/integration/utils/App.java) with the app's location. 
 * Create a record in [BuildAndRunCmd.java](./src/it/java/cc/quarkus/qcc/tests/integration/utils/BuildAndRunCmd.java) capturing how is the application supposed to be built and executed. There is not supposed to be any automagic. Just a simple list of commands the test suite executes within the app's directory. 
 * Create a test in [SimpleAppTest.java](./src/it/java/cc/quarkus/qcc/tests/integration/SimpleAppTest.java) or in a new test class, testing the application as you see fit.

## Classpath, QCC runtime, Java base

The necessary locations are controlled with these properties as noted in the log if you do not specify them:

```
Failed to detect any of QCC_RUNTIME_API_JAR, qcc.runtime.api.jar as env or sys props, 
defaulting to ~/.m2/repository/cc/quarkus/qcc-runtime-api/1.0.0-SNAPSHOT/qcc-runtime-api-1.0.0-SNAPSHOT.jar

Failed to detect any of QCC_MAIN_JAR, qcc.main.jar as env or sys props, 
defaulting to ${basedir}/qcc/main/target/qcc-main-1.0.0-SNAPSHOT.jar

Failed to detect any of QCC_BOOT_MODULE_PATH, qcc.boot.module.path as env or sys props, 
defaulting to ~/.m2/repository/cc/quarkus/qccrt-java.base/11.0.1-SNAPSHOT/qccrt-java.base-11.0.1-SNAPSHOT.jar:
~/.m2/repository/cc/quarkus/qcc-runtime-unwind/1.0.0-SNAPSHOT/qcc-runtime-unwind-1.0.0-SNAPSHOT.jar:
~/.m2/repository/cc/quarkus/qcc-runtime-api/1.0.0-SNAPSHOT/qcc-runtime-api-1.0.0-SNAPSHOT.jar
```
where `${basedir}` is the top level directory of the `qcc` project.

One can either set those with e.g. `export QCC_RUNTIME_API_JAR=...` or append to maven command as e.g. `-Dqcc.runtime.api.jar=...`.
There is no automagic in the test suite. It counts on those artifacts to be already built and available beforehand. The test suite merely use them to assemble compilation commands such as noted below:

```java
new String[]{"javac", "-cp",
        QCC_RUNTIME_API_JAR,
        "mypackage/Main.java"},
new String[]{"jar", "cvf", "main.jar", "mypackage/Main.class"},
new String[]{"java", "-jar", QCC_MAIN_JAR,
        "--boot-module-path",
        "main.jar:" + QCC_BOOT_MODULE_PATH,
        "--output-path",
        APP_BUILD_OUT_DIR,
        "mypackage.Main"},
new String[]{APP_BUILD_OUT_DIR + File.separator + "a.out"}
```

or simpler:

```java
new String[]{"javac", "-cp",
        QCC_RUNTIME_API_JAR,
        snippet.getFileName().toString()},
new String[]{"java", "-jar", QCC_MAIN_JAR,
        "--boot-module-path",
        ".:" + QCC_BOOT_MODULE_PATH,
        "--output-path",
        APP_BUILD_OUT_DIR,
        snippetName},
new String[]{APP_BUILD_OUT_DIR + File.separator + "a.out"}
```

## Logs, cleanup

The test suite automatically archives log files, command outputs and `out` directories with 
executables and `.ll` files to `integration-tests/target/archived-logs/` directory to facilitate 
debugging of test failures.

After the test, all `.class` and `.jar` files and `out` directories are deleted recursively from the
snippets or application directories using these globs:

```java
 delete("glob:**/{target," + APP_BUILD_OUT_DIR + ",logs}**", Path.of(app.dir));
 delete("glob:**/*.{class,jar}", Path.of(app.dir));
``` 
If your test generates some other files elsewhere, your test is responsible for cleaning those.

All compilation and runtime logs are archived in `integration-tests/target/archived-logs` **including** the exact verbatim
commands used to run the compilation. One can easily retrace the steps the test suite took to compile and run particular code example or snippet. 

## Tests' tags and organization
Test classes and test methods can by annotated with tags so as we can run just selected subsets. Those tags then must be explicitly included in [pom.xml](./pom.xml). See:

```xml
...
<properties>
...
<includeTags>simple-apps,snippets</includeTags>
<excludeTags>none</excludeTags>
...
</properties>
``` 

This is an alternative way to run just the `snippets` tests and nothing else:

```
mvn verify -pl integration-tests -DexcludeTags=all -DincludeTags=snippets 
```

---
Happy testing 🔥
