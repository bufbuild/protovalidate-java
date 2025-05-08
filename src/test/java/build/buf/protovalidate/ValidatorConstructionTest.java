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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

import build.buf.protovalidate.exceptions.ValidationException;
import com.example.imports.validationtest.FieldExpressionMapInt32;
import com.google.protobuf.Descriptors.Descriptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ValidatorConstructionTest {
  @Test
  public void testDefaultInstance() {
    Map<Integer, Integer> testMap = new HashMap<Integer, Integer>();
    testMap.put(42, 42);
    FieldExpressionMapInt32 msg = FieldExpressionMapInt32.newBuilder().putAllVal(testMap).build();

    Validator validator = ValidatorFactory.defaultInstance();
    try {
      ValidationResult result = validator.validate(msg);
      assertThat(result.isSuccess()).isFalse();
      assertThat(result.getViolations().size()).isEqualTo(1);
      assertThat(result.getViolations().get(0).toProto().getMessage())
          .isEqualTo("all map values must equal 1");
    } catch (ValidationException e) {
      fail("unexpected exception thrown", e);
    }
  }

  @Test
  public void testLazyBuilderWithConfig() {
    Map<Integer, Integer> testMap = new HashMap<Integer, Integer>();
    testMap.put(42, 42);
    FieldExpressionMapInt32 msg = FieldExpressionMapInt32.newBuilder().putAllVal(testMap).build();

    Config cfg = Config.newBuilder().setFailFast(true).build();
    Validator validator = ValidatorFactory.newBuilder().withConfig(cfg).build();
    try {
      ValidationResult result = validator.validate(msg);
      assertThat(result.isSuccess()).isFalse();
      assertThat(result.getViolations().size()).isEqualTo(1);
      assertThat(result.getViolations().get(0).toProto().getMessage())
          .isEqualTo("all map values must equal 1");
    } catch (ValidationException e) {
      fail("unexpected exception thrown", e);
    }
  }

  @Test
  public void testEagerBuilderWithConfig() {
    Map<Integer, Integer> testMap = new HashMap<Integer, Integer>();
    testMap.put(42, 42);
    FieldExpressionMapInt32 msg = FieldExpressionMapInt32.newBuilder().putAllVal(testMap).build();

    List<Descriptor> seedDescriptors = new ArrayList<Descriptor>();
    FieldExpressionMapInt32 reg = FieldExpressionMapInt32.newBuilder().build();
    seedDescriptors.add(reg.getDescriptorForType());

    Config cfg = Config.newBuilder().setFailFast(true).build();
    try {
      Validator validator =
          ValidatorFactory.newBuilder(seedDescriptors, true).withConfig(cfg).build();
      ValidationResult result = validator.validate(msg);
      assertThat(result.isSuccess()).isFalse();
      assertThat(result.getViolations().size()).isEqualTo(1);
      assertThat(result.getViolations().get(0).toProto().getMessage())
          .isEqualTo("all map values must equal 1");
    } catch (ValidationException e) {
      fail("unexpected exception thrown", e);
    }
  }

  @Test
  public void testEagerBuilderWithInvalidState() {
    List<Descriptor> seedDescriptors = new ArrayList<Descriptor>();
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(
            () -> {
              ValidatorFactory.newBuilder(seedDescriptors, true).build();
            });
  }

  // @Test
  // public void testDisableLazy() {
  //   Map<Integer, Integer> testMap = new HashMap<Integer, Integer>();
  //   testMap.put(42, 42);
  //   FieldExpressionMapInt32 msg =
  // FieldExpressionMapInt32.newBuilder().putAllVal(testMap).build();

  //   Config cfg = Config.newBuilder().setDisableLazy(true).build();
  //   Validator validator = new Validator(cfg);
  //   try {
  //       System.err.println("goosh");
  //       validator.loadMessages(FieldExpressionMapInt32.newBuilder().build());
  //     ValidationResult result = validator.validate(msg);
  //       assertThat(result.isSuccess()).isFalse();
  //       assertThat(result.getViolations().size()).isEqualTo(1);
  //       System.err.println(result);
  //   } catch (Exception e) {
  //       System.err.println("AAAAAAAAAAAAAAAAAAAAAAAAAa");
  //   }
  // }
}
