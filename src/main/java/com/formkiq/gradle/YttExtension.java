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

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.NamedDomainObjectContainer;

import javax.inject.Inject;

/**
 * Ytt Extension.
 */
public class YttExtension {

  /** Ytt Executable. */
  private final Property<String> yttExecutable;
  /** Default Data Values. */
  private final MapProperty<String, String> defaultDataValues;
  /** Output Directory. */
  private final DirectoryProperty outputDir;
  /** {@link com.formkiq.gradle.YttSpec}. */
  private final NamedDomainObjectContainer<YttSpec> specs;

  /**
   * Constructor.
   * 
   * @param objects {@link ObjectFactory}
   */
  @Inject
  public YttExtension(final ObjectFactory objects) {
    this.yttExecutable = objects.property(String.class).convention("ytt");
    this.defaultDataValues = objects.mapProperty(String.class, String.class);
    this.outputDir = objects.directoryProperty();
    this.specs = objects.domainObjectContainer(YttSpec.class, name -> new YttSpec(name, objects));
  }

  /**
   * Get Default Data Values.
   * 
   * @return {@link MapProperty}
   */
  public MapProperty<String, String> getDefaultDataValues() {
    return defaultDataValues;
  }

  /**
   * Get {@link DirectoryProperty}.
   * 
   * @return {@link DirectoryProperty}
   */
  public DirectoryProperty getOutputDir() {
    return outputDir;
  }

  /**
   * Get {@link NamedDomainObjectContainer}.
   * 
   * @return {@link NamedDomainObjectContainer}
   */
  public NamedDomainObjectContainer<YttSpec> getSpecs() {
    return specs;
  }

  /**
   * Get Ytt Executable.
   * 
   * @return {@link Property}
   */
  public Property<String> getYttExecutable() {
    return yttExecutable;
  }
}
