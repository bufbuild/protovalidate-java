import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    `version-catalog`
    id("com.diffplug.spotless") version "6.13.0"
    java
    application
    id("com.vanniktech.maven.publish") version "0.25.3"
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
    implementation(enforcedPlatform("org.projectnessie.cel:cel-bom:0.3.19"))
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

mavenPublishing {
    plugins.withId("com.vanniktech.maven.publish.base") {
        configure<MavenPublishBaseExtension> {
            val isAutoReleased = project.hasProperty("signingInMemoryKey")
            publishToMavenCentral(SonatypeHost.S01)
            if (isAutoReleased) {
                signAllPublications()
            }
            pom {
                description.set("Protocol Buffer Validation")
                name.set("protovalidate") // This is overwritten in subprojects.
                group = "build.buf"
                val releaseVersion = project.findProperty("releaseVersion") as String? ?: System.getenv("VERSION")
                // Default to snapshot versioning for local publishing.
                version = releaseVersion ?: "0.0.0-SNAPSHOT"
                url.set("https://github.com/bufbuild/protovalidate-java")
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("bufbuild")
                        name.set("Buf Technologies")
                    }
                }
                scm {
                    url.set("https://github.com/bufbuild/protovalidate-java")
                    connection.set("scm:git:https://github.com/bufbuild/protovalidate-java.git")
                    developerConnection.set("scm:git:ssh://git@github.com/bufbuild/protovalidate-java.git")
                }
            }
        }
    }
}

tasks.withType<GenerateModuleMetadata> {
    suppressedValidationErrors.add("enforced-platform")
}
