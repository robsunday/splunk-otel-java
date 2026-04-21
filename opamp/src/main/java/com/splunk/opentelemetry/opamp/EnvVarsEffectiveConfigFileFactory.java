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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.UnaryOperator;
import okio.ByteString;
import opamp.proto.AgentConfigFile;

class EnvVarsEffectiveConfigFileFactory implements EffectiveConfigFactory {
  static final String REDACTION_MARKER = "**Redacted**";
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

    // Mask SPLUNK_ACCESS_TOKEN in the effective config file because it is a sensitive data.
    if (config.getString(SplunkConfiguration.SPLUNK_ACCESS_TOKEN) != null) {
      builder.addValue(SplunkConfiguration.SPLUNK_ACCESS_TOKEN, REDACTION_MARKER);
    }

    return builder
        .add(SplunkConfiguration.METRICS_FULL_COMMAND_LINE, false)
        .add("splunk.otel.instrumentation.nocode.yml.file", "")
        .addValue(
            ProfilerEnvVarsConfiguration.CONFIG_KEY_CALL_STACK_INTERVAL,
            profilerConfiguration.getCallStackInterval())
        .addValue(
            ProfilerEnvVarsConfiguration.CONFIG_KEY_PROFILER_DIRECTORY,
            profilerConfiguration.getProfilerDirectory())
        .addValue(SplunkConfiguration.PROFILER_ENABLED_PROPERTY, profilerConfiguration.isEnabled())
        .addValue(
            ProfilerEnvVarsConfiguration.CONFIG_KEY_INCLUDE_AGENT_INTERNALS,
            profilerConfiguration.getIncludeAgentInternalStacks())
        .addValue(
            ProfilerEnvVarsConfiguration.CONFIG_KEY_INCLUDE_INTERNAL_STACKS,
            config.getBoolean(
                ProfilerEnvVarsConfiguration.CONFIG_KEY_INCLUDE_INTERNAL_STACKS, false))
        .addValue(
            ProfilerEnvVarsConfiguration.CONFIG_KEY_INCLUDE_JVM_INTERNALS,
            profilerConfiguration.getIncludeJvmInternalStacks())
        .addValue(
            ProfilerEnvVarsConfiguration.CONFIG_KEY_KEEP_FILES,
            profilerConfiguration.getKeepFiles())
        .addValue(
            ProfilerEnvVarsConfiguration.CONFIG_KEY_INGEST_URL,
            profilerConfiguration.getIngestUrl())
        .addValue(
            ProfilerEnvVarsConfiguration.CONFIG_KEY_STACK_DEPTH,
            profilerConfiguration.getStackDepth())
        .addValue(
            ProfilerEnvVarsConfiguration.CONFIG_KEY_MEMORY_ENABLED,
            profilerConfiguration.getMemoryEnabled())
        .addValue(
            ProfilerEnvVarsConfiguration.CONFIG_KEY_MEMORY_EVENT_RATE,
            profilerConfiguration.getMemoryEventRate())
        .addValue(
            ProfilerEnvVarsConfiguration.CONFIG_KEY_MEMORY_EVENT_RATE_LIMIT_ENABLED,
            profilerConfiguration.getMemoryEventRateLimitEnabled())
        .addValue(
            ProfilerEnvVarsConfiguration.CONFIG_KEY_MEMORY_NATIVE_SAMPLING,
            profilerConfiguration.getUseAllocationSampleEvent())
        .addValue(
            ProfilerEnvVarsConfiguration.CONFIG_KEY_PROFILER_OTLP_PROTOCOL,
            profilerConfiguration.getOtlpProtocol())
        .addValue(
            ProfilerEnvVarsConfiguration.CONFIG_KEY_RECORDING_DURATION,
            profilerConfiguration.getRecordingDuration())
        .addValue(
            ProfilerEnvVarsConfiguration.CONFIG_KEY_TRACING_STACKS_ONLY,
            profilerConfiguration.getTracingStacksOnly())
        .add(SplunkConfiguration.SPLUNK_REALM_PROPERTY, SplunkConfiguration.SPLUNK_REALM_NONE)
        .add("splunk.trace-response-header.enabled", true)
        .addValue(
            SnapshotProfilingEnvVarsConfiguration.CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER,
            snapshotConfiguration.isEnabled())
        .addValue(
            SnapshotProfilingEnvVarsConfiguration.SELECTION_PROBABILITY_KEY,
            snapshotConfiguration.getSnapshotSelectionProbability())
        .addValue(
            SnapshotProfilingEnvVarsConfiguration.STACK_DEPTH_KEY,
            snapshotConfiguration.getStackDepth())
        .addValue(
            SnapshotProfilingEnvVarsConfiguration.SAMPLING_INTERVAL_KEY,
            snapshotConfiguration.getSamplingInterval())
        .addValue(
            SnapshotProfilingEnvVarsConfiguration.EXPORT_INTERVAL_KEY,
            snapshotConfiguration.getExportInterval())
        .addValue(
            SnapshotProfilingEnvVarsConfiguration.STAGING_CAPACITY_KEY,
            snapshotConfiguration.getStagingCapacity());
  }

  private FileContentBuilder addOtelEnvVars(FileContentBuilder builder) {
    return builder
        .add("otel.attribute.count.limit", 128)
        .addInt("otel.attribute.value.length.limit")
        .add("otel.blrp.export.timeout", 30000)
        .add("otel.blrp.max.export.batch.size", 512)
        .add("otel.blrp.max.queue.size", 2048)
        .add("otel.blrp.schedule.delay", 1000)
        .add("otel.bsp.export.timeout", 30000)
        .add("otel.bsp.max.export.batch.size", 512)
        .add("otel.bsp.max.queue.size", 2048)
        .add("otel.bsp.schedule.delay", 5000)
        .add("otel.config.file", "")
        .add("otel.experimental.javascript-snippet", "")
        .add("otel.experimental.resource.disabled-keys", "")
        .add("otel.exporter.otlp.certificate", "")
        .add("otel.exporter.otlp.client.certificate", "")
        .add("otel.exporter.otlp.client.key", "")
        .add("otel.exporter.otlp.compression", "")
        .add(OTEL_EXPORTER_OTLP_ENDPOINT, getOtlpEndpoint(config))
        .add("otel.exporter.otlp.headers", "", EnvVarsEffectiveConfigFileFactory::sanitizeHeaders)
        .add("otel.exporter.otlp.logs.certificate", "")
        .add("otel.exporter.otlp.logs.client.certificate", "")
        .add("otel.exporter.otlp.logs.client.key", "")
        .add("otel.exporter.otlp.logs.compression", "")
        .add("otel.exporter.otlp.logs.endpoint", getSignalOtlpEndpoint(config, OTLP_SIGNAL_LOGS))
        .add(
            "otel.exporter.otlp.logs.headers",
            "",
            EnvVarsEffectiveConfigFileFactory::sanitizeHeaders)
        .add("otel.exporter.otlp.logs.protocol", getSignalOtlpProtocol(config, OTLP_SIGNAL_LOGS))
        .add("otel.exporter.otlp.logs.timeout", 10000)
        .add("otel.exporter.otlp.metrics.certificate", "")
        .add("otel.exporter.otlp.metrics.client.certificate", "")
        .add("otel.exporter.otlp.metrics.client.key", "")
        .add("otel.exporter.otlp.metrics.compression", "")
        .add(
            "otel.exporter.otlp.metrics.default.histogram.aggregation", "EXPLICIT_BUCKET_HISTOGRAM")
        .add(
            "otel.exporter.otlp.metrics.endpoint",
            getSignalOtlpEndpoint(config, OTLP_SIGNAL_METRICS))
        .add(
            "otel.exporter.otlp.metrics.headers",
            "",
            EnvVarsEffectiveConfigFileFactory::sanitizeHeaders)
        .add(
            "otel.exporter.otlp.metrics.protocol",
            getSignalOtlpProtocol(config, OTLP_SIGNAL_METRICS))
        .add("otel.exporter.otlp.metrics.temporality.preference", "CUMULATIVE")
        .add("otel.exporter.otlp.metrics.timeout", 10000)
        .add(OTEL_EXPORTER_OTLP_PROTOCOL, getOtlpProtocol(config))
        .add("otel.exporter.otlp.timeout", 10000)
        .add("otel.exporter.otlp.traces.certificate", "")
        .add("otel.exporter.otlp.traces.client.certificate", "")
        .add("otel.exporter.otlp.traces.client.key", "")
        .add("otel.exporter.otlp.traces.compression", "")
        .add(
            "otel.exporter.otlp.traces.endpoint", getSignalOtlpEndpoint(config, OTLP_SIGNAL_TRACES))
        .add(
            "otel.exporter.otlp.traces.headers",
            "",
            EnvVarsEffectiveConfigFileFactory::sanitizeHeaders)
        .add(
            "otel.exporter.otlp.traces.protocol", getSignalOtlpProtocol(config, OTLP_SIGNAL_TRACES))
        .add("otel.exporter.otlp.traces.timeout", 10000)
        .add("otel.exporter.prometheus.host", "0.0.0.0")
        .add("otel.exporter.prometheus.port", 9464)
        .add("otel.exporter.zipkin.endpoint", "http://localhost:9411/api/v2/spans")
        .add("otel.instrumentation.apache-elasticjob.experimental-span-attributes", false)
        .add("otel.instrumentation.apache-shenyu.experimental-span-attributes", false)
        .add("otel.instrumentation.aws-sdk.experimental-span-attributes", false)
        .add("otel.instrumentation.aws-sdk.experimental-use-propagator-for-messaging", false)
        .add("otel.instrumentation.camel.experimental-span-attributes", false)
        .add("otel.instrumentation.common.db-statement-sanitizer.enabled", true)
        .add("otel.instrumentation.common.default-enabled", true)
        .add("otel.instrumentation.common.enduser.enabled", false)
        .add("otel.instrumentation.common.enduser.id.enabled", false)
        .add("otel.instrumentation.common.enduser.role.enabled", false)
        .add("otel.instrumentation.common.enduser.scope.enabled", false)
        .add("otel.instrumentation.common.experimental.controller-telemetry.enabled", false)
        .add("otel.instrumentation.common.experimental.view-telemetry.enabled", false)
        .add("otel.instrumentation.common.logging.span-id", "span_id")
        .add("otel.instrumentation.common.logging.trace-flags", "trace_flags")
        .add("otel.instrumentation.common.logging.trace-id", "trace_id")
        .add("otel.instrumentation.common.mdc.resource-attributes", "")
        .add("otel.instrumentation.common.peer-service-mapping", "")
        .add("otel.instrumentation.couchbase.experimental-span-attributes", false)
        .add("otel.instrumentation.elasticsearch.capture-search-query", false)
        .add("otel.instrumentation.elasticsearch.experimental-span-attributes", false)
        .add("otel.instrumentation.experimental.span-suppression-strategy", "semconv")
        .add("otel.instrumentation.genai.capture-message-content", false)
        .add("otel.instrumentation.graphql.add-operation-name-to-span-name.enabled", false)
        .add("otel.instrumentation.graphql.capture-query", true)
        .add("otel.instrumentation.graphql.data-fetcher.enabled", false)
        .add("otel.instrumentation.graphql.query-sanitizer.enabled", true)
        .add("otel.instrumentation.graphql.trivial-data-fetcher.enabled", false)
        .add("otel.instrumentation.grpc.capture-metadata.client.request", "")
        .add("otel.instrumentation.grpc.capture-metadata.server.request", "")
        .add("otel.instrumentation.grpc.emit-message-events", true)
        .add("otel.instrumentation.grpc.experimental-span-attributes", false)
        .add("otel.instrumentation.guava.experimental-span-attributes", false)
        .add("otel.instrumentation.hibernate.experimental-span-attributes", false)
        .add("otel.instrumentation.http.client.capture-request-headers", "")
        .add("otel.instrumentation.http.client.capture-response-headers", "")
        .add("otel.instrumentation.http.client.emit-experimental-telemetry", false)
        .add("otel.instrumentation.http.client.experimental.redact-query-parameters", true)
        .add(
            "otel.instrumentation.http.known-methods",
            "CONNECT, DELETE, GET, HEAD, OPTIONS, PATCH, POST, PUT, TRACE")
        .add("otel.instrumentation.http.server.capture-request-headers", "")
        .add("otel.instrumentation.http.server.capture-response-headers", "")
        .add("otel.instrumentation.http.server.emit-experimental-telemetry", false)
        .add("otel.instrumentation.hystrix.experimental-span-attributes", false)
        .add("otel.instrumentation.java-util-logging.experimental-log-attributes", false)
        .add("otel.instrumentation.jaxrs.experimental-span-attributes", false)
        .add("otel.instrumentation.jboss-logmanager.experimental.capture-event-name", false)
        .add("otel.instrumentation.jboss-logmanager.experimental.capture-mdc-attributes", "")
        .add("otel.instrumentation.jboss-logmanager.experimental-log-attributes", false)
        .add("otel.instrumentation.jdbc.experimental.capture-query-parameters", false)
        .add("otel.instrumentation.jdbc.experimental.sqlcommenter.enabled", false)
        .add("otel.instrumentation.jdbc.experimental.transaction.enabled", false)
        .add("otel.instrumentation.jdbc.statement-sanitizer.enabled", true)
        .add("otel.instrumentation.jsp.experimental-span-attributes", false)
        .add("otel.instrumentation.kafka.experimental-span-attributes", false)
        .add("otel.instrumentation.kafka.producer-propagation.enabled", true)
        .add("otel.instrumentation.kubernetes-client.experimental-span-attributes", false)
        .add("otel.instrumentation.lettuce.experimental-span-attributes", false)
        .add("otel.instrumentation.log4j-appender.experimental.capture-code-attributes", false)
        .add("otel.instrumentation.log4j-appender.experimental.capture-event-name", false)
        .add(
            "otel.instrumentation.log4j-appender.experimental.capture-map-message-attributes",
            false)
        .add("otel.instrumentation.log4j-appender.experimental.capture-marker-attribute", false)
        .add("otel.instrumentation.log4j-appender.experimental.capture-mdc-attributes", "")
        .add("otel.instrumentation.log4j-appender.experimental-log-attributes", false)
        .add("otel.instrumentation.log4j-context-data.add-baggage", false)
        .add("otel.instrumentation.logback-appender.experimental.capture-arguments", false)
        .add("otel.instrumentation.logback-appender.experimental.capture-code-attributes", false)
        .add("otel.instrumentation.logback-appender.experimental.capture-event-name", false)
        .add(
            "otel.instrumentation.logback-appender.experimental.capture-key-value-pair-attributes",
            false)
        .add(
            "otel.instrumentation.logback-appender.experimental.capture-logger-context-attributes",
            false)
        .add(
            "otel.instrumentation.logback-appender.experimental.capture-logstash-marker-attributes",
            false)
        .add(
            "otel.instrumentation.logback-appender.experimental.capture-logstash-structured-arguments",
            false)
        .add("otel.instrumentation.logback-appender.experimental.capture-marker-attribute", false)
        .add("otel.instrumentation.logback-appender.experimental.capture-mdc-attributes", "")
        .add("otel.instrumentation.logback-appender.experimental-log-attributes", false)
        .add("otel.instrumentation.logback-mdc.add-baggage", false)
        .add("otel.instrumentation.messaging.experimental.receive-telemetry.enabled", false)
        .add("otel.instrumentation.methods.include", "")
        .add("otel.instrumentation.micrometer.base-time-unit", "s")
        .add("otel.instrumentation.micrometer.histogram-gauges.enabled", false)
        .add("otel.instrumentation.micrometer.prometheus-mode.enabled", false)
        .add("otel.instrumentation.mongo.statement-sanitizer.enabled", true)
        .add("otel.instrumentation.netty.connection-telemetry.enabled", false)
        .add("otel.instrumentation.netty.ssl-telemetry.enabled", false)
        .add("otel.instrumentation.opensearch.capture-search-query", true)
        .add("otel.instrumentation.opensearch.experimental-span-attributes", false)
        .add("otel.instrumentation.opentelemetry-annotations.exclude-methods", "")
        .add("otel.instrumentation.opentelemetry-instrumentation-annotations.exclude-methods", "")
        .add("otel.instrumentation.oshi.experimental-metrics.enabled", false)
        .add("otel.instrumentation.powerjob.experimental-span-attributes", false)
        .add("otel.instrumentation.pulsar.experimental-span-attributes", false)
        .add("otel.instrumentation.quartz.experimental-span-attributes", false)
        .add("otel.instrumentation.r2dbc.statement-sanitizer.enabled", true)
        .add("otel.instrumentation.rabbitmq.experimental-span-attributes", false)
        .add("otel.instrumentation.reactor.experimental-span-attributes", false)
        .add("otel.instrumentation.reactor-netty.connection-telemetry.enabled", false)
        .add("otel.instrumentation.rocketmq-client.experimental-span-attributes", false)
        .add("otel.instrumentation.runtime-telemetry.emit-experimental-telemetry", false)
        .add("otel.instrumentation.runtime-telemetry-java17.enabled", false)
        .add("otel.instrumentation.runtime-telemetry-java17.enable-all", false)
        .add("otel.instrumentation.runtime-telemetry.package-emitter.enabled", false)
        .add("otel.instrumentation.runtime-telemetry.package-emitter.jars-per-second", 10)
        .add("otel.instrumentation.rxjava.experimental-span-attributes", false)
        .add(
            "otel.instrumentation.sanitization.url.experimental.sensitive-query-parameters",
            "AWSAccessKeyId, Signature, sig, X-Goog-Signature")
        .add("otel.instrumentation.servlet.experimental.capture-request-parameters", "")
        .add("otel.instrumentation.servlet.experimental-span-attributes", false)
        .add("otel.instrumentation.spring-batch.experimental.chunk.new-trace", false)
        .add("otel.instrumentation.spring-batch.item.enabled", false)
        .add("otel.instrumentation.spring-cloud-gateway.experimental-span-attributes", false)
        .add("otel.instrumentation.spring-integration.global-channel-interceptor-patterns", "*")
        .add("otel.instrumentation.spring-integration.producer.enabled", false)
        .add("otel.instrumentation.spring-scheduling.experimental-span-attributes", false)
        .add("otel.instrumentation.spring-security.enduser.role.granted-authority-prefix", "ROLE_")
        .add(
            "otel.instrumentation.spring-security.enduser.scope.granted-authority-prefix", "SCOPE_")
        .add("otel.instrumentation.spring-webflux.experimental-span-attributes", false)
        .add("otel.instrumentation.spring-webmvc.experimental-span-attributes", false)
        .add("otel.instrumentation.spymemcached.experimental-span-attributes", false)
        .add("otel.instrumentation.twilio.experimental-span-attributes", false)
        .add("otel.instrumentation.xxl-job.experimental-span-attributes", false)
        .add("otel.javaagent.configuration-file", "")
        .add("otel.javaagent.debug", false)
        .add("otel.javaagent.enabled", true)
        .add("otel.javaagent.exclude-classes", "")
        .add("otel.javaagent.exclude-class-loaders", "")
        .add("otel.javaagent.experimental.security-manager-support.enabled", false)
        .add("otel.javaagent.extensions", "")
        .add("otel.javaagent.logging", "simple")
        .add("otel.java.disabled.resource.providers", "")
        .add("otel.java.enabled.resource.providers", "")
        .add("otel.java.experimental.exporter.memory_mode", "immutable_data")
        .add("otel.java.exporter.otlp.retry.disabled", true)
        .add("otel.java.metrics.cardinality.limit", 2000)
        .add("otel.logs.exporter", "otlp")
        .add("otel.metrics.exemplar.filter", "TRACE_BASED")
        .add("otel.metrics.exporter", "otlp")
        .add("otel.metric.export.interval", 60000)
        .add("otel.propagators", "tracecontext,baggage")
        .add("otel.resource.attributes", "")
        .add("otel.resource.providers.aws.enabled", false)
        .add("otel.resource.providers.gcp.enabled", false)
        .add("otel.sdk.disabled", false)
        .add("otel.service.name", "")
        .add("otel.span.attribute.count.limit", 128)
        .addInt("otel.span.attribute.value.length.limit")
        .add("otel.span.event.count.limit", 128)
        .add("otel.span.link.count.limit", 128)
        .add("otel.traces.exporter", "otlp")
        .add("otel.traces.sampler", "always_on")
        .add("otel.traces.sampler.arg", "");
  }

  private static String getOtlpEndpoint(ConfigProperties config) {
    String endpoint = config.getString(OTEL_EXPORTER_OTLP_ENDPOINT);
    if (endpoint != null) {
      return endpoint;
    }
    return OTLP_PROTOCOL_HTTP_PROTOBUF.equals(getOtlpProtocol(config))
        ? "http://localhost:4318"
        : "http://localhost:4317";
  }

  private static String getSignalOtlpEndpoint(ConfigProperties config, String signal) {
    String propertyName = "otel.exporter.otlp." + signal + ".endpoint";
    String endpoint = config.getString(propertyName);
    if (endpoint != null) {
      return endpoint;
    }

    String baseEndpoint = config.getString(OTEL_EXPORTER_OTLP_ENDPOINT);
    if (baseEndpoint == null) {
      return OTLP_PROTOCOL_HTTP_PROTOBUF.equals(getSignalOtlpProtocol(config, signal))
          ? "http://localhost:4318/v1/" + signal
          : "http://localhost:4317";
    }
    if (OTLP_PROTOCOL_HTTP_PROTOBUF.equals(getSignalOtlpProtocol(config, signal))) {
      return appendSignalPath(baseEndpoint, signal);
    }
    return baseEndpoint;
  }

  private static String getSignalOtlpProtocol(ConfigProperties config, String signal) {
    if (OTLP_SIGNAL_LOGS.equals(signal)) {
      String protocol = SplunkConfiguration.getOtlpLogsProtocol(config);
      return protocol == null ? getOtlpProtocol(config) : protocol;
    }
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

  @VisibleForTesting
  static String sanitizeHeaders(String headers) {
    if (headers.isEmpty()) {
      return headers;
    }

    List<String> sanitized = new ArrayList<>();
    for (String header : headers.split(",")) {
      String trimmedHeader = header.trim();
      if (trimmedHeader.isEmpty()) {
        continue;
      }

      int separator = trimmedHeader.indexOf('=');
      String headerName = separator == -1 ? trimmedHeader : trimmedHeader.substring(0, separator);
      // Mask X-SF-TOKEN values because they are sensitive data
      if ("X-SF-TOKEN".equalsIgnoreCase(headerName.trim())) {
        sanitized.add(headerName + "=" + REDACTION_MARKER);
      } else {
        sanitized.add(trimmedHeader);
      }
    }
    return String.join(",", sanitized);
  }

  private class FileContentBuilder {
    private final StringBuilder stringBuilder = new StringBuilder();

    FileContentBuilder addValue(String propertyName, Object value) {
      stringBuilder.append(toEnvVarName(propertyName)).append('=').append(value).append('\n');
      return this;
    }

    FileContentBuilder addValue(String propertyName, int value) {
      return addValue(propertyName, Integer.valueOf(value));
    }

    FileContentBuilder addValue(String propertyName, double value) {
      return addValue(propertyName, Double.valueOf(value));
    }

    FileContentBuilder addValue(String propertyName, Duration value) {
      return addValue(propertyName, value.toMillis() + "ms");
    }

    FileContentBuilder add(String propertyName, String defaultValue) {
      return addValue(propertyName, config.getString(propertyName, defaultValue));
    }

    FileContentBuilder add(String propertyName, boolean defaultValue) {
      return addValue(propertyName, config.getBoolean(propertyName, defaultValue));
    }

    FileContentBuilder add(String propertyName, int defaultValue) {
      return addValue(propertyName, config.getInt(propertyName, defaultValue));
    }

    FileContentBuilder addInt(String propertyName) {
      Integer value = config.getInt(propertyName);
      return addValue(propertyName, Objects.toString(value, ""));
    }

    FileContentBuilder add(
        String propertyName, String defaultValue, UnaryOperator<String> valueTransformer) {
      return addValue(
          propertyName, valueTransformer.apply(config.getString(propertyName, defaultValue)));
    }

    String build() {
      return stringBuilder.toString();
    }
  }
}
