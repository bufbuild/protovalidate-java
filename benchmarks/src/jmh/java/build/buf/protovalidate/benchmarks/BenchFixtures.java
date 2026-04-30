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

import build.buf.protovalidate.benchmarks.gen.BenchComplexSchema;
import build.buf.protovalidate.benchmarks.gen.BenchEnum;
import build.buf.protovalidate.benchmarks.gen.BenchGT;
import build.buf.protovalidate.benchmarks.gen.BenchMap;
import build.buf.protovalidate.benchmarks.gen.BenchRepeatedBytesUnique;
import build.buf.protovalidate.benchmarks.gen.BenchRepeatedMessage;
import build.buf.protovalidate.benchmarks.gen.BenchRepeatedScalar;
import build.buf.protovalidate.benchmarks.gen.BenchRepeatedScalarUnique;
import build.buf.protovalidate.benchmarks.gen.BenchScalar;
import build.buf.protovalidate.benchmarks.gen.MultiRule;
import build.buf.protovalidate.benchmarks.gen.StringMatching;
import build.buf.protovalidate.benchmarks.gen.TestByteMatching;
import build.buf.protovalidate.benchmarks.gen.WrapperTesting;
import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;

/**
 * Hand-built deterministic fixtures for the native-rules benchmark suite.
 *
 * <p>Each factory returns a fully-populated message that satisfies all of its validation rules.
 * Values are literal (no Random) so benchmarks are reproducible run-to-run. Chosen to match the
 * intent of the gofakeit annotations in the Go reference protos without depending on a faker
 * library.
 */
final class BenchFixtures {
  private BenchFixtures() {}

  static BenchScalar benchScalar() {
    return BenchScalar.newBuilder().setX(42).build();
  }

  static BenchRepeatedScalar benchRepeatedScalar() {
    BenchRepeatedScalar.Builder b = BenchRepeatedScalar.newBuilder();
    for (int i = 1; i <= 5; i++) {
      b.addX(i);
    }
    return b.build();
  }

  static BenchRepeatedMessage benchRepeatedMessage() {
    BenchRepeatedMessage.Builder b = BenchRepeatedMessage.newBuilder();
    for (int i = 1; i <= 5; i++) {
      b.addX(BenchScalar.newBuilder().setX(i).build());
    }
    return b.build();
  }

  static BenchRepeatedScalarUnique benchRepeatedScalarUnique() {
    BenchRepeatedScalarUnique.Builder b = BenchRepeatedScalarUnique.newBuilder();
    for (int i = 1; i <= 8; i++) {
      b.addX((float) i);
    }
    return b.build();
  }

  static BenchRepeatedBytesUnique benchRepeatedBytesUnique() {
    BenchRepeatedBytesUnique.Builder b = BenchRepeatedBytesUnique.newBuilder();
    for (int i = 1; i <= 8; i++) {
      b.addX(ByteString.copyFromUtf8("entry-" + i));
    }
    return b.build();
  }

  static BenchMap benchMap() {
    BenchMap.Builder b = BenchMap.newBuilder();
    for (int i = 1; i <= 5; i++) {
      b.putEntries("key-" + i, "value-" + i);
    }
    return b.build();
  }

  static BenchComplexSchema benchComplexSchema() {
    BenchComplexSchema.Builder b =
        BenchComplexSchema.newBuilder()
            .setS1("hello")
            .setS2("world")
            .setI32(42)
            .setI64(42L)
            .setU32(42)
            .setU64(42L)
            .setSi32(42)
            .setSi64(42L)
            .setF32(42)
            .setF64(42L)
            .setSf32(42)
            .setSf64(42L)
            .setFl(42.0f)
            .setDb(42.0)
            .setBl(true)
            .setBy(ByteString.copyFromUtf8("payload"))
            .setNested(BenchScalar.newBuilder().setX(1).build())
            // self_ref intentionally left null; proto3 message fields default to absent
            .setEnumField(BenchEnum.BENCH_ENUM_ONE)
            .setOneofStr("hello");

    for (int i = 1; i <= 3; i++) {
      b.addRepStr("item-" + i);
      b.addRepI32(i);
      b.addRepBytes(ByteString.copyFromUtf8("bytes-" + i));
      b.addRepMsg(BenchScalar.newBuilder().setX(i).build());
    }

    for (int i = 1; i <= 3; i++) {
      b.putMapStrStr("k" + i, "v" + i);
      b.putMapI32I64(i, (long) i);
      b.putMapU64Bool((long) i, i % 2 == 0);
      b.putMapStrBytes("k" + i, ByteString.copyFromUtf8("v" + i));
      b.putMapStrMsg("k" + i, BenchScalar.newBuilder().setX(i).build());
      b.putMapI64Msg((long) i, BenchScalar.newBuilder().setX(i).build());
    }

    return b.build();
  }

  static BenchGT benchGT() {
    // For gt > lt / gte > lte cases, protovalidate interprets the range as
    // exclusive (value not in [lt, gt]). 50 is outside [-20, 0] for all four.
    return BenchGT.newBuilder()
        .setGt(50)
        .setGte(50)
        .setLt(50)
        .setLte(50)
        .setGtltin(50)
        .setGtltein(50)
        .setGtltex(50)
        .setGtlteex(50)
        .setGteltin(50)
        .setGteltein(50)
        .setGteltex(50)
        .setGtelteex(50)
        .setConst(10)
        .setConstgt(10)
        .setInTest(3)
        .setNotInTest(4)
        .build();
  }

  static TestByteMatching testByteMatching() {
    return TestByteMatching.newBuilder()
        .setIpAddr(ByteString.copyFrom(new byte[16])) // any 16 bytes (ip rule = 4 or 16)
        .setIpv4Addr(ByteString.copyFrom(new byte[4]))
        .setIpv6Addr(ByteString.copyFrom(new byte[16]))
        .setUuid(ByteString.copyFrom(new byte[16]))
        .build();
  }

  static StringMatching stringMatching() {
    return StringMatching.newBuilder()
        .setHostname("example.com")
        .setHostAndPort("example.com:8080")
        .setEmail("alice@example.com")
        .setUuid("550e8400-e29b-41d4-a716-446655440000")
        .build();
  }

  static WrapperTesting wrapperTesting() {
    return WrapperTesting.newBuilder()
        .setI32(Int32Value.of(11))
        .setD(DoubleValue.of(11))
        .setF(FloatValue.of(11))
        .setI64(Int64Value.of(11))
        .setU64(UInt64Value.of(11))
        .setU32(UInt32Value.of(11))
        .setB(BoolValue.of(true))
        .setS(StringValue.of("hello"))
        .setBs(BytesValue.of(ByteString.copyFromUtf8("hello")))
        .build();
  }

  /** Multi-rule fixture that PASSES — many=10 satisfies const=10 and gt=5. */
  static MultiRule multiRuleNoError() {
    return MultiRule.newBuilder().setMany(10).build();
  }

  /** Multi-rule fixture that FAILS both rules — many=1 violates const=10 and gt=5. */
  static MultiRule multiRuleError() {
    return MultiRule.newBuilder().setMany(1).build();
  }
}