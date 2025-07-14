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
        "build/generated/sources/bufgen/",
        "--ignore",
        "conformance/build/generated/sources/bufgen/",
        "--ignore",
        "src/main/resources/buf/validate/",
    )
}

tasks.register<Copy>("filterBufGenYaml") {
    from(".")
    include("buf.gen.yaml", "src/**/buf*gen*.yaml")
    includeEmptyDirs = false
    into(layout.buildDirectory.dir("buf-gen-templates"))
    expand("protocJavaPluginVersion" to "v${libs.versions.protobuf.get().substringAfter('.')}")
    filteringCharset = "UTF-8"
}

tasks.register<Exec>("generateTestSourcesImports") {
    dependsOn("exportProtovalidateModule", "filterBufGenYaml")
    description = "Generates code with buf generate --include-imports for unit tests."
    commandLine(
        buf.asPath,
        "generate",
        "--template",
        "${layout.buildDirectory.get()}/buf-gen-templates/src/test/resources/proto/buf.gen.imports.yaml",
        "--include-imports",
    )
}

tasks.register<Exec>("generateTestSourcesNoImports") {
    dependsOn("exportProtovalidateModule", "filterBufGenYaml")
    description = "Generates code with buf generate --include-imports for unit tests."
    commandLine(
        buf.asPath,
        "generate",
        "--template",
        "${layout.buildDirectory.get()}/buf-gen-templates/src/test/resources/proto/buf.gen.noimports.yaml",
    )
}

tasks.register<Exec>("generateCelConformance") {
    dependsOn("generateCelConformanceTestTypes", "filterBufGenYaml")
    description = "Generates CEL conformance code with buf generate for unit tests."
    commandLine(
        buf.asPath,
        "generate",
        "--template",
        "${layout.buildDirectory.get()}/buf-gen-templates/src/test/resources/proto/buf.gen.cel.yaml",
        "buf.build/google/cel-spec:${project.findProperty("cel.spec.version")}",
        "--exclude-path",
        "cel/expr/conformance/proto2",
        "--exclude-path",
        "cel/expr/conformance/proto3",
    )
}

// The conformance tests use the Protobuf package path for tests that use these types.
// i.e. cel.expr.conformance.proto3.TestAllTypes. But, if we use managed mode it adds 'com'
// to the prefix. Additionally, we can't disable managed mode because the java_package option
// specified in these proto files is "dev.cel.expr.conformance.proto3". So, to get around this,
// we're generating these separately and specifying a java_package override of the package we need.
tasks.register<Exec>("generateCelConformanceTestTypes") {
    dependsOn("exportProtovalidateModule", "filterBufGenYaml")
    description = "Generates CEL conformance test types with buf generate for unit tests using a Java package override."
    commandLine(
        buf.asPath,
        "generate",
        "--template",
        "${layout.buildDirectory.get()}/buf-gen-templates/src/test/resources/proto/buf.gen.cel.testtypes.yaml",
        "buf.build/google/cel-spec:${project.findProperty("cel.spec.version")}",
        "--path",
        "cel/expr/conformance/proto3",
    )
}

var getCelTestData =
    tasks.register<Exec>("getCelTestData") {
        val celVersion = project.findProperty("cel.spec.version")
        val fileUrl = "https://raw.githubusercontent.com/google/cel-spec/refs/tags/$celVersion/tests/simple/testdata/string_ext.textproto"
        val targetDir = File("${project.projectDir}/src/test/resources/testdata")
        val file = File(targetDir, "string_ext_$celVersion.textproto")

        onlyIf {
            // Only run curl if file doesn't exist
            !file.exists()
        }
        doFirst {
            file.parentFile.mkdirs()
            commandLine(
                "curl",
                "-fsSL",
                "-o",
                file.absolutePath,
                fileUrl,
            )
        }
    }

tasks.register("generateTestSources") {
    dependsOn("generateTestSourcesImports", "generateTestSourcesNoImports", "generateCelConformance")
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
    dependsOn("exportProtovalidateModule", "filterBufGenYaml")
    description = "Generates sources for the bufbuild/protovalidate module sources to build/generated/sources/bufgen."
    commandLine(buf.asPath, "generate", "--template", "${layout.buildDirectory.get()}/buf-gen-templates/buf.gen.yaml", "src/main/resources")
}

tasks.register("generate") {
    description = "Generates sources with buf generate and buf export."
    dependsOn(
        "generateTestSources",
        "generateSources",
        "licenseHeader",
    )
}

tasks.withType<JavaCompile> {
    dependsOn("generate")
    if (JavaVersion.current().isJava9Compatible) {
        doFirst {
            options.compilerArgs = mutableListOf("--release", "8")
        }
    }
    // Disable errorprone on generated code
    options.errorprone.excludedPaths.set(".*/build/generated/.*")
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
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated/sources/bufgen"))
        }
    }
    test {
        java {
            srcDir(layout.buildDirectory.dir("generated/test-sources/bufgen"))
        }
    }
}

apply(plugin = "com.diffplug.spotless")
configure<SpotlessExtension> {
    java {
        targetExclude("build/generated/sources/bufgen/build/buf/validate/**/*.java", "build/generated/test-sources/bufgen/**/*.java")
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
        dependsOn(getCelTestData)
        useJUnitPlatform()
        this.testLogging {
            events("failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
    }
}

mavenPublishing {
    val isAutoReleased = project.hasProperty("signingInMemoryKey")
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
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
    api(libs.jspecify)
    api(libs.protobuf.java)
    implementation(libs.cel) {
        // https://github.com/google/cel-java/issues/748
        exclude(group = "com.google.protobuf", module = "protobuf-javalite")
    }

    buf("build.buf:buf:${libs.versions.buf.get()}:${osdetector.classifier}@exe")

    testImplementation(libs.assertj)
    testImplementation(libs.grpc.protobuf)
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    errorprone(libs.errorprone.core)
}
