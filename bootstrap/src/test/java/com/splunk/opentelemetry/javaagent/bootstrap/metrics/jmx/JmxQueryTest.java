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

package com.splunk.opentelemetry.javaagent.bootstrap.metrics.jmx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.junit.jupiter.api.Test;

class JmxQueryTest {
  @Test
  void shouldConvertToObjectName() throws MalformedObjectNameException {
    var query = JmxQuery.create("com.splunk.test", Map.of("key", "value", "key2", "value2"));

    assertEquals(
        new ObjectName("com.splunk.test:key=value,key2=value2,*"), query.toObjectNameQuery());
  }

  @Test
  void shouldConvertToObjectName_noKeyPropertiesCriteria() throws MalformedObjectNameException {
    var query = JmxQuery.create("com.splunk.test", Map.of());

    assertEquals(new ObjectName("com.splunk.test:*"), query.toObjectNameQuery());
  }

  @Test
  void shouldNotMatchDifferentDomain() throws MalformedObjectNameException {
    var query = JmxQuery.create("com.splunk.test", "type", "Test");
    var objectName = new ObjectName("com.splunk.anotherDomain:type=Test");

    assertFalse(query.matches(objectName));
  }

  @Test
  void shouldNotMatchDifferentKeyProperties() throws MalformedObjectNameException {
    var query = JmxQuery.create("com.splunk.test", "type", "Test");
    var objectName = new ObjectName("com.splunk.test:type=AnotherType");

    assertFalse(query.matches(objectName));
  }

  @Test
  void shouldMatchObjectName() throws MalformedObjectNameException {
    var query = JmxQuery.create("com.splunk.test", "type", "Test");
    var objectName = new ObjectName("com.splunk.test:type=Test");

    assertTrue(query.matches(objectName));
  }
}
