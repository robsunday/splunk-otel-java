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
import static com.splunk.opentelemetry.SplunkConfiguration.SPLUNK_ACCESS_TOKEN;
import static com.splunk.opentelemetry.opamp.EnvVarsEffectiveConfigFileFactory.REDACTION_MARKER;
import static com.splunk.opentelemetry.opamp.EnvVarsEffectiveConfigFileFactory.toEnvVarName;
import static org.assertj.core.api.Assertions.assertThat;

import com.splunk.opentelemetry.profiler.ProfilerConfiguration;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class EnvVarsEffectiveConfigFileFactoryTest {
  private static final String OTEL_EXPORTER_OTLP_HEADERS_PROPERTY = "otel.exporter.otlp.headers";
  private static final String OTEL_EXPORTER_OTLP_LOGS_HEADERS_PROPERTY =
      "otel.exporter.otlp.logs.headers";
  private static final String OTEL_EXPORTER_OTLP_METRICS_HEADERS_PROPERTY =
      "otel.exporter.otlp.metrics.headers";
  private static final String OTEL_EXPORTER_OTLP_TRACES_HEADERS_PROPERTY =
      "otel.exporter.otlp.traces.headers";

  @Test
  void testReportedValuesInitiallySetToNonDefaultValues() throws IOException {
    Map<String, String> configuredValues = createConfiguredValues();
    Properties fileContent = loadProperties(createFileContent(configuredValues));

    assertReportedConfiguredValues(fileContent, configuredValues);
  }

  @Test
  void testReportedDefaultValues() throws IOException {
    Properties fileContent = loadProperties(createFileContent(Map.of()));

    assertReportedDefaultValues(fileContent);
  }

  @Test
  void testSanitization() {
    assertThat(EnvVarsEffectiveConfigFileFactory.sanitizeHeaders("")).isEqualTo("");

    assertThat(EnvVarsEffectiveConfigFileFactory.sanitizeHeaders("header-1=abc"))
        .isEqualTo("header-1=abc");

    assertThat(EnvVarsEffectiveConfigFileFactory.sanitizeHeaders("x-sf-token=something"))
        .isEqualTo("x-sf-token=" + EnvVarsEffectiveConfigFileFactory.REDACTION_MARKER);

    assertThat(
            EnvVarsEffectiveConfigFileFactory.sanitizeHeaders(
                "HEADER-1=abc, X-SF-TOKEN=something, HEADER-2=def"))
        .isEqualTo(
            "HEADER-1=abc,X-SF-TOKEN="
                + EnvVarsEffectiveConfigFileFactory.REDACTION_MARKER
                + ",HEADER-2=def");
  }

  private static Map<String, String> createConfiguredValues() {
    Map<String, String> config = new HashMap<>();

    config.put("otel.attribute.count.limit", "129");
    config.put("otel.attribute.value.length.limit", "256");
    config.put("otel.blrp.export.timeout", "30001");
    config.put("otel.blrp.max.export.batch.size", "513");
    config.put("otel.blrp.max.queue.size", "2049");
    config.put("otel.blrp.schedule.delay", "1001");
    config.put("otel.bsp.export.timeout", "30001");
    config.put("otel.bsp.max.export.batch.size", "513");
    config.put("otel.bsp.max.queue.size", "2049");
    config.put("otel.bsp.schedule.delay", "5001");
    config.put("otel.config.file", "/tmp/otel_config_file");
    config.put("otel.experimental.javascript.snippet", "configured.javascript");
    config.put("otel.experimental.resource.disabled.keys", "service.name,process.pid");
    config.put("otel.exporter.otlp.certificate", "/tmp/otel_exporter_otlp_certificate.crt");
    config.put(
        "otel.exporter.otlp.client.certificate", "/tmp/otel_exporter_otlp_client_certificate.crt");
    config.put("otel.exporter.otlp.client.key", "/tmp/otel_exporter_otlp_client_key.key");
    config.put("otel.exporter.otlp.compression", "gzip");
    config.put(
        "otel.exporter.otlp.endpoint",
        "https://configured.example.com/otel_exporter_otlp_endpoint");
    config.put(OTEL_EXPORTER_OTLP_HEADERS_PROPERTY, "Authorization=abc,X-SF-TOKEN=token");
    config.put(
        "otel.exporter.otlp.logs.certificate", "/tmp/otel_exporter_otlp_logs_certificate.crt");
    config.put(
        "otel.exporter.otlp.logs.client.certificate",
        "/tmp/otel_exporter_otlp_logs_client_certificate.crt");
    config.put("otel.exporter.otlp.logs.client.key", "/tmp/otel_exporter_otlp_logs_client_key.key");
    config.put("otel.exporter.otlp.logs.compression", "gzip");
    config.put(
        "otel.exporter.otlp.logs.endpoint",
        "https://configured.example.com/otel_exporter_otlp_logs_endpoint");
    config.put(OTEL_EXPORTER_OTLP_LOGS_HEADERS_PROPERTY, "logs-header=abc,X-SF-TOKEN=token");
    config.put("otel.exporter.otlp.logs.protocol", "grpc");
    config.put("otel.exporter.otlp.logs.timeout", "10001");
    config.put(
        "otel.exporter.otlp.metrics.certificate",
        "/tmp/otel_exporter_otlp_metrics_certificate.crt");
    config.put(
        "otel.exporter.otlp.metrics.client.certificate",
        "/tmp/otel_exporter_otlp_metrics_client_certificate.crt");
    config.put(
        "otel.exporter.otlp.metrics.client.key", "/tmp/otel_exporter_otlp_metrics_client_key.key");
    config.put("otel.exporter.otlp.metrics.compression", "gzip");
    config.put(
        "otel.exporter.otlp.metrics.default.histogram.aggregation",
        "BASE2_EXPONENTIAL_BUCKET_HISTOGRAM");
    config.put(
        "otel.exporter.otlp.metrics.endpoint",
        "https://configured.example.com/otel_exporter_otlp_metrics_endpoint");
    config.put(OTEL_EXPORTER_OTLP_METRICS_HEADERS_PROPERTY, "metrics-header=abc,X-SF-TOKEN=token");
    config.put("otel.exporter.otlp.metrics.protocol", "grpc");
    config.put("otel.exporter.otlp.metrics.temporality.preference", "DELTA");
    config.put("otel.exporter.otlp.metrics.timeout", "10001");
    config.put("otel.exporter.otlp.protocol", "grpc");
    config.put("otel.exporter.otlp.timeout", "10001");
    config.put(
        "otel.exporter.otlp.traces.certificate", "/tmp/otel_exporter_otlp_traces_certificate.crt");
    config.put(
        "otel.exporter.otlp.traces.client.certificate",
        "/tmp/otel_exporter_otlp_traces_client_certificate.crt");
    config.put(
        "otel.exporter.otlp.traces.client.key", "/tmp/otel_exporter_otlp_traces_client_key.key");
    config.put("otel.exporter.otlp.traces.compression", "gzip");
    config.put(
        "otel.exporter.otlp.traces.endpoint",
        "https://configured.example.com/otel_exporter_otlp_traces_endpoint");
    config.put(OTEL_EXPORTER_OTLP_TRACES_HEADERS_PROPERTY, "traces-header=abc,X-SF-TOKEN=token");
    config.put("otel.exporter.otlp.traces.protocol", "grpc");
    config.put("otel.exporter.otlp.traces.timeout", "10001");
    config.put("otel.exporter.prometheus.host", "127.0.0.1");
    config.put("otel.exporter.prometheus.port", "9465");
    config.put(
        "otel.exporter.zipkin.endpoint",
        "https://configured.example.com/otel_exporter_zipkin_endpoint");
    config.put("otel.instrumentation.apache.elasticjob.experimental.span.attributes", "true");
    config.put("otel.instrumentation.apache.shenyu.experimental.span.attributes", "true");
    config.put("otel.instrumentation.aws.sdk.experimental.span.attributes", "true");
    config.put("otel.instrumentation.aws.sdk.experimental.use.propagator.for.messaging", "true");
    config.put("otel.instrumentation.camel.experimental.span.attributes", "true");
    config.put("otel.instrumentation.common.db.statement.sanitizer.enabled", "false");
    config.put("otel.instrumentation.common.default.enabled", "false");
    config.put("otel.instrumentation.common.enduser.enabled", "true");
    config.put("otel.instrumentation.common.enduser.id.enabled", "true");
    config.put("otel.instrumentation.common.enduser.role.enabled", "true");
    config.put("otel.instrumentation.common.enduser.scope.enabled", "true");
    config.put("otel.instrumentation.common.experimental.controller.telemetry.enabled", "true");
    config.put("otel.instrumentation.common.experimental.view.telemetry.enabled", "true");
    config.put("otel.instrumentation.common.logging.span.id", "spanId");
    config.put("otel.instrumentation.common.logging.trace.flags", "traceFlags");
    config.put("otel.instrumentation.common.logging.trace.id", "traceId");
    config.put(
        "otel.instrumentation.common.mdc.resource.attributes", "service.name,service.version");
    config.put("otel.instrumentation.common.peer.service.mapping", "db=orders-db");
    config.put("otel.instrumentation.couchbase.experimental.span.attributes", "true");
    config.put("otel.instrumentation.elasticsearch.capture.search.query", "true");
    config.put("otel.instrumentation.elasticsearch.experimental.span.attributes", "true");
    config.put("otel.instrumentation.experimental.span.suppression.strategy", "none");
    config.put("otel.instrumentation.genai.capture.message.content", "true");
    config.put("otel.instrumentation.graphql.add.operation.name.to.span.name.enabled", "true");
    config.put("otel.instrumentation.graphql.capture.query", "false");
    config.put("otel.instrumentation.graphql.data.fetcher.enabled", "true");
    config.put("otel.instrumentation.graphql.query.sanitizer.enabled", "false");
    config.put("otel.instrumentation.graphql.trivial.data.fetcher.enabled", "true");
    config.put("otel.instrumentation.grpc.capture.metadata.client.request", "x-request-id");
    config.put("otel.instrumentation.grpc.capture.metadata.server.request", "x-request-id");
    config.put("otel.instrumentation.grpc.emit.message.events", "false");
    config.put("otel.instrumentation.grpc.experimental.span.attributes", "true");
    config.put("otel.instrumentation.guava.experimental.span.attributes", "true");
    config.put("otel.instrumentation.hibernate.experimental.span.attributes", "true");
    config.put("otel.instrumentation.http.client.capture.request.headers", "x-request-id");
    config.put("otel.instrumentation.http.client.capture.response.headers", "x-response-id");
    config.put("otel.instrumentation.http.client.emit.experimental.telemetry", "true");
    config.put("otel.instrumentation.http.client.experimental.redact.query.parameters", "false");
    config.put("otel.instrumentation.http.known.methods", "GET, POST");
    config.put("otel.instrumentation.http.server.capture.request.headers", "x-request-id");
    config.put("otel.instrumentation.http.server.capture.response.headers", "x-response-id");
    config.put("otel.instrumentation.http.server.emit.experimental.telemetry", "true");
    config.put("otel.instrumentation.hystrix.experimental.span.attributes", "true");
    config.put("otel.instrumentation.java.util.logging.experimental.log.attributes", "true");
    config.put("otel.instrumentation.jaxrs.experimental.span.attributes", "true");
    config.put("otel.instrumentation.jboss.logmanager.experimental.capture.event.name", "true");
    config.put(
        "otel.instrumentation.jboss.logmanager.experimental.capture.mdc.attributes", "mdc.key");
    config.put("otel.instrumentation.jboss.logmanager.experimental.log.attributes", "true");
    config.put("otel.instrumentation.jdbc.experimental.capture.query.parameters", "true");
    config.put("otel.instrumentation.jdbc.experimental.sqlcommenter.enabled", "true");
    config.put("otel.instrumentation.jdbc.experimental.transaction.enabled", "true");
    config.put("otel.instrumentation.jdbc.statement.sanitizer.enabled", "false");
    config.put("otel.instrumentation.jsp.experimental.span.attributes", "true");
    config.put("otel.instrumentation.kafka.experimental.span.attributes", "true");
    config.put("otel.instrumentation.kafka.producer.propagation.enabled", "false");
    config.put("otel.instrumentation.kubernetes.client.experimental.span.attributes", "true");
    config.put("otel.instrumentation.lettuce.experimental.span.attributes", "true");
    config.put("otel.instrumentation.log4j.appender.experimental.capture.code.attributes", "true");
    config.put("otel.instrumentation.log4j.appender.experimental.capture.event.name", "true");
    config.put(
        "otel.instrumentation.log4j.appender.experimental.capture.map.message.attributes", "true");
    config.put("otel.instrumentation.log4j.appender.experimental.capture.marker.attribute", "true");
    config.put(
        "otel.instrumentation.log4j.appender.experimental.capture.mdc.attributes", "mdc.key");
    config.put("otel.instrumentation.log4j.appender.experimental.log.attributes", "true");
    config.put("otel.instrumentation.log4j.context.data.add.baggage", "true");
    config.put("otel.instrumentation.logback.appender.experimental.capture.arguments", "true");
    config.put(
        "otel.instrumentation.logback.appender.experimental.capture.code.attributes", "true");
    config.put("otel.instrumentation.logback.appender.experimental.capture.event.name", "true");
    config.put(
        "otel.instrumentation.logback.appender.experimental.capture.key.value.pair.attributes",
        "true");
    config.put(
        "otel.instrumentation.logback.appender.experimental.capture.logger.context.attributes",
        "true");
    config.put(
        "otel.instrumentation.logback.appender.experimental.capture.logstash.marker.attributes",
        "true");
    config.put(
        "otel.instrumentation.logback.appender.experimental.capture.logstash.structured.arguments",
        "true");
    config.put(
        "otel.instrumentation.logback.appender.experimental.capture.marker.attribute", "true");
    config.put(
        "otel.instrumentation.logback.appender.experimental.capture.mdc.attributes", "mdc.key");
    config.put("otel.instrumentation.logback.appender.experimental.log.attributes", "true");
    config.put("otel.instrumentation.logback.mdc.add.baggage", "true");
    config.put("otel.instrumentation.messaging.experimental.receive.telemetry.enabled", "true");
    config.put("otel.instrumentation.methods.include", "com.example.Foo[bar,baz]");
    config.put("otel.instrumentation.micrometer.base.time.unit", "ms");
    config.put("otel.instrumentation.micrometer.histogram.gauges.enabled", "true");
    config.put("otel.instrumentation.micrometer.prometheus.mode.enabled", "true");
    config.put("otel.instrumentation.mongo.statement.sanitizer.enabled", "false");
    config.put("otel.instrumentation.netty.connection.telemetry.enabled", "true");
    config.put("otel.instrumentation.netty.ssl.telemetry.enabled", "true");
    config.put("otel.instrumentation.opensearch.capture.search.query", "false");
    config.put("otel.instrumentation.opensearch.experimental.span.attributes", "true");
    config.put(
        "otel.instrumentation.opentelemetry.annotations.exclude.methods", "configured.value");
    config.put(
        "otel.instrumentation.opentelemetry.instrumentation.annotations.exclude.methods",
        "configured.value");
    config.put("otel.instrumentation.oshi.experimental.metrics.enabled", "true");
    config.put("otel.instrumentation.powerjob.experimental.span.attributes", "true");
    config.put("otel.instrumentation.pulsar.experimental.span.attributes", "true");
    config.put("otel.instrumentation.quartz.experimental.span.attributes", "true");
    config.put("otel.instrumentation.r2dbc.statement.sanitizer.enabled", "false");
    config.put("otel.instrumentation.rabbitmq.experimental.span.attributes", "true");
    config.put("otel.instrumentation.reactor.experimental.span.attributes", "true");
    config.put("otel.instrumentation.reactor.netty.connection.telemetry.enabled", "true");
    config.put("otel.instrumentation.rocketmq.client.experimental.span.attributes", "true");
    config.put("otel.instrumentation.runtime.telemetry.emit.experimental.telemetry", "true");
    config.put("otel.instrumentation.runtime.telemetry.java17.enabled", "true");
    config.put("otel.instrumentation.runtime.telemetry.java17.enable.all", "true");
    config.put("otel.instrumentation.runtime.telemetry.package.emitter.enabled", "true");
    config.put("otel.instrumentation.runtime.telemetry.package.emitter.jars.per.second", "11");
    config.put("otel.instrumentation.rxjava.experimental.span.attributes", "true");
    config.put(
        "otel.instrumentation.sanitization.url.experimental.sensitive.query.parameters",
        "token,secret");
    config.put(
        "otel.instrumentation.servlet.experimental.capture.request.parameters", "configured.value");
    config.put("otel.instrumentation.servlet.experimental.span.attributes", "true");
    config.put("otel.instrumentation.spring.batch.experimental.chunk.new.trace", "true");
    config.put("otel.instrumentation.spring.batch.item.enabled", "true");
    config.put("otel.instrumentation.spring.cloud.gateway.experimental.span.attributes", "true");
    config.put(
        "otel.instrumentation.spring.integration.global.channel.interceptor.patterns",
        "orders*,payments*");
    config.put("otel.instrumentation.spring.integration.producer.enabled", "true");
    config.put("otel.instrumentation.spring.scheduling.experimental.span.attributes", "true");
    config.put(
        "otel.instrumentation.spring.security.enduser.role.granted.authority.prefix", "APP_");
    config.put(
        "otel.instrumentation.spring.security.enduser.scope.granted.authority.prefix", "PERM_");
    config.put("otel.instrumentation.spring.webflux.experimental.span.attributes", "true");
    config.put("otel.instrumentation.spring.webmvc.experimental.span.attributes", "true");
    config.put("otel.instrumentation.spymemcached.experimental.span.attributes", "true");
    config.put("otel.instrumentation.twilio.experimental.span.attributes", "true");
    config.put("otel.instrumentation.xxl.job.experimental.span.attributes", "true");
    config.put("otel.javaagent.configuration.file", "/tmp/otel_javaagent_configuration_file");
    config.put("otel.javaagent.debug", "true");
    config.put("otel.javaagent.enabled", "false");
    config.put("otel.javaagent.exclude.classes", "configured.value");
    config.put("otel.javaagent.exclude.class.loaders", "configured.value");
    config.put("otel.javaagent.experimental.security.manager.support.enabled", "true");
    config.put("otel.javaagent.extensions", "/tmp/otel_javaagent_extensions.jar");
    config.put("otel.javaagent.logging", "application");
    config.put("otel.java.disabled.resource.providers", "configured.value");
    config.put("otel.java.enabled.resource.providers", "configured.value");
    config.put("otel.java.experimental.exporter.memory_mode", "reusable_data");
    config.put("otel.java.exporter.otlp.retry.disabled", "false");
    config.put("otel.java.metrics.cardinality.limit", "2001");
    config.put("otel.logs.exporter", "none");
    config.put("otel.metrics.exemplar.filter", "ALWAYS_OFF");
    config.put("otel.metrics.exporter", "prometheus");
    config.put("otel.metric.export.interval", "60001");
    config.put("otel.propagators", "b3,baggage");
    config.put("otel.resource.attributes", "service.name=checkout,service.version=1.2.3");
    config.put("otel.resource.providers.aws.enabled", "true");
    config.put("otel.resource.providers.gcp.enabled", "true");
    config.put("otel.sdk.disabled", "true");
    config.put("otel.service.name", "checkout");
    config.put("otel.span.attribute.count.limit", "129");
    config.put("otel.span.attribute.value.length.limit", "256");
    config.put("otel.span.event.count.limit", "129");
    config.put("otel.span.link.count.limit", "129");
    config.put("otel.traces.exporter", "zipkin");
    config.put("otel.traces.sampler", "traceidratio");
    config.put("otel.traces.sampler.arg", "0.5");
    config.put(METRICS_FULL_COMMAND_LINE, "true");
    config.put("splunk.otel.instrumentation.nocode.yml.file", "/tmp/nocode.yml");
    config.put("splunk.profiler.call.stack.interval", "1235ms");
    config.put("splunk.profiler.directory", "/tmp/profiler");
    config.put("splunk.profiler.enabled", "true");
    config.put("splunk.profiler.include.agent.internals", "true");
    config.put("splunk.profiler.include.internal.stacks", "true");
    config.put("splunk.profiler.include.jvm.internals", "true");
    config.put("splunk.profiler.keep.files", "true");
    config.put("splunk.profiler.logs.endpoint", "https://configured.example.com/profiler/logs");
    config.put("splunk.profiler.max.stack.depth", "1025");
    config.put("splunk.profiler.memory.enabled", "true");
    config.put("splunk.profiler.memory.event.rate", "321/s");
    config.put("splunk.profiler.memory.event.rate.limit.enabled", "false");
    config.put("splunk.profiler.memory.native.sampling", "true");
    config.put("splunk.profiler.otlp.protocol", "grpc");
    config.put("splunk.profiler.recording.duration", "15001ms");
    config.put("splunk.profiler.tracing.stacks.only", "true");
    config.put("splunk.realm", "us1");
    config.put("splunk.trace.response.header.enabled", "false");
    config.put("splunk.snapshot.profiler.enabled", "true");
    config.put("splunk.snapshot.selection.probability", "0.25");
    config.put("splunk.snapshot.profiler.max.stack.depth", "1025");
    config.put("splunk.snapshot.sampling.interval", "26ms");
    config.put("splunk.snapshot.profiler.export.interval", "43ms");
    config.put("splunk.snapshot.profiler.staging.capacity", "2001");
    config.put(SPLUNK_ACCESS_TOKEN, "token");

    return config;
  }

  private static void assertReportedConfiguredValues(
      Properties fileContent, Map<String, String> configuredValues) {
    Properties expected = toProperties(configuredValues);
    expected.setProperty(
        OTEL_EXPORTER_OTLP_HEADERS_PROPERTY, "Authorization=abc,X-SF-TOKEN=" + REDACTION_MARKER);
    expected.setProperty(
        OTEL_EXPORTER_OTLP_LOGS_HEADERS_PROPERTY, "logs-header=abc,X-SF-TOKEN=" + REDACTION_MARKER);
    expected.setProperty(
        OTEL_EXPORTER_OTLP_METRICS_HEADERS_PROPERTY,
        "metrics-header=abc,X-SF-TOKEN=" + REDACTION_MARKER);
    expected.setProperty(
        OTEL_EXPORTER_OTLP_TRACES_HEADERS_PROPERTY,
        "traces-header=abc,X-SF-TOKEN=" + REDACTION_MARKER);
    expected.setProperty(SPLUNK_ACCESS_TOKEN, REDACTION_MARKER);

    // See ProfilerEnvVarsConfiguration.getUseAllocationSampleEvent()
    expected.setProperty(
        "splunk.profiler.memory.native.sampling",
        String.valueOf(ProfilerConfiguration.HAS_OBJECT_ALLOCATION_SAMPLE_EVENT));

    assertProperties(fileContent, expected);
  }

  private static void assertReportedDefaultValues(Properties fileContent) {
    Properties expected = new Properties();

    expected.setProperty("otel.attribute.count.limit", "128");
    expected.setProperty("otel.attribute.value.length.limit", "");
    expected.setProperty("otel.blrp.export.timeout", "30000");
    expected.setProperty("otel.blrp.max.export.batch.size", "512");
    expected.setProperty("otel.blrp.max.queue.size", "2048");
    expected.setProperty("otel.blrp.schedule.delay", "1000");
    expected.setProperty("otel.bsp.export.timeout", "30000");
    expected.setProperty("otel.bsp.max.export.batch.size", "512");
    expected.setProperty("otel.bsp.max.queue.size", "2048");
    expected.setProperty("otel.bsp.schedule.delay", "5000");
    expected.setProperty("otel.config.file", "");
    expected.setProperty("otel.experimental.javascript.snippet", "");
    expected.setProperty("otel.experimental.resource.disabled.keys", "");
    expected.setProperty("otel.exporter.otlp.certificate", "");
    expected.setProperty("otel.exporter.otlp.client.certificate", "");
    expected.setProperty("otel.exporter.otlp.client.key", "");
    expected.setProperty("otel.exporter.otlp.compression", "");
    expected.setProperty("otel.exporter.otlp.endpoint", "http://localhost:4318");
    expected.setProperty(OTEL_EXPORTER_OTLP_HEADERS_PROPERTY, "");
    expected.setProperty("otel.exporter.otlp.logs.certificate", "");
    expected.setProperty("otel.exporter.otlp.logs.client.certificate", "");
    expected.setProperty("otel.exporter.otlp.logs.client.key", "");
    expected.setProperty("otel.exporter.otlp.logs.compression", "");
    expected.setProperty("otel.exporter.otlp.logs.endpoint", "http://localhost:4318/v1/logs");
    expected.setProperty(OTEL_EXPORTER_OTLP_LOGS_HEADERS_PROPERTY, "");
    expected.setProperty("otel.exporter.otlp.logs.protocol", "http/protobuf");
    expected.setProperty("otel.exporter.otlp.logs.timeout", "10000");
    expected.setProperty("otel.exporter.otlp.metrics.certificate", "");
    expected.setProperty("otel.exporter.otlp.metrics.client.certificate", "");
    expected.setProperty("otel.exporter.otlp.metrics.client.key", "");
    expected.setProperty("otel.exporter.otlp.metrics.compression", "");
    expected.setProperty(
        "otel.exporter.otlp.metrics.default.histogram.aggregation", "EXPLICIT_BUCKET_HISTOGRAM");
    expected.setProperty("otel.exporter.otlp.metrics.endpoint", "http://localhost:4318/v1/metrics");
    expected.setProperty(OTEL_EXPORTER_OTLP_METRICS_HEADERS_PROPERTY, "");
    expected.setProperty("otel.exporter.otlp.metrics.protocol", "http/protobuf");
    expected.setProperty("otel.exporter.otlp.metrics.temporality.preference", "CUMULATIVE");
    expected.setProperty("otel.exporter.otlp.metrics.timeout", "10000");
    expected.setProperty("otel.exporter.otlp.protocol", "http/protobuf");
    expected.setProperty("otel.exporter.otlp.timeout", "10000");
    expected.setProperty("otel.exporter.otlp.traces.certificate", "");
    expected.setProperty("otel.exporter.otlp.traces.client.certificate", "");
    expected.setProperty("otel.exporter.otlp.traces.client.key", "");
    expected.setProperty("otel.exporter.otlp.traces.compression", "");
    expected.setProperty("otel.exporter.otlp.traces.endpoint", "http://localhost:4318/v1/traces");
    expected.setProperty(OTEL_EXPORTER_OTLP_TRACES_HEADERS_PROPERTY, "");
    expected.setProperty("otel.exporter.otlp.traces.protocol", "http/protobuf");
    expected.setProperty("otel.exporter.otlp.traces.timeout", "10000");
    expected.setProperty("otel.exporter.prometheus.host", "0.0.0.0");
    expected.setProperty("otel.exporter.prometheus.port", "9464");
    expected.setProperty("otel.exporter.zipkin.endpoint", "http://localhost:9411/api/v2/spans");
    expected.setProperty(
        "otel.instrumentation.apache.elasticjob.experimental.span.attributes", "false");
    expected.setProperty(
        "otel.instrumentation.apache.shenyu.experimental.span.attributes", "false");
    expected.setProperty("otel.instrumentation.aws.sdk.experimental.span.attributes", "false");
    expected.setProperty(
        "otel.instrumentation.aws.sdk.experimental.use.propagator.for.messaging", "false");
    expected.setProperty("otel.instrumentation.camel.experimental.span.attributes", "false");
    expected.setProperty("otel.instrumentation.common.db.statement.sanitizer.enabled", "true");
    expected.setProperty("otel.instrumentation.common.default.enabled", "true");
    expected.setProperty("otel.instrumentation.common.enduser.enabled", "false");
    expected.setProperty("otel.instrumentation.common.enduser.id.enabled", "false");
    expected.setProperty("otel.instrumentation.common.enduser.role.enabled", "false");
    expected.setProperty("otel.instrumentation.common.enduser.scope.enabled", "false");
    expected.setProperty(
        "otel.instrumentation.common.experimental.controller.telemetry.enabled", "false");
    expected.setProperty(
        "otel.instrumentation.common.experimental.view.telemetry.enabled", "false");
    expected.setProperty("otel.instrumentation.common.logging.span.id", "span_id");
    expected.setProperty("otel.instrumentation.common.logging.trace.flags", "trace_flags");
    expected.setProperty("otel.instrumentation.common.logging.trace.id", "trace_id");
    expected.setProperty("otel.instrumentation.common.mdc.resource.attributes", "");
    expected.setProperty("otel.instrumentation.common.peer.service.mapping", "");
    expected.setProperty("otel.instrumentation.couchbase.experimental.span.attributes", "false");
    expected.setProperty("otel.instrumentation.elasticsearch.capture.search.query", "false");
    expected.setProperty(
        "otel.instrumentation.elasticsearch.experimental.span.attributes", "false");
    expected.setProperty("otel.instrumentation.experimental.span.suppression.strategy", "semconv");
    expected.setProperty("otel.instrumentation.genai.capture.message.content", "false");
    expected.setProperty(
        "otel.instrumentation.graphql.add.operation.name.to.span.name.enabled", "false");
    expected.setProperty("otel.instrumentation.graphql.capture.query", "true");
    expected.setProperty("otel.instrumentation.graphql.data.fetcher.enabled", "false");
    expected.setProperty("otel.instrumentation.graphql.query.sanitizer.enabled", "true");
    expected.setProperty("otel.instrumentation.graphql.trivial.data.fetcher.enabled", "false");
    expected.setProperty("otel.instrumentation.grpc.capture.metadata.client.request", "");
    expected.setProperty("otel.instrumentation.grpc.capture.metadata.server.request", "");
    expected.setProperty("otel.instrumentation.grpc.emit.message.events", "true");
    expected.setProperty("otel.instrumentation.grpc.experimental.span.attributes", "false");
    expected.setProperty("otel.instrumentation.guava.experimental.span.attributes", "false");
    expected.setProperty("otel.instrumentation.hibernate.experimental.span.attributes", "false");
    expected.setProperty("otel.instrumentation.http.client.capture.request.headers", "");
    expected.setProperty("otel.instrumentation.http.client.capture.response.headers", "");
    expected.setProperty("otel.instrumentation.http.client.emit.experimental.telemetry", "false");
    expected.setProperty(
        "otel.instrumentation.http.client.experimental.redact.query.parameters", "true");
    expected.setProperty(
        "otel.instrumentation.http.known.methods",
        "CONNECT, DELETE, GET, HEAD, OPTIONS, PATCH, POST, PUT, TRACE");
    expected.setProperty("otel.instrumentation.http.server.capture.request.headers", "");
    expected.setProperty("otel.instrumentation.http.server.capture.response.headers", "");
    expected.setProperty("otel.instrumentation.http.server.emit.experimental.telemetry", "false");
    expected.setProperty("otel.instrumentation.hystrix.experimental.span.attributes", "false");
    expected.setProperty(
        "otel.instrumentation.java.util.logging.experimental.log.attributes", "false");
    expected.setProperty("otel.instrumentation.jaxrs.experimental.span.attributes", "false");
    expected.setProperty(
        "otel.instrumentation.jboss.logmanager.experimental.capture.event.name", "false");
    expected.setProperty(
        "otel.instrumentation.jboss.logmanager.experimental.capture.mdc.attributes", "");
    expected.setProperty(
        "otel.instrumentation.jboss.logmanager.experimental.log.attributes", "false");
    expected.setProperty(
        "otel.instrumentation.jdbc.experimental.capture.query.parameters", "false");
    expected.setProperty("otel.instrumentation.jdbc.experimental.sqlcommenter.enabled", "false");
    expected.setProperty("otel.instrumentation.jdbc.experimental.transaction.enabled", "false");
    expected.setProperty("otel.instrumentation.jdbc.statement.sanitizer.enabled", "true");
    expected.setProperty("otel.instrumentation.jsp.experimental.span.attributes", "false");
    expected.setProperty("otel.instrumentation.kafka.experimental.span.attributes", "false");
    expected.setProperty("otel.instrumentation.kafka.producer.propagation.enabled", "true");
    expected.setProperty(
        "otel.instrumentation.kubernetes.client.experimental.span.attributes", "false");
    expected.setProperty("otel.instrumentation.lettuce.experimental.span.attributes", "false");
    expected.setProperty(
        "otel.instrumentation.log4j.appender.experimental.capture.code.attributes", "false");
    expected.setProperty(
        "otel.instrumentation.log4j.appender.experimental.capture.event.name", "false");
    expected.setProperty(
        "otel.instrumentation.log4j.appender.experimental.capture.map.message.attributes", "false");
    expected.setProperty(
        "otel.instrumentation.log4j.appender.experimental.capture.marker.attribute", "false");
    expected.setProperty(
        "otel.instrumentation.log4j.appender.experimental.capture.mdc.attributes", "");
    expected.setProperty(
        "otel.instrumentation.log4j.appender.experimental.log.attributes", "false");
    expected.setProperty("otel.instrumentation.log4j.context.data.add.baggage", "false");
    expected.setProperty(
        "otel.instrumentation.logback.appender.experimental.capture.arguments", "false");
    expected.setProperty(
        "otel.instrumentation.logback.appender.experimental.capture.code.attributes", "false");
    expected.setProperty(
        "otel.instrumentation.logback.appender.experimental.capture.event.name", "false");
    expected.setProperty(
        "otel.instrumentation.logback.appender.experimental.capture.key.value.pair.attributes",
        "false");
    expected.setProperty(
        "otel.instrumentation.logback.appender.experimental.capture.logger.context.attributes",
        "false");
    expected.setProperty(
        "otel.instrumentation.logback.appender.experimental.capture.logstash.marker.attributes",
        "false");
    expected.setProperty(
        "otel.instrumentation.logback.appender.experimental.capture.logstash.structured.arguments",
        "false");
    expected.setProperty(
        "otel.instrumentation.logback.appender.experimental.capture.marker.attribute", "false");
    expected.setProperty(
        "otel.instrumentation.logback.appender.experimental.capture.mdc.attributes", "");
    expected.setProperty(
        "otel.instrumentation.logback.appender.experimental.log.attributes", "false");
    expected.setProperty("otel.instrumentation.logback.mdc.add.baggage", "false");
    expected.setProperty(
        "otel.instrumentation.messaging.experimental.receive.telemetry.enabled", "false");
    expected.setProperty("otel.instrumentation.methods.include", "");
    expected.setProperty("otel.instrumentation.micrometer.base.time.unit", "s");
    expected.setProperty("otel.instrumentation.micrometer.histogram.gauges.enabled", "false");
    expected.setProperty("otel.instrumentation.micrometer.prometheus.mode.enabled", "false");
    expected.setProperty("otel.instrumentation.mongo.statement.sanitizer.enabled", "true");
    expected.setProperty("otel.instrumentation.netty.connection.telemetry.enabled", "false");
    expected.setProperty("otel.instrumentation.netty.ssl.telemetry.enabled", "false");
    expected.setProperty("otel.instrumentation.opensearch.capture.search.query", "true");
    expected.setProperty("otel.instrumentation.opensearch.experimental.span.attributes", "false");
    expected.setProperty("otel.instrumentation.opentelemetry.annotations.exclude.methods", "");
    expected.setProperty(
        "otel.instrumentation.opentelemetry.instrumentation.annotations.exclude.methods", "");
    expected.setProperty("otel.instrumentation.oshi.experimental.metrics.enabled", "false");
    expected.setProperty("otel.instrumentation.powerjob.experimental.span.attributes", "false");
    expected.setProperty("otel.instrumentation.pulsar.experimental.span.attributes", "false");
    expected.setProperty("otel.instrumentation.quartz.experimental.span.attributes", "false");
    expected.setProperty("otel.instrumentation.r2dbc.statement.sanitizer.enabled", "true");
    expected.setProperty("otel.instrumentation.rabbitmq.experimental.span.attributes", "false");
    expected.setProperty("otel.instrumentation.reactor.experimental.span.attributes", "false");
    expected.setProperty(
        "otel.instrumentation.reactor.netty.connection.telemetry.enabled", "false");
    expected.setProperty(
        "otel.instrumentation.rocketmq.client.experimental.span.attributes", "false");
    expected.setProperty(
        "otel.instrumentation.runtime.telemetry.emit.experimental.telemetry", "false");
    expected.setProperty("otel.instrumentation.runtime.telemetry.java17.enabled", "false");
    expected.setProperty("otel.instrumentation.runtime.telemetry.java17.enable.all", "false");
    expected.setProperty("otel.instrumentation.runtime.telemetry.package.emitter.enabled", "false");
    expected.setProperty(
        "otel.instrumentation.runtime.telemetry.package.emitter.jars.per.second", "10");
    expected.setProperty("otel.instrumentation.rxjava.experimental.span.attributes", "false");
    expected.setProperty(
        "otel.instrumentation.sanitization.url.experimental.sensitive.query.parameters",
        "AWSAccessKeyId, Signature, sig, X-Goog-Signature");
    expected.setProperty(
        "otel.instrumentation.servlet.experimental.capture.request.parameters", "");
    expected.setProperty("otel.instrumentation.servlet.experimental.span.attributes", "false");
    expected.setProperty("otel.instrumentation.spring.batch.experimental.chunk.new.trace", "false");
    expected.setProperty("otel.instrumentation.spring.batch.item.enabled", "false");
    expected.setProperty(
        "otel.instrumentation.spring.cloud.gateway.experimental.span.attributes", "false");
    expected.setProperty(
        "otel.instrumentation.spring.integration.global.channel.interceptor.patterns", "*");
    expected.setProperty("otel.instrumentation.spring.integration.producer.enabled", "false");
    expected.setProperty(
        "otel.instrumentation.spring.scheduling.experimental.span.attributes", "false");
    expected.setProperty(
        "otel.instrumentation.spring.security.enduser.role.granted.authority.prefix", "ROLE_");
    expected.setProperty(
        "otel.instrumentation.spring.security.enduser.scope.granted.authority.prefix", "SCOPE_");
    expected.setProperty(
        "otel.instrumentation.spring.webflux.experimental.span.attributes", "false");
    expected.setProperty(
        "otel.instrumentation.spring.webmvc.experimental.span.attributes", "false");
    expected.setProperty("otel.instrumentation.spymemcached.experimental.span.attributes", "false");
    expected.setProperty("otel.instrumentation.twilio.experimental.span.attributes", "false");
    expected.setProperty("otel.instrumentation.xxl.job.experimental.span.attributes", "false");
    expected.setProperty("otel.javaagent.configuration.file", "");
    expected.setProperty("otel.javaagent.debug", "false");
    expected.setProperty("otel.javaagent.enabled", "true");
    expected.setProperty("otel.javaagent.exclude.classes", "");
    expected.setProperty("otel.javaagent.exclude.class.loaders", "");
    expected.setProperty("otel.javaagent.experimental.security.manager.support.enabled", "false");
    expected.setProperty("otel.javaagent.extensions", "");
    expected.setProperty("otel.javaagent.logging", "simple");
    expected.setProperty("otel.java.disabled.resource.providers", "");
    expected.setProperty("otel.java.enabled.resource.providers", "");
    expected.setProperty("otel.java.experimental.exporter.memory_mode", "immutable_data");
    expected.setProperty("otel.java.exporter.otlp.retry.disabled", "true");
    expected.setProperty("otel.java.metrics.cardinality.limit", "2000");
    expected.setProperty("otel.logs.exporter", "otlp");
    expected.setProperty("otel.metrics.exemplar.filter", "TRACE_BASED");
    expected.setProperty("otel.metrics.exporter", "otlp");
    expected.setProperty("otel.metric.export.interval", "60000");
    expected.setProperty("otel.propagators", "tracecontext,baggage");
    expected.setProperty("otel.resource.attributes", "");
    expected.setProperty("otel.resource.providers.aws.enabled", "false");
    expected.setProperty("otel.resource.providers.gcp.enabled", "false");
    expected.setProperty("otel.sdk.disabled", "false");
    expected.setProperty("otel.service.name", "");
    expected.setProperty("otel.span.attribute.count.limit", "128");
    expected.setProperty("otel.span.attribute.value.length.limit", "");
    expected.setProperty("otel.span.event.count.limit", "128");
    expected.setProperty("otel.span.link.count.limit", "128");
    expected.setProperty("otel.traces.exporter", "otlp");
    expected.setProperty("otel.traces.sampler", "always_on");
    expected.setProperty("otel.traces.sampler.arg", "");
    expected.setProperty(METRICS_FULL_COMMAND_LINE, "false");
    expected.setProperty("splunk.otel.instrumentation.nocode.yml.file", "");
    expected.setProperty("splunk.profiler.call.stack.interval", "10000ms");
    expected.setProperty("splunk.profiler.directory", System.getProperty("java.io.tmpdir"));
    expected.setProperty("splunk.profiler.enabled", "false");
    expected.setProperty("splunk.profiler.include.agent.internals", "false");
    expected.setProperty("splunk.profiler.include.internal.stacks", "false");
    expected.setProperty("splunk.profiler.include.jvm.internals", "false");
    expected.setProperty("splunk.profiler.keep.files", "false");
    expected.setProperty("splunk.profiler.logs.endpoint", "http://localhost:4318/v1/logs");
    expected.setProperty("splunk.profiler.max.stack.depth", "1024");
    expected.setProperty("splunk.profiler.memory.enabled", "false");
    expected.setProperty("splunk.profiler.memory.event.rate", "150/s");
    expected.setProperty("splunk.profiler.memory.event.rate.limit.enabled", "true");
    expected.setProperty("splunk.profiler.memory.native.sampling", "false");
    expected.setProperty("splunk.profiler.otlp.protocol", "http/protobuf");
    expected.setProperty("splunk.profiler.recording.duration", "20000ms");
    expected.setProperty("splunk.profiler.tracing.stacks.only", "false");
    expected.setProperty("splunk.realm", "none");
    expected.setProperty("splunk.trace.response.header.enabled", "true");
    expected.setProperty("splunk.snapshot.profiler.enabled", "false");
    expected.setProperty("splunk.snapshot.selection.probability", "0.01");
    expected.setProperty("splunk.snapshot.profiler.max.stack.depth", "1024");
    expected.setProperty("splunk.snapshot.sampling.interval", "10ms");
    expected.setProperty("splunk.snapshot.profiler.export.interval", "5000ms");
    expected.setProperty("splunk.snapshot.profiler.staging.capacity", "2000");

    assertProperties(fileContent, expected);
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

  private static Properties toProperties(Map<String, String> values) {
    Properties properties = new Properties();
    values.forEach(properties::setProperty);
    return properties;
  }

  private static void assertProperties(Properties actual, Properties expected) {
    assertThat(actual.size()).isEqualTo(expected.size());
    for (String propertyName : expected.stringPropertyNames()) {
      assertProperty(actual, propertyName, expected.getProperty(propertyName));
    }
  }

  private static void assertProperty(
      Properties fileContent, String propertyName, String expectedValue) {
    assertThat(fileContent.getProperty(toEnvVarName(propertyName))).isEqualTo(expectedValue);
  }
}
