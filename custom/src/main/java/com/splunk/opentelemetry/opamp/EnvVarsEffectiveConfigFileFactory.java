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
import static com.splunk.opentelemetry.SplunkConfiguration.SPLUNK_REALM_NONE;
import static com.splunk.opentelemetry.SplunkConfiguration.SPLUNK_REALM_PROPERTY;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.annotations.VisibleForTesting;
import com.splunk.opentelemetry.SplunkConfiguration;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import okio.ByteString;
import opamp.proto.AgentConfigFile;

class EnvVarsEffectiveConfigFileFactory {
  private final ConfigProperties config;

  EnvVarsEffectiveConfigFileFactory(ConfigProperties config) {
    this.config = config;
  }

  AgentConfigFile createFile() {
    ByteString content = createFileContent(config);
    return new AgentConfigFile(content, "text/plain+properties");
  }

  @VisibleForTesting
  static ByteString createFileContent(ConfigProperties config) {
    StringBuilder sb = new StringBuilder();

    addOtelEnvVars(sb, config);
    addSplunkEnvVars(sb, config);

    return new ByteString(sb.toString().getBytes(UTF_8));
  }

  private static void addSplunkEnvVars(StringBuilder sb, ConfigProperties config) {
    addEnvVar(sb, config, METRICS_FULL_COMMAND_LINE, false);
    addEnvVar(sb, config, "splunk.otel.instrumentation.nocode.yml.file", "");
    addEnvVar(sb, config, "splunk.profiler.call.stack.interval", "10000ms");
    addEnvVar(sb, config, "splunk.profiler.directory", System.getProperty("java.io.tmpdir"));
    // Do not report SPLUNK_ACCESS_TOKEN in the effective config file because it is sensitive.
    addEnvVar(sb, PROFILER_ENABLED_PROPERTY, SplunkConfiguration.isProfilerEnabled(config));
    addEnvVar(sb, config, "splunk.profiler.include.agent.internals", false);
    addEnvVar(sb, config, "splunk.profiler.include.internal.stacks", false);
    addEnvVar(sb, config, "splunk.profiler.include.jvm.internals", false);
    addEnvVar(sb, config, "splunk.profiler.keep-files", false);
    addEnvVar(sb, "splunk.profiler.logs-endpoint", getProfilerLogsEndpoint(config));
    addEnvVar(sb, config, "splunk.profiler.max.stack.depth", 1024);
    addEnvVar(sb, config, PROFILER_MEMORY_ENABLED_PROPERTY, false);
    addEnvVar(sb, config, "splunk.profiler.memory.event.rate", "150/s");
    addEnvVar(sb, config, "splunk.profiler.memory.event.rate-limit.enabled", true);
    addEnvVar(sb, config, "splunk.profiler.memory.native.sampling", false);
    addEnvVar(sb, "splunk.profiler.otlp.protocol", getProfilerOtlpProtocol(config));
    addEnvVar(sb, config, "splunk.profiler.recording.duration", "20s");
    addEnvVar(sb, config, "splunk.profiler.tracing.stacks.only", false);
    addEnvVar(sb, config, SPLUNK_REALM_PROPERTY, SPLUNK_REALM_NONE);
    addEnvVar(sb, config, "splunk.trace-response-header.enabled", true);
  }

