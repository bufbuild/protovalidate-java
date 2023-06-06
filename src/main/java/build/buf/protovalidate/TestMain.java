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

import build.buf.protovalidate.Errors.ValidationError;
import build.buf.validate.conformance.harness.TestConformanceRequest;
import build.buf.validate.conformance.harness.TestConformanceResponse;
import build.buf.validate.conformance.harness.TestResult;
import com.google.protobuf.*;
import com.google.protobuf.util.JsonFormat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class TestMain {
    public static void main(String[] args) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            StringWriter writer = new StringWriter();
            PrintWriter printer = new PrintWriter(writer);

            TestConformanceRequest.Builder requestBuilder = TestConformanceRequest.newBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                printer.println(line);
                printer.flush();
                if (line.isEmpty()) {
                    break;
                }
            }
            printer.close();
            String input = writer.toString();
            JsonFormat.parser().ignoringUnknownFields().merge(input, requestBuilder);

            TestConformanceRequest request = requestBuilder.build();
            TestConformanceResponse response = testConformance(request);
            String output = JsonFormat.printer().includingDefaultValueFields().print(response);
            System.out.println(output);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static TestConformanceResponse testConformance(TestConformanceRequest request) {
        try {
            DescriptorProtos.FileDescriptorSet fdSet = request.getFdset();
            Descriptors.FileDescriptor[] fileDescriptors = new Descriptors.FileDescriptor[fdSet.getFileCount()];
            for (int i = 0; i < fdSet.getFileCount(); i++) {
                fileDescriptors[i] = Descriptors.FileDescriptor.buildFrom(fdSet.getFile(i), new Descriptors.FileDescriptor[]{});
            }
            Validator validator = new Validator(new Validator.Config());
            TestConformanceResponse.Builder responseBuilder = TestConformanceResponse.newBuilder();
            Map<String, TestResult> resultsMap = new HashMap<>();
            for (Map.Entry<String, Any> entry : request.getCasesMap().entrySet()) {
                String caseName = entry.getKey();
                Any testCase = entry.getValue();
                TestResult testResult = testCase(validator, fileDescriptors, testCase);
                resultsMap.put(caseName, testResult);
            }
            responseBuilder.putAllResults(resultsMap);
            return responseBuilder.build();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    public static TestResult testCase(Validator validator, Descriptors.FileDescriptor[] fileDescriptors, Any testCase) {
        try {
            String typeUrl = testCase.getTypeUrl();
            String[] urlParts = typeUrl.split("/");
            String fullName = urlParts[urlParts.length - 1];
            Descriptors.Descriptor descriptor = null;
            for (Descriptors.FileDescriptor fileDescriptor : fileDescriptors) {
                descriptor = fileDescriptor.findMessageTypeByName(fullName);
                if (descriptor != null) {
                    break;
                }
            }
            if (descriptor == null) {
                return unexpectedErrorResult("Unable to find descriptor: " + fullName);
            }
            Descriptors.Descriptor messageDescriptor = descriptor;
            Message.Builder messageBuilder = DynamicMessage.newBuilder(messageDescriptor);
            JsonFormat.parser().ignoringUnknownFields().merge(testCase.getValue().toString(), messageBuilder);
            Message message = messageBuilder.build();
            DynamicMessage.Builder dynamicBuilder = DynamicMessage.newBuilder(messageDescriptor);
            JsonFormat.parser().ignoringUnknownFields().merge(testCase.getValue().toString(), dynamicBuilder);
            Message dynamicMessage = dynamicBuilder.build();
            try {
                validator.validate(dynamicMessage);
                return TestResult.newBuilder()
                        .setSuccess(true)
                        .build();
            } catch (Exception e) {
                return TestResult.newBuilder()
//                        .setValidationError(e.toProto())
                        .build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    public static TestResult unexpectedErrorResult(String format, Object... args) {
        String errorMessage = String.format(format, args);
        return TestResult.newBuilder()
                .setUnexpectedError(errorMessage)
                .build();
    }
}
