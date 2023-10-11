import com.diffplug.gradle.spotless.SpotlessExtension
import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    `version-catalog`

    `java-library`
    alias(libs.plugins.errorprone)
    alias(libs.plugins.maven)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val bufCLIFile = project.layout.buildDirectory.file("gobin/buf").get().asFile
val bufCLIPath: String = bufCLIFile.absolutePath
val bufLicenseHeaderCLIFile = project.layout.buildDirectory.file("gobin/license-header").get().asFile
val bufLicenseHeaderCLIPath: String = bufLicenseHeaderCLIFile.absolutePath

tasks.register<Exec>("installBuf") {
    description = "Installs the Buf CLI."
    environment("GOBIN", bufCLIFile.parentFile.absolutePath)
    outputs.file(bufCLIFile)
    commandLine("go", "install", "github.com/bufbuild/buf/cmd/buf@latest")
}

tasks.register<Exec>("installLicenseHeader") {
    description = "Installs the Buf license-header CLI."
    environment("GOBIN", bufLicenseHeaderCLIFile.parentFile.absolutePath)
    outputs.file(bufLicenseHeaderCLIFile)
    commandLine("go", "install", "github.com/bufbuild/buf/private/pkg/licenseheader/cmd/license-header@latest")
}

tasks.register<Exec>("licenseHeader") {
    dependsOn("installLicenseHeader")
    description = "Runs the Buf license-header CLI."
    commandLine(
        bufLicenseHeaderCLIPath,
        "--license-type",
        "apache",
        "--copyright-holder",
        "Buf Technologies, Inc.",
        "--year-range",
        project.findProperty("license-header.years")!!.toString(),
        "--ignore",
        "src/main/java/build/buf/validate/",
        "--ignore",
        "conformance/src/main/java/build/buf/validate/conformance/",
    )
}

tasks.register<Exec>("generateTestSourcesImports") {
    dependsOn("installBuf")
    description = "Generates code with buf generate --include-imports for unit tests."
    commandLine(
        bufCLIPath,
        "generate",
        "--template",
        "src/test/resources/proto/buf.gen.imports.yaml",
        "src/test/resources/proto",
        "--include-imports",
    )
}

tasks.register<Exec>("generateTestSourcesNoImports") {
    dependsOn("installBuf")
    description = "Generates code with buf generate --include-imports for unit tests."
    commandLine(bufCLIPath, "generate", "--template", "src/test/resources/proto/buf.gen.noimports.yaml", "src/test/resources/proto")
}

tasks.register("generateTestSources") {
    dependsOn("generateTestSourcesImports", "generateTestSourcesNoImports")
    description = "Generates code with buf generate for unit tests"
}

tasks.register<Exec>("exportProtovalidateModule") {
    dependsOn("installBuf")
    description = "Exports the bufbuild/protovalidate module sources to src/main/resources."
    commandLine(
        bufCLIPath,
        "export",
        "buf.build/bufbuild/protovalidate:${project.findProperty("protovalidate.version")}",
        "--output",
        "src/main/resources",
    )
}

tasks.register<Exec>("generateSources") {
    dependsOn("installBuf")
    description = "Generates sources for the bufbuild/protovalidate module sources to src/main/java."
    commandLine(bufCLIPath, "generate", "--template", "buf.gen.yaml", "src/main/resources")
}

tasks.register<Exec>("generateConformance") {
    dependsOn("installBuf")
    description = "Generates sources for the bufbuild/protovalidate-testing module to conformance/src/main/java."
    commandLine(
        bufCLIPath,
        "generate",
        "--template",
        "conformance/buf.gen.yaml",
        "-o",
        "conformance/",
        "buf.build/bufbuild/protovalidate-testing:${project.findProperty("protovalidate.version")}",
    )
}

tasks.register("generate") {
    description = "Generates sources with buf generate and buf export."
    dependsOn(
        "generateTestSources",
        "exportProtovalidateModule",
        "generateSources",
        "generateConformance",
        "licenseHeader",
    )
}

tasks.withType<JavaCompile> {
    dependsOn("generateTestSources")
    if (JavaVersion.current().isJava9Compatible) {
        doFirst {
            options.compilerArgs = mutableListOf("--release", "8")
        }
    }
    // Disable errorprone on generated code
    options.errorprone.excludedPaths.set("(.*/src/main/java/build/buf/validate/.*|.*/build/generated/.*)")
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

sourceSets {
    test {
        java {
            srcDir(layout.buildDirectory.dir("generated/test-sources/bufgen"))
        }
    }
}

apply(plugin = "com.diffplug.spotless")
configure<SpotlessExtension> {
    java {
        targetExclude("src/main/java/build/buf/validate/**/*.java", "build/generated/test-sources/bufgen/**/*.java")
    }
    kotlinGradle {
        ktlint()
        target("**/*.kts")
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

mavenPublishing {
    val isAutoReleased = project.hasProperty("signingInMemoryKey")
    publishToMavenCentral(SonatypeHost.S01)
    if (isAutoReleased) {
        signAllPublications()
    }
    val releaseVersion = project.findProperty("releaseVersion") as String? ?: System.getenv("VERSION")
    coordinates("build.buf", "protovalidate", releaseVersion ?: "0.0.0-SNAPSHOT")
    pomFromGradleProperties()
    configure(
        JavaLibrary(
            // configures the -javadoc artifact, possible values:
            // - `JavadocJar.None()` don't publish this artifact
            // - `JavadocJar.Empty()` publish an empty jar
            // - `JavadocJar.Javadoc()` to publish standard javadocs
            javadocJar = JavadocJar.Javadoc(),
            // whether to publish a sources jar
            sourcesJar = true,
        ),
    )
    pom {
        name.set("protovalidate-java")
        group = "build.buf"
        // Default to snapshot versioning for local publishing.
        version = releaseVersion ?: "0.0.0-SNAPSHOT"
        description.set("Protocol Buffer Validation")
        url.set("https://github.com/bufbuild/protovalidate-java")
        licenses {
            license {
                name.set("The Apache Software License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
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
    implementation(enforcedPlatform(libs.cel))
    implementation(libs.cel.core)
    implementation(libs.guava)
    implementation(libs.jakarta.mail.api)

    testImplementation(libs.assertj)
    testImplementation(libs.junit)

    errorprone(libs.errorprone)
}
