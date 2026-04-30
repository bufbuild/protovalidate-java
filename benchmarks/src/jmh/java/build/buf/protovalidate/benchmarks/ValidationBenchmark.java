// Copyright 2023-2026 Buf Technologies, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package build.buf.protovalidate.benchmarks;

import build.buf.protovalidate.Validator;
import build.buf.protovalidate.ValidatorFactory;
import build.buf.protovalidate.benchmarks.gen.BenchComplexSchema;
import build.buf.protovalidate.benchmarks.gen.BenchGT;
import build.buf.protovalidate.benchmarks.gen.BenchMap;
import build.buf.protovalidate.benchmarks.gen.BenchRepeatedBytesUnique;
import build.buf.protovalidate.benchmarks.gen.BenchRepeatedMessage;
import build.buf.protovalidate.benchmarks.gen.BenchRepeatedScalar;
import build.buf.protovalidate.benchmarks.gen.BenchRepeatedScalarUnique;
import build.buf.protovalidate.benchmarks.gen.BenchScalar;
import build.buf.protovalidate.benchmarks.gen.ManyUnruledFieldsMessage;
import build.buf.protovalidate.benchmarks.gen.MultiRule;
import build.buf.protovalidate.benchmarks.gen.RegexPatternMessage;
import build.buf.protovalidate.benchmarks.gen.RepeatedRuleMessage;
import build.buf.protovalidate.benchmarks.gen.SimpleStringMessage;
import build.buf.protovalidate.benchmarks.gen.StringMatching;
import build.buf.protovalidate.benchmarks.gen.TestByteMatching;
import build.buf.protovalidate.benchmarks.gen.WrapperTesting;
import build.buf.protovalidate.exceptions.ValidationException;
import com.google.protobuf.Descriptors.FieldDescriptor;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Steady-state validation benchmarks. Exercises the hot path after the evaluator cache is warm.
 *
 * <p>The set of {@code validateBench*} methods mirrors the Go benchmark suite in
 * protovalidate-go's {@code validator_bench_test.go} and provides the baseline against which the
 * native-rules port measures its improvements. The original {@code validate*} methods exercise
 * past PR fixes (tautology skip, AST cache, etc.) and remain as regression guards.
 *
 * <p>Phase 1 will refactor this into a {@code @Param}-driven A/B once {@code
 * Config.disableNativeRules} exists; for now this is the single-mode pre-port baseline.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class ValidationBenchmark {

  private Validator validator;

  // --- Existing regression-guard fixtures ---
  private SimpleStringMessage simple;
  private ManyUnruledFieldsMessage manyUnruled;
  private RepeatedRuleMessage repeatedRule;
  private RegexPatternMessage regexPattern;

  // --- Native-rules port fixtures ---
  private BenchScalar benchScalar;
  private BenchRepeatedScalar benchRepeatedScalar;
  private BenchRepeatedMessage benchRepeatedMessage;
  private BenchRepeatedScalarUnique benchRepeatedScalarUnique;
  private BenchRepeatedBytesUnique benchRepeatedBytesUnique;
  private BenchMap benchMap;
  private BenchComplexSchema benchComplexSchema;
  private BenchGT benchGT;
  private TestByteMatching testByteMatching;
  private StringMatching stringMatching;
  private WrapperTesting wrapperTesting;
  private MultiRule multiRuleNoError;
  private MultiRule multiRuleError;

  @Setup
  public void setup() throws ValidationException {
    validator = ValidatorFactory.newBuilder().build();

    simple = SimpleStringMessage.newBuilder().setEmail("alice@example.com").build();

    manyUnruled =
        ManyUnruledFieldsMessage.newBuilder()
            .setNonEmpty("x")
            .setF1("v1")
            .setF2("v2")
            .setF3("v3")
            .setF4("v4")
            .setF5("v5")
            .setF6("v6")
            .setF7("v7")
            .setF8("v8")
            .setF9("v9")
            .build();

    RepeatedRuleMessage.Builder repeatedRuleBuilder = RepeatedRuleMessage.newBuilder();
    for (FieldDescriptor fd : RepeatedRuleMessage.getDescriptor().getFields()) {
      repeatedRuleBuilder.setField(fd, "v");
    }
    repeatedRule = repeatedRuleBuilder.build();

    regexPattern = RegexPatternMessage.newBuilder().setName("Alice Example").build();

    benchScalar = BenchFixtures.benchScalar();
    benchRepeatedScalar = BenchFixtures.benchRepeatedScalar();
    benchRepeatedMessage = BenchFixtures.benchRepeatedMessage();
    benchRepeatedScalarUnique = BenchFixtures.benchRepeatedScalarUnique();
    benchRepeatedBytesUnique = BenchFixtures.benchRepeatedBytesUnique();
    benchMap = BenchFixtures.benchMap();
    benchComplexSchema = BenchFixtures.benchComplexSchema();
    benchGT = BenchFixtures.benchGT();
    testByteMatching = BenchFixtures.testByteMatching();
    stringMatching = BenchFixtures.stringMatching();
    wrapperTesting = BenchFixtures.wrapperTesting();
    multiRuleNoError = BenchFixtures.multiRuleNoError();
    multiRuleError = BenchFixtures.multiRuleError();

    // Warm evaluator cache for steady-state benchmarks.
    validator.validate(simple);
    validator.validate(manyUnruled);
    validator.validate(repeatedRule);
    validator.validate(regexPattern);
    validator.validate(benchScalar);
    validator.validate(benchRepeatedScalar);
    validator.validate(benchRepeatedMessage);
    validator.validate(benchRepeatedScalarUnique);
    validator.validate(benchRepeatedBytesUnique);
    validator.validate(benchMap);
    validator.validate(benchComplexSchema);
    validator.validate(benchGT);
    validator.validate(testByteMatching);
    validator.validate(stringMatching);
    validator.validate(wrapperTesting);
    validator.validate(multiRuleNoError);
    validator.validate(multiRuleError);
  }

  // --- Existing regression-guard benchmarks ---

  @Benchmark
  public void validateSimple(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(simple));
  }

  @Benchmark
  public void validateManyUnruled(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(manyUnruled));
  }

  @Benchmark
  public void validateRepeatedRule(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(repeatedRule));
  }

  @Benchmark
  public void validateRegexPattern(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(regexPattern));
  }

  // --- Native-rules port benchmarks (mirror Go BenchmarkXxx names) ---

  @Benchmark
  public void validateBenchScalar(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchScalar));
  }

  @Benchmark
  public void validateBenchRepeatedScalar(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchRepeatedScalar));
  }

  @Benchmark
  public void validateBenchRepeatedMessage(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchRepeatedMessage));
  }

  @Benchmark
  public void validateBenchRepeatedScalarUnique(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchRepeatedScalarUnique));
  }

  @Benchmark
  public void validateBenchRepeatedBytesUnique(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchRepeatedBytesUnique));
  }

  @Benchmark
  public void validateBenchMap(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchMap));
  }

  @Benchmark
  public void validateBenchComplexSchema(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchComplexSchema));
  }

  @Benchmark
  public void validateBenchInt32GT(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchGT));
  }

  @Benchmark
  public void validateTestByteMatching(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(testByteMatching));
  }

  @Benchmark
  public void validateStringMatching(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(stringMatching));
  }

  @Benchmark
  public void validateWrapperTesting(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(wrapperTesting));
  }

  @Benchmark
  public void validateMultiRuleNoError(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(multiRuleNoError));
  }

  @Benchmark
  public void validateMultiRuleError(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(multiRuleError));
  }
}