import com.diffplug.gradle.spotless.SpotlessExtension
import net.ltgt.gradle.errorprone.errorprone

plugins {
    `version-catalog`

    application
    java
    alias(libs.plugins.errorprone)
}

val conformanceCLIFile =
    project.layout.buildDirectory
        .file("gobin/protovalidate-conformance")
        .get()
        .asFile
val conformanceCLIPath: String = conformanceCLIFile.absolutePath
val conformanceAppScript: String =
    project.layout.buildDirectory
        .file("install/conformance/bin/conformance")
        .get()
        .asFile.absolutePath
val conformanceArgs = (project.findProperty("protovalidate.conformance.args")?.toString() ?: "").split("\\s+".toRegex())

tasks.register<Exec>("installProtovalidateConformance") {
    description = "Installs the Protovalidate Conformance CLI."
    environment("GOBIN", conformanceCLIFile.parentFile.absolutePath)
    outputs.file(conformanceCLIFile)
    commandLine(
        "go",
        "install",
        "github.com/bufbuild/protovalidate/tools/protovalidate-conformance@${project.findProperty("protovalidate.version")}",
    )
}

tasks.register<Exec>("conformance") {
    dependsOn("installDist")
    description = "Runs protovalidate conformance tests."
    commandLine(*(listOf(conformanceCLIPath) + conformanceArgs + listOf(conformanceAppScript)).toTypedArray())
}

tasks.withType<JavaCompile> {
    if (JavaVersion.current().isJava9Compatible) {
        doFirst {
            options.compilerArgs = mutableListOf("--release", "8")
        }
    }
    // Disable errorprone on generated code
    options.errorprone.excludedPaths.set(".*/src/main/java/build/buf/validate/conformance/.*")
}

// Disable javadoc for conformance tests
tasks.withType<Javadoc> {
    enabled = false
}

application {
    mainClass.set("build.buf.protovalidate.conformance.Main")
}

tasks {
    jar {
        dependsOn(":jar")
        manifest {
            attributes(mapOf("Main-Class" to "build.buf.protovalidate.conformance.Main"))
        }
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        // This line of code recursively collects and copies all of a project's files
        // and adds them to the JAR itself. One can extend this task, to skip certain
        // files or particular types at will
        val sourcesMain = sourceSets.main.get()
        val contents =
            configurations.runtimeClasspath
                .get()
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
    implementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    errorprone(libs.errorprone)
}
