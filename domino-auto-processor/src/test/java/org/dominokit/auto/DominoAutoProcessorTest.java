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

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class DominoAutoProcessorTest {

  @Test
  public void prefersSystemPropertyOverEnvironmentAndProcessorOption() {
    Set<String> includes =
        DominoAutoProcessor.resolveConfiguredPackages(
            DominoAutoProcessor.DOMINO_AUTO_INCLUDE,
            optionName ->
                DominoAutoProcessor.DOMINO_AUTO_INCLUDE.equals(optionName)
                    ? "com.example.vm, org.example.vm"
                    : null,
            optionName ->
                DominoAutoProcessor.DOMINO_AUTO_INCLUDE.equals(optionName)
                    ? "com.example.env"
                    : null,
            Map.of(DominoAutoProcessor.DOMINO_AUTO_INCLUDE, "com.example.option"));

    assertEquals(Set.of("com.example.vm", "org.example.vm"), includes);
  }

  @Test
  public void prefersEnvironmentOverProcessorOption() {
    Set<String> excludes =
        DominoAutoProcessor.resolveConfiguredPackages(
            DominoAutoProcessor.DOMINO_AUTO_EXCLUDE,
            optionName -> null,
            optionName ->
                DominoAutoProcessor.DOMINO_AUTO_EXCLUDE.equals(optionName)
                    ? "com.example.env, org.example.env"
                    : null,
            Map.of(DominoAutoProcessor.DOMINO_AUTO_EXCLUDE, "com.example.option"));

    assertEquals(Set.of("com.example.env", "org.example.env"), excludes);
  }

  @Test
  public void supportsUpperSnakeCaseEnvironmentVariableNames() {
    Set<String> includes =
        DominoAutoProcessor.resolveConfiguredPackages(
            DominoAutoProcessor.DOMINO_AUTO_INCLUDE,
            optionName -> null,
            optionName -> "DOMINO_AUTO_INCLUDE".equals(optionName) ? "com.example.env.upper" : null,
            Map.of(DominoAutoProcessor.DOMINO_AUTO_INCLUDE, "com.example.option"));

    assertEquals(Set.of("com.example.env.upper"), includes);
  }

  @Test
  public void fallsBackToProcessorOptionWhenSystemPropertyAndEnvironmentAreMissing() {
    Set<String> includes =
        DominoAutoProcessor.resolveConfiguredPackages(
            DominoAutoProcessor.DOMINO_AUTO_INCLUDE,
            optionName -> null,
            optionName -> null,
            Map.of(DominoAutoProcessor.DOMINO_AUTO_INCLUDE, "com.example.option"));

    assertEquals(Set.of("com.example.option"), includes);
  }
}
