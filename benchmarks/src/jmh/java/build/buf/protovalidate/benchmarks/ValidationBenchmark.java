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

import build.buf.protovalidate.Config;
import build.buf.protovalidate.Validator;
import build.buf.protovalidate.ValidatorFactory;
import build.buf.protovalidate.benchmarks.gen.BenchBoolConst;
import build.buf.protovalidate.benchmarks.gen.BenchBytesConst;
import build.buf.protovalidate.benchmarks.gen.BenchBytesIn;
import build.buf.protovalidate.benchmarks.gen.BenchComplexSchema;
import build.buf.protovalidate.benchmarks.gen.BenchDoubleIn;
import build.buf.protovalidate.benchmarks.gen.BenchEnumConst;
import build.buf.protovalidate.benchmarks.gen.BenchEnumNotIn;
import build.buf.protovalidate.benchmarks.gen.BenchEnumRules;
import build.buf.protovalidate.benchmarks.gen.BenchGT;
import build.buf.protovalidate.benchmarks.gen.BenchInt64Const;
import build.buf.protovalidate.benchmarks.gen.BenchInt64In;
import build.buf.protovalidate.benchmarks.gen.BenchMap;
import build.buf.protovalidate.benchmarks.gen.BenchRepeatedBytesUnique;
import build.buf.protovalidate.benchmarks.gen.BenchRepeatedInt32Unique;
import build.buf.protovalidate.benchmarks.gen.BenchRepeatedMessage;
import build.buf.protovalidate.benchmarks.gen.BenchRepeatedScalar;
import build.buf.protovalidate.benchmarks.gen.BenchRepeatedScalarUnique;
import build.buf.protovalidate.benchmarks.gen.BenchRepeatedStringUnique;
import build.buf.protovalidate.benchmarks.gen.BenchScalar;
import build.buf.protovalidate.benchmarks.gen.BenchStringConst;
import build.buf.protovalidate.benchmarks.gen.BenchStringContains;
import build.buf.protovalidate.benchmarks.gen.BenchStringIn;
import build.buf.protovalidate.benchmarks.gen.BenchStringLen;
import build.buf.protovalidate.benchmarks.gen.BenchStringMinLen;
import build.buf.protovalidate.benchmarks.gen.BenchStringPrefix;
import build.buf.protovalidate.benchmarks.gen.BenchUint32In;
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
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Steady-state validation benchmarks. Exercises the hot path after the evaluator cache is warm.
 *
 * <p>The set of {@code validateBench*} methods mirrors the Go benchmark suite in protovalidate-go's
 * {@code validator_bench_test.go} and provides the baseline against which the native-rules port
 * measures its improvements. The original {@code validate*} methods exercise past PR fixes
 * (tautology skip, AST cache, etc.) and remain as regression guards.
 *
 * <p>The {@code enableNativeRules} parameter A/Bs the native-rules flag: {@code "false"} matches
 * the Phase 0 CEL-only baseline; {@code "true"} measures native evaluation. Each subsequent phase
 * reports the gap between the two modes for its covered benchmarks.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class ValidationBenchmark {

  @Param({"false", "true"})
  public boolean enableNativeRules;

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
  private BenchBoolConst benchBoolConst;
  private BenchEnumRules benchEnumRules;

  // Single-rule fixtures filling earlier coverage gaps.
  private BenchStringConst benchStringConst;
  private BenchStringLen benchStringLen;
  private BenchStringMinLen benchStringMinLen;
  private BenchStringPrefix benchStringPrefix;
  private BenchStringContains benchStringContains;
  private BenchStringIn benchStringIn;
  private BenchBytesConst benchBytesConst;
  private BenchBytesIn benchBytesIn;
  private BenchInt64Const benchInt64Const;
  private BenchInt64In benchInt64In;
  private BenchUint32In benchUint32In;
  private BenchDoubleIn benchDoubleIn;
  private BenchEnumConst benchEnumConst;
  private BenchEnumNotIn benchEnumNotIn;
  private BenchRepeatedStringUnique benchRepeatedStringUnique;
  private BenchRepeatedInt32Unique benchRepeatedInt32Unique;

  @Setup
  public void setup() throws ValidationException {
    Config config;
    if (enableNativeRules) {
      config = Config.newBuilder().setEnableNativeRules().build();
    } else {
      config = Config.newBuilder().setDisableNativeRules().build();
    }

    validator = ValidatorFactory.newBuilder().withConfig(config).build();

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
    benchBoolConst = BenchFixtures.benchBoolConst();
    benchEnumRules = BenchFixtures.benchEnumRules();

    benchStringConst = BenchFixtures.benchStringConst();
    benchStringLen = BenchFixtures.benchStringLen();
    benchStringMinLen = BenchFixtures.benchStringMinLen();
    benchStringPrefix = BenchFixtures.benchStringPrefix();
    benchStringContains = BenchFixtures.benchStringContains();
    benchStringIn = BenchFixtures.benchStringIn();
    benchBytesConst = BenchFixtures.benchBytesConst();
    benchBytesIn = BenchFixtures.benchBytesIn();
    benchInt64Const = BenchFixtures.benchInt64Const();
    benchInt64In = BenchFixtures.benchInt64In();
    benchUint32In = BenchFixtures.benchUint32In();
    benchDoubleIn = BenchFixtures.benchDoubleIn();
    benchEnumConst = BenchFixtures.benchEnumConst();
    benchEnumNotIn = BenchFixtures.benchEnumNotIn();
    benchRepeatedStringUnique = BenchFixtures.benchRepeatedStringUnique();
    benchRepeatedInt32Unique = BenchFixtures.benchRepeatedInt32Unique();

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
    validator.validate(benchBoolConst);
    validator.validate(benchEnumRules);
    validator.validate(benchStringConst);
    validator.validate(benchStringLen);
    validator.validate(benchStringMinLen);
    validator.validate(benchStringPrefix);
    validator.validate(benchStringContains);
    validator.validate(benchStringIn);
    validator.validate(benchBytesConst);
    validator.validate(benchBytesIn);
    validator.validate(benchInt64Const);
    validator.validate(benchInt64In);
    validator.validate(benchUint32In);
    validator.validate(benchDoubleIn);
    validator.validate(benchEnumConst);
    validator.validate(benchEnumNotIn);
    validator.validate(benchRepeatedStringUnique);
    validator.validate(benchRepeatedInt32Unique);
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

  @Benchmark
  public void validateBenchBoolConst(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchBoolConst));
  }

  @Benchmark
  public void validateBenchEnumRules(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchEnumRules));
  }

  // --- Single-rule fixtures filling earlier coverage gaps ---

  @Benchmark
  public void validateBenchStringConst(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchStringConst));
  }

  @Benchmark
  public void validateBenchStringLen(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchStringLen));
  }

  @Benchmark
  public void validateBenchStringMinLen(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchStringMinLen));
  }

  @Benchmark
  public void validateBenchStringPrefix(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchStringPrefix));
  }

  @Benchmark
  public void validateBenchStringContains(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchStringContains));
  }

  @Benchmark
  public void validateBenchStringIn(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchStringIn));
  }

  @Benchmark
  public void validateBenchBytesConst(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchBytesConst));
  }

  @Benchmark
  public void validateBenchBytesIn(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchBytesIn));
  }

  @Benchmark
  public void validateBenchInt64Const(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchInt64Const));
  }

  @Benchmark
  public void validateBenchInt64In(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchInt64In));
  }

  @Benchmark
  public void validateBenchUint32In(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchUint32In));
  }

  @Benchmark
  public void validateBenchDoubleIn(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchDoubleIn));
  }

  @Benchmark
  public void validateBenchEnumConst(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchEnumConst));
  }

  @Benchmark
  public void validateBenchEnumNotIn(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchEnumNotIn));
  }

  @Benchmark
  public void validateBenchRepeatedStringUnique(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchRepeatedStringUnique));
  }

  @Benchmark
  public void validateBenchRepeatedInt32Unique(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(benchRepeatedInt32Unique));
  }
}
