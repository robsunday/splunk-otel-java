/*
 * Copyright Splunk Inc.
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

package com.splunk.opentelemetry.opamp;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationBuilder;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeclarativeEffectiveConfigFileFactoryTest {

  @AfterEach
  void afterEach() {
    DeclarativeConfigurationInterceptor.reset();
  }

  @Test
  void createFileSerializesDeclarativeConfigAsYaml(@TempDir Path tempDir) throws Exception {
    // given
    Path configFile = tempDir.resolve("declarative-config.yaml");
    Files.writeString(
        configFile,
        """
            file_format: 1.0
            disabled: true
            resource:
              attributes:
                - name: service.name
                  value: ${OTEL_SERVICE_NAME}
            """,
        UTF_8);

    // Environment variables cannot be set in current process, so a new process must be launched.
    ProcessBuilder processBuilder =
        new ProcessBuilder(
                System.getProperty("java.home") + "/bin/java",
                "-cp",
                System.getProperty("java.class.path"),
                "-Dotel.config.file=" + configFile,
                FactoryRunner.class.getName())
            .redirectErrorStream(true);
    processBuilder.environment().put("OTEL_SERVICE_NAME", "test-service");

    // when
    Process process = processBuilder.start();
    boolean processExecutionStatus = process.waitFor(10, TimeUnit.SECONDS);

    // then
    assertThat(processExecutionStatus).isTrue();
    String yaml = new String(process.getInputStream().readAllBytes(), UTF_8);
    assertThat(process.exitValue()).describedAs(yaml).isZero();

    OpenTelemetryConfigurationModel model =
        DeclarativeConfiguration.parse(new ByteArrayInputStream(yaml.getBytes(UTF_8)));
    assertThat(model.getFileFormat()).isEqualTo("1.0");
    assertThat(model.getDisabled()).isTrue();
    assertThat(model.getResource()).isNotNull();
    assertThat(model.getResource().getAttributes()).hasSize(1);
    assertThat(model.getResource().getAttributes().get(0).getName()).isEqualTo("service.name");
    assertThat(model.getResource().getAttributes().get(0).getValue()).isEqualTo("test-service");
  }

  private static class FactoryRunner {
    public static void main(String[] args) throws Exception {
      Path configFile = Path.of(System.getProperty("otel.config.file"));
      try (InputStream inputStream = Files.newInputStream(configFile)) {
        OpenTelemetryConfigurationModel model = DeclarativeConfiguration.parse(inputStream);
        DeclarativeConfigurationBuilder builder = new DeclarativeConfigurationBuilder();
        new DeclarativeConfigurationInterceptor().customize(builder);
        builder.customizeModel(model);
      }

      System.out.print(new DeclarativeEffectiveConfigFileFactory().createFile().body.utf8());
    }
  }
}
