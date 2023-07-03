// Copyright 2023 Buf Technologies, Inc.
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

package build.buf.protovalidate;

import build.buf.protovalidate.results.CompilationException;
import build.buf.protovalidate.results.ExecutionException;
import build.buf.protovalidate.results.ValidationException;
import build.buf.protovalidate.results.ValidationResult;
import build.buf.validate.conformance.cases.StringPrefix;
import build.buf.validate.conformance.cases.*;
import build.buf.validate.conformance.cases.custom_constraints.DynRuntimeError;
import build.buf.validate.conformance.cases.custom_constraints.FieldExpressions;
import com.google.protobuf.ByteString;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import org.junit.Before;
import org.junit.Test;


import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleTest {

    private Validator validator;

    @Before
    public void setUp() throws CompilationException {
        validator = new Validator(new Config());
    }

    @Test
    public void strprefix() throws Exception {
        StringPrefix invalid = StringPrefix.newBuilder().setVal("foo").build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isSuccess()).isTrue();
        assertThat(validate.violations).hasSize(0);
    }

    @Test
    public void bytescontains() throws Exception {
        BytesContains invalid = BytesContains.newBuilder().setVal(ByteString.copyFromUtf8("candy bars")).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isSuccess()).isTrue();
        assertThat(validate.violations).hasSize(0);
    }

    @Test
    public void strcontains() throws Exception {
        StringContains invalid = StringContains.newBuilder().setVal("foobar").build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isSuccess()).isTrue();
        assertThat(validate.violations).hasSize(0);
    }

    @Test
    public void boolconsttrue() throws Exception {
        BoolConstTrue invalid = BoolConstTrue.newBuilder().build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.violations).hasSize(1);
        assertThat(validate.isFailure()).isTrue();
    }

    @Test
    public void timestampwithin() throws Exception {
        TimestampWithin invalid = TimestampWithin.newBuilder().setVal(Timestamp.newBuilder().build()).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.violations).hasSize(1);
        assertThat(validate.isFailure()).isTrue();
    }

    @Test
    public void timestampcost() throws Exception {
        TimestampConst invalid = TimestampConst.newBuilder().setVal(Timestamp.newBuilder().setSeconds(3).build()).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isSuccess()).isTrue();
        assertThat(validate.violations).hasSize(0);
    }

    @Test
    public void OneofIgnoreEmpty() throws Exception {
        OneofIgnoreEmpty invalid = OneofIgnoreEmpty.newBuilder().setY(ByteString.copyFromUtf8("")).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isSuccess()).isTrue();
        assertThat(validate.violations).hasSize(0);
    }

    @Test
    public void enumdefined() throws Exception {
        EnumDefined invalid = EnumDefined.newBuilder().setValValue(2147483647).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.violations).hasSize(1);
        assertThat(validate.isFailure()).isTrue();
    }

    @Test
    public void strictFixed32LT() throws Exception {
        Fixed32LT invalid = Fixed32LT.newBuilder().setVal(5).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.violations).hasSize(1);
        assertThat(validate.isFailure()).isTrue();
    }

    @Test
    public void strictWrapperDouble() throws Exception {
        WrapperDouble invalid = WrapperDouble.newBuilder().setVal(DoubleValue.newBuilder().build()).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.violations).hasSize(1);
        assertThat(validate.isFailure()).isTrue();
    }

    @Test
    public void strictFieldExpressions() throws Exception {
        FieldExpressions invalid = FieldExpressions.newBuilder().build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.violations).hasSize(2);
        assertThat(validate.isFailure()).isTrue();
    }

    @Test
    public void strictDurationGTELTE() throws Exception {
        DurationGTELTE invalid = DurationGTELTE.newBuilder().setVal(Duration.newBuilder().setSeconds(3600).setNanos(1).build()).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.violations).hasSize(1);
        assertThat(validate.isFailure()).isTrue();
    }

    @Test
    public void strictRepeatedExact() throws Exception {
        RepeatedExact invalid = RepeatedExact.newBuilder().addAllVal(Arrays.asList(1, 2)).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isFailure()).isTrue();
        assertThat(validate.violations).hasSize(1);
    }

    @Test
    public void strictSFixed64In() throws Exception {
        SFixed64In invalid = SFixed64In.newBuilder().setVal(5).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isFailure()).isTrue();
        assertThat(validate.violations).hasSize(1);
    }

    @Test
    public void strictFieldExpressionsNested() throws Exception {
        FieldExpressions invalid = FieldExpressions.newBuilder()
                .setA(42)
                .setC(FieldExpressions.Nested.newBuilder().setA(-3).build())
                .build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isFailure()).isTrue();
        assertThat(validate.violations).hasSize(4);
    }

    @Test
    public void strictRepeatedExactIgnore() throws Exception {
        RepeatedExactIgnore invalid = RepeatedExactIgnore.newBuilder().build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isSuccess()).isTrue();
        assertThat(validate.violations).hasSize(0);
    }

    @Test
    public void strictInt32In() throws Exception {
        Int32In invalid = Int32In.newBuilder().setVal(4).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isFailure()).isTrue();
        assertThat(validate.violations).hasSize(1);
    }

    @Test
    public void strictRepeatedEnumIn() throws Exception {
        RepeatedEnumIn invalid = RepeatedEnumIn.newBuilder().addVal(AnEnum.AN_ENUM_X).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isFailure()).isTrue();
        assertThat(validate.violations).hasSize(1);
    }

    @Test
    public void strictRepeatedMin() throws Exception {
        RepeatedMin invalid = RepeatedMin.newBuilder().addVal(Embed.newBuilder().setVal(1).build()).addVal(Embed.newBuilder().setVal(-1).build()).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isFailure()).isTrue();
        assertThat(validate.violations).hasSize(1);
    }

    @Test(expected = ExecutionException.class)
    public void testDynRuntimeError() throws Exception {
        DynRuntimeError invalid = DynRuntimeError.newBuilder().setA(123).build();
        ValidationResult validate = validator.validate(invalid);
    }

    // Needs : https://github.com/projectnessie/cel-java/pull/419
//    @Test
//    public void strictBytesIn() throws ValidationException {
//        BytesIn invalid = BytesIn.newBuilder().setVal(ByteString.copyFromUtf8("bar")).build();
//        ValidationResult validate = validator.validate(invalid);
//        assertThat(validate.isSuccess()).isTrue();
//    }

    @Test
    public void strictRepeatedUnique() throws ValidationException {
        RepeatedUnique invalid = RepeatedUnique.newBuilder().addAllVal(Arrays.asList("foo", "bar", "foo", "baz")).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isFailure()).isTrue();
    }

    @Test
    public void strictRepeatedUniqueFoofoo() throws ValidationException {
        RepeatedUnique invalid = RepeatedUnique.newBuilder().addAllVal(Arrays.asList("foo", "Foo")).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isSuccess()).isTrue();
    }
}
