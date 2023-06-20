package build.buf.protovalidate;

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
import build.buf.validate.conformance.cases.custom_constraints.MessageExpressions;
import build.buf.validate.conformance.cases.custom_constraints.MissingField;
import build.buf.validate.conformance.cases.custom_constraints.NowEqualsNow;
import build.buf.validate.java.Simple;
import com.google.protobuf.ByteString;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidatorTest {

    private Validator validator;

    @Before
    public void setUp() throws Exception {
        validator = new Validator(new Config());
    }

    @Test
    public void testSuccess() {
        assertThat(validator.validateOrThrow(Simple.newBuilder().setA(100).build())).isTrue();
    }

    @Test
    public void testFailure() {
        assertThat(validator.validate(Simple.newBuilder().setA(0).build()).isFailure()).isTrue();
    }

    @Test
    public void testValidMessageExpressions() throws InvalidProtocolBufferException {
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
        MessageExpressions msg = MessageExpressions.newBuilder()
                .build();
        DynamicMessage message = DynamicMessage.newBuilder(msg.getDescriptorForType()).mergeFrom(msg.toByteArray()).build();
        ValidationResult validationResult = validator.validate(message);
        assertThat(validationResult.error().getViolationsCount()).isEqualTo(2);
    }

    @Test
    public void testMissingField() throws InvalidProtocolBufferException {
        MissingField msg = MissingField.newBuilder()
                .build();
        DynamicMessage message = DynamicMessage.newBuilder(msg.getDescriptorForType()).mergeFrom(msg.toByteArray()).build();
        ValidationResult validationResult = validator.validate(message);
        assertThat(validationResult.isSuccess()).isFalse();
    }

    @Test
    public void testNowIsNow() {
        NowEqualsNow now = NowEqualsNow.newBuilder().build();
        ValidationResult validate = validator.validate(now);
        assertThat(validate.isSuccess()).isTrue();
    }

    @Test
    public void testGTLT() {
        Int64ExLTGT invalid = Int64ExLTGT.newBuilder().setVal(1).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isSuccess()).isFalse();
    }

    @Test
    public void strEmail() {
        StringEmail invalid = StringEmail.newBuilder().setVal("a@buf.build").build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isSuccess()).isTrue();
    }

    @Test
    public void strEmailFailure() {
        StringEmail invalid = StringEmail.newBuilder().setVal("abufbuild").build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isFailure()).isTrue();
    }

    @Test
    public void sFixed32NotInValid() {
        SFixed32NotIn test = SFixed32NotIn.newBuilder().setVal(1).build();
        ValidationResult validate = validator.validate(test);
        assertThat(validate.isSuccess()).isTrue();
    }

    @Test
    public void floatConstSuccess() {
        FloatConst invalid = FloatConst.newBuilder().setVal(1.23f).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isSuccess()).isTrue();
    }

    @Test
    public void floatConstFailure() {
        FloatConst invalid = FloatConst.newBuilder().setVal(3.21f).build();
        ValidationResult validate = validator.validate(invalid);
        assertThat(validate.isFailure()).isTrue();
    }

//    @Test
//    public void FloatLT() {
//        FloatIncorrectType test = FloatIncorrectType.newBuilder().setVal(123).build();
//        ValidationResult validate = validator.validate(test);
//        assertThat(validate.isFailure()).isTrue();
//    }
//    @Test
//    public void strlen() {
//        byte[] bytes = new byte[1];
//        bytes[0] = (byte) 0x99;
//        StringPrefix invalid = StringPrefix.newBuilder().setVal("foobar").build();
//        ValidationResult validate = validator.validate(invalid);
////        assertThat(validate.error().violations).isNotEmpty();
////        assertThat(validate.isFailure()).isTrue();
//        assertThat(validate.isSuccess()).isTrue();
//    }
}
