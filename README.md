# YTT Gradle Plugin

A Gradle plugin that integrates Carvel ytt (YAML Templating Tool) into your build.
It lets you declaratively render YAML templates as part of your Gradle workflow —
no more ad-hoc exec { } blocks or shell redirections.


## Features

- Cross-platform: works on macOS, Linux, and Windows (no bash/cmd hacks).
- Declarative DSL: use from(...) and into(...) just like the Gradle Copy task.
- Automatic hash generation: a random SHA-256 hash value is generated per build if not provided.
- Incremental builds: up-to-date checks for input templates and data values.
- Composable: define multiple specs (template renderings), all grouped under yttRenderAll.
- Substitute mode: replace data value expressions without consuming ytt overlay annotations.


## Installation

Publish the plugin to your local Maven repository:

  ./gradlew publishToMavenLocal

Then apply it in your Gradle project:

  plugins {
    id 'com.formkiq.gradle.ytt' version '1.0.0'
  }

Alternatively, you can include it directly with a composite build:

  plugins {
    id 'com.formkiq.gradle.ytt'
  }

  includeBuild("../ytt-gradle-plugin")


## Usage

In your `build.gradle`:

```groovy
ytt {
  outputDir = layout.buildDirectory.dir("build/mydir") // OPTIONAL, defaults to build/ytt
  defaultDataValues.put("version", project.version.toString()) // OPTIONAL data values

  specs {
    api {
      from("src/main/resources/cloudformation/api.yaml",
           "src/main/resources/cloudformation/openapi-jwt.yaml")
      into("api.yaml")
    }
  }
}

tasks.named("assemble").configure {
  dependsOn("yttRenderAll")
}
```

Run all renderings:

```shell
./gradlew yttRenderAll
```

Or run a single spec:

```shell
./gradlew yttRender_api
```

## Substitute Mode

Use `renderMode "substitute"` when you need to replace `#@ data.values.*` expressions but keep the rest of the file as ytt source. This is useful for generated overlay files where normal ytt rendering would consume `#@overlay` annotations.

```groovy
ytt {
  specs {
    apiTagschemaLambda {
      from("src/main/resources/cloudformation/api/module-extra-tagschema-api-lambda.yaml")
      into("api/module-extra-tagschema-api-lambda.yaml")
      hash "sha256"
      renderMode "substitute"
    }
  }
}
```

Given an input line like:

```yaml
AutoPublishCodeSha256: #@ data.values.hash or assert.fail("missing version")
```

The output will contain a generated SHA-256 value:

```yaml
AutoPublishCodeSha256: 8c9f0d...
```

Other ytt annotations, including `#@ load(...)`, `#@overlay/match`, and `#@overlay/replace`, are preserved.

Substitute mode has two important constraints:

- It accepts exactly one input file.
- It only replaces inline `#@ data.values.<key>` expressions, optionally followed by `or assert.fail(...)`; it does not evaluate arbitrary ytt/Starlark.


## Configuration Options

Top-level ytt extension
- outputDir: Base directory for rendered outputs.
- yttExecutable: Path to the ytt binary (default: "ytt" on your PATH).
- defaultDataValues: Global --data-value key=value passed to all specs.

Each spec
- `from(...)`: One or more YAML template files to include for normal ytt rendering. Substitute mode requires exactly one file.
- `into("file.yaml")`: Output file name relative to `ytt.outputDir`.
- `outputFile.set(file)`: Alternative to `into(...)` if you want a fully qualified path.
- `dataValues.put("key", "value")`: Extra `--data-value` options for this spec.
- `hash "sha256"`: Generate a random SHA-256 value and expose it as `data.values.hash`.
- `renderMode "ytt"`: Default. Invoke ytt and write rendered output.
- `renderMode "substitute"`: Do not invoke ytt. Copy one input file and replace inline `#@ data.values.key` expressions with data values, preserving other ytt annotations.


## Requirements

- Carvel ytt installed and available in your PATH.
- Gradle 7.6+ (tested with Gradle 8+).
- Java 17+ (plugin is built with toolchains).


## Development

Clone and build the plugin:

  git clone https://github.com/your-org/ytt-gradle-plugin.git
  cd ytt-gradle-plugin
  ./gradlew build

Run functional tests (Gradle TestKit):

  ./gradlew functionalTest

Publish locally:

  ./gradlew publishToMavenLocal


## License

Apache License 2.0
