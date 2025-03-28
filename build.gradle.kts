import com.diffplug.gradle.spotless.SpotlessExtension
import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    `java-library`
    alias(libs.plugins.errorprone)
    alias(libs.plugins.git)
    alias(libs.plugins.maven)
    alias(libs.plugins.osdetector)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

// The releaseVersion property is set on official releases in the release.yml workflow.
// If not specified, we attempt to calculate a snapshot version based on the last tagged release.
// So if the local build's last tag was v0.1.9, this will set snapshotVersion to 0.1.10-SNAPSHOT.
// If this fails for any reason, we'll fall back to using 0.0.0-SNAPSHOT version.
val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra
val details = versionDetails()
var snapshotVersion = "0.0.0-SNAPSHOT"
val matchResult = """^v(\d+)\.(\d+)\.(\d+)$""".toRegex().matchEntire(details.lastTag)
if (matchResult != null) {
    val (major, minor, patch) = matchResult.destructured
    snapshotVersion = "$major.$minor.${patch.toInt() + 1}-SNAPSHOT"
}
val releaseVersion = project.findProperty("releaseVersion") as String? ?: snapshotVersion

val buf: Configuration by configurations.creating
val bufLicenseHeaderCLIFile =
    project.layout.buildDirectory
        .file("gobin/license-header")
        .get()
        .asFile
val bufLicenseHeaderCLIPath: String = bufLicenseHeaderCLIFile.absolutePath

tasks.register("configureBuf") {
    description = "Installs the Buf CLI."
    File(buf.asPath).setExecutable(true)
}

tasks.register<Exec>("installLicenseHeader") {
    description = "Installs the Buf license-header CLI."
    environment("GOBIN", bufLicenseHeaderCLIFile.parentFile.absolutePath)
    outputs.file(bufLicenseHeaderCLIFile)
    commandLine("go", "install", "github.com/bufbuild/buf/private/pkg/licenseheader/cmd/license-header@v${libs.versions.buf.get()}")
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
        "--ignore",
        "src/main/resources/buf/validate/",
    )
}

tasks.register<Exec>("generateTestSourcesImports") {
    dependsOn("exportProtovalidateModule")
    description = "Generates code with buf generate --include-imports for unit tests."
    commandLine(
        buf.asPath,
        "generate",
        "--template",
        "src/test/resources/proto/buf.gen.imports.yaml",
        "--include-imports",
    )
}

tasks.register<Exec>("generateTestSourcesNoImports") {
    dependsOn("exportProtovalidateModule")
    description = "Generates code with buf generate --include-imports for unit tests."
    commandLine(buf.asPath, "generate", "--template", "src/test/resources/proto/buf.gen.noimports.yaml")
}

tasks.register("generateTestSources") {
    dependsOn("generateTestSourcesImports", "generateTestSourcesNoImports")
    description = "Generates code with buf generate for unit tests"
}

tasks.register<Exec>("exportProtovalidateModule") {
    dependsOn("configureBuf")
    description = "Exports the bufbuild/protovalidate module sources to src/main/resources."
    commandLine(
        buf.asPath,
        "export",
        "buf.build/bufbuild/protovalidate:${project.findProperty("protovalidate.version")}",
        "--output",
        "src/main/resources",
    )
}

tasks.register<Exec>("generateSources") {
    dependsOn("exportProtovalidateModule")
    description = "Generates sources for the bufbuild/protovalidate module sources to src/main/java."
    commandLine(buf.asPath, "generate", "--template", "buf.gen.yaml", "src/main/resources")
}

tasks.register<Exec>("generateConformance") {
    dependsOn("configureBuf")
    description = "Generates sources for the bufbuild/protovalidate-testing module to conformance/src/main/java."
    commandLine(
        buf.asPath,
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
    val stdOptions = options as StandardJavadocDocletOptions
    stdOptions.addBooleanOption("Xwerror", true)
    // Ignore warnings for generated code.
    stdOptions.addBooleanOption("Xdoclint/package:-build.buf.validate,-build.buf.validate.priv", true)
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
    version = releaseVersion
    repositories {
        mavenCentral()
    }
    apply(plugin = "com.diffplug.spotless")
    configure<SpotlessExtension> {
        isEnforceCheck = false // Disables lint on gradle builds.
        java {
            importOrder()
            removeUnusedImports()
            replaceRegex("Remove wildcard imports", "import\\s+[^\\*\\s]+\\*;(\\r\\n|\\r|\\n)", "$1")
            googleJavaFormat()
            endWithNewline()
            trimTrailingWhitespace()
        }
    }
    tasks.withType<Jar>().configureEach {
        if (name == "jar") {
            manifest {
                attributes("Implementation-Version" to releaseVersion)
            }
        }
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

mavenPublishing {
    val isAutoReleased = project.hasProperty("signingInMemoryKey")
    publishToMavenCentral(SonatypeHost.S01)
    if (isAutoReleased) {
        signAllPublications()
    }
    coordinates("build.buf", "protovalidate", releaseVersion)
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
    implementation(libs.ipaddress)

    buf("build.buf:buf:${libs.versions.buf.get()}:${osdetector.classifier}@exe")

    testImplementation(libs.assertj)
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    errorprone(libs.errorprone)
}
