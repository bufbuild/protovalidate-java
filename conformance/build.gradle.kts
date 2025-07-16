import com.diffplug.gradle.spotless.SpotlessExtension
import net.ltgt.gradle.errorprone.errorprone

plugins {
    `version-catalog`

    application
    java
    alias(libs.plugins.errorprone)
    alias(libs.plugins.osdetector)
}

// Conformance tests aren't bound by lowest common library version.
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

val buf: Configuration by configurations.creating

tasks.register("configureBuf") {
    description = "Installs the Buf CLI."
    File(buf.asPath).setExecutable(true)
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
        "github.com/bufbuild/protovalidate/tools/protovalidate-conformance@next",
    )
}

tasks.register<Exec>("conformance") {
    dependsOn("installDist", "installProtovalidateConformance")
    description = "Runs protovalidate conformance tests."
    commandLine(*(listOf(conformanceCLIPath) + conformanceArgs + listOf(conformanceAppScript)).toTypedArray())
}

tasks.register<Copy>("filterBufGenYaml") {
    from(".")
    include("buf.gen.yaml")
    includeEmptyDirs = false
    into(layout.buildDirectory.dir("buf-gen-templates"))
    expand("protocJavaPluginVersion" to "v${libs.versions.protobuf.get().substringAfter('.')}")
    filteringCharset = "UTF-8"
}

tasks.register<Exec>("generateConformance") {
    dependsOn("configureBuf", "filterBufGenYaml")
    description = "Generates sources for the bufbuild/protovalidate-testing module to build/generated/sources/bufgen."
    commandLine(
        buf.asPath,
        "generate",
        "--template",
        "${layout.buildDirectory.get()}/buf-gen-templates/buf.gen.yaml",
        "https://github.com/bufbuild/protovalidate.git#branch=next,subdir=proto/protovalidate-testing",
    )
}

sourceSets {
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated/sources/bufgen"))
        }
    }
}

tasks.withType<JavaCompile> {
    dependsOn("generateConformance")
    if (JavaVersion.current().isJava9Compatible) {
        doFirst {
            options.compilerArgs = mutableListOf("--release", "8")
        }
    }
    // Disable errorprone on generated code
    options.errorprone.excludedPaths.set(".*/build/generated/sources/bufgen/.*")
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
        targetExclude("build/generated/sources/bufgen/**/*.java")
    }
}

dependencies {
    implementation(project(":"))
    implementation(libs.errorprone.annotations)
    implementation(libs.protobuf.java)

    implementation(libs.assertj)
    implementation(platform(libs.junit.bom))

    buf("build.buf:buf:${libs.versions.buf.get()}:${osdetector.classifier}@exe")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    errorprone(libs.errorprone.core)
}
