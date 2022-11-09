# Integration test suite

## TL;DR:
Make sure you have both [qbicc](https://github.com/qbicc/qbicc) and [qbicc-class-library](https://github.com/qbicc/qbicc-class-library) installed.

```
$ mvn verify
```

The command compiles and runs all `.java` classes and their `.pattern` expected outputs.

All those classes are compiled, executables run and their output is matched with the corresponding regexp pattern.

Build logs, run logs and resulting binaries alongside their `.ll` and `.dot` files are archived in `integration-tests/target/archived-logs/` directory.

All logs are scanned for error and warning messages. Messages that were not allowlisted are treated as test errors. 

## Testing

The test suite aims at two main use cases:
1. Providing an easy, dead simple way to test snippets of code exploiting particular language features.
2. Keeping example apps and test apps working. Enabling us to play with packages, multiple classes, specific compilation flags etc.

### Snippets

#### Adding a snippet
 * Create a `.java` file correctly named so as its name is the same as the class within it containing the entrypoint `main`. E.g. `MyCodeFeature.java`.
 * Create a `.pattern` file of the same name. The string within the file will be compiled as `java.util.regex.Pattern` and used to test the program output. Nothing else is needed, the test suite will pick the file up automatically.

Snippets can be organized in subdirectories. The semantic is just organization, not package names. All snippets are in the `org.qbicc.tests.snippets` package.

## Tests' tags and organization
Test classes and test methods can by annotated with tags so as we can run just selected subsets. Those tags then must be explicitly included in [pom.xml](./pom.xml). See:

```xml
...
<properties>
...
<failsafeTags>snippets</failsafeTags>
<surefireTags>none</surefireTags>
...
</properties>
``` 

---
Happy testing 🔥