  private static void addOtelEnvVars(StringBuilder sb, ConfigProperties config) {
    addEnvVar(sb, config, "otel.attribute.count.limit", 128);
    addIntEnvVar(sb, config, "otel.attribute.value.length.limit");
    addEnvVar(sb, config, "otel.blrp.export.timeout", 30000);
    addEnvVar(sb, config, "otel.blrp.max.export.batch.size", 512);
    addEnvVar(sb, config, "otel.blrp.max.queue.size", 2048);
    addEnvVar(sb, config, "otel.blrp.schedule.delay", 1000);
    addEnvVar(sb, config, "otel.bsp.export.timeout", 30000);
    addEnvVar(sb, config, "otel.bsp.max.export.batch.size", 512);
    addEnvVar(sb, config, "otel.bsp.max.queue.size", 2048);
    addEnvVar(sb, config, "otel.bsp.schedule.delay", 5000);
    addEnvVar(sb, config, "otel.config.file", "");
    addEnvVar(sb, config, "otel.experimental.javascript-snippet", "");
    addEnvVar(sb, config, "otel.experimental.resource.disabled-keys", "");
    addEnvVar(sb, config, "otel.exporter.otlp.certificate", "");
    addEnvVar(sb, config, "otel.exporter.otlp.client.certificate", "");
    addEnvVar(sb, config, "otel.exporter.otlp.client.key", "");
    addEnvVar(sb, config, "otel.exporter.otlp.compression", "");
    addEnvVar(sb, "otel.exporter.otlp.endpoint", getOtlpEndpoint(config));
    // Do not report X-SF-TOKEN values because they can come from SPLUNK_ACCESS_TOKEN.
    addEnvVar(
        sb,
        config,
        "otel.exporter.otlp.headers",
        "",
        EnvVarsEffectiveConfigFileFactory::sanitizeHeaders);
    addEnvVar(sb, config, "otel.exporter.otlp.logs.certificate", "");
    addEnvVar(sb, config, "otel.exporter.otlp.logs.client.certificate", "");
    addEnvVar(sb, config, "otel.exporter.otlp.logs.client.key", "");
    addEnvVar(sb, config, "otel.exporter.otlp.logs.compression", "");
    addEnvVar(sb, "otel.exporter.otlp.logs.endpoint", getSignalOtlpEndpoint(config, "logs"));
    addEnvVar(
        sb,
        config,
        "otel.exporter.otlp.logs.headers",
        "",
        EnvVarsEffectiveConfigFileFactory::sanitizeHeaders);
    addEnvVar(sb, "otel.exporter.otlp.logs.protocol", getSignalOtlpProtocol(config, "logs"));
    addEnvVar(sb, config, "otel.exporter.otlp.logs.timeout", 10000);
    addEnvVar(sb, config, "otel.exporter.otlp.metrics.certificate", "");
    addEnvVar(sb, config, "otel.exporter.otlp.metrics.client.certificate", "");
    addEnvVar(sb, config, "otel.exporter.otlp.metrics.client.key", "");
    addEnvVar(sb, config, "otel.exporter.otlp.metrics.compression", "");
    addEnvVar(
        sb,
        config,
        "otel.exporter.otlp.metrics.default.histogram.aggregation",
        "EXPLICIT_BUCKET_HISTOGRAM");
    addEnvVar(sb, "otel.exporter.otlp.metrics.endpoint", getSignalOtlpEndpoint(config, "metrics"));
    addEnvVar(
        sb,
        config,
        "otel.exporter.otlp.metrics.headers",
        "",
        EnvVarsEffectiveConfigFileFactory::sanitizeHeaders);
    addEnvVar(sb, "otel.exporter.otlp.metrics.protocol", getSignalOtlpProtocol(config, "metrics"));
    addEnvVar(sb, config, "otel.exporter.otlp.metrics.temporality.preference", "CUMULATIVE");
    addEnvVar(sb, config, "otel.exporter.otlp.metrics.timeout", 10000);
    addEnvVar(sb, "otel.exporter.otlp.protocol", getOtlpProtocol(config));
    addEnvVar(sb, config, "otel.exporter.otlp.timeout", 10000);
    addEnvVar(sb, config, "otel.exporter.otlp.traces.certificate", "");
    addEnvVar(sb, config, "otel.exporter.otlp.traces.client.certificate", "");
    addEnvVar(sb, config, "otel.exporter.otlp.traces.client.key", "");
    addEnvVar(sb, config, "otel.exporter.otlp.traces.compression", "");
    addEnvVar(sb, "otel.exporter.otlp.traces.endpoint", getSignalOtlpEndpoint(config, "traces"));
    addEnvVar(
        sb,
        config,
        "otel.exporter.otlp.traces.headers",
        "",
        EnvVarsEffectiveConfigFileFactory::sanitizeHeaders);
    addEnvVar(sb, "otel.exporter.otlp.traces.protocol", getSignalOtlpProtocol(config, "traces"));
    addEnvVar(sb, config, "otel.exporter.otlp.traces.timeout", 10000);
    addEnvVar(sb, config, "otel.exporter.prometheus.host", "0.0.0.0");
    addEnvVar(sb, config, "otel.exporter.prometheus.port", 9464);
    addEnvVar(sb, config, "otel.exporter.zipkin.endpoint", "http://localhost:9411/api/v2/spans");
    addEnvVar(
        sb, config, "otel.instrumentation.apache-elasticjob.experimental-span-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.apache-shenyu.experimental-span-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.aws-sdk.experimental-span-attributes", false);
    addEnvVar(
        sb,
        config,
        "otel.instrumentation.aws-sdk.experimental-use-propagator-for-messaging",
        false);
    addEnvVar(sb, config, "otel.instrumentation.camel.experimental-span-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.common.db-statement-sanitizer.enabled", true);
    addEnvVar(sb, config, "otel.instrumentation.common.default-enabled", true);
    addEnvVar(sb, config, "otel.instrumentation.common.enduser.enabled", false);
    addEnvVar(sb, config, "otel.instrumentation.common.enduser.id.enabled", false);
    addEnvVar(sb, config, "otel.instrumentation.common.enduser.role.enabled", false);
    addEnvVar(sb, config, "otel.instrumentation.common.enduser.scope.enabled", false);
    addEnvVar(
        sb, config, "otel.instrumentation.common.experimental.controller-telemetry.enabled", false);
    addEnvVar(sb, config, "otel.instrumentation.common.experimental.view-telemetry.enabled", false);
    addEnvVar(sb, config, "otel.instrumentation.common.logging.span-id", "span_id");
    addEnvVar(sb, config, "otel.instrumentation.common.logging.trace-flags", "trace_flags");
    addEnvVar(sb, config, "otel.instrumentation.common.logging.trace-id", "trace_id");
    addEnvVar(sb, config, "otel.instrumentation.common.mdc.resource-attributes", "");
    addEnvVar(sb, config, "otel.instrumentation.common.peer-service-mapping", "");
    addEnvVar(sb, config, "otel.instrumentation.couchbase.experimental-span-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.elasticsearch.capture-search-query", false);
    addEnvVar(sb, config, "otel.instrumentation.elasticsearch.experimental-span-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.experimental.span-suppression-strategy", "semconv");
    addEnvVar(sb, config, "otel.instrumentation.genai.capture-message-content", false);
    addEnvVar(
        sb, config, "otel.instrumentation.graphql.add-operation-name-to-span-name.enabled", false);
    addEnvVar(sb, config, "otel.instrumentation.graphql.capture-query", true);
    addEnvVar(sb, config, "otel.instrumentation.graphql.data-fetcher.enabled", false);
    addEnvVar(sb, config, "otel.instrumentation.graphql.query-sanitizer.enabled", true);
    addEnvVar(sb, config, "otel.instrumentation.graphql.trivial-data-fetcher.enabled", false);
    addEnvVar(sb, config, "otel.instrumentation.grpc.capture-metadata.client.request", "");
    addEnvVar(sb, config, "otel.instrumentation.grpc.capture-metadata.server.request", "");
    addEnvVar(sb, config, "otel.instrumentation.grpc.emit-message-events", true);
    addEnvVar(sb, config, "otel.instrumentation.grpc.experimental-span-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.guava.experimental-span-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.hibernate.experimental-span-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.http.client.capture-request-headers", "");
    addEnvVar(sb, config, "otel.instrumentation.http.client.capture-response-headers", "");
    addEnvVar(sb, config, "otel.instrumentation.http.client.emit-experimental-telemetry", false);
    addEnvVar(
        sb, config, "otel.instrumentation.http.client.experimental.redact-query-parameters", true);
    addEnvVar(
        sb,
        config,
        "otel.instrumentation.http.known-methods",
        "CONNECT, DELETE, GET, HEAD, OPTIONS, PATCH, POST, PUT, TRACE");
    addEnvVar(sb, config, "otel.instrumentation.http.server.capture-request-headers", "");
    addEnvVar(sb, config, "otel.instrumentation.http.server.capture-response-headers", "");
    addEnvVar(sb, config, "otel.instrumentation.http.server.emit-experimental-telemetry", false);
    addEnvVar(sb, config, "otel.instrumentation.hystrix.experimental-span-attributes", false);
    addEnvVar(
        sb, config, "otel.instrumentation.java-util-logging.experimental-log-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.jaxrs.experimental-span-attributes", false);
    addEnvVar(
        sb, config, "otel.instrumentation.jboss-logmanager.experimental.capture-event-name", false);
    addEnvVar(
        sb,
        config,
        "otel.instrumentation.jboss-logmanager.experimental.capture-mdc-attributes",
        "");
    addEnvVar(
        sb, config, "otel.instrumentation.jboss-logmanager.experimental-log-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.jdbc.experimental.capture-query-parameters", false);
    addEnvVar(sb, config, "otel.instrumentation.jdbc.experimental.sqlcommenter.enabled", false);
    addEnvVar(sb, config, "otel.instrumentation.jdbc.experimental.transaction.enabled", false);
    addEnvVar(sb, config, "otel.instrumentation.jdbc.statement-sanitizer.enabled", true);
    addEnvVar(sb, config, "otel.instrumentation.jsp.experimental-span-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.kafka.experimental-span-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.kafka.producer-propagation.enabled", true);
    addEnvVar(
        sb, config, "otel.instrumentation.kubernetes-client.experimental-span-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.lettuce.experimental-span-attributes", false);
    addEnvVar(
        sb,
        config,
        "otel.instrumentation.log4j-appender.experimental.capture-code-attributes",
        false);
    addEnvVar(
        sb, config, "otel.instrumentation.log4j-appender.experimental.capture-event-name", false);
    addEnvVar(
        sb,
        config,
        "otel.instrumentation.log4j-appender.experimental.capture-map-message-attributes",
        false);
    addEnvVar(
        sb,
        config,
        "otel.instrumentation.log4j-appender.experimental.capture-marker-attribute",
        false);
    addEnvVar(
        sb, config, "otel.instrumentation.log4j-appender.experimental.capture-mdc-attributes", "");
    addEnvVar(sb, config, "otel.instrumentation.log4j-appender.experimental-log-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.log4j-context-data.add-baggage", false);
    addEnvVar(
        sb, config, "otel.instrumentation.logback-appender.experimental.capture-arguments", false);
    addEnvVar(
        sb,
        config,
        "otel.instrumentation.logback-appender.experimental.capture-code-attributes",
        false);
    addEnvVar(
        sb, config, "otel.instrumentation.logback-appender.experimental.capture-event-name", false);
    addEnvVar(
        sb,
        config,
        "otel.instrumentation.logback-appender.experimental.capture-key-value-pair-attributes",
        false);
    addEnvVar(
        sb,
        config,
        "otel.instrumentation.logback-appender.experimental.capture-logger-context-attributes",
        false);
    addEnvVar(
        sb,
        config,
        "otel.instrumentation.logback-appender.experimental.capture-logstash-marker-attributes",
        false);
    addEnvVar(
        sb,
        config,
        "otel.instrumentation.logback-appender.experimental.capture-logstash-structured-arguments",
        false);
    addEnvVar(
        sb,
        config,
        "otel.instrumentation.logback-appender.experimental.capture-marker-attribute",
        false);
    addEnvVar(
        sb,
        config,
        "otel.instrumentation.logback-appender.experimental.capture-mdc-attributes",
        "");
    addEnvVar(
        sb, config, "otel.instrumentation.logback-appender.experimental-log-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.logback-mdc.add-baggage", false);
    addEnvVar(
        sb, config, "otel.instrumentation.messaging.experimental.receive-telemetry.enabled", false);
    addEnvVar(sb, config, "otel.instrumentation.methods.include", "");
    addEnvVar(sb, config, "otel.instrumentation.micrometer.base-time-unit", "s");
    addEnvVar(sb, config, "otel.instrumentation.micrometer.histogram-gauges.enabled", false);
    addEnvVar(sb, config, "otel.instrumentation.micrometer.prometheus-mode.enabled", false);
    addEnvVar(sb, config, "otel.instrumentation.mongo.statement-sanitizer.enabled", true);
    addEnvVar(sb, config, "otel.instrumentation.netty.connection-telemetry.enabled", false);
    addEnvVar(sb, config, "otel.instrumentation.netty.ssl-telemetry.enabled", false);
    addEnvVar(sb, config, "otel.instrumentation.opensearch.capture-search-query", true);
    addEnvVar(sb, config, "otel.instrumentation.opensearch.experimental-span-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.opentelemetry-annotations.exclude-methods", "");
    addEnvVar(
        sb,
        config,
        "otel.instrumentation.opentelemetry-instrumentation-annotations.exclude-methods",
        "");
    addEnvVar(sb, config, "otel.instrumentation.oshi.experimental-metrics.enabled", false);
    addEnvVar(sb, config, "otel.instrumentation.powerjob.experimental-span-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.pulsar.experimental-span-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.quartz.experimental-span-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.r2dbc.statement-sanitizer.enabled", true);
    addEnvVar(sb, config, "otel.instrumentation.rabbitmq.experimental-span-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.reactor.experimental-span-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.reactor-netty.connection-telemetry.enabled", false);
    addEnvVar(
        sb, config, "otel.instrumentation.rocketmq-client.experimental-span-attributes", false);
    addEnvVar(
        sb, config, "otel.instrumentation.runtime-telemetry.emit-experimental-telemetry", false);
    addEnvVar(sb, config, "otel.instrumentation.runtime-telemetry-java17.enabled", false);
    addEnvVar(sb, config, "otel.instrumentation.runtime-telemetry-java17.enable-all", false);
    addEnvVar(sb, config, "otel.instrumentation.runtime-telemetry.package-emitter.enabled", false);
    addEnvVar(
        sb, config, "otel.instrumentation.runtime-telemetry.package-emitter.jars-per-second", 10);
    addEnvVar(sb, config, "otel.instrumentation.rxjava.experimental-span-attributes", false);
    addEnvVar(
        sb,
        config,
        "otel.instrumentation.sanitization.url.experimental.sensitive-query-parameters",
        "AWSAccessKeyId, Signature, sig, X-Goog-Signature");
    addEnvVar(
        sb, config, "otel.instrumentation.servlet.experimental.capture-request-parameters", "");
    addEnvVar(sb, config, "otel.instrumentation.servlet.experimental-span-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.spring-batch.experimental.chunk.new-trace", false);
    addEnvVar(sb, config, "otel.instrumentation.spring-batch.item.enabled", false);
    addEnvVar(
        sb,
        config,
        "otel.instrumentation.spring-cloud-gateway.experimental-span-attributes",
        false);
    addEnvVar(
        sb,
        config,
        "otel.instrumentation.spring-integration.global-channel-interceptor-patterns",
        "*");
    addEnvVar(sb, config, "otel.instrumentation.spring-integration.producer.enabled", false);
    addEnvVar(
        sb, config, "otel.instrumentation.spring-scheduling.experimental-span-attributes", false);
    addEnvVar(
        sb,
        config,
        "otel.instrumentation.spring-security.enduser.role.granted-authority-prefix",
        "ROLE_");
    addEnvVar(
        sb,
        config,
        "otel.instrumentation.spring-security.enduser.scope.granted-authority-prefix",
        "SCOPE_");
    addEnvVar(
        sb, config, "otel.instrumentation.spring-webflux.experimental-span-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.spring-webmvc.experimental-span-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.spymemcached.experimental-span-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.twilio.experimental-span-attributes", false);
    addEnvVar(sb, config, "otel.instrumentation.xxl-job.experimental-span-attributes", false);
    addEnvVar(sb, config, "otel.javaagent.configuration-file", "");
    addEnvVar(sb, config, "otel.javaagent.debug", false);
    addEnvVar(sb, config, "otel.javaagent.enabled", true);
    addEnvVar(sb, config, "otel.javaagent.exclude-classes", "");
    addEnvVar(sb, config, "otel.javaagent.exclude-class-loaders", "");
    addEnvVar(sb, config, "otel.javaagent.experimental.security-manager-support.enabled", false);
    addEnvVar(sb, config, "otel.javaagent.extensions", "");
    addEnvVar(sb, config, "otel.javaagent.logging", "simple");
    addEnvVar(sb, config, "otel.java.disabled.resource.providers", "");
    addEnvVar(sb, config, "otel.java.enabled.resource.providers", "");
    addEnvVar(sb, config, "otel.java.experimental.exporter.memory_mode", "immutable_data");
    addEnvVar(sb, config, "otel.java.exporter.otlp.retry.disabled", true);
    addEnvVar(sb, config, "otel.java.metrics.cardinality.limit", 2000);
    addEnvVar(sb, config, "otel.logs.exporter", "otlp");
    addEnvVar(sb, config, "otel.metrics.exemplar.filter", "TRACE_BASED");
    addEnvVar(sb, config, "otel.metrics.exporter", "otlp");
    addEnvVar(sb, config, "otel.metric.export.interval", 60000);
    addEnvVar(sb, config, "otel.propagators", "tracecontext,baggage");
    addEnvVar(sb, config, "otel.resource.attributes", "");
    addEnvVar(sb, config, "otel.resource.providers.aws.enabled", false);
    addEnvVar(sb, config, "otel.resource.providers.gcp.enabled", false);
    addEnvVar(sb, config, "otel.sdk.disabled", false);
    addEnvVar(sb, config, "otel.service.name", "");
    addEnvVar(sb, config, "otel.span.attribute.count.limit", 128);
    addIntEnvVar(sb, config, "otel.span.attribute.value.length.limit");
    addEnvVar(sb, config, "otel.span.event.count.limit", 128);
    addEnvVar(sb, config, "otel.span.link.count.limit", 128);
    addEnvVar(sb, config, "otel.traces.exporter", "otlp");
    addEnvVar(sb, config, "otel.traces.sampler", "always_on");
    addEnvVar(sb, config, "otel.traces.sampler.arg", "");
  }

