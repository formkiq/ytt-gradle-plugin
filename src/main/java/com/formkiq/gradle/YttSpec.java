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

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.file.RegularFileProperty;

import java.util.Arrays;

/**
 * Ytt Spec.
 */
public class YttSpec {

  /** Ytt Name. */
  private final String name;
  /** Hash. */
  private final Property<String> hash;
  /** Input Files. */
  private final ConfigurableFileCollection inputFiles;
  /** absolute file target. */
  private final RegularFileProperty outputFile;
  /** Data values. */
  private final MapProperty<String, String> dataValues;
  /** file name relative to extension.outputDir (copy-like "into"). */
  private final Property<String> intoFileName;

  /**
   * constructor.
   * 
   * @param yttName {@link String}
   * @param objects {@link ObjectFactory}
   */
  public YttSpec(final String yttName, final ObjectFactory objects) {
    this.name = yttName;
    this.hash = objects.property(String.class);
    this.inputFiles = objects.fileCollection();
    this.outputFile = objects.fileProperty();
    this.dataValues = objects.mapProperty(String.class, String.class);
    this.intoFileName = objects.property(String.class);
  }

  /**
   * Files.
   * 
   * @param paths {@link Object}
   */
  public void files(final Object... paths) {
    from(paths);
  }

  /**
   * From.
   *
   * @param paths {@link Object}
   */
  public void from(final Object... paths) {
    inputFiles.from(Arrays.asList(paths));
  }

  /**
   * Get Data Values.
   * 
   * @return {@link MapProperty}
   */
  public MapProperty<String, String> getDataValues() {
    return dataValues;
  }

  /**
   * Get Hash.
   * 
   * @return {@link Property}
   */
  public Property<String> getHash() {
    return hash;
  }

  /**
   * Get Input Files.
   * 
   * @return {@link ConfigurableFileCollection}
   */
  public ConfigurableFileCollection getInputFiles() {
    return inputFiles;
  }

  /**
   * Get Into File Name.
   *
   * @return {@link Property}
   */
  public Property<String> getIntoFileName() {
    return intoFileName;
  }

  /**
   * Get Name.
   * 
   * @return {@link String}
   */
  public String getName() {
    return name;
  }

  /**
   * Get Output File.
   * 
   * @return {@link RegularFileProperty}
   */
  public RegularFileProperty getOutputFile() {
    return outputFile;
  }

  /**
   * Set Hash.
   * 
   * @param mode {@link String}
   */
  public void hash(final String mode) {
    this.hash.set(mode);
  }

  /**
   * Set the output file *name* (relative to extension.outputDir).
   * 
   * @param fileName {@link String}
   */
  public void into(final String fileName) {
    this.intoFileName.set(fileName);
  }
}
