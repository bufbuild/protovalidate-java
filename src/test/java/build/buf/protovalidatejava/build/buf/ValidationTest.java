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

package build.buf.protovalidatejava.build.buf;

import build.buf.protovalidate.Config;
import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.Validator;
import build.buf.validate.conformance.cases.custom_constraints.Enum;
import build.buf.validate.conformance.cases.custom_constraints.MessageExpressions;
import build.buf.validate.conformance.cases.custom_constraints.MissingField;
import build.buf.validate.conformance.cases.custom_constraints.NowEqualsNow;
import build.buf.validate.java.Simple;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidationTest {

    @Test
    public void testSuccess() {
        Validator validator = new Validator(new Config());
        assertThat(validator.validateOrThrow(Simple.newBuilder().setA(100).build())).isTrue();
    }

    @Test
    public void testFailure() {
        Validator validator = new Validator(new Config());
        assertThat(validator.validate(Simple.newBuilder().setA(0).build()).isFailure()).isTrue();
    }

    @Test
    public void testValidMessageExpressions() throws InvalidProtocolBufferException {
        Validator validator = new Validator(new Config());
        MessageExpressions msg = MessageExpressions.newBuilder()
                .setA(3)
                .setB(4)
                .setC(Enum.ENUM_ONE)
                .setE(MessageExpressions.Nested.newBuilder().setA(4).setB(3).build())
                .setF(MessageExpressions.Nested.newBuilder().setA(4).setB(2).build())
                .build();
        DynamicMessage message = DynamicMessage.newBuilder(msg.getDescriptorForType()).mergeFrom(msg.toByteArray()).build();
        assertThat(validator.validate(message).isSuccess()).isTrue();
    }

    @Test
    public void testInvalidMessageExpressions() throws InvalidProtocolBufferException {
        Validator validator = new Validator(new Config());
        MessageExpressions msg = MessageExpressions.newBuilder()
                .build();
        DynamicMessage message = DynamicMessage.newBuilder(msg.getDescriptorForType()).mergeFrom(msg.toByteArray()).build();
        ValidationResult validationResult = validator.validate(message);
        assertThat(validationResult.error().getViolationsCount()).isEqualTo(2);
    }

    @Test
    public void testMissingField() throws InvalidProtocolBufferException {
        Validator validator = new Validator(new Config());
        MissingField msg = MissingField.newBuilder()
                .build();
        DynamicMessage message = DynamicMessage.newBuilder(msg.getDescriptorForType()).mergeFrom(msg.toByteArray()).build();
        ValidationResult validationResult = validator.validate(message);
        assertThat(validationResult.isSuccess()).isFalse();
    }

    @Test
    public void testNowIsNow() {
        Validator validator = new Validator(new Config());
        NowEqualsNow now = NowEqualsNow.newBuilder().build();
        ValidationResult validate = validator.validate(now);
        assertThat(validate.isSuccess()).isTrue();
    }
}
