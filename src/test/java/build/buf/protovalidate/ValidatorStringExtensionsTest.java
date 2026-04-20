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

package build.buf.protovalidate;

import static org.assertj.core.api.Assertions.assertThat;

import build.buf.protovalidate.exceptions.ValidationException;
import build.buf.validate.Violation;
import com.example.noimports.validationtest.ExampleStringExtensions;
import org.junit.jupiter.api.Test;

public class ValidatorStringExtensionsTest {
  @Test
  public void testStringExtensionsPathMatchesParent() throws ValidationException {
    // Test for https://github.com/bufbuild/protovalidate-java/issues/438.
    Validator validator = ValidatorFactory.newBuilder().build();

    ExampleStringExtensions validMsg =
        ExampleStringExtensions.newBuilder().setParent("foo/bar").setPath("foo/bar/baz").build();
    ValidationResult result = validator.validate(validMsg);
    assertThat(result.isSuccess()).isTrue();

    ExampleStringExtensions invalidMsg =
        ExampleStringExtensions.newBuilder().setParent("foo/bar").setPath("foo/other/baz").build();
    result = validator.validate(invalidMsg);
    assertThat(result.isSuccess()).isFalse();
    Violation expectedViolation =
        Violation.newBuilder()
            .setRuleId("path_matches_parent")
            .setMessage("path must be a child of parent")
            .build();
    assertThat(result.toProto().getViolationsList()).containsExactly(expectedViolation);
  }
}
