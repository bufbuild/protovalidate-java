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

package build.buf;

import build.buf.validate.ValidateProto;
import build.buf.validate.Violations;
import build.buf.validate.conformance.harness.TestConformanceRequest;
import build.buf.validate.conformance.harness.TestConformanceResponse;
import build.buf.validate.conformance.harness.TestResult;
import build.buf.protovalidate.Config;
import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.Validator;
import build.buf.protovalidate.exceptions.CompilationException;
import build.buf.protovalidate.exceptions.ExecutionException;
import build.buf.validate.Violation;
import com.google.common.base.Splitter;
import com.google.errorprone.annotations.FormatMethod;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
  public static void main(String[] args) {
    try {
      ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();
      extensionRegistry.add(ValidateProto.message);
      extensionRegistry.add(ValidateProto.field);
      extensionRegistry.add(ValidateProto.oneof);
      TestConformanceRequest request =
          TestConformanceRequest.parseFrom(System.in, extensionRegistry);
      TestConformanceResponse response = testConformance(request);
      response.writeTo(System.out);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  static TestConformanceResponse testConformance(TestConformanceRequest request) {
    try {
      Map<String, Descriptors.Descriptor> descriptorMap =
          FileDescriptorUtil.parse(request.getFdset());
      Validator validator = new Validator(Config.newBuilder().build());
      TestConformanceResponse.Builder responseBuilder = TestConformanceResponse.newBuilder();
      Map<String, TestResult> resultsMap = new HashMap<>();
      for (Map.Entry<String, Any> entry : request.getCasesMap().entrySet()) {
        TestResult testResult = testCase(validator, descriptorMap, entry.getValue());
        resultsMap.put(entry.getKey(), testResult);
      }
      responseBuilder.putAllResults(resultsMap);
      return responseBuilder.build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  static TestResult testCase(
      Validator validator, Map<String, Descriptors.Descriptor> fileDescriptors, Any testCase)
      throws InvalidProtocolBufferException {
    List<String> urlParts = Splitter.on('/').limit(2).splitToList(testCase.getTypeUrl());
    String fullName = urlParts.get(urlParts.size() - 1);
    Descriptors.Descriptor descriptor = fileDescriptors.get(fullName);
    if (descriptor == null) {
      return unexpectedErrorResult("Unable to find descriptor: %s", fullName);
    }
    ByteString testCaseValue = testCase.getValue();
    DynamicMessage dynamicMessage =
        DynamicMessage.newBuilder(descriptor).mergeFrom(testCaseValue).build();
    return validate(validator, dynamicMessage);
  }

  private static TestResult validate(Validator validator, DynamicMessage dynamicMessage) {
    try {
      ValidationResult result = validator.validate(dynamicMessage);
      List<Violation> violations = result.getViolations();
      if (violations.isEmpty()) {
        return TestResult.newBuilder().setSuccess(true).build();
      } else {
        return TestResult.newBuilder()
            .setValidationError(
                Violations.newBuilder().addAllViolations(violations).build())
            .build();
      }
    } catch (CompilationException e) {
      return TestResult.newBuilder().setCompilationError(e.getMessage()).build();
    } catch (ExecutionException e) {
      return TestResult.newBuilder().setRuntimeError(e.getMessage()).build();
    } catch (Exception e) {
      return unexpectedErrorResult("unknown error: %s", e.toString());
    }
  }

  @FormatMethod
  static TestResult unexpectedErrorResult(String format, Object... args) {
    String errorMessage = String.format(format, args);
    return TestResult.newBuilder().setUnexpectedError(errorMessage).build();
  }
}
