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
}

dependencies {
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.java.util)
    implementation(enforcedPlatform("org.projectnessie.cel:cel-bom:0.3.17"))
    implementation("org.projectnessie.cel:cel-tools")
    implementation("javax.mail:mail:1.4.7")

    testImplementation(libs.junit)
    testImplementation(libs.assertj)
    testImplementation(libs.protobuf.java)
    testImplementation(libs.protobuf.java.util)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
