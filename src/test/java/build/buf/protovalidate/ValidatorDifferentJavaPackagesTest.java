// Copyright 2023-2024 Buf Technologies, Inc.
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

import static org.assertj.core.api.Assertions.assertThat;

import build.buf.protovalidate.exceptions.ValidationException;
import build.buf.validate.FieldPath;
import build.buf.validate.FieldPathElement;
import build.buf.validate.FieldRules;
import build.buf.validate.Violation;
import com.example.imports.buf.validate.StringRules;
import com.example.imports.validationtest.ExampleFieldRules;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * protovalidate-java contains protoc generated classes for the <a
 * href="https://buf.build/bufbuild/protovalidate">bufbuild/protovalidate</a> module. In some cases
 * however, using <code>buf generate</code> in managed mode (without <a
 * href="https://buf.build/docs/configuration/v1/buf-gen-yaml#except-2">except</a>) or with remote
 * packages will result in the generated code for this module being created in another package.
 * While not desirable, we shouldn't fail with an exception in this case and should make a
 * best-effort attempt to load the validation rules and validate them.
 *
 * <p>Prior to the fix, calling validate would fail up front with:
 *
 * <pre>IllegalArgumentException: mergeFrom(Message) can only merge messages of the same type.</pre>
 *
 * For the tests in this class, we've generated java code to two separate packages for the module
 * found in <code>src/test/resources/proto</code>.
 *
 * <ol>
 *   <li><code>com.example.imports.*</code> is generated with managed mode and <code>
 *       --include-imports</code>, so it contains references to the bufbuild/protovalidate
 *       extensions under <code>com.example.imports.buf.validate.*</code>
 *   <li><code>com.example.noimports.*</code> is generated with managed mode and an exception for
 *       <code>buf.build/bufbuild/protovalidate</code>, so it contains references to the
 *       bufbuild/protovalidate extensions under <code>build.buf.validate.*</code> (honoring the
 *       <code>java_package</code> option).
 * </ol>
 *
 * These tests ensure that the same classes can be validated identically regardless of where the
 * protovalidate extensions are found.
 */
public class ValidatorDifferentJavaPackagesTest {
  @Test
  public void testValidationFieldRules() throws Exception {
    // Valid message - matches regex
    com.example.imports.validationtest.ExampleFieldRules validMsgImports =
        com.example.imports.validationtest.ExampleFieldRules.newBuilder()
            .setRegexStringField("abc123")
            .build();
    expectNoViolations(validMsgImports);

    // Create same message under noimports package. Validation behavior should match.
    com.example.noimports.validationtest.ExampleFieldRules validMsgNoImports =
        com.example.noimports.validationtest.ExampleFieldRules.parseFrom(
            validMsgImports.toByteString());
    expectNoViolations(validMsgNoImports);

    // 10 chars long - regex requires 1-9 chars
    com.example.imports.validationtest.ExampleFieldRules invalidMsgImports =
        com.example.imports.validationtest.ExampleFieldRules.newBuilder()
            .setRegexStringField("0123456789")
            .build();
    Violation expectedViolation =
        Violation.newBuilder()
            .setField(
                FieldPath.newBuilder()
                    .addElements(
                        FieldPathUtils.fieldPathElement(
                            com.example.imports.validationtest.ExampleFieldRules.getDescriptor()
                                .findFieldByNumber(
                                    ExampleFieldRules.REGEX_STRING_FIELD_FIELD_NUMBER))))
            .setRule(
                FieldPath.newBuilder()
                    .addElements(
                        FieldPathUtils.fieldPathElement(
                            FieldRules.getDescriptor()
                                .findFieldByNumber(FieldRules.STRING_FIELD_NUMBER)))
                    .addElements(
                        FieldPathUtils.fieldPathElement(
                            StringRules.getDescriptor()
                                .findFieldByNumber(StringRules.PATTERN_FIELD_NUMBER))))
            .setRuleId("string.pattern")
            .setMessage("value does not match regex pattern `^[a-z0-9]{1,9}$`")
            .build();
    expectViolation(invalidMsgImports, expectedViolation);

    // Create same message under noimports package. Validation behavior should match.
    com.example.noimports.validationtest.ExampleFieldRules invalidMsgNoImports =
        com.example.noimports.validationtest.ExampleFieldRules.newBuilder()
            .setRegexStringField("0123456789")
            .build();
    expectViolation(invalidMsgNoImports, expectedViolation);
  }

