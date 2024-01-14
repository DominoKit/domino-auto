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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

public class SourceUtil {

  private final HasProcessorEnv env;

  public SourceUtil(HasProcessorEnv env) {
    this.env = env;
  }

  /**
   * gets an annotation on a specified {@link Element} and return the value from the specified
   * parameter, where the parameter value is a {@link Class}
   *
   * @param element classElement the {@link Element} to be checked
   * @param annotation {@link Class} the represent the annotation
   * @param paramName The annotation member parameter that holds the value
   * @return the {@link Optional} of {@link TypeMirror} that represent the value.
   */
  public Optional<TypeMirror> getClassValueFromAnnotation(
      Element element, Class<? extends Annotation> annotation, String paramName) {
    for (AnnotationMirror am : element.getAnnotationMirrors()) {
      if (env.types()
          .isSameType(
              am.getAnnotationType(),
              env.elements().getTypeElement(annotation.getCanonicalName()).asType())) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
            am.getElementValues().entrySet()) {
          if (paramName.equals(entry.getKey().getSimpleName().toString())) {
            AnnotationValue annotationValue = entry.getValue();
            return Optional.of((DeclaredType) annotationValue.getValue());
          }
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Finds a list of type mirrors for classes defined as an annotation parameter.
   *
   * <p>For example:
   *
   * <pre>
   * interface &#64;MyAnnotation {
   *  Class&#60;?&#62;[] myClasses();
   * }
   * </pre>
   *
   * <p>
   *
   * @param element the element
   * @param annotation the annotation
   * @param paramName the class parameter name
   * @return The list of type mirrors for the classes, empty list otherwise
   */
  public List<TypeMirror> getClassArrayValueFromAnnotation(
      Element element, Class<? extends Annotation> annotation, String paramName) {

    List<TypeMirror> values = new ArrayList<>();

    for (AnnotationMirror am : element.getAnnotationMirrors()) {
      if (env.types()
          .isSameType(
              am.getAnnotationType(),
              env.elements().getTypeElement(annotation.getCanonicalName()).asType())) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
            am.getElementValues().entrySet()) {
          if (paramName.equals(entry.getKey().getSimpleName().toString())) {
            List<AnnotationValue> classesTypes =
                (List<AnnotationValue>) entry.getValue().getValue();
            Iterator<? extends AnnotationValue> iterator = classesTypes.iterator();

            while (iterator.hasNext()) {
              AnnotationValue next = iterator.next();
              values.add((TypeMirror) next.getValue());
            }
          }
        }
      }
    }
    return values;
  }

  /**
   * isAssignableFrom. checks if a specific {@link TypeMirror} is assignable from a specific {@link
   * java.lang.Class}.
   *
   * @param typeMirror a {@link javax.lang.model.type.TypeMirror} object.
   * @param targetClass a {@link java.lang.Class} object.
   * @return a boolean.
   */
  public boolean isAssignableFrom(TypeMirror typeMirror, Class<?> targetClass) {
    return env.types()
        .isAssignable(
            env.types()
                .getDeclaredType(env.elements().getTypeElement(targetClass.getCanonicalName())),
            typeMirror);
  }

  public static void errorStackTrace(Messager messager, Exception e) {
    StringWriter out = new StringWriter();
    e.printStackTrace(new PrintWriter(out));
    messager.printMessage(
        Diagnostic.Kind.ERROR, "error while creating source file " + out.getBuffer().toString());
  }

  public static void warningStackTrace(Messager messager, Exception e) {
    StringWriter out = new StringWriter();
    e.printStackTrace(new PrintWriter(out));
    messager.printMessage(
        Diagnostic.Kind.WARNING, "error while creating source file " + out.getBuffer().toString());
  }

  public static String errorStackTrace(Exception e) {
    StringWriter out = new StringWriter();
    e.printStackTrace(new PrintWriter(out));
    return out.getBuffer().toString();
  }
}
