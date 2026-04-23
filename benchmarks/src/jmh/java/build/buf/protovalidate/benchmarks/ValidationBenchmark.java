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
import build.buf.protovalidate.benchmarks.gen.ManyUnruledFieldsMessage;
import build.buf.protovalidate.benchmarks.gen.RepeatedRuleMessage;
import build.buf.protovalidate.benchmarks.gen.SimpleStringMessage;
import build.buf.protovalidate.exceptions.CompilationException;
import build.buf.protovalidate.exceptions.ValidationException;
import com.google.protobuf.Descriptors.Descriptor;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class ValidationBenchmark {

  private Validator validator;
  private SimpleStringMessage simple;
  private ManyUnruledFieldsMessage manyUnruled;

  // Descriptor captured once; cheap to reference during benchmark.
  private static final Descriptor REPEATED_RULE_DESC = RepeatedRuleMessage.getDescriptor();

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

    // Warm evaluator cache for steady-state benchmarks.
    validator.validate(simple);
    validator.validate(manyUnruled);
  }

  // Steady-state validate() benchmarks. These exercise the hot path after the
  // evaluator cache is warm. PR #451 does not affect this path.

  @Benchmark
  public void validateSimple(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(simple));
  }

  @Benchmark
  public void validateManyUnruled(Blackhole bh) throws ValidationException {
    bh.consume(validator.validate(manyUnruled));
  }

  // Compile-path benchmark. Measures building a fresh validator and warming
  // its RuleCache for RepeatedRuleMessage (20 string fields, all min_len).
  // PR #451 affects exactly this path: without the fix, the AST is rebuilt
  // for every field; with the fix, fields 2..20 hit the cache.
  //
  // Time is dominated by Cel environment construction in newCel(); the #451
  // signal is the delta on top of that baseline.
  @Benchmark
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void compileValidatorForRepeated(Blackhole bh) throws CompilationException {
    Validator v =
        ValidatorFactory.newBuilder()
            .buildWithDescriptors(Collections.singletonList(REPEATED_RULE_DESC), false);
    bh.consume(v);
  }
}
