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

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
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
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    try {
      if (roundEnv.processingOver()) {

        Set<? extends Element> dominoAutoElements =
            roundEnv.getElementsAnnotatedWith(DominoAuto.class);

        Set<String> blackList = getBlackListedServices(dominoAutoElements);

        Map<String, Set<String>> services = new HashMap<>();

        // Load META-INF/services entries from the classpath
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = loader.getResources("META-INF/services");

        while (resources.hasMoreElements()) {
          URL url = resources.nextElement();

          try (InputStream is = url.openStream();
              BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
              if (!blackList.contains(line)) {
                URL service = loader.getResource("META-INF/services/" + line);
                assert service != null;
                try (InputStream serviceStream = service.openStream();
                    BufferedReader serviceReader =
                        new BufferedReader(new InputStreamReader(serviceStream))) {
                  String serviceLine;
                  while ((serviceLine = serviceReader.readLine()) != null) {
                    if (!services.containsKey(line)) {
                      services.put(line, new HashSet<>());
                    }
                    services.get(line).add(serviceLine);
                  }
                }
              }
            }
          }
        }

        writeServiceLoaders(services);
      }
    } catch (Exception ex) {
      env.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to generate service loaders.");
    }
    return false;
  }

  private Set<String> getBlackListedServices(Set<? extends Element> dominoAutoElements) {
    Set<String> blackList = new HashSet<>();
    blackList.add(Processor.class.getCanonicalName());

    dominoAutoElements.forEach(
        element ->
            sourceUtil
                .getClassArrayValueFromAnnotation(element, DominoAuto.class, "blackList")
                .forEach(
                    typeMirror ->
                        blackList.add(env.getTypeUtils().erasure(typeMirror).toString())));
    return blackList;
  }

  private void writeServiceLoaders(Map<String, Set<String>> services) {
    services.forEach(
        (key, impls) -> {
          CodeBlock.Builder bodyBuilder = CodeBlock.builder();
          bodyBuilder.addStatement(
              "$T<$T> services = new $T()", List.class, ClassName.bestGuess(key), ArrayList.class);
          impls.forEach(
              impl -> {
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
          } catch (IOException e) {
            processingEnv
                .getMessager()
                .printMessage(
                    Diagnostic.Kind.ERROR,
                    "Failed to write service loader for service : [" + key + "]");
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
}
