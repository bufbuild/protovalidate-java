plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.java.util)
    testImplementation(libs.junit)
    testRuntimeOnly(libs.mockito)
    testRuntimeOnly(libs.assertj)
}
