plugins {
    id("com.diffplug.spotless") version "6.13.0"
    java
    application
}

group = "build.buf"
version = "1.0.0-dev-1"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.28")
    annotationProcessor("org.projectlombok:lombok:1.18.28")
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.java.util)
    implementation(enforcedPlatform("org.projectnessie.cel:cel-bom:0.3.17"))
    implementation("org.projectnessie.cel:cel-tools")

    testCompileOnly("org.projectlombok:lombok:1.18.28")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.28")
    testImplementation(libs.junit)
    testRuntimeOnly(libs.mockito)
    testImplementation(libs.assertj)
    testImplementation(libs.protobuf.java)
    testImplementation(libs.protobuf.java.util)
}
