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

import build.buf.protovalidate.Errors.ValidationError;
import build.buf.protovalidate.Validator;
import build.buf.validate.conformance.harness.TestConformanceRequest;
import build.buf.validate.conformance.harness.TestConformanceResponse;
import build.buf.validate.conformance.harness.TestResult;
import com.google.protobuf.Any;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        try {
            TestConformanceRequest request = TestConformanceRequest.parseFrom(System.in);
            TestConformanceResponse response = testConformance(request);
            response.writeTo(System.out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static TestConformanceResponse testConformance(TestConformanceRequest request) {
        try {
            // TODO: Something for JH: Is this the right way to create field descriptors from a file descriptor proto?
            List<Descriptors.FileDescriptor> descriptorPool = new ArrayList<>();
            for (DescriptorProtos.FileDescriptorProto fileDescriptorProto : request.getFdset().getFileList()) {
                try {
                    Descriptors.FileDescriptor fileDescriptor = Descriptors.FileDescriptor.buildFrom(
                            fileDescriptorProto,
                            new Descriptors.FileDescriptor[]{},
                            true
                    );
                    descriptorPool.add(fileDescriptor);
                } catch (Exception e) {
                    // noop
                }
            }
            List<Descriptors.FileDescriptor> fileDescriptors = new ArrayList<>();
            Descriptors.FileDescriptor[] dependencies = descriptorPool.toArray(new Descriptors.FileDescriptor[0]);
            for (DescriptorProtos.FileDescriptorProto fileDescriptorProto : request.getFdset().getFileList()) {
                Descriptors.FileDescriptor fileDescriptor = Descriptors.FileDescriptor.buildFrom(
                        fileDescriptorProto,
                        dependencies,
                        true
                );
                fileDescriptors.add(fileDescriptor);
            }
            Validator validator = new Validator();
            TestConformanceResponse.Builder responseBuilder = TestConformanceResponse.newBuilder();
            Map<String, TestResult> resultsMap = new HashMap<>();
            for (Map.Entry<String, Any> entry : request.getCasesMap().entrySet()) {
                TestResult testResult = testCase(validator, fileDescriptors, entry.getValue());
                resultsMap.put(entry.getKey(), testResult);
            }
            responseBuilder.putAllResults(resultsMap);
            return responseBuilder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static TestResult testCase(Validator validator, List<Descriptors.FileDescriptor> fileDescriptors, Any testCase) {
        try {
            String[] urlParts = testCase.getTypeUrl().split("/");
            String fullName = urlParts[urlParts.length - 1];
            Descriptors.Descriptor descriptor = getDescriptor(fileDescriptors, fullName);
            if (descriptor == null) {
                return unexpectedErrorResult("Unable to find descriptor: " + fullName);
            }
            try {
                // run test case:
                validator.validate(DynamicMessage.newBuilder(descriptor)
                        .mergeFrom(testCase.getValue())
                        .build());
                return TestResult.newBuilder()
                        .setSuccess(true)
                        .build();
            } catch (ValidationError e) {
                return TestResult.newBuilder()
                        .setValidationError(e.toProto())
                        .build();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Descriptors.Descriptor getDescriptor(List<Descriptors.FileDescriptor> fileDescriptors, String fullName) {
        Descriptors.Descriptor descriptor = null;
        for (Descriptors.FileDescriptor fileDescriptor : fileDescriptors) {
            descriptor = fileDescriptor.findMessageTypeByName(fullName);
            if (descriptor != null) {
                break;
            }
        }
        return descriptor;
    }

    public static TestResult unexpectedErrorResult(String format, Object... args) {
        String errorMessage = String.format(format, args);
        return TestResult.newBuilder()
                .setUnexpectedError(errorMessage)
                .build();
    }
}
