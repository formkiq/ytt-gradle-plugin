/**
 * MIT License
 * 
 * Copyright (c) 2018 - 2025 FormKiQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.formkiq.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Functional Tests.
 */
public class YttPluginFunctionalTest {

  private static Path createFakeYttExecutable(final Path dir) throws IOException {
    boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
    Path exe = dir.resolve(isWindows ? "fake-ytt.cmd" : "fake-ytt.sh");
    String script;

    if (isWindows) {
      script = """
          @echo off\r
          REM Fake ytt: just print banner and the args\r
          echo FAKE_YTT START\r
          echo args: %*\r
          echo FAKE_YTT END\r
          """;
    } else {
      script = """
          #!/usr/bin/env bash
          set -euo pipefail
          echo FAKE_YTT START
          echo args: "$@"
          echo FAKE_YTT END
          """;
    }

    Files.writeString(exe, script, StandardCharsets.UTF_8);
    if (!isWindows) {
      exe.toFile().setExecutable(true);
    }
    return exe;
  }

  /**
   * Minimal escaping so Windows paths and spaces survive inside the Groovy string literal.
   * 
   * @param p {@link Path}
   * @return {@link String}
   */
  private static String escapeForGroovy(final Path p) {
    return p.toAbsolutePath().toString().replace("\\", "\\\\");
  }

  /** Test Project Dir. */
  @TempDir
  Path testProjectDir;
  /** Build File. */
  Path buildFile;
  /** Settings File. */
  Path settingsFile;
  /** Resource Dir. */
  Path resourcesDir;
  /** Fake Ytt. */
  Path fakeYtt;

  // --- helpers ---

  @Test
  void firstRunProducesOutput_thenUpToDate_thenRerunsOnChange() throws IOException {
    // 1) First run: expect SUCCESS and output file created
    BuildResult result1 = GradleRunner.create().withProjectDir(testProjectDir.toFile())
        .withPluginClasspath().withArguments("yttRenderAll", "--stacktrace").build();

    assertThat(result1.task(":yttRender_api").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    Path out = testProjectDir.resolve("build/distributions/api/api.yaml");
    assertThat(out).exists();
    String content1 = Files.readString(out);
    assertThat(content1).contains("FAKE_YTT").contains("api.yaml").contains("openapi-jwt.yaml");

    // 2) Second run: no changes → UP_TO_DATE
    BuildResult result2 = GradleRunner.create().withProjectDir(testProjectDir.toFile())
        .withPluginClasspath().withArguments("yttRenderAll").build();

    assertThat(result2.task(":yttRender_api").getOutcome()).isEqualTo(TaskOutcome.UP_TO_DATE);

    // 3) Touch an input file → expect SUCCESS (re-run)
    Path apiYaml = resourcesDir.resolve("api.yaml");
    Files.writeString(apiYaml, "api: v2\n", StandardCharsets.UTF_8,
        StandardOpenOption.TRUNCATE_EXISTING);

    BuildResult result3 = GradleRunner.create().withProjectDir(testProjectDir.toFile())
        .withPluginClasspath().withArguments("yttRenderAll").build();

    assertThat(result3.task(":yttRender_api").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    String content3 = Files.readString(out);
    assertThat(content3).contains("FAKE_YTT");

    // 4) Change a data value → expect SUCCESS (re-run)
    // Update build.gradle to change version
    String updated = Files.readString(buildFile).replace(
        "defaultDataValues.put('version', '1.0.0')", "defaultDataValues.put('version', '2.0.0')");
    Files.writeString(buildFile, updated, StandardCharsets.UTF_8,
        StandardOpenOption.TRUNCATE_EXISTING);

    BuildResult result4 = GradleRunner.create().withProjectDir(testProjectDir.toFile())
        .withPluginClasspath().withArguments("yttRenderAll").build();

    assertThat(result4.task(":yttRender_api").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
  }

  @BeforeEach
  void setup() throws IOException {
    // Basic Gradle project
    settingsFile = testProjectDir.resolve("settings.gradle");
    Files.writeString(settingsFile, "rootProject.name = 'sample-ytt-consumer'\n",
        StandardCharsets.UTF_8);

    buildFile = testProjectDir.resolve("build.gradle");
    resourcesDir = testProjectDir.resolve("src/main/resources/cloudformation");
    Files.createDirectories(resourcesDir);

    // Create minimal input templates
    Files.writeString(resourcesDir.resolve("api.yaml"), "api: v1\n", StandardCharsets.UTF_8);
    Files.writeString(resourcesDir.resolve("openapi-jwt.yaml"), "jwt: enabled\n",
        StandardCharsets.UTF_8);

    // Create a fake ytt executable that prints a deterministic banner and the list of -f files
    fakeYtt = createFakeYttExecutable(testProjectDir);

    // Build script that applies plugin from classpath and configures a spec
    String buildScript = "plugins {\n" + "  id 'java'\n"
        + "  // Because GradleRunner.withPluginClasspath() + gradlePlugin { testSourceSets ... }\n"
        + "  // the plugin is available here without a version:\n"
        + "  id 'com.formkiq.gradle.ytt'\n" + "}\n" + "\n"
        + "def apiDistDir = layout.buildDirectory.dir('distributions/api')\n" + "ytt {\n"
        + "  yttExecutable = file('" + escapeForGroovy(fakeYtt) + "').absolutePath\n"
        + "  outputDir = apiDistDir\n" + "  defaultDataValues.put('version', '1.0.0')\n"
        + "  specs {\n" + "    api {\n"
        + "      from('src/main/resources/cloudformation/api.yaml',\n"
        + "           'src/main/resources/cloudformation/openapi-jwt.yaml')\n"
        + "      into('api.yaml')\n" + "    }\n" + "  }\n" + "}\n";
    Files.writeString(buildFile, buildScript, StandardCharsets.UTF_8);
  }
}
