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


import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Ytt Render Task.
 */
public abstract class AbstractYttRenderTask extends DefaultTask {

  /**
   * Called from plugin wiring to keep normalized list in sync with the map.
   * 
   * @param map {@link Map}
   * @return {@link List} {@link String}
   */
  public static List<String> normalizeDataValues(final Map<String, String> map) {
    if (map == null || map.isEmpty()) {
      return Collections.emptyList();
    }

    List<String> list = new ArrayList<>(map.size());
    map.entrySet().stream().sorted(Map.Entry.comparingByKey())
        .forEach(e -> list.add(e.getKey() + "=" + Objects.toString(e.getValue(), "")));
    return list;
  }

  private static Map<String, String> normalizedListToMap(final List<String> list) {
    Map<String, String> m = new LinkedHashMap<>();
    for (String kv : list) {
      int i = kv.indexOf('=');
      if (i < 0) {
        continue;
      }

      m.put(kv.substring(0, i), kv.substring(i + 1));
    }
    return m;
  }

  private static String randomAscii() {
    final int n = 10;
    Random r = new Random();
    final int fe = 48;
    final int sf = 75;
    StringBuilder sb = new StringBuilder(n);
    for (int i = 0; i < n; i++) {
      sb.append((char) (fe + r.nextInt(sf)));
    }

    return sb.toString();
  }

  private static String sha256Hex(final String txt) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] b = md.digest(txt.getBytes(StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder();
    for (byte x : b) {
      sb.append(String.format("%02x", x));
    }
    return sb.toString();
  }

  /**
   * Keep the raw map INTERNAL to avoid non-deterministic iteration issues.
   * 
   * @return {@link MapProperty}
   */
  @Internal
  public abstract MapProperty<String, String> getDataValues();

  /**
   * Deterministic, sorted representation used for incrementality.
   * 
   * @return {@link ListProperty}
   */
  @Input
  public abstract ListProperty<String> getDataValuesNormalized();

  /**
   * Hash.
   * 
   * @return {@link Property}
   */
  @Input
  @Optional
  public abstract Property<String> getHash();

  /**
   * Get Input Files.
   * 
   * @return {@link ConfigurableFileCollection}
   */
  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE) // ensure good cache keys across machines
  public abstract ConfigurableFileCollection getInputFiles();

  /**
   * Get Output File.
   * 
   * @return {@link RegularFileProperty}
   */
  @OutputFile
  public abstract RegularFileProperty getOutputFile();

  /**
   * Ytt Executable.
   * 
   * @return {@link Property}
   */
  @Input
  public abstract Property<String> getYttExecutable();

  /**
   * Run Ytt Task.
   * 
   * @throws Exception Exception
   */
  @TaskAction
  public void runYtt() throws Exception {
    // Rebuild a map from normalized form (sorted "k=v" pairs) for execution-time convenience
    Map<String, String> dv = new HashMap<>(normalizedListToMap(getDataValuesNormalized().get()));

    String hash = getHash().getOrElse(null);
    if ("sha256".equals(hash)) {
      dv.put("hash", sha256Hex(randomAscii()));
    }

    List<String> cmd = new ArrayList<>();
    cmd.add(getYttExecutable().get());

    dv.forEach((k, v) -> {
      cmd.add("--data-value");
      cmd.add(k + "=" + v);
    });

    getInputFiles().forEach(f -> {
      cmd.add("-f");
      cmd.add(f.getAbsolutePath());
    });

    getLogger().lifecycle("Running: {}", String.join(" ", cmd));

    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.redirectError(ProcessBuilder.Redirect.INHERIT);
    Process proc = pb.start();

    File outFile = getOutputFile().get().getAsFile();
    Path parent = outFile.toPath().getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }

    try (InputStream is = proc.getInputStream(); OutputStream os = new FileOutputStream(outFile)) {
      is.transferTo(os);
    }

    int exit = proc.waitFor();
    if (exit != 0) {
      throw new RuntimeException("ytt exited with code " + exit + " for " + outFile);
    }
  }
}
