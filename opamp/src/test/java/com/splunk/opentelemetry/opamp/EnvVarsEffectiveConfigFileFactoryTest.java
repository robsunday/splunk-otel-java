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
import static org.assertj.core.api.Assertions.assertThat;

import com.splunk.opentelemetry.profiler.ProfilerConfiguration;
import com.splunk.opentelemetry.profiler.ProfilerEnvVarsConfiguration;
import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilingConfiguration;
import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilingEnvVarsConfiguration;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class EnvVarsEffectiveConfigFileFactoryTest {
  private static final String TRACE_RESPONSE_HEADER_ENABLED_PROPERTY =
      "splunk.trace-response-header.enabled";
  private static final String NOCODE_YML_FILE_PROPERTY =
      "splunk.otel.instrumentation.nocode.yml.file";
  private static final String OTEL_EXPORTER_OTLP_HEADERS_PROPERTY = "otel.exporter.otlp.headers";
  private static final String OTEL_EXPORTER_OTLP_TRACES_ENDPOINT_PROPERTY =
      "otel.exporter.otlp.traces.endpoint";
  private static final String OTEL_EXPORTER_OTLP_LOGS_PROTOCOL_PROPERTY =
      "otel.exporter.otlp.logs.protocol";
  private static final String OTEL_JAVAAGENT_DEBUG_PROPERTY = "otel.javaagent.debug";
  private static final String OTEL_JAVAAGENT_ENABLED_PROPERTY = "otel.javaagent.enabled";
  private static final String OTEL_SERVICE_NAME_PROPERTY = "otel.service.name";
  private static final String OTEL_TRACES_EXPORTER_PROPERTY = "otel.traces.exporter";

  @Test
  void testCreateFileUsesConfiguredValues() throws IOException {
    var configMap = new HashMap<String, String>();
    configMap.put(SPLUNK_REALM_PROPERTY, "us1");
    configMap.put(METRICS_FULL_COMMAND_LINE, Boolean.TRUE.toString());
    configMap.put(TRACE_RESPONSE_HEADER_ENABLED_PROPERTY, Boolean.FALSE.toString());
    configMap.put(PROFILER_ENABLED_PROPERTY, Boolean.TRUE.toString());
    configMap.put(ProfilerEnvVarsConfiguration.CONFIG_KEY_PROFILER_DIRECTORY, "/tmp/profiler");
    configMap.put(ProfilerEnvVarsConfiguration.CONFIG_KEY_RECORDING_DURATION, "15s");
    configMap.put(ProfilerEnvVarsConfiguration.CONFIG_KEY_KEEP_FILES, Boolean.TRUE.toString());
    configMap.put(
        ProfilerEnvVarsConfiguration.CONFIG_KEY_INGEST_URL, "http://collector:4318/custom/logs");
    configMap.put(ProfilerEnvVarsConfiguration.CONFIG_KEY_CALL_STACK_INTERVAL, "1234ms");
    configMap.put(PROFILER_MEMORY_ENABLED_PROPERTY, Boolean.TRUE.toString());
    configMap.put(
        ProfilerEnvVarsConfiguration.CONFIG_KEY_MEMORY_EVENT_RATE_LIMIT_ENABLED,
        Boolean.FALSE.toString());
    configMap.put(ProfilerEnvVarsConfiguration.CONFIG_KEY_MEMORY_EVENT_RATE, "321/s");
    configMap.put(
        ProfilerEnvVarsConfiguration.CONFIG_KEY_MEMORY_NATIVE_SAMPLING, Boolean.TRUE.toString());
    configMap.put(
        ProfilerEnvVarsConfiguration.CONFIG_KEY_INCLUDE_AGENT_INTERNALS, Boolean.TRUE.toString());
    configMap.put(
        ProfilerEnvVarsConfiguration.CONFIG_KEY_INCLUDE_JVM_INTERNALS, Boolean.TRUE.toString());
    configMap.put(
        ProfilerEnvVarsConfiguration.CONFIG_KEY_INCLUDE_INTERNAL_STACKS, Boolean.TRUE.toString());
    configMap.put(
        ProfilerEnvVarsConfiguration.CONFIG_KEY_TRACING_STACKS_ONLY, Boolean.TRUE.toString());
    configMap.put(ProfilerEnvVarsConfiguration.CONFIG_KEY_STACK_DEPTH, "2048");
    configMap.put(ProfilerEnvVarsConfiguration.CONFIG_KEY_PROFILER_OTLP_PROTOCOL, "grpc");
    configMap.put(NOCODE_YML_FILE_PROPERTY, "/tmp/nocode.yml");
    configMap.put(OTEL_JAVAAGENT_DEBUG_PROPERTY, Boolean.TRUE.toString());
    configMap.put(OTEL_JAVAAGENT_ENABLED_PROPERTY, Boolean.FALSE.toString());
    configMap.put(OTEL_SERVICE_NAME_PROPERTY, "checkout");
    configMap.put(OTEL_TRACES_EXPORTER_PROPERTY, "zipkin");
    configMap.put(OTEL_EXPORTER_OTLP_HEADERS_PROPERTY, "Authorization=abc,X-SF-TOKEN=token");
    configMap.put(SPLUNK_ACCESS_TOKEN, "token");
    configMap.put(
        SnapshotProfilingEnvVarsConfiguration.CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER,
        Boolean.TRUE.toString());
    configMap.put(SnapshotProfilingEnvVarsConfiguration.SELECTION_PROBABILITY_KEY, "0.25");
    configMap.put(SnapshotProfilingEnvVarsConfiguration.STACK_DEPTH_KEY, "321");
    configMap.put(SnapshotProfilingEnvVarsConfiguration.SAMPLING_INTERVAL_KEY, "25ms");
    configMap.put(SnapshotProfilingEnvVarsConfiguration.EXPORT_INTERVAL_KEY, "42ms");
    configMap.put(SnapshotProfilingEnvVarsConfiguration.STAGING_CAPACITY_KEY, "777");

    Properties fileContent = loadProperties(createFileContent(configMap));

    assertThat(fileContent.getProperty(toEnvVarName(SPLUNK_REALM_PROPERTY))).isEqualTo("us1");
    assertThat(fileContent.getProperty(toEnvVarName(METRICS_FULL_COMMAND_LINE))).isEqualTo("true");
    assertThat(fileContent.getProperty(toEnvVarName(TRACE_RESPONSE_HEADER_ENABLED_PROPERTY)))
        .isEqualTo("false");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_ENABLED_PROPERTY))).isEqualTo("true");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_PROFILER_DIRECTORY)))
        .isEqualTo("/tmp/profiler");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_RECORDING_DURATION)))
        .isEqualTo("15000ms");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_KEEP_FILES)))
        .isEqualTo("true");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_INGEST_URL)))
        .isEqualTo("http://collector:4318/custom/logs");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_CALL_STACK_INTERVAL)))
        .isEqualTo("1234ms");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_MEMORY_ENABLED_PROPERTY)))
        .isEqualTo("true");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(
                    ProfilerEnvVarsConfiguration.CONFIG_KEY_MEMORY_EVENT_RATE_LIMIT_ENABLED)))
        .isEqualTo("false");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_MEMORY_EVENT_RATE)))
        .isEqualTo("321/s");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_MEMORY_NATIVE_SAMPLING)))
        .isEqualTo(String.valueOf(ProfilerConfiguration.HAS_OBJECT_ALLOCATION_SAMPLE_EVENT));
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_INCLUDE_AGENT_INTERNALS)))
        .isEqualTo("true");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_INCLUDE_JVM_INTERNALS)))
        .isEqualTo("true");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_INCLUDE_INTERNAL_STACKS)))
        .isEqualTo("true");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_TRACING_STACKS_ONLY)))
        .isEqualTo("true");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_STACK_DEPTH)))
        .isEqualTo("2048");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_PROFILER_OTLP_PROTOCOL)))
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
    assertThat(
            fileContent.getProperty(
                toEnvVarName(
                    SnapshotProfilingEnvVarsConfiguration.CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER)))
        .isEqualTo("true");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(SnapshotProfilingEnvVarsConfiguration.SELECTION_PROBABILITY_KEY)))
        .isEqualTo("0.25");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(SnapshotProfilingEnvVarsConfiguration.STACK_DEPTH_KEY)))
        .isEqualTo("321");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(SnapshotProfilingEnvVarsConfiguration.SAMPLING_INTERVAL_KEY)))
        .isEqualTo("25ms");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(SnapshotProfilingEnvVarsConfiguration.EXPORT_INTERVAL_KEY)))
        .isEqualTo("42ms");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(SnapshotProfilingEnvVarsConfiguration.STAGING_CAPACITY_KEY)))
        .isEqualTo("777");
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
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_PROFILER_DIRECTORY)))
        .isEqualTo(System.getProperty("java.io.tmpdir"));
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_RECORDING_DURATION)))
        .isEqualTo("20000ms");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_KEEP_FILES)))
        .isEqualTo("false");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_INGEST_URL)))
        .isEqualTo("http://localhost:4318/v1/logs");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_CALL_STACK_INTERVAL)))
        .isEqualTo("10000ms");
    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_MEMORY_ENABLED_PROPERTY)))
        .isEqualTo("false");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(
                    ProfilerEnvVarsConfiguration.CONFIG_KEY_MEMORY_EVENT_RATE_LIMIT_ENABLED)))
        .isEqualTo("true");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_MEMORY_EVENT_RATE)))
        .isEqualTo("150/s");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_MEMORY_NATIVE_SAMPLING)))
        .isEqualTo("false");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_INCLUDE_AGENT_INTERNALS)))
        .isEqualTo("false");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_INCLUDE_JVM_INTERNALS)))
        .isEqualTo("false");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_INCLUDE_INTERNAL_STACKS)))
        .isEqualTo("false");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_TRACING_STACKS_ONLY)))
        .isEqualTo("false");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_STACK_DEPTH)))
        .isEqualTo("1024");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(ProfilerEnvVarsConfiguration.CONFIG_KEY_PROFILER_OTLP_PROTOCOL)))
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
    assertThat(
            fileContent.getProperty(
                toEnvVarName(
                    SnapshotProfilingEnvVarsConfiguration.CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER)))
        .isEqualTo("false");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(SnapshotProfilingEnvVarsConfiguration.SELECTION_PROBABILITY_KEY)))
        .isEqualTo(String.valueOf(SnapshotProfilingConfiguration.DEFAULT_SELECTION_PROBABILITY));
    assertThat(
            fileContent.getProperty(
                toEnvVarName(SnapshotProfilingEnvVarsConfiguration.STACK_DEPTH_KEY)))
        .isEqualTo(String.valueOf(SnapshotProfilingConfiguration.DEFAULT_STACK_DEPTH));
    assertThat(
            fileContent.getProperty(
                toEnvVarName(SnapshotProfilingEnvVarsConfiguration.SAMPLING_INTERVAL_KEY)))
        .isEqualTo(SnapshotProfilingConfiguration.DEFAULT_SAMPLING_INTERVAL + "ms");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(SnapshotProfilingEnvVarsConfiguration.EXPORT_INTERVAL_KEY)))
        .isEqualTo(SnapshotProfilingConfiguration.DEFAULT_EXPORT_INTERVAL + "ms");
    assertThat(
            fileContent.getProperty(
                toEnvVarName(SnapshotProfilingEnvVarsConfiguration.STAGING_CAPACITY_KEY)))
        .isEqualTo(String.valueOf(SnapshotProfilingConfiguration.DEFAULT_STAGING_CAPACITY));
  }

  private static String createFileContent(Map<String, String> configMap) {
    DefaultConfigProperties config = DefaultConfigProperties.createFromMap(configMap);
    return new EnvVarsEffectiveConfigFileFactory(config).buildFileContent();
  }

  private static Properties loadProperties(String fileContent) throws IOException {
    Properties properties = new Properties();
    properties.load(new StringReader(fileContent));
    return properties;
  }
}
