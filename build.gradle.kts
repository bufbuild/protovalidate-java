import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import com.diffplug.gradle.spotless.SpotlessExtension
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    `version-catalog`

    `java-library`
    alias(libs.plugins.errorprone.plugin)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile> {
    if (JavaVersion.current().isJava9Compatible) doFirst {
        options.compilerArgs = mutableListOf("--release", "8")
    }
    if (!name.lowercase().contains("test")) {
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", "build.buf.protovalidate")
        }
    }
}

tasks.withType<Javadoc> {
    // TODO: Enable when Javadoc changes are final
//    val stdOptions = options as StandardJavadocDocletOptions
//    stdOptions.addBooleanOption("Xwerror", true)
}

tasks.withType<GenerateModuleMetadata> {
    suppressedValidationErrors.add("enforced-platform")
}

buildscript {
    dependencies {
        classpath(libs.maven.plugin)
        classpath(libs.spotless)
    }
}

allprojects {
    repositories {
        mavenCentral()
        maven {
            name = "buf"
            url = uri("https://buf.build/gen/maven")
        }
    }
    apply(plugin = "com.diffplug.spotless")
    configure<SpotlessExtension> {
        setEnforceCheck(false) // Disables lint on gradle builds.
        java {
            importOrder()
            removeUnusedImports()
            googleJavaFormat()
            endWithNewline()
            trimTrailingWhitespace()
        }
    }
}

apply(plugin = "com.vanniktech.maven.publish.base")
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

dependencies {
    annotationProcessor(libs.nullaway)
    api(libs.protobuf.java)
    api(libs.protovalidate)
    implementation(enforcedPlatform(libs.cel))
    implementation(libs.cel.core)
    implementation(libs.guava)
    implementation(libs.jakarta.mail.api)

    testImplementation(libs.assertj)
    testImplementation(libs.junit)
    testImplementation(libs.protovalidate.testing)

    errorprone(libs.errorprone)
}
