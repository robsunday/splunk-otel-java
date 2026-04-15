package com.splunk.opentelemetry.opamp;

import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Properties;

import static com.splunk.opentelemetry.SplunkConfiguration.PROFILER_ENABLED_PROPERTY;
import static com.splunk.opentelemetry.opamp.EnvVarsEffectiveConfigFileFactory.toEnvVarName;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class EnvVarsEffectiveConfigFileFactoryTest {
  @Test
  void testCreateFile() throws IOException {
    var configMap = new HashMap<String, String>();
    configMap.put(PROFILER_ENABLED_PROPERTY, Boolean.TRUE.toString());

    var config = DefaultConfigProperties.createFromMap(configMap);

    var file = EnvVarsEffectiveConfigFileFactory.createFileContent(config);
    var fileContent = new Properties();
    fileContent.load(new StringReader(file.utf8()));

    assertThat(fileContent.getProperty(toEnvVarName(PROFILER_ENABLED_PROPERTY))).isEqualTo("true");
  }
}