  @VisibleForTesting
  static String toEnvVarName(String name) {
    return name.toUpperCase().replace('.', '_').replace('-', '_');
  }

  private static String getProfilerLogsEndpoint(ConfigProperties config) {
    String ingestUrl = config.getString("splunk.profiler.logs-endpoint");
    if (ingestUrl != null) {
      return ingestUrl;
    }

    String defaultLogsEndpoint = getDefaultProfilerLogsEndpoint(config);
    ingestUrl = config.getString("otel.exporter.otlp.endpoint", defaultLogsEndpoint);

    if (ingestUrl.startsWith("https://ingest.")
        && ingestUrl.endsWith(".observability.splunkcloud.com")) {
      return defaultLogsEndpoint;
    }

    if ("http/protobuf".equals(getProfilerOtlpProtocol(config))) {
      return maybeAppendHttpLogsPath(ingestUrl);
    }

    return ingestUrl;
  }

  private static String getDefaultProfilerLogsEndpoint(ConfigProperties config) {
    return "http/protobuf".equals(getProfilerOtlpProtocol(config))
        ? "http://localhost:4318/v1/logs"
        : "http://localhost:4317";
  }

  private static String getProfilerOtlpProtocol(ConfigProperties config) {
    return config.getString(
        "splunk.profiler.otlp.protocol",
        config.getString("otel.exporter.otlp.protocol", "http/protobuf"));
  }

