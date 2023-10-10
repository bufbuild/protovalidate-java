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

import static org.assertj.core.api.Assertions.assertThat;

import build.buf.protovalidate.exceptions.ValidationException;
import build.buf.validate.Violation;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

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
  public void testValidationFieldConstraints() throws Exception {
    // Valid message - matches regex
    com.example.imports.validationtest.ExampleFieldConstraints validMsgImports =
        com.example.imports.validationtest.ExampleFieldConstraints.newBuilder()
            .setRegexStringField("abc123")
            .build();
    expectNoViolations(validMsgImports);

    // Create same message under noimports package. Validation behavior should match.
    com.example.noimports.validationtest.ExampleFieldConstraints validMsgNoImports =
        com.example.noimports.validationtest.ExampleFieldConstraints.parseFrom(
            validMsgImports.toByteString());
    expectNoViolations(validMsgNoImports);

    // 10 chars long - regex requires 1-9 chars
    com.example.imports.validationtest.ExampleFieldConstraints invalidMsgImports =
        com.example.imports.validationtest.ExampleFieldConstraints.newBuilder()
            .setRegexStringField("0123456789")
            .build();
    Violation expectedViolation =
        Violation.newBuilder()
            .setConstraintId("string.pattern")
            .setFieldPath("regex_string_field")
            .setMessage("value does not match regex pattern `^[a-z0-9]{1,9}$`")
            .build();
    expectViolation(invalidMsgImports, expectedViolation);

    // Create same message under noimports package. Validation behavior should match.
    com.example.noimports.validationtest.ExampleFieldConstraints invalidMsgNoImports =
        com.example.noimports.validationtest.ExampleFieldConstraints.newBuilder()
            .setRegexStringField("0123456789")
            .build();
    expectViolation(invalidMsgNoImports, expectedViolation);
  }

  @Test
  public void testValidationOneofConstraints()
      throws ValidationException, InvalidProtocolBufferException {
    // Valid message - matches oneof constraint
    com.example.imports.validationtest.ExampleOneofConstraints validMsgImports =
        com.example.imports.validationtest.ExampleOneofConstraints.newBuilder()
            .setEmail("foo@bar.com")
            .build();
    expectNoViolations(validMsgImports);

    // Create same message under noimports package. Validation behavior should match.
    com.example.noimports.validationtest.ExampleOneofConstraints validMsgNoImports =
        com.example.noimports.validationtest.ExampleOneofConstraints.parseFrom(
            validMsgImports.toByteString());
    expectNoViolations(validMsgNoImports);

    com.example.imports.validationtest.ExampleOneofConstraints invalidMsgImports =
        com.example.imports.validationtest.ExampleOneofConstraints.getDefaultInstance();
    Violation expectedViolation =
        Violation.newBuilder()
            .setFieldPath("contact_info")
            .setConstraintId("required")
            .setMessage("exactly one field is required in oneof")
            .build();
    expectViolation(invalidMsgImports, expectedViolation);

    // Create same message under noimports package. Validation behavior should match.
    com.example.noimports.validationtest.ExampleOneofConstraints invalidMsgNoImports =
        com.example.noimports.validationtest.ExampleOneofConstraints.parseFrom(
            invalidMsgImports.toByteString());
    expectViolation(invalidMsgNoImports, expectedViolation);
  }

  @Test
  public void testValidationMessageConstraintsDifferentJavaPackage() throws Exception {
    com.example.imports.validationtest.ExampleMessageConstraints validMsg =
        com.example.imports.validationtest.ExampleMessageConstraints.newBuilder()
            .setPrimaryEmail("foo@bar.com")
            .build();
    expectNoViolations(validMsg);

    // Create same message under noimports package. Validation behavior should match.
    com.example.noimports.validationtest.ExampleMessageConstraints validMsgNoImports =
        com.example.noimports.validationtest.ExampleMessageConstraints.parseFrom(
            validMsg.toByteString());
    expectNoViolations(validMsgNoImports);

    com.example.imports.validationtest.ExampleMessageConstraints invalidMsgImports =
        com.example.imports.validationtest.ExampleMessageConstraints.newBuilder()
            .setSecondaryEmail("foo@bar.com")
            .build();
    Violation expectedViolation =
        Violation.newBuilder()
            .setConstraintId("secondary_email_depends_on_primary")
            .setMessage("cannot set a secondary email without setting a primary one")
            .build();
    expectViolation(invalidMsgImports, expectedViolation);

    // Create same message under noimports package. Validation behavior should match.
    com.example.noimports.validationtest.ExampleMessageConstraints invalidMsgNoImports =
        com.example.noimports.validationtest.ExampleMessageConstraints.parseFrom(
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
    Validator validator = new Validator();
    List<Violation> violations = validator.validate(msg).getViolations();
    assertThat(violations).containsExactlyInAnyOrderElementsOf(expected);
  }
}
