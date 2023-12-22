/*
 * #%L
 * gwt-websockets-processor
 * %%
 * Copyright (C) 2011 - 2018 Vertispan LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.dominokit.auto;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Sets;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

public class DominoAutoProcessorStep implements BasicAnnotationProcessor.Step, HasProcessorEnv {

  private final ProcessingEnvironment processingEnv;
  private final SourceUtil sourceUtil;

  public DominoAutoProcessorStep(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
    this.sourceUtil = new SourceUtil(this);
  }

  @Override
  public Set<String> annotations() {
    return new HashSet<>(Arrays.asList(DominoAuto.class.getCanonicalName()));
  }

  @Override
  public Set<? extends Element> process(
      ImmutableSetMultimap<String, Element> elementsByAnnotation) {
    elementsByAnnotation
        .get(DominoAuto.class.getCanonicalName())
        .forEach(
            element -> {
              sourceUtil
                  .getClassValueFromAnnotation(element, DominoAuto.class, "value")
                  .ifPresent(
                      typeMirror -> {
                        CodeBlock.Builder bodyBuilder = CodeBlock.builder();
                        bodyBuilder.addStatement(
                            "$T<$T> services = new $T()", List.class, typeMirror, ArrayList.class);

                        ServiceLoader.load(
                                DominoAutoService.class,
                                DominoAutoProcessorStep.class.getClassLoader())
                            .stream()
                            .map(ServiceLoader.Provider::get)
                            .filter(
                                dominoAutoService ->
                                    sourceUtil.isAssignableFrom(
                                        typeMirror, dominoAutoService.getClass()))
                            .forEach(
                                dominoAutoService -> {
                                  bodyBuilder.addStatement(
                                      "services.add(new $T())",
                                      ClassName.get(dominoAutoService.getClass()));
                                });

                        bodyBuilder.addStatement("return services");
                        TypeSpec classSpec =
                            TypeSpec.classBuilder(
                                    element.getSimpleName().toString() + "_ServiceLoader")
                                .addModifiers(Modifier.PUBLIC)
                                .addMethod(
                                    MethodSpec.methodBuilder("load")
                                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                        .returns(
                                            ParameterizedTypeName.get(
                                                ClassName.get(List.class),
                                                ClassName.get(typeMirror)))
                                        .addCode(bodyBuilder.build())
                                        .build())
                                .build();

                        try {
                          JavaFile.builder(
                                  elements().getPackageOf(element).getQualifiedName().toString(),
                                  classSpec)
                              .build()
                              .writeTo(processingEnv.getFiler());
                        } catch (IOException e) {
                          processingEnv
                              .getMessager()
                              .printMessage(
                                  Diagnostic.Kind.ERROR, "Failed to write service loader", element);
                        }
                      });
            });

    return Sets.newHashSet();
  }

  @Override
  public Types types() {
    return processingEnv.getTypeUtils();
  }

  @Override
  public Elements elements() {
    return processingEnv.getElementUtils();
  }

  @Override
  public Messager messager() {
    return processingEnv.getMessager();
  }
}
