plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  compileOnly(project(":custom"))
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")

  testImplementation(project(":custom"))
  testCompileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  testRuntimeOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
}

tasks.withType<Test>().configureEach {
  jvmArgs("-Dotel.instrumentation.jvm-metrics-splunk.enabled=true")
}
