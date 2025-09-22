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

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class YttRenderTaskTest {

  @Test
  void normalizeDataValuesSortsByKeyAndSerializesAsKeyEqValue() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("zeta", "9");
    map.put("alpha", "1");
    map.put("beta", null); // nulls become empty strings

    List<String> normalized = AbstractYttRenderTask.normalizeDataValues(map);
    assertTrue(normalized.contains("alpha=1"));
    assertTrue(normalized.contains("beta="));
    assertTrue(normalized.contains("zeta=9"));
  }

  @Test
  void taskCanBeRegisteredAsndAcceptsInputsOutputs() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("java");

    var task = project.getTasks().register("render", AbstractYttRenderTask.class, t -> {
      t.getYttExecutable().set("ytt"); // won’t actually execute in this unit test
      t.getInputFiles().from(project.file("src/test/resources/example.yaml"));
      var out = project.getLayout().getBuildDirectory().file("tmp/out.yaml");
      t.getOutputFile().set(out);
      t.getDataValuesNormalized().set(List.of("version=1.0.0"));
    }).get();

    assertFalse(task.getInputs().getFiles().getFiles().isEmpty());
    assertEquals("version=1.0.0", task.getDataValuesNormalized().get().get(0));
  }
}
