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

import static com.splunk.opentelemetry.SplunkConfiguration.METRICS_FULL_COMMAND_LINE;
import static com.splunk.opentelemetry.SplunkConfiguration.PROFILER_ENABLED_PROPERTY;
import static com.splunk.opentelemetry.SplunkConfiguration.PROFILER_MEMORY_ENABLED_PROPERTY;
import static com.splunk.opentelemetry.SplunkConfiguration.SPLUNK_ACCESS_TOKEN;
import static com.splunk.opentelemetry.SplunkConfiguration.SPLUNK_REALM_PROPERTY;
import static com.splunk.opentelemetry.opamp.EnvVarsEffectiveConfigFileFactory.toEnvVarName;
import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getConfig;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class EnvVarsEffectiveConfigFileFactoryTest {
  private static final String TRACE_RESPONSE_HEADER_ENABLED_PROPERTY =
      "splunk.trace-response-header.enabled";
  private static final String PROFILER_DIRECTORY_PROPERTY = "splunk.profiler.directory";
  private static final String PROFILER_RECORDING_DURATION_PROPERTY =
      "splunk.profiler.recording.duration";
  private static final String PROFILER_KEEP_FILES_PROPERTY = "splunk.profiler.keep-files";
  private static final String PROFILER_LOGS_ENDPOINT_PROPERTY = "splunk.profiler.logs-endpoint";
  private static final String PROFILER_CALL_STACK_INTERVAL_PROPERTY =
      "splunk.profiler.call.stack.interval";
  private static final String PROFILER_MEMORY_EVENT_RATE_PROPERTY =
      "splunk.profiler.memory.event.rate";
  private static final String PROFILER_MEMORY_EVENT_RATE_LIMIT_ENABLED_PROPERTY =
      "splunk.profiler.memory.event.rate-limit.enabled";
  private static final String PROFILER_MEMORY_NATIVE_SAMPLING_PROPERTY =
      "splunk.profiler.memory.native.sampling";
  private static final String PROFILER_INCLUDE_AGENT_INTERNALS_PROPERTY =
      "splunk.profiler.include.agent.internals";
  private static final String PROFILER_INCLUDE_JVM_INTERNALS_PROPERTY =
      "splunk.profiler.include.jvm.internals";
  private static final String PROFILER_INCLUDE_INTERNAL_STACKS_PROPERTY =
      "splunk.profiler.include.internal.stacks";
  private static final String PROFILER_TRACING_STACKS_ONLY_PROPERTY =
      "splunk.profiler.tracing.stacks.only";
  private static final String PROFILER_STACK_DEPTH_PROPERTY = "splunk.profiler.max.stack.depth";
  private static final String PROFILER_OTLP_PROTOCOL_PROPERTY = "splunk.profiler.otlp.protocol";
  private static final String NOCODE_YML_FILE_PROPERTY =
      "splunk.otel.instrumentation.nocode.yml.file";
  private static final String OTEL_EXPORTER_OTLP_HEADERS_PROPERTY = "otel.exporter.otlp.headers";
  private static final String OTEL_EXPORTER_OTLP_TRACES_ENDPOINT_PROPERTY =
      "otel.exporter.otlp.traces.endpoint";
  private static final String OTEL_EXPORTER_OTLP_METRICS_ENDPOINT_PROPERTY =
      "otel.exporter.otlp.metrics.endpoint";
  private static final String OTEL_EXPORTER_OTLP_LOGS_ENDPOINT_PROPERTY =
      "otel.exporter.otlp.logs.endpoint";
  private static final String OTEL_EXPORTER_OTLP_TRACES_PROTOCOL_PROPERTY =
      "otel.exporter.otlp.traces.protocol";
  private static final String OTEL_EXPORTER_OTLP_LOGS_PROTOCOL_PROPERTY =
      "otel.exporter.otlp.logs.protocol";
  private static final String OTEL_JAVAAGENT_DEBUG_PROPERTY = "otel.javaagent.debug";
  private static final String OTEL_JAVAAGENT_ENABLED_PROPERTY = "otel.javaagent.enabled";
  private static final String OTEL_SERVICE_NAME_PROPERTY = "otel.service.name";
  private static final String OTEL_TRACES_EXPORTER_PROPERTY = "otel.traces.exporter";
  private static final String OTEL_TRACES_SAMPLER_PROPERTY = "otel.traces.sampler";
  private static final String OTEL_SPRING_BATCH_ITEM_ENABLED_PROPERTY =
      "otel.instrumentation.spring-batch.item.enabled";

  @Test
  void testCreateFileUsesConfiguredValues() throws IOException {
    var configMap = new HashMap<String, String>();
    configMap.put(SPLUNK_REALM_PROPERTY, "us1");
    configMap.put(METRICS_FULL_COMMAND_LINE, Boolean.TRUE.toString());
    configMap.put(TRACE_RESPONSE_HEADER_ENABLED_PROPERTY, Boolean.FALSE.toString());
    configMap.put(PROFILER_ENABLED_PROPERTY, Boolean.TRUE.toString());
    configMap.put(PROFILER_DIRECTORY_PROPERTY, "/tmp/profiler");
    configMap.put(PROFILER_RECORDING_DURATION_PROPERTY, "15s");
    configMap.put(PROFILER_KEEP_FILES_PROPERTY, Boolean.TRUE.toString());
    configMap.put(PROFILER_LOGS_ENDPOINT_PROPERTY, "http://collector:4318/custom/logs");
    configMap.put(PROFILER_CALL_STACK_INTERVAL_PROPERTY, "1234ms");
    configMap.put(PROFILER_MEMORY_ENABLED_PROPERTY, Boolean.TRUE.toString());
    configMap.put(PROFILER_MEMORY_EVENT_RATE_LIMIT_ENABLED_PROPERTY, Boolean.FALSE.toString());
    configMap.put(PROFILER_MEMORY_EVENT_RATE_PROPERTY, "321/s");
    configMap.put(PROFILER_MEMORY_NATIVE_SAMPLING_PROPERTY, Boolean.TRUE.toString());
    configMap.put(PROFILER_INCLUDE_AGENT_INTERNALS_PROPERTY, Boolean.TRUE.toString());
    configMap.put(PROFILER_INCLUDE_JVM_INTERNALS_PROPERTY, Boolean.TRUE.toString());
    configMap.put(PROFILER_INCLUDE_INTERNAL_STACKS_PROPERTY, Boolean.TRUE.toString());
    configMap.put(PROFILER_TRACING_STACKS_ONLY_PROPERTY, Boolean.TRUE.toString());
    configMap.put(PROFILER_STACK_DEPTH_PROPERTY, "2048");
    configMap.put(PROFILER_OTLP_PROTOCOL_PROPERTY, "grpc");
    configMap.put(NOCODE_YML_FILE_PROPERTY, "/tmp/nocode.yml");
    configMap.put(OTEL_JAVAAGENT_DEBUG_PROPERTY, Boolean.TRUE.toString());
    configMap.put(OTEL_JAVAAGENT_ENABLED_PROPERTY, Boolean.FALSE.toString());
    configMap.put(OTEL_SERVICE_NAME_PROPERTY, "checkout");
    configMap.put(OTEL_TRACES_EXPORTER_PROPERTY, "zipkin");
    configMap.put(OTEL_EXPORTER_OTLP_HEADERS_PROPERTY, "Authorization=abc,X-SF-TOKEN=token");
    configMap.put(SPLUNK_ACCESS_TOKEN, "token");

    Properties fileContent = loadProperties(createFileContent(configMap));

    assertThat(fileContent.getProperty(toEnvVarName(SPLUNK_REALM_PROPERTY))).isEqualTo("us1");
    assertThat(fileContent.getProperty(toEnvVarName(METRICS_FULL_COMMAND_LINE))).isEqualTo("true");
    assertThat(fileContent.getProperty(toEnvVarName(TRACE_RESPONSE_HEADER_ENABLED_PROPERTY)))
        .isEqualTo("false");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_ENABLED_PROPERTY))).isEqualTo("true");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_DIRECTORY_PROPERTY)))
        .isEqualTo("/tmp/profiler");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_RECORDING_DURATION_PROPERTY)))
        .isEqualTo("15s");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_KEEP_FILES_PROPERTY)))
        .isEqualTo("true");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_LOGS_ENDPOINT_PROPERTY)))
        .isEqualTo("http://collector:4318/custom/logs");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_CALL_STACK_INTERVAL_PROPERTY)))
        .isEqualTo("1234ms");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_MEMORY_ENABLED_PROPERTY)))
        .isEqualTo("true");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(PROFILER_MEMORY_EVENT_RATE_LIMIT_ENABLED_PROPERTY)))
        .isEqualTo("false");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_MEMORY_EVENT_RATE_PROPERTY)))
        .isEqualTo("321/s");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_MEMORY_NATIVE_SAMPLING_PROPERTY)))
        .isEqualTo("true");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_INCLUDE_AGENT_INTERNALS_PROPERTY)))
        .isEqualTo("true");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_INCLUDE_JVM_INTERNALS_PROPERTY)))
        .isEqualTo("true");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_INCLUDE_INTERNAL_STACKS_PROPERTY)))
        .isEqualTo("true");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_TRACING_STACKS_ONLY_PROPERTY)))
        .isEqualTo("true");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_STACK_DEPTH_PROPERTY)))
        .isEqualTo("2048");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_OTLP_PROTOCOL_PROPERTY)))
        .isEqualTo("grpc");
    assertThat(fileContent.getProperty(toEnvVarName(NOCODE_YML_FILE_PROPERTY)))
        .isEqualTo("/tmp/nocode.yml");
    assertThat(fileContent.getProperty(toEnvVarName(OTEL_JAVAAGENT_DEBUG_PROPERTY)))
        .isEqualTo("true");
    assertThat(fileContent.getProperty(toEnvVarName(OTEL_JAVAAGENT_ENABLED_PROPERTY)))
        .isEqualTo("false");
    assertThat(fileContent.getProperty(toEnvVarName(OTEL_SERVICE_NAME_PROPERTY)))
        .isEqualTo("checkout");
    assertThat(fileContent.getProperty(toEnvVarName(OTEL_TRACES_EXPORTER_PROPERTY)))
        .isEqualTo("zipkin");
    assertThat(fileContent.getProperty(toEnvVarName(OTEL_EXPORTER_OTLP_HEADERS_PROPERTY)))
        .isEqualTo("Authorization=abc");
    assertThat(fileContent.getProperty(toEnvVarName(SPLUNK_ACCESS_TOKEN))).isNull();
  }

  @Test
  void testCreateFileUsesDefaults() throws IOException {
    Properties fileContent = loadProperties(createFileContent(Map.of()));

    assertThat(fileContent.getProperty(toEnvVarName(SPLUNK_REALM_PROPERTY))).isEqualTo("none");
    assertThat(fileContent.getProperty(toEnvVarName(METRICS_FULL_COMMAND_LINE))).isEqualTo("false");
    assertThat(fileContent.getProperty(toEnvVarName(TRACE_RESPONSE_HEADER_ENABLED_PROPERTY)))
        .isEqualTo("true");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_ENABLED_PROPERTY))).isEqualTo("false");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_DIRECTORY_PROPERTY)))
        .isEqualTo(System.getProperty("java.io.tmpdir"));
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_RECORDING_DURATION_PROPERTY)))
        .isEqualTo("20s");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_KEEP_FILES_PROPERTY)))
        .isEqualTo("false");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_LOGS_ENDPOINT_PROPERTY)))
        .isEqualTo("http://localhost:4318/v1/logs");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_CALL_STACK_INTERVAL_PROPERTY)))
        .isEqualTo("10000ms");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_MEMORY_ENABLED_PROPERTY)))
        .isEqualTo("false");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(PROFILER_MEMORY_EVENT_RATE_LIMIT_ENABLED_PROPERTY)))
        .isEqualTo("true");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_MEMORY_EVENT_RATE_PROPERTY)))
        .isEqualTo("150/s");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_MEMORY_NATIVE_SAMPLING_PROPERTY)))
        .isEqualTo("false");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_INCLUDE_AGENT_INTERNALS_PROPERTY)))
        .isEqualTo("false");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_INCLUDE_JVM_INTERNALS_PROPERTY)))
        .isEqualTo("false");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_INCLUDE_INTERNAL_STACKS_PROPERTY)))
        .isEqualTo("false");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_TRACING_STACKS_ONLY_PROPERTY)))
        .isEqualTo("false");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_STACK_DEPTH_PROPERTY)))
        .isEqualTo("1024");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_OTLP_PROTOCOL_PROPERTY)))
        .isEqualTo("http/protobuf");
    assertThat(fileContent.getProperty(toEnvVarName(NOCODE_YML_FILE_PROPERTY))).isEmpty();
    assertThat(fileContent.getProperty(toEnvVarName(OTEL_JAVAAGENT_DEBUG_PROPERTY)))
        .isEqualTo("false");
    assertThat(fileContent.getProperty(toEnvVarName(OTEL_JAVAAGENT_ENABLED_PROPERTY)))
        .isEqualTo("true");
    assertThat(fileContent.getProperty(toEnvVarName(OTEL_TRACES_EXPORTER_PROPERTY)))
        .isEqualTo("otlp");
    assertThat(fileContent.getProperty(toEnvVarName("otel.exporter.otlp.protocol")))
        .isEqualTo("http/protobuf");
    assertThat(fileContent.getProperty(toEnvVarName("otel.exporter.otlp.endpoint")))
        .isEqualTo("http://localhost:4318");
    assertThat(fileContent.getProperty(toEnvVarName(OTEL_EXPORTER_OTLP_TRACES_ENDPOINT_PROPERTY)))
        .isEqualTo("http://localhost:4318/v1/traces");
    assertThat(fileContent.getProperty(toEnvVarName(OTEL_EXPORTER_OTLP_LOGS_PROTOCOL_PROPERTY)))
        .isEqualTo("http/protobuf");
  }

  @Test
  void testCreateFileDerivesProfilerAndOtelValuesFromGlobalOtlpSettings() throws IOException {
    Properties fileContent =
        loadProperties(
            createFileContent(
                Map.of(
                    "otel.exporter.otlp.endpoint", "http://collector:4317",
                    "otel.exporter.otlp.protocol", "grpc")));

    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_OTLP_PROTOCOL_PROPERTY)))
        .isEqualTo("grpc");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_LOGS_ENDPOINT_PROPERTY)))
        .isEqualTo("http://collector:4317");
    assertThat(fileContent.getProperty(toEnvVarName(OTEL_EXPORTER_OTLP_TRACES_PROTOCOL_PROPERTY)))
        .isEqualTo("grpc");
    assertThat(fileContent.getProperty(toEnvVarName(OTEL_EXPORTER_OTLP_TRACES_ENDPOINT_PROPERTY)))
        .isEqualTo("http://collector:4317");
    assertThat(fileContent.getProperty(toEnvVarName(OTEL_EXPORTER_OTLP_METRICS_ENDPOINT_PROPERTY)))
        .isEqualTo("http://collector:4317");
    assertThat(fileContent.getProperty(toEnvVarName(OTEL_EXPORTER_OTLP_LOGS_ENDPOINT_PROPERTY)))
        .isEqualTo("http://collector:4317");
  }

  @Test
  void testCreateFileUsesSignalSpecificProtocolDefaults() throws IOException {
    Properties fileContent =
        loadProperties(
            createFileContent(Map.of(OTEL_EXPORTER_OTLP_LOGS_PROTOCOL_PROPERTY, "grpc")));

    assertThat(fileContent.getProperty(toEnvVarName(OTEL_EXPORTER_OTLP_LOGS_ENDPOINT_PROPERTY)))
        .isEqualTo("http://localhost:4317");
  }

  @Test
  void testCreateFileUsesLocalLogsEndpointForSplunkIngest() throws IOException {
    Properties fileContent =
        loadProperties(
            createFileContent(
                Map.of(
                    "otel.exporter.otlp.endpoint",
                    "https://ingest.us1.observability.splunkcloud.com")));

    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_LOGS_ENDPOINT_PROPERTY)))
        .isEqualTo("http://localhost:4318/v1/logs");
  }

  @Test
  void testCreateFileUsesAutoConfiguredValues() throws IOException {
    Properties fileContent =
        loadProperties(
            EnvVarsEffectiveConfigFileFactory.createFileContent(
                    autoConfiguredConfig(
                        () ->
                            Map.of(
                                SPLUNK_ACCESS_TOKEN, "token",
                                OTEL_EXPORTER_OTLP_HEADERS_PROPERTY, "key=value")))
                .utf8());

    assertThat(fileContent.getProperty(toEnvVarName(OTEL_TRACES_SAMPLER_PROPERTY)))
        .isEqualTo("always_on");
    assertThat(fileContent.getProperty(toEnvVarName(OTEL_SPRING_BATCH_ITEM_ENABLED_PROPERTY)))
        .isEqualTo("true");
    assertThat(fileContent.getProperty(toEnvVarName(OTEL_EXPORTER_OTLP_HEADERS_PROPERTY)))
        .isEqualTo("key=value");
    assertThat(fileContent.getProperty(toEnvVarName(SPLUNK_ACCESS_TOKEN))).isNull();
  }

  private static String createFileContent(Map<String, String> configMap) {
    return EnvVarsEffectiveConfigFileFactory.createFileContent(
            DefaultConfigProperties.createFromMap(configMap))
        .utf8();
  }

  private static ConfigProperties autoConfiguredConfig(
      Supplier<Map<String, String>> testPropertiesSupplier) {
    AutoConfiguredOpenTelemetrySdk sdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .addPropertiesCustomizer(config -> testPropertiesSupplier.get())
            .build();
    return getConfig(sdk);
  }

  private static Properties loadProperties(String file) throws IOException {
    Properties properties = new Properties();
    properties.load(new StringReader(file));
    return properties;
  }
}
