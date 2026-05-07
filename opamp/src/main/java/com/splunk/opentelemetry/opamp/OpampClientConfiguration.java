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

public class OpampClientConfiguration {
  private boolean enabled;
  private String endpoint;
  private long pollingInterval;

  private OpampClientConfiguration() {}

  public static Builder builder() {
    return new Builder();
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public long getPollingInterval() {
    return pollingInterval;
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("OpampClientConfiguration{");
    sb.append("enabled='").append(enabled).append('\'');
    sb.append(", endpoint='").append(endpoint).append('\'');
    sb.append(", pollingInterval=").append(pollingInterval);
    sb.append('}');
    return sb.toString();
  }

  public static class Builder {
    OpampClientConfiguration configuredInstance = new OpampClientConfiguration();

    private Builder() {}

    public Builder withEnabled(boolean enabled) {
      configuredInstance.enabled = enabled;
      return this;
    }

    public Builder withEndpoint(String endpoint) {
      configuredInstance.endpoint = endpoint;
      return this;
    }

    public Builder withPollingInterval(long pollingInterval) {
      configuredInstance.pollingInterval = pollingInterval;
      return this;
    }

    public OpampClientConfiguration build() {
      return configuredInstance;
    }
  }
}
