package com.splunk.opentelemetry.opamp;

import okio.ByteString;
import opamp.proto.AgentConfigFile;

import static java.nio.charset.StandardCharsets.UTF_8;

class DeclarativeEffectiveConfigFileFactory {
  DeclarativeEffectiveConfigFileFactory() {}

  AgentConfigFile createFile() {
    ByteString content = new ByteString(buildFileContent().getBytes(UTF_8));
    return new AgentConfigFile(content, "text/plain+properties");
  }

  private String buildFileContent() {
    return "";
  }
}
