import com.diffplug.gradle.spotless.SpotlessExtension
import net.ltgt.gradle.errorprone.errorprone

plugins {
    `version-catalog`

    java
    alias(libs.plugins.errorprone)
}

tasks.withType<JavaCompile> {
    if (JavaVersion.current().isJava9Compatible) doFirst {
        options.compilerArgs = mutableListOf("--release", "8")
    }
    // Disable errorprone on generated code
    options.errorprone.excludedPaths.set(".*/src/main/java/build/buf/validate/conformance/.*")
}

// Disable javadoc for conformance tests
tasks.withType<Javadoc> {
    enabled = false
}

tasks {
    jar {
        manifest {
            attributes(mapOf("Main-Class" to "build.buf.Main"))
        }
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        // This line of code recursively collects and copies all of a project's files
        // and adds them to the JAR itself. One can extend this task, to skip certain
        // files or particular types at will
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) } +
                sourcesMain.output
        from(contents)
    }
}

apply(plugin = "com.diffplug.spotless")
configure<SpotlessExtension> {
    java {
        targetExclude("src/main/java/build/buf/validate/**/*.java")
    }
}

dependencies {
    implementation(project(":"))
    implementation(libs.guava)
    implementation(libs.protobuf.java)

    implementation(libs.assertj)
    implementation(libs.junit)

    errorprone(libs.errorprone)
}
