// Copyright 2023-2025 Buf Technologies, Inc.
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

import build.buf.validate.Violation;
import org.junit.jupiter.api.Test;

public class Issue427Test {
  @Test
  public void testMessageOneofWithNameOnly() throws Exception {
    com.example.imports.validationtest.Issue427 msg =
        com.example.imports.validationtest.Issue427.newBuilder().setName("foo").build();
    Validator validator = ValidatorFactory.newBuilder().build();
    assertThat(validator.validate(msg).toProto().getViolationsList()).isEmpty();
  }

  @Test
  public void testMessageOneofWithTagsOnly() throws Exception {
    com.example.imports.validationtest.Issue427 msg =
        com.example.imports.validationtest.Issue427.newBuilder().addTags("a").addTags("b").build();
    Validator validator = ValidatorFactory.newBuilder().build();
    assertThat(validator.validate(msg).toProto().getViolationsList()).isEmpty();
  }

  @Test
  public void testMessageOneofWithMappingsOnly() throws Exception {
    com.example.imports.validationtest.Issue427 msg =
        com.example.imports.validationtest.Issue427.newBuilder().putMappings("k", "v").build();
    Validator validator = ValidatorFactory.newBuilder().build();
    assertThat(validator.validate(msg).toProto().getViolationsList()).isEmpty();
  }

  @Test
  public void testMessageOneofNoneSet() throws Exception {
    com.example.imports.validationtest.Issue427 msg =
        com.example.imports.validationtest.Issue427.getDefaultInstance();
    Validator validator = ValidatorFactory.newBuilder().build();
    assertThat(validator.validate(msg).toProto().getViolationsList())
        .containsExactly(
            Violation.newBuilder()
                .setRuleId("message.oneof")
                .setMessage("one of name, tags, mappings must be set")
                .build());
  }
}
