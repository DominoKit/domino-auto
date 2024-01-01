/*
 * Copyright © 2019 Dominokit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dominokit.auto;

import static java.util.Objects.nonNull;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
public class DominoAutoProcessor extends AbstractProcessor implements HasProcessorEnv {

  private ProcessingEnvironment env;
  private SourceUtil sourceUtil;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.env = processingEnv;
    this.sourceUtil = new SourceUtil(this);
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return new HashSet<>(Arrays.asList("*"));
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    try {

      Set<? extends Element> dominoAutoElements =
          roundEnv.getElementsAnnotatedWith(DominoAuto.class);

      Set<String> includes = new HashSet<>();
      Set<String> exclude = new HashSet<>();

      if (options().containsKey("dominoAutoInclude")) {
        includes.addAll(
            Arrays.stream(options().get("dominoAutoInclude").split(","))
                .collect(Collectors.toSet()));
      }

      if (options().containsKey("dominoAutoExclude")) {
        exclude.addAll(
            Arrays.stream(options().get("dominoAutoExclude").split(","))
                .collect(Collectors.toSet()));
      }

      dominoAutoElements.forEach(
          element -> {
            includes.addAll(Arrays.asList(element.getAnnotation(DominoAuto.class).include()));
            exclude.addAll(Arrays.asList(element.getAnnotation(DominoAuto.class).exclude()));
          });

      Map<String, Set<String>> services = new HashMap<>();

      try (ScanResult scanResult =
          new ClassGraph().acceptPathsNonRecursive("META-INF/services").scan()) {
        scanResult
            .getAllResources()
            .forEachByteArrayThrowingIOException(
                (Resource res, byte[] content) -> {
                  String serviceName = res.getPath().replace("META-INF/services/", "");

                  if (includes.stream().anyMatch(serviceName::startsWith)
                      && exclude.stream().noneMatch(serviceName::startsWith)) {
                    if (!services.containsKey(serviceName)) {
                      services.put(serviceName, new HashSet<>());
                    }
                    Stream<String> impls = new String(content, StandardCharsets.UTF_8).lines();
                    impls
                        .filter(
                            impl ->
                                nonNull(impl) && !impl.trim().isEmpty() && !impl.startsWith("#"))
                        .forEach(
                            impl -> {
                              services.get(serviceName).add(impl);
                            });
                  }
                });
      }
      writeServiceLoaders(services);
    } catch (Exception ex) {
      SourceUtil.errorStackTrace(env.getMessager(), ex);
      env.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to generate service loaders.");
    }
    return false;
  }

  private void writeServiceLoaders(Map<String, Set<String>> services) {
    services.forEach(
        (key, impls) -> {
          CodeBlock.Builder bodyBuilder = CodeBlock.builder();
          bodyBuilder.addStatement(
              "$T<$T> services = new $T()", List.class, ClassName.bestGuess(key), ArrayList.class);
          impls.stream()
              .forEach(
                  impl -> {
                    env.getMessager()
                        .printMessage(Diagnostic.Kind.WARNING, "Adding service entry : " + impl);
                    bodyBuilder.addStatement("services.add(new $T())", ClassName.bestGuess(impl));
                  });

          bodyBuilder.addStatement("return services");
          TypeSpec classSpec =
              TypeSpec.classBuilder(getClassName(key) + "_ServiceLoader")
                  .addModifiers(Modifier.PUBLIC)
                  .addMethod(
                      MethodSpec.methodBuilder("load")
                          .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                          .returns(
                              ParameterizedTypeName.get(
                                  ClassName.get(List.class), ClassName.bestGuess(key)))
                          .addCode(bodyBuilder.build())
                          .build())
                  .build();

          try {
            JavaFile.builder(getPackageName(key), classSpec)
                .build()
                .writeTo(processingEnv.getFiler());
          } catch (Exception e) {
            messager()
                .printMessage(
                    Diagnostic.Kind.WARNING, "Failed to write service loader : " + e.getMessage());
          }
        });
  }

  private String getPackageName(String qualifiedName) {
    int lastDotIndex = qualifiedName.lastIndexOf('.');
    // Check for default package or no dot present
    if (lastDotIndex == -1) {
      return "";
    }
    return qualifiedName.substring(0, lastDotIndex);
  }

  private String getClassName(String qualifiedName) {
    int lastDotIndex = qualifiedName.lastIndexOf('.');
    // Check if no dot is present, meaning the entire string is the class name
    if (lastDotIndex == -1) {
      return qualifiedName;
    }
    return qualifiedName.substring(lastDotIndex + 1);
  }

  @Override
  public Types types() {
    return env.getTypeUtils();
  }

  @Override
  public Elements elements() {
    return env.getElementUtils();
  }

  @Override
  public Messager messager() {
    return env.getMessager();
  }

  @Override
  public Filer getFiler() {
    return env.getFiler();
  }

  @Override
  public Map<String, String> options() {
    return env.getOptions();
  }
}