  private static String maybeAppendHttpLogsPath(String ingestUrl) {
    if (ingestUrl.endsWith("v1/logs")) {
      return ingestUrl;
    }
    if (!ingestUrl.endsWith("/")) {
      ingestUrl += "/";
    }
    return ingestUrl + "v1/logs";
  }

  private static String getOtlpEndpoint(ConfigProperties config) {
    String endpoint = config.getString("otel.exporter.otlp.endpoint");
    if (endpoint != null) {
      return endpoint;
    }
    return "http/protobuf".equals(getOtlpProtocol(config))
        ? "http://localhost:4318"
        : "http://localhost:4317";
  }

  private static String getSignalOtlpEndpoint(ConfigProperties config, String signal) {
    String propertyName = "otel.exporter.otlp." + signal + ".endpoint";
    String endpoint = config.getString(propertyName);
    if (endpoint != null) {
      return endpoint;
    }

    String baseEndpoint = config.getString("otel.exporter.otlp.endpoint");
    if (baseEndpoint == null) {
      return "http/protobuf".equals(getSignalOtlpProtocol(config, signal))
          ? "http://localhost:4318/v1/" + signal
          : "http://localhost:4317";
    }
    if ("http/protobuf".equals(getSignalOtlpProtocol(config, signal))) {
      return appendSignalPath(baseEndpoint, signal);
    }
    return baseEndpoint;
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

  private static String getSignalOtlpProtocol(ConfigProperties config, String signal) {
    if ("logs".equals(signal)) {
      String protocol = SplunkConfiguration.getOtlpLogsProtocol(config);
      return protocol == null ? getOtlpProtocol(config) : protocol;
    }
    String propertyName = "otel.exporter.otlp." + signal + ".protocol";
    return config.getString(propertyName, getOtlpProtocol(config));
  }

  private static String getOtlpProtocol(ConfigProperties config) {
    return config.getString("otel.exporter.otlp.protocol", "http/protobuf");
  }

  private static String sanitizeHeaders(String headers) {
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
      if (!"X-SF-TOKEN".equalsIgnoreCase(headerName.trim())) {
        sanitized.add(trimmedHeader);
      }
    }
    return String.join(",", sanitized);
  }

