plugins {
    `version-catalog`
    id("com.diffplug.spotless") version "6.13.0"
    java
    application
}

group = "build.buf"
version = "1.0.0-dev-1"

repositories {
    mavenCentral()
    maven {
        name = "buf"
        url = uri("https://buf.build/gen/maven")
    }
}

dependencies {
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.java.util)
    implementation(enforcedPlatform("org.projectnessie.cel:cel-bom:0.3.18"))
    implementation("org.projectnessie.cel:cel-tools")
    implementation("javax.mail:mail:1.4.7")
    implementation(libs.guava)
    implementation("build.buf.gen:bufbuild_protovalidate_protocolbuffers_java:23.3.0.1.20230704214709.336f5dd89681")

    testImplementation(libs.junit)
    testImplementation(libs.assertj)
    testImplementation(libs.protobuf.java)
    testImplementation(libs.protobuf.java.util)
    testImplementation("build.buf.gen:bufbuild_protovalidate-testing_protocolbuffers_java:23.3.0.1.20230704214710.61d5e0152a75")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
