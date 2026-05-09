# Benchmarks

JMH microbenchmarks for protovalidate-java.
Used locally to quantify performance changes.
Not executed in CI; `./gradlew build` only verifies that benchmark code compiles.

## Prerequisites

- JDK 21
- `buf` CLI (installed automatically by Gradle)
- `jq` and `column` (preinstalled on macOS)

## Running benchmarks

Run all benchmarks:

```
./gradlew :benchmarks:jmh
```

Filter to a subset via `-Pbench` (accepts a regex over method names):

```
./gradlew :benchmarks:jmh -Pbench=validateSimple     # one method
./gradlew :benchmarks:jmh -Pbench='compile.*'        # prefix match
./gradlew :benchmarks:jmh -Pbench='validate.*'       # all steady-state
```

Results land in `build/results/jmh/results.json`.

## Comparing before and after a change

Typical A/B workflow:

```
# 1. run baseline on the current tree and save it
./gradlew :benchmarks:jmh -Pbench='compile.*' :benchmarks:jmhSaveBaseline

# 2. apply your change (edit code, or gh pr checkout <N>)

# 3. re-run and diff against the saved baseline
./gradlew :benchmarks:jmh -Pbench='compile.*' :benchmarks:jmhCompare
```

Output:

```
benchmark                    metric  before            after             delta
compileValidatorForRepeated  time    4696209.43 ns/op  1064942.21 ns/op  -77.3%
compileValidatorForRepeated  alloc   12950196.95 B/op  3262651.61 B/op   -74.8%
```

`jmhSaveBaseline` copies the current `results.json` to `results-before.json`.
`jmhCompare` diffs `results-before.json` against `results.json` by default.
Pass explicit paths with `-Pbefore=<path> -Pafter=<path>`.

## Adding a new benchmark

Benchmarks live in `src/jmh/java/...` and target proto messages in `src/jmh/proto/...`.

### 1. Define (or reuse) a proto message

Edit `src/jmh/proto/bench/v1/bench.proto` to add a message that exercises the code path you want to measure.
`buf generate` runs automatically before `compileJmhJava`, so no separate codegen step is needed.

### 2. Add a `@Benchmark` method

Edit `src/jmh/java/build/buf/protovalidate/benchmarks/ValidationBenchmark.java`.
Put one-time state (validator, messages) in `@Setup` and the measured work in the `@Benchmark` method.

Steady-state (hot-path) pattern:

```java
@Benchmark
public void validateMyMessage(Blackhole bh) throws ValidationException {
  bh.consume(validator.validate(myMessage));
}
```

Cold/compile-path pattern (each iteration builds a fresh validator):

```java
@Benchmark
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public void compileValidatorForMyMessage(Blackhole bh) throws CompilationException {
  Validator v = ValidatorFactory.newBuilder()
      .buildWithDescriptors(Collections.singletonList(MyMessage.getDescriptor()), false);
  bh.consume(v);
}
```

Choose based on what the change you want to measure actually touches.
`EvaluatorBuilder` caches compiled evaluators per descriptor, so after the first `validate()` call, further calls skip compilation.
If your fix is in the compile path (e.g. `RuleCache`, `DescriptorCacheBuilder`), a steady-state benchmark will not show the effect because `@Setup` absorbs it.

## Configuration

`build.gradle.kts` holds the JMH plugin config.
Defaults are tuned for fast local iteration (~30s per benchmark):

- 3 warmup iterations of 2s each
- 5 measurement iterations of 2s each
- 2 forks
- Average-time mode, nanoseconds
- GC profiler on (`gc.alloc.rate.norm` for per-op allocations)

For higher-confidence numbers (tighter confidence intervals, useful for deltas under ~10%), bump `fork`, `warmup`, and `timeOnIteration` in the `jmh {}` block.
Expect ~5 min per benchmark at `fork=5, warmup=5s, timeOnIteration=5s`.

## Metrics

Each benchmark emits:

- **Primary:** average time per `@Benchmark` invocation (`ns/op` by default).
- **Secondary (GC profiler):**
  - `gc.alloc.rate.norm` - bytes allocated per op; deterministic, used by `jmhCompare`.
  - `gc.alloc.rate` - allocation rate in MB/sec; varies with CPU.
  - `gc.count` / `gc.time` - GC activity during the run.

For allocation flame graphs, uncomment the `async` profiler line in `build.gradle.kts`.
Requires `async-profiler` installed locally.