  private static String nullable(Integer value) {
    return value == null ? "" : value.toString();
  }

  private static void addEnvVar(
      StringBuilder sb, ConfigProperties config, String propertyName, String defaultValue) {
    addEnvVar(sb, propertyName, config.getString(propertyName, defaultValue));
  }

  private static void addEnvVar(
      StringBuilder sb,
      ConfigProperties config,
      String propertyName,
      String defaultValue,
      UnaryOperator<String> valueTransformer) {
    addEnvVar(
        sb, propertyName, valueTransformer.apply(config.getString(propertyName, defaultValue)));
  }

  private static void addEnvVar(
      StringBuilder sb, ConfigProperties config, String propertyName, boolean defaultValue) {
    addEnvVar(sb, propertyName, config.getBoolean(propertyName, defaultValue));
  }

  private static void addEnvVar(
      StringBuilder sb, ConfigProperties config, String propertyName, int defaultValue) {
    addEnvVar(sb, propertyName, config.getInt(propertyName, defaultValue));
  }

  private static void addIntEnvVar(StringBuilder sb, ConfigProperties config, String propertyName) {
    Integer value = config.getInt(propertyName);
    addEnvVar(sb, propertyName, nullable(value));
  }

  private static void addEnvVar(StringBuilder sb, String propertyName, Object value) {
    sb.append(toEnvVarName(propertyName)).append('=').append(value).append('\n');
  }
}
