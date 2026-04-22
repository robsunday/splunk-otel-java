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

import static com.splunk.opentelemetry.opamp.EnvVarsEffectiveConfigFileFactory.toEnvVarName;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class EnvVarsEffectiveConfigFileFactoryTest {

  @Test
  void buildFileContent_reportsConfiguredValues() throws IOException {
    Properties fileContent =
        loadProperties(
            Map.of(
                "splunk.profiler.enabled", "true",
                "splunk.profiler.memory.enabled", "true",
                "splunk.snapshot.profiler.enabled", "true",
                "splunk.snapshot.sampling.interval", "26ms",
                "splunk.profiler.call.stack.interval", "1235ms",
                "otel.exporter.otlp.endpoint", "https://base.example.com",
                "otel.exporter.otlp.traces.endpoint", "https://traces.example.com",
                "otel.exporter.otlp.metrics.endpoint", "https://metrics.example.com",
                "otel.exporter.otlp.logs.endpoint", "https://logs.example.com",
                "otel.service.name", "checkout"));

    assertProperties(
        fileContent,
        Map.of(
            "splunk.profiler.enabled", "true",
            "splunk.profiler.memory.enabled", "true",
            "splunk.snapshot.profiler.enabled", "true",
            "splunk.snapshot.sampling.interval", "26ms",
            "splunk.profiler.call.stack.interval", "1235ms",
            "otel.exporter.otlp.traces.endpoint", "https://traces.example.com",
            "otel.exporter.otlp.metrics.endpoint", "https://metrics.example.com",
            "otel.exporter.otlp.logs.endpoint", "https://logs.example.com",
            "otel.service.name", "checkout"));
    assertThat(fileContent.size()).isEqualTo(9);
  }

  @Test
  void buildFileContent_reportsDefaultValuesWhenNotConfigured() throws IOException {
    Properties fileContent = loadProperties(Map.of());

    assertProperties(
        fileContent,
        Map.of(
            "splunk.profiler.enabled", "false",
            "splunk.profiler.memory.enabled", "false",
            "splunk.snapshot.profiler.enabled", "false",
            "splunk.snapshot.sampling.interval", "10ms",
            "splunk.profiler.call.stack.interval", "10000ms",
            "otel.exporter.otlp.traces.endpoint", "http://localhost:4318/v1/traces",
            "otel.exporter.otlp.metrics.endpoint", "http://localhost:4318/v1/metrics",
            "otel.exporter.otlp.logs.endpoint", "http://localhost:4318/v1/logs",
            "otel.service.name", ""));
    assertThat(fileContent.size()).isEqualTo(9);
  }

  @Test
  void buildFileContent_appendsSignalPathsToBaseHttpProtobufEndpoint() throws IOException {
    Properties fileContent =
        loadProperties(Map.of("otel.exporter.otlp.endpoint", "https://collector:4318"));

    assertProperties(
        fileContent,
        Map.of(
            "otel.exporter.otlp.traces.endpoint", "https://collector:4318/v1/traces",
            "otel.exporter.otlp.metrics.endpoint", "https://collector:4318/v1/metrics",
            "otel.exporter.otlp.logs.endpoint", "https://collector:4318/v1/logs"));
  }

  @Test
  void buildFileContent_usesBaseGrpcEndpointForAllSignals() throws IOException {
    Properties fileContent =
        loadProperties(
            Map.of(
                "otel.exporter.otlp.endpoint", "https://collector:4317",
                "otel.exporter.otlp.protocol", "grpc"));

    assertProperties(
        fileContent,
        Map.of(
            "otel.exporter.otlp.traces.endpoint", "https://collector:4317",
            "otel.exporter.otlp.metrics.endpoint", "https://collector:4317",
            "otel.exporter.otlp.logs.endpoint", "https://collector:4317"));
  }

  @Test
  void buildFileContent_usesSignalSpecificProtocolWhenResolvingEndpoints() throws IOException {
    Properties fileContent =
        loadProperties(
            Map.of(
                "otel.exporter.otlp.endpoint", "https://collector:4317",
                "otel.exporter.otlp.traces.protocol", "grpc",
                "otel.exporter.otlp.metrics.protocol", "grpc",
                "otel.exporter.otlp.logs.protocol", "grpc"));

    assertProperties(
        fileContent,
        Map.of(
            "otel.exporter.otlp.traces.endpoint", "https://collector:4317",
            "otel.exporter.otlp.metrics.endpoint", "https://collector:4317",
            "otel.exporter.otlp.logs.endpoint", "https://collector:4317"));
  }

  private static String createFileContent(Map<String, String> configMap) {
    DefaultConfigProperties config = DefaultConfigProperties.createFromMap(configMap);
    return new EnvVarsEffectiveConfigFileFactory(config).buildFileContent();
  }

  private static Properties loadProperties(Map<String, String> configMap) throws IOException {
    Properties properties = new Properties();
    properties.load(new StringReader(createFileContent(configMap)));
    return properties;
  }

  private static void assertProperties(Properties fileContent, Map<String, String> expectedValues) {
    expectedValues.forEach(
        (propertyName, expectedValue) -> assertProperty(fileContent, propertyName, expectedValue));
  }

  private static void assertProperty(
      Properties fileContent, String propertyName, String expectedValue) {
    assertThat(fileContent.getProperty(toEnvVarName(propertyName))).isEqualTo(expectedValue);
  }
}
