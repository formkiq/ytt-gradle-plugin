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

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Gradle Ytt Plugin.
 */
public class YttPlugin implements Plugin<Project> {

  @Override
  public void apply(final Project project) {
    YttExtension ext = project.getExtensions().create("ytt", YttExtension.class);

    project.getTasks().register("yttRenderAll", t -> {
      t.setGroup("ytt");
      t.setDescription("Render all ytt templates");
    });

    project.afterEvaluate(p -> ext.getSpecs().all(spec -> {
      String taskName = "yttRender_" + spec.getName();

      var tp = project.getTasks().register(taskName, AbstractYttRenderTask.class, t -> {
        t.setGroup("ytt");
        t.setDescription("Render ytt spec '" + spec.getName() + "'");
        t.getYttExecutable().set(ext.getYttExecutable());
        t.getInputFiles().from(spec.getInputFiles());

        // Merge default + spec data-values
        t.getDataValues().putAll(ext.getDefaultDataValues());
        t.getDataValues().putAll(spec.getDataValues());

        // NEW: lazy input binding so changes invalidate up-to-date correctly
        t.getDataValuesNormalized()
            .set(t.getDataValues().map(AbstractYttRenderTask::normalizeDataValues));

        // Resolve output file:
        if (spec.getOutputFile().isPresent()) {
          // Absolute/explicit file set on spec
          t.getOutputFile().set(spec.getOutputFile());
        } else if (spec.getIntoFileName().isPresent()) {
          if (!ext.getOutputDir().isPresent()) {
            throw new IllegalArgumentException(
                "ytt.outputDir must be set to use spec.into(\"file.yaml\") for spec '"
                    + spec.getName() + "'.");
          }
          var resolved = ext.getOutputDir().file(spec.getIntoFileName().get());
          t.getOutputFile().set(resolved);
        } else {
          throw new IllegalArgumentException("No output configured for spec '" + spec.getName()
              + "'. Use spec.into(\"file.yaml\") or spec.outputFile.set(...).");
        }
      });

      project.getTasks().named("yttRenderAll").configure(task -> task.dependsOn(tp));
    }));
  }
}
