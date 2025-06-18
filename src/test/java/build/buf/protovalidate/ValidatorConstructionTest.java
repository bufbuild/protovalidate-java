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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

import build.buf.protovalidate.exceptions.ValidationException;
import com.example.imports.validationtest.ExampleFieldRules;
import com.example.imports.validationtest.FieldExpressionMapInt32;
import com.example.imports.validationtest.FieldExpressionMultiple;
import com.google.protobuf.Descriptors.Descriptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ValidatorConstructionTest {

  // Tests validation works as planned with default builder.
  @Test
  public void testDefaultBuilder() {
    Map<Integer, Integer> testMap = new HashMap<Integer, Integer>();
    testMap.put(42, 42);
    FieldExpressionMapInt32 msg = FieldExpressionMapInt32.newBuilder().putAllVal(testMap).build();

    Validator validator = ValidatorFactory.newBuilder().build();
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

  // Tests validation works as planned with default builder and config
  @Test
  public void testDefaultBuilderWithConfig() {
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

  // Tests that if the correct seed descriptors are provided and lazy is disabled,
  // validation works as planned.
  @Test
  public void testSeedDescriptorsLazyDisabled() {
    Map<Integer, Integer> testMap = new HashMap<Integer, Integer>();
    testMap.put(42, 42);
    FieldExpressionMapInt32 msg = FieldExpressionMapInt32.newBuilder().putAllVal(testMap).build();

    List<Descriptor> seedDescriptors = new ArrayList<Descriptor>();
    FieldExpressionMapInt32 reg = FieldExpressionMapInt32.newBuilder().build();
    seedDescriptors.add(reg.getDescriptorForType());

    Config cfg = Config.newBuilder().setFailFast(true).build();

    // Note that buildWithDescriptors throws the exception so the validator builder
    // can be created ahead of time without having to catch an exception.
    ValidatorFactory.ValidatorBuilder bldr = ValidatorFactory.newBuilder().withConfig(cfg);
    try {
      Validator validator = bldr.buildWithDescriptors(seedDescriptors, true);
      ValidationResult result = validator.validate(msg);
      assertThat(result.isSuccess()).isFalse();
      assertThat(result.getViolations().size()).isEqualTo(1);
      assertThat(result.getViolations().get(0).toProto().getMessage())
          .isEqualTo("all map values must equal 1");
    } catch (ValidationException e) {
      fail("unexpected exception thrown", e);
    }
  }

  // Tests that the seed descriptor list is immutable inside the validator and that if
  // a descriptor is removed after the validator is created, validation still works as planned.
  @Test
  public void testSeedDescriptorsImmutable() {
    Map<Integer, Integer> testMap = new HashMap<Integer, Integer>();
    testMap.put(42, 42);
    FieldExpressionMapInt32 msg = FieldExpressionMapInt32.newBuilder().putAllVal(testMap).build();

    List<Descriptor> seedDescriptors = new ArrayList<Descriptor>();
    seedDescriptors.add(msg.getDescriptorForType());

    Config cfg = Config.newBuilder().setFailFast(true).build();
    try {
      Validator validator =
          ValidatorFactory.newBuilder().withConfig(cfg).buildWithDescriptors(seedDescriptors, true);

      // Remove descriptor from list after the validator is created to verify validation still works
      seedDescriptors.clear();

      ValidationResult result = validator.validate(msg);
      assertThat(result.isSuccess()).isFalse();
      assertThat(result.getViolations().size()).isEqualTo(1);
      assertThat(result.getViolations().get(0).toProto().getMessage())
          .isEqualTo("all map values must equal 1");
    } catch (ValidationException e) {
      fail("unexpected exception thrown", e);
    }
  }

  // Tests that if a message is attempted to be validated and it wasn't in the initial
  // list of seed descriptors AND lazy is disabled, that a message is returned that
  // no evaluator is available.
  @Test
  public void testSeedDescriptorsWithWrongDescriptorAndLazyDisabled() {
    Map<Integer, Integer> testMap = new HashMap<Integer, Integer>();
    testMap.put(42, 42);
    FieldExpressionMapInt32 msg = FieldExpressionMapInt32.newBuilder().putAllVal(testMap).build();

    List<Descriptor> seedDescriptors = new ArrayList<Descriptor>();
    ExampleFieldRules wrong = ExampleFieldRules.newBuilder().build();
    seedDescriptors.add(wrong.getDescriptorForType());

    Config cfg = Config.newBuilder().setFailFast(true).build();
    try {
      Validator validator =
          ValidatorFactory.newBuilder().withConfig(cfg).buildWithDescriptors(seedDescriptors, true);
      ValidationResult result = validator.validate(msg);
      assertThat(result.isSuccess()).isFalse();
      assertThat(result.getViolations().size()).isEqualTo(1);
      assertThat(result.getViolations().get(0).toProto().getMessage())
          .isEqualTo("No evaluator available for " + msg.getDescriptorForType().getFullName());
    } catch (ValidationException e) {
      fail("unexpected exception thrown", e);
    }
  }

  // Tests that an IllegalStateException is thrown if an empty descriptor list is given
  // and lazy is disabled.
  @Test
  public void testEmptySeedDescriptorsInvalidState() {
    List<Descriptor> seedDescriptors = new ArrayList<Descriptor>();
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(
            () -> {
              ValidatorFactory.newBuilder().buildWithDescriptors(seedDescriptors, true);
            });
  }

  // Tests that an IllegalStateException is thrown if a null descriptor list is given
  // and lazy is disabled.
  @Test
  public void testNullSeedDescriptorsInvalidState() {
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(
            () -> {
              ValidatorFactory.newBuilder().buildWithDescriptors(null, true);
            });
  }

  // Tests that when an empty list of seed descriptors is provided and lazy is enabled
  // that the missing message descriptor is successfully built and validation works as planned.
  @Test
  public void testEmptySeedDescriptorsLazyEnabled() {
    Map<Integer, Integer> testMap = new HashMap<Integer, Integer>();
    testMap.put(42, 42);
    FieldExpressionMapInt32 msg = FieldExpressionMapInt32.newBuilder().putAllVal(testMap).build();

    List<Descriptor> seedDescriptors = new ArrayList<Descriptor>();
    Config cfg = Config.newBuilder().setFailFast(true).build();
    try {
      Validator validator =
          ValidatorFactory.newBuilder()
              .withConfig(cfg)
              .buildWithDescriptors(seedDescriptors, false);
      ValidationResult result = validator.validate(msg);
      assertThat(result.isSuccess()).isFalse();
      assertThat(result.getViolations().size()).isEqualTo(1);
      assertThat(result.getViolations().get(0).toProto().getMessage())
          .isEqualTo("all map values must equal 1");
    } catch (ValidationException e) {
      fail("unexpected exception thrown", e);
    }
  }

  // Tests that when a null list of seed descriptors is provided, a NullPointerException
  // is thrown with a message that descriptors cannot be null.
  @Test
  public void testNullSeedDescriptorsLazyEnabled() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(
            () -> {
              ValidatorFactory.newBuilder().buildWithDescriptors(null, false);
            })
        .withMessageContaining("descriptors must not be null");
    ;
  }

  // Tests that the config is applied when building a validator.
  @Test
  public void testConfigApplied() {
    // Value must be at most 5 characters and must be lowercase alpha chars or numbers.
    FieldExpressionMultiple msg = FieldExpressionMultiple.newBuilder().setVal("INVALID").build();

    // Set fail fast to true, so we exit after the first validation failure.
    Config cfg = Config.newBuilder().setFailFast(true).build();
    try {
      Validator validator = ValidatorFactory.newBuilder().withConfig(cfg).build();
      ValidationResult result = validator.validate(msg);
      assertThat(result.isSuccess()).isFalse();
      assertThat(result.getViolations().size()).isEqualTo(1);
      assertThat(result.getViolations().get(0).toProto().getMessage())
          .isEqualTo("value length must be at most 5 characters");
    } catch (ValidationException e) {
      fail("unexpected exception thrown", e);
    }
  }

  // Tests that the config is applied when building a validator with seed descriptors.
  @Test
  public void testConfigAppliedWithSeedDescriptors() {
    // Value must be at most 5 characters and must be lowercase alpha chars or numbers.
    FieldExpressionMultiple msg = FieldExpressionMultiple.newBuilder().setVal("INVALID").build();

    FieldExpressionMultiple desc = FieldExpressionMultiple.newBuilder().build();
    List<Descriptor> seedDescriptors = new ArrayList<Descriptor>();
    seedDescriptors.add(desc.getDescriptorForType());

    // Set fail fast to true, so we exit after the first validation failure.
    Config cfg = Config.newBuilder().setFailFast(true).build();
    try {
      Validator validator =
          ValidatorFactory.newBuilder()
              .withConfig(cfg)
              .buildWithDescriptors(seedDescriptors, false);
      ValidationResult result = validator.validate(msg);
      assertThat(result.isSuccess()).isFalse();
      assertThat(result.getViolations().size()).isEqualTo(1);
      assertThat(result.getViolations().get(0).toProto().getMessage())
          .isEqualTo("value length must be at most 5 characters");
    } catch (ValidationException e) {
      fail("unexpected exception thrown", e);
    }
  }
}