  @Test
  public void testValidationOneofRules()
      throws ValidationException, InvalidProtocolBufferException {
    // Valid message - matches oneof rule
    com.example.imports.validationtest.ExampleOneofRules validMsgImports =
        com.example.imports.validationtest.ExampleOneofRules.newBuilder()
            .setEmail("foo@bar.com")
            .build();
    expectNoViolations(validMsgImports);

    // Create same message under noimports package. Validation behavior should match.
    com.example.noimports.validationtest.ExampleOneofRules validMsgNoImports =
        com.example.noimports.validationtest.ExampleOneofRules.parseFrom(
            validMsgImports.toByteString());
    expectNoViolations(validMsgNoImports);

    com.example.imports.validationtest.ExampleOneofRules invalidMsgImports =
        com.example.imports.validationtest.ExampleOneofRules.getDefaultInstance();
    Violation expectedViolation =
        Violation.newBuilder()
            .setField(
                FieldPath.newBuilder()
                    .addElements(FieldPathElement.newBuilder().setFieldName("contact_info")))
            .setRuleId("required")
            .setMessage("exactly one field is required in oneof")
            .build();
    expectViolation(invalidMsgImports, expectedViolation);

    // Create same message under noimports package. Validation behavior should match.
    com.example.noimports.validationtest.ExampleOneofRules invalidMsgNoImports =
        com.example.noimports.validationtest.ExampleOneofRules.parseFrom(
            invalidMsgImports.toByteString());
    expectViolation(invalidMsgNoImports, expectedViolation);
  }

  @Test
  public void testValidationMessageRulesDifferentJavaPackage() throws Exception {
    com.example.imports.validationtest.ExampleMessageRules validMsg =
        com.example.imports.validationtest.ExampleMessageRules.newBuilder()
            .setPrimaryEmail("foo@bar.com")
            .build();
    expectNoViolations(validMsg);

    // Create same message under noimports package. Validation behavior should match.
    com.example.noimports.validationtest.ExampleMessageRules validMsgNoImports =
        com.example.noimports.validationtest.ExampleMessageRules.parseFrom(validMsg.toByteString());
    expectNoViolations(validMsgNoImports);

    com.example.imports.validationtest.ExampleMessageRules invalidMsgImports =
        com.example.imports.validationtest.ExampleMessageRules.newBuilder()
            .setSecondaryEmail("foo@bar.com")
            .build();
    Violation expectedViolation =
        Violation.newBuilder()
            .setRuleId("secondary_email_depends_on_primary")
            .setMessage("cannot set a secondary email without setting a primary one")
            .build();
    expectViolation(invalidMsgImports, expectedViolation);

    // Create same message under noimports package. Validation behavior should match.
    com.example.noimports.validationtest.ExampleMessageRules invalidMsgNoImports =
        com.example.noimports.validationtest.ExampleMessageRules.parseFrom(
            invalidMsgImports.toByteString());
    expectViolation(invalidMsgNoImports, expectedViolation);
  }

  private void expectNoViolations(Message msg) throws ValidationException {
    expectViolations(msg, Collections.emptyList());
  }

  private void expectViolation(Message msg, Violation violation) throws ValidationException {
    expectViolations(msg, Collections.singletonList(violation));
  }

  private void expectViolations(Message msg, List<Violation> expected) throws ValidationException {
    Validator validator = ValidatorFactory.newBuilder().build();
    List<Violation> violations = validator.validate(msg).toProto().getViolationsList();
    assertThat(violations).containsExactlyInAnyOrderElementsOf(expected);
  }
}
