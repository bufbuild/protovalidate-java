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
import build.buf.protovalidate.benchmarks.gen.BenchComplexSchema;
import build.buf.protovalidate.benchmarks.gen.BenchGT;
import build.buf.protovalidate.exceptions.ValidationException;
import com.google.protobuf.Message;
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
 * Compile-time evaluator construction benchmarks. Mirrors Go's {@code BenchmarkCompile} and {@code
 * BenchmarkCompileInt32GT}. These measure how long it takes to build a validator (compile rules,
 * cache evaluators) for a given message type — the cost paid once per descriptor.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class EvaluatorBuildBenchmark {

  @Param({"false", "true"})
  public boolean enableNativeRules;

  private Config config;
  private Message benchComplexSchema;
  private Message benchGT;

  @Setup
  public void setup() {
    if (enableNativeRules) {
      config = Config.newBuilder().setEnableNativeRules().build();
    } else {
      config = Config.newBuilder().setDisableNativeRules().build();
    }
    benchComplexSchema = BenchComplexSchema.getDefaultInstance();
    benchGT = BenchGT.getDefaultInstance();
  }

  @Benchmark
  public Validator buildBenchComplexSchema(Blackhole bh) throws ValidationException {
    Validator v = ValidatorFactory.newBuilder().withConfig(config).build();
    // Force evaluator construction by validating the default instance.
    bh.consume(v.validate(benchComplexSchema));
    return v;
  }

  @Benchmark
  public Validator buildBenchInt32GT(Blackhole bh) throws ValidationException {
    Validator v = ValidatorFactory.newBuilder().withConfig(config).build();
    bh.consume(v.validate(benchGT));
    return v;
  }
}
