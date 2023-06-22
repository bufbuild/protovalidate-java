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

import build.buf.protovalidate.errors.CompilationError;
import build.buf.validate.conformance.cases.FloatIn;
import build.buf.validate.conformance.cases.Int64ExLTGT;
import build.buf.validate.conformance.cases.SFixed32NotIn;
import build.buf.validate.conformance.cases.StringAddress;
import build.buf.validate.conformance.cases.StringConst;
import build.buf.validate.conformance.cases.StringEmail;
import build.buf.validate.conformance.cases.StringHostname;
import build.buf.validate.conformance.cases.StringIP;
import build.buf.validate.conformance.cases.StringIPv6;
import build.buf.validate.conformance.cases.StringLen;
import build.buf.validate.conformance.cases.StringNotIn;
import build.buf.validate.conformance.cases.StringPrefix;
import build.buf.validate.conformance.cases.StringURIRef;
import build.buf.validate.conformance.cases.*;
import build.buf.validate.conformance.cases.custom_constraints.Enum;
import build.buf.validate.conformance.cases.custom_constraints.FieldExpressions;
import build.buf.validate.conformance.cases.custom_constraints.MessageExpressions;
import build.buf.validate.conformance.cases.custom_constraints.MissingField;
import build.buf.validate.conformance.cases.custom_constraints.NowEqualsNow;
import build.buf.validate.java.Simple;
import com.google.protobuf.ByteString;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Duration;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import org.junit.Before;
import org.junit.Test;


import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleTest {

    private Validator validator;

    @Before
    public void setUp() throws Exception {
        validator = new Validator(new Config());
    }

    @Test
    public void strprefix() throws CompilationError {
        StringPrefix invalid = StringPrefix.newBuilder().setVal("foo").build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isSuccess()).isTrue();
    }
    @Test
    public void bytescontains() throws CompilationError {
        BytesContains invalid = BytesContains.newBuilder().setVal(ByteString.copyFromUtf8("candy bars")).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isSuccess()).isTrue();
    }

    @Test
    public void strcontains() throws CompilationError {
        StringContains invalid = StringContains.newBuilder().setVal("foobar").build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isSuccess()).isTrue();
    }

    @Test
    public void boolconsttrue() throws CompilationError {
        BoolConstTrue invalid = BoolConstTrue.newBuilder().build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.error().violations).hasSize(1);
        assertThat(validate.isFailure()).isTrue();
    }

    @Test
    public void timestampwithin() throws CompilationError {
        TimestampWithin invalid = TimestampWithin.newBuilder().setVal(Timestamp.newBuilder().build()).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.error().violations).hasSize(1);
        assertThat(validate.isFailure()).isTrue();
    }

    @Test
    public void timestampcost() throws CompilationError {
        TimestampConst invalid = TimestampConst.newBuilder().setVal(Timestamp.newBuilder().setSeconds(3).build()).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isSuccess()).isTrue();
    }

    @Test
    public void OneofIgnoreEmpty() throws CompilationError {
        OneofIgnoreEmpty invalid = OneofIgnoreEmpty.newBuilder().setY(ByteString.copyFromUtf8("")).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isSuccess()).isTrue();
    }

    @Test
    public void enumdefined() throws CompilationError {
        EnumDefined invalid = EnumDefined.newBuilder().setValValue(2147483647).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.error().violations).hasSize(1);
        assertThat(validate.isFailure()).isTrue();
    }

    @Test
    public void strictFixed32LT() throws CompilationError {
        Fixed32LT invalid = Fixed32LT.newBuilder().setVal(5).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.error().violations).hasSize(1);
        assertThat(validate.isFailure()).isTrue();
    }

    @Test
    public void strictWrapperDouble() throws CompilationError {
        WrapperDouble invalid = WrapperDouble.newBuilder().setVal(DoubleValue.newBuilder().build()).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.error().violations).hasSize(1);
        assertThat(validate.isFailure()).isTrue();
    }

    @Test
    public void strictFieldExpressions() throws CompilationError {
        FieldExpressions invalid = FieldExpressions.newBuilder().build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.error().violations).hasSize(2);
        assertThat(validate.isFailure()).isTrue();
    }

    @Test
    public void strictDurationGTELTE() throws CompilationError {
        DurationGTELTE invalid = DurationGTELTE.newBuilder().setVal(Duration.newBuilder().setSeconds(3600).setNanos(1).build()).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.error().violations).hasSize(1);
        assertThat(validate.isFailure()).isTrue();
    }

    @Test
    public void strictRepeatedExact() throws CompilationError {
        RepeatedExact invalid = RepeatedExact.newBuilder().addAllVal(Arrays.asList(1, 2)).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isFailure()).isTrue();
        assertThat(validate.error().violations).hasSize(1);
    }

    @Test
    public void strictSFixed64In() throws CompilationError {
        SFixed64In invalid = SFixed64In.newBuilder().setVal(5).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isFailure()).isTrue();
        assertThat(validate.error().violations).hasSize(1);
    }

    @Test
    public void strictFieldExpressionsNested() throws CompilationError {
        FieldExpressions invalid = FieldExpressions.newBuilder()
                .setA(42)
                .setC(FieldExpressions.Nested.newBuilder().setA(-3).build())
                .build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isFailure()).isTrue();
        assertThat(validate.error().violations).hasSize(4);
    }

    @Test
    public void strictRepeatedExactIgnore() throws CompilationError {
        RepeatedExactIgnore invalid = RepeatedExactIgnore.newBuilder().build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isSuccess()).isTrue();
    }
}
