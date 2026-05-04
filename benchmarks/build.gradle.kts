import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    java
    alias(libs.plugins.jmh)
    alias(libs.plugins.osdetector)
}

// JMH can use modern bytecode; benchmarks aren't shipped.
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

configure<SpotlessExtension> {
    java {
        targetExclude("build/generated/**/*.java")
    }
}

val buf: Configuration by configurations.creating

tasks.register("configureBuf") {
    description = "Installs the Buf CLI."
    File(buf.asPath).setExecutable(true)
}

tasks.register<Copy>("filterBufGenYaml") {
    from(files("buf.gen.yaml"))
    includeEmptyDirs = false
    into(layout.buildDirectory.dir("buf-gen-templates"))
    expand("protocJavaPluginVersion" to "v${libs.versions.protobuf.get().substringAfter('.')}")
    filteringCharset = "UTF-8"
}

tasks.register<Exec>("generateBenchmarkSources") {
    dependsOn("configureBuf", "filterBufGenYaml")
    description = "Generates Java sources for benchmark protos via buf generate."
    val template = layout.buildDirectory.file("buf-gen-templates/buf.gen.yaml")
    inputs.files(buf)
    inputs.dir("src/jmh/proto")
    inputs.file("buf.yaml")
    inputs.file(template)
    outputs.dir(layout.buildDirectory.dir("generated/sources/bufgen"))
    commandLine(buf.asPath, "generate", "--template", template.get().asFile.absolutePath)
}

sourceSets {
    named("jmh") {
        java {
            srcDir(layout.buildDirectory.dir("generated/sources/bufgen"))
        }
    }
}

tasks.matching { it.name == "compileJmhJava" }.configureEach {
    dependsOn("generateBenchmarkSources")
}

// Ensure `./gradlew build` (and `make build`) compiles the JMH sources so CI
// catches breakages in benchmark code. Execution remains gated behind the
// explicit `:benchmarks:jmh` task.
tasks.named("build") {
    dependsOn("compileJmhJava")
}

dependencies {
    jmhImplementation(project(":"))
    jmhImplementation(libs.protobuf.java)
    buf("build.buf:buf:${libs.versions.buf.get()}:${osdetector.classifier}@exe")
}

// Benchmarks produce fresh timing data each run; disable Gradle's up-to-date
// check so the task always executes (otherwise -Pbench changes are ignored).
tasks.named("jmh") {
    outputs.upToDateWhen { false }
}

jmh {
    // Defaults tuned for fast local A/B runs (~90s total).
    // For higher-confidence numbers bump iteration time and fork count.
    warmupIterations.set(3)
    warmup.set("2s")
    iterations.set(5)
    timeOnIteration.set("2s")
    fork.set(2)
    timeUnit.set("ns")
    benchmarkMode.set(listOf("avgt"))
    resultFormat.set("JSON")
    // GC profiler reports bytes allocated per op (gc.alloc.rate.norm), which
    // jmhCompare can diff alongside timing. ~5-10% overhead on timings.
    profilers.set(listOf("gc"))
    // For allocation flame graphs (requires async-profiler installed locally):
    // profilers.set(listOf("async:event=alloc;output=flamegraph;dir=build/reports/jmh/async"))

    // Filter to a subset of benchmarks via `-Pbench=<regex>`. Example:
    //   ./gradlew :benchmarks:jmh -Pbench=validateSimple
    //   ./gradlew :benchmarks:jmh -Pbench='compile.*'
    project.findProperty("bench")?.toString()?.let {
        includes.set(listOf(it))
    }
}

val jmhResults = layout.buildDirectory.file("results/jmh/results.json")
val jmhBaseline = layout.buildDirectory.file("results/jmh/results-before.json")

// Saves the latest JMH results.json as the baseline for jmhCompare.
//
// Usage:
//   ./gradlew :benchmarks:jmh :benchmarks:jmhSaveBaseline
//   # apply change...
//   ./gradlew :benchmarks:jmh :benchmarks:jmhCompare
tasks.register<Copy>("jmhSaveBaseline") {
    description = "Copies the latest JMH results.json to results-before.json as the baseline."
    from(jmhResults)
    into(jmhResults.get().asFile.parentFile)
    rename { "results-before.json" }
    mustRunAfter("jmh")
}

// Diffs two JMH results.json files as a concise benchstat-style table.
// Defaults to comparing results-before.json (written by jmhSaveBaseline)
// against the latest results.json.
//
// Override paths:
//   ./gradlew :benchmarks:jmhCompare -Pbefore=a.json -Pafter=b.json
tasks.register<Exec>("jmhCompare") {
    description = "Diffs two JMH result JSON files as a concise table."
    val before =
        project.findProperty("before")?.toString()
            ?: jmhBaseline.get().asFile.absolutePath
    val after =
        project.findProperty("after")?.toString()
            ?: jmhResults.get().asFile.absolutePath
    val jqScript = file("jmh-compare.jq").absolutePath
    commandLine(
        "bash",
        "-c",
        "jq --slurp --raw-output --from-file \"\$1\" \"\$2\" \"\$3\" | column -t -s \$'\\t'",
        "jmh-compare", // $0
        jqScript, // $1
        before, // $2
        after, // $3
    )
}

// Diffs the two enableNativeRules variants within a single results.json.
// `before` column is CEL (enableNativeRules=false), `after` is native
// (enableNativeRules=true), so a negative delta means native is faster /
// allocates less.
//
// Override the input file:
//   ./gradlew :benchmarks:jmhCompareParams -Presults=path/to/results.json
tasks.register<Exec>("jmhCompareParams") {
    description = "Diffs enableNativeRules=true vs false from a single JMH results.json."
    val results =
        project.findProperty("results")?.toString()
            ?: jmhResults.get().asFile.absolutePath
    val jqScript = file("jmh-compare-params.jq").absolutePath
    commandLine(
        "bash",
        "-c",
        "jq --raw-output --from-file \"\$1\" \"\$2\" | column -t -s \$'\\t'",
        "jmh-compare-params", // $0
        jqScript, // $1
        results, // $2
    )
}
