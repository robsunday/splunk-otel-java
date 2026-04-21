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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import okio.ByteString;
import opamp.proto.AgentConfigFile;

class DeclarativeEffectiveConfigFileFactory implements EffectiveConfigFactory {
  private static final Logger logger =
      Logger.getLogger(DeclarativeEffectiveConfigFileFactory.class.getName());

  private static final YAMLMapper YAML_MAPPER =
      YAMLMapper.builder().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER).build();

  DeclarativeEffectiveConfigFileFactory() {}

  @Override
  public AgentConfigFile createFile() {
    ByteString content = new ByteString(buildFileContent().getBytes(UTF_8));
    return new AgentConfigFile(content, "application/yaml");
  }

  private String buildFileContent() {
    String declarativeConfigFilePath =
        System.getProperty("otel.config.file", System.getenv("OTEL_CONFIG_FILE"));
    if (declarativeConfigFilePath == null) {
      logger.warning("Unable to get declarative config file path");
      return "";
    }

    OpenTelemetryConfigurationModel model = loadDeclarativeConfigFile(declarativeConfigFilePath);
    if (model == null) {
      return "";
    }

    model = postprocessModel(model);

    try {
      return YAML_MAPPER.writeValueAsString(model);
    } catch (JsonProcessingException e) {
      logger.log(Level.SEVERE, "Error serializing declarative config model", e);
      return "";
    }
  }

  @VisibleForTesting
  static OpenTelemetryConfigurationModel postprocessModel(OpenTelemetryConfigurationModel model) {
    // Masking of sensitive data and removing unnecessary data will happen here
    return model;
  }

  @Nullable
  private static OpenTelemetryConfigurationModel loadDeclarativeConfigFile(
      String declarativeConfigFilePath) {
    OpenTelemetryConfigurationModel model = null;

    try (InputStream fis =
        new BufferedInputStream(Files.newInputStream(Paths.get(declarativeConfigFilePath)))) {
      model = DeclarativeConfiguration.parse(fis);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Error reading declarative config file", e);
    }

    return model;
  }
}
