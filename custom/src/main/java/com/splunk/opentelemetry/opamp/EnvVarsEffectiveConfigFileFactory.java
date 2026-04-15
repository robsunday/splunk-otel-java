package com.splunk.opentelemetry.opamp;

import com.google.common.annotations.VisibleForTesting;
import com.splunk.opentelemetry.SplunkConfiguration;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import okio.ByteString;
import opamp.proto.AgentConfigFile;

import static com.splunk.opentelemetry.SplunkConfiguration.PROFILER_ENABLED_PROPERTY;
import static java.nio.charset.StandardCharsets.UTF_8;

class EnvVarsEffectiveConfigFileFactory {
  ConfigProperties config;

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

    addEnvVar(sb, PROFILER_ENABLED_PROPERTY, SplunkConfiguration.isProfilerEnabled(config));

    return new ByteString(sb.toString().getBytes(UTF_8));
  }

  @VisibleForTesting
  static String toEnvVarName(String name) {
    return name.toUpperCase().replace('.', '_').replace('-', '_');
  }

  private static void addEnvVar(StringBuilder sb, String propertyName, Object value) {
    sb.append(toEnvVarName(propertyName)).append('=').append(value).append('\n');
  }
}
