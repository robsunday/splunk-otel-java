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

import com.google.common.annotations.VisibleForTesting;
import com.splunk.opentelemetry.SplunkConfiguration;
import com.splunk.opentelemetry.profiler.ProfilerEnvVarsConfiguration;
import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilingEnvVarsConfiguration;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.time.Duration;
import java.util.Locale;
import okio.ByteString;
import opamp.proto.AgentConfigFile;

class EnvVarsEffectiveConfigFileFactory implements EffectiveConfigFactory {
  private static final String OTEL_EXPORTER_OTLP_ENDPOINT = "otel.exporter.otlp.endpoint";
  private static final String OTEL_EXPORTER_OTLP_PROTOCOL = "otel.exporter.otlp.protocol";
  private static final String OTLP_PROTOCOL_HTTP_PROTOBUF = "http/protobuf";
  private static final String OTLP_SIGNAL_LOGS = "logs";
  private static final String OTLP_SIGNAL_METRICS = "metrics";
  private static final String OTLP_SIGNAL_TRACES = "traces";

  private final ConfigProperties config;

  EnvVarsEffectiveConfigFileFactory(ConfigProperties config) {
    this.config = config;
  }

  @Override
  public AgentConfigFile createFile() {
    ByteString content = new ByteString(buildFileContent().getBytes(UTF_8));
    return new AgentConfigFile(content, "text/plain+properties");
  }

  @VisibleForTesting
  String buildFileContent() {
    return addSplunkEnvVars(addOtelEnvVars(new FileContentBuilder())).build();
  }

  private FileContentBuilder addSplunkEnvVars(FileContentBuilder builder) {
    ProfilerEnvVarsConfiguration profilerConfiguration = new ProfilerEnvVarsConfiguration(config);
    SnapshotProfilingEnvVarsConfiguration snapshotConfiguration =
        new SnapshotProfilingEnvVarsConfiguration(config);

    return builder
        .addValue(SplunkConfiguration.PROFILER_ENABLED_PROPERTY, profilerConfiguration.isEnabled())
        .addValue(
            ProfilerEnvVarsConfiguration.CONFIG_KEY_MEMORY_ENABLED,
            profilerConfiguration.getMemoryEnabled())
        .addValue(
            SnapshotProfilingEnvVarsConfiguration.CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER,
            snapshotConfiguration.isEnabled())
        .addValue(
            SnapshotProfilingEnvVarsConfiguration.SAMPLING_INTERVAL_KEY,
            snapshotConfiguration.getSamplingInterval())
        .addValue(
            ProfilerEnvVarsConfiguration.CONFIG_KEY_CALL_STACK_INTERVAL,
            profilerConfiguration.getCallStackInterval());
  }

  private FileContentBuilder addOtelEnvVars(FileContentBuilder builder) {
    return builder
        .add("otel.exporter.otlp.traces.endpoint", getSignalEndpoint(config, OTLP_SIGNAL_TRACES))
        .add("otel.exporter.otlp.metrics.endpoint", getSignalEndpoint(config, OTLP_SIGNAL_METRICS))
        .add("otel.exporter.otlp.logs.endpoint", getSignalEndpoint(config, OTLP_SIGNAL_LOGS))
        .add("otel.service.name", "");
  }

  private static String getSignalEndpoint(ConfigProperties config, String signal) {
    String propertyName = "otel.exporter.otlp." + signal + ".endpoint";
    String endpoint = config.getString(propertyName);
    if (endpoint != null) {
      return endpoint;
    }

    String baseEndpoint = config.getString(OTEL_EXPORTER_OTLP_ENDPOINT);
    boolean isProtBuf = OTLP_PROTOCOL_HTTP_PROTOBUF.equals(getSignalOtlpProtocol(config, signal));
    if (baseEndpoint == null) {
      return isProtBuf ? "http://localhost:4318/v1/" + signal : "http://localhost:4317";
    }
    if (isProtBuf) {
      return appendSignalPath(baseEndpoint, signal);
    }
    return baseEndpoint;
  }

  private static String getSignalOtlpProtocol(ConfigProperties config, String signal) {
    String propertyName = "otel.exporter.otlp." + signal + ".protocol";
    return config.getString(propertyName, getOtlpProtocol(config));
  }

  private static String getOtlpProtocol(ConfigProperties config) {
    return config.getString(OTEL_EXPORTER_OTLP_PROTOCOL, OTLP_PROTOCOL_HTTP_PROTOBUF);
  }

  private static String appendSignalPath(String endpoint, String signal) {
    String signalPath = "v1/" + signal;
    if (endpoint.endsWith(signalPath)) {
      return endpoint;
    }
    if (!endpoint.endsWith("/")) {
      endpoint += "/";
    }
    return endpoint + signalPath;
  }

  @VisibleForTesting
  static String toEnvVarName(String name) {
    return name.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
  }

  private class FileContentBuilder {
    private final StringBuilder stringBuilder = new StringBuilder();

    FileContentBuilder addValue(String propertyName, Object value) {
      stringBuilder.append(toEnvVarName(propertyName)).append('=').append(value).append('\n');
      return this;
    }

    FileContentBuilder addValue(String propertyName, Duration value) {
      return addValue(propertyName, value.toMillis() + "ms");
    }

    FileContentBuilder add(String propertyName, String defaultValue) {
      return addValue(propertyName, config.getString(propertyName, defaultValue));
    }

    String build() {
      return stringBuilder.toString();
    }
  }
}
