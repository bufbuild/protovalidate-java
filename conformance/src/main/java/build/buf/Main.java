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

import build.buf.protovalidate.Config;
import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.errors.CompilationError;
import build.buf.protovalidate.errors.RuntimeError;
import build.buf.protovalidate.errors.ValidationError;
import build.buf.protovalidate.Validator;
import build.buf.validate.ValidateProto;
import build.buf.validate.Violation;
import build.buf.validate.conformance.cases.custom_constraints.MessageExpressions;
import build.buf.validate.conformance.harness.TestConformanceRequest;
import build.buf.validate.conformance.harness.TestConformanceResponse;
import build.buf.validate.conformance.harness.TestResult;
import com.google.protobuf.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        try {
            ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();
            extensionRegistry.add(ValidateProto.message);
            extensionRegistry.add(ValidateProto.field);
            extensionRegistry.add(ValidateProto.oneof);
            // add file to extension registry for
            TestConformanceRequest request = TestConformanceRequest.parseFrom(System.in, extensionRegistry);
            TestConformanceResponse response = testConformance(request);
            response.writeTo(System.out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static TestConformanceResponse testConformance(TestConformanceRequest request) {
        try {
            Map<String, Descriptors.Descriptor> descriptorMap = FileDescriptorUtil.parse(request.getFdset());
            Validator validator = new Validator(new Config());
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

    static TestResult testCase(Validator validator, Map<String, Descriptors.Descriptor> fileDescriptors, Any testCase) {
        String[] urlParts = testCase.getTypeUrl().split("/");
        String fullName = urlParts[urlParts.length - 1];
        Descriptors.Descriptor descriptor = fileDescriptors.get(fullName);
        if (descriptor == null) {
            return unexpectedErrorResult("Unable to find descriptor: " + fullName);
        }
        // run test case:
        ByteString testCaseValue = testCase.getValue();
        try {
            DynamicMessage dynamicMessage = DynamicMessage.newBuilder(descriptor)
                    .mergeFrom(testCaseValue)
                    .build();
            boolean result = validateOrThrow(validator, dynamicMessage);
            return TestResult.newBuilder()
                    .setSuccess(result)
                    .build();
        } catch (CompilationError e) {
            return TestResult.newBuilder()
                    .setCompilationError(e.getMessage())
                    .build();
        } catch (ValidationError e) {
            return TestResult.newBuilder()
                    .setValidationError(e.asViolations())
                    .build();
        } catch (Exception e) {
            return unexpectedErrorResult("unknown error: %s", e.toString());
        }
    }

    static TestResult unexpectedErrorResult(String format, Object... args) {
        String errorMessage = String.format(format, args);
        return TestResult.newBuilder()
                .setUnexpectedError(errorMessage)
                .build();
    }

    static boolean validateOrThrow(Validator validator, Message msg) throws CompilationError {
        ValidationResult validationResult = validator.validate(msg);
        if (validationResult.error() != null) {
            throw validationResult.error();
        }
        return validationResult.isSuccess();
    }
}
