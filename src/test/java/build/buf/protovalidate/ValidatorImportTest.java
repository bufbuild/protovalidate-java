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

import com.example.imports.validationtest.ExampleImportMessage;
import com.example.imports.validationtest.ExampleImportMessageFieldRule;
import com.example.imports.validationtest.ExampleImportMessageInMap;
import com.example.imports.validationtest.ExampleImportMessageInMapFieldRule;
import com.example.imports.validationtest.ExampleImportedMessage;
import org.junit.jupiter.api.Test;

public class ValidatorImportTest {
  @Test
  public void testImportedMessageFromAnotherFile() throws Exception {
    com.example.imports.validationtest.ExampleImportMessage valid =
        ExampleImportMessage.newBuilder()
            .setImportedSubmessage(
                ExampleImportedMessage.newBuilder().setHexString("0123456789abcdef").build())
            .build();
    assertThat(
            ValidatorFactory.newBuilder()
                .build()
                .validate(valid)
                .toProto()
                .getViolationsList()
                .size())
        .isEqualTo(0);

    com.example.imports.validationtest.ExampleImportMessage invalid =
        ExampleImportMessage.newBuilder()
            .setImportedSubmessage(ExampleImportedMessage.newBuilder().setHexString("zyx").build())
            .build();
    assertThat(
            ValidatorFactory.newBuilder()
                .build()
                .validate(invalid)
                .toProto()
                .getViolationsList()
                .size())
        .isEqualTo(1);
  }

  @Test
  public void testImportedMessageFromAnotherFileInField() throws Exception {
    com.example.imports.validationtest.ExampleImportMessageFieldRule valid =
        ExampleImportMessageFieldRule.newBuilder()
            .setMessageWithImport(
                ExampleImportMessage.newBuilder()
                    .setImportedSubmessage(
                        ExampleImportedMessage.newBuilder()
                            .setHexString("0123456789abcdef")
                            .build())
                    .build())
            .build();
    assertThat(
            ValidatorFactory.newBuilder()
                .build()
                .validate(valid)
                .toProto()
                .getViolationsList()
                .size())
        .isEqualTo(0);

    com.example.imports.validationtest.ExampleImportMessageFieldRule invalid =
        ExampleImportMessageFieldRule.newBuilder()
            .setMessageWithImport(
                ExampleImportMessage.newBuilder()
                    .setImportedSubmessage(
                        ExampleImportedMessage.newBuilder().setHexString("zyx").build())
                    .build())
            .build();
    assertThat(
            ValidatorFactory.newBuilder()
                .build()
                .validate(invalid)
                .toProto()
                .getViolationsList()
                .size())
        .isEqualTo(1);
  }

  @Test
  public void testImportedMessageFromAnotherFileInMap() throws Exception {
    com.example.imports.validationtest.ExampleImportMessageInMap valid =
        ExampleImportMessageInMap.newBuilder()
            .putImportedSubmessage(
                0, ExampleImportedMessage.newBuilder().setHexString("0123456789abcdef").build())
            .build();
    assertThat(
            ValidatorFactory.newBuilder()
                .build()
                .validate(valid)
                .toProto()
                .getViolationsList()
                .size())
        .isEqualTo(0);

    com.example.imports.validationtest.ExampleImportMessageInMap invalid =
        ExampleImportMessageInMap.newBuilder()
            .putImportedSubmessage(
                0, ExampleImportedMessage.newBuilder().setHexString("zyx").build())
            .build();
    assertThat(
            ValidatorFactory.newBuilder()
                .build()
                .validate(invalid)
                .toProto()
                .getViolationsList()
                .size())
        .isEqualTo(1);
  }

  @Test
  public void testImportedMessageFromAnotherFileInMapInField() throws Exception {
    com.example.imports.validationtest.ExampleImportMessageInMapFieldRule valid =
        ExampleImportMessageInMapFieldRule.newBuilder()
            .setMessageWithImport(
                ExampleImportMessageInMap.newBuilder()
                    .putImportedSubmessage(
                        0,
                        ExampleImportedMessage.newBuilder()
                            .setHexString("0123456789abcdef")
                            .build())
                    .build())
            .build();
    assertThat(
            ValidatorFactory.newBuilder()
                .build()
                .validate(valid)
                .toProto()
                .getViolationsList()
                .size())
        .isEqualTo(0);

    com.example.imports.validationtest.ExampleImportMessageInMapFieldRule invalid =
        ExampleImportMessageInMapFieldRule.newBuilder()
            .setMessageWithImport(
                ExampleImportMessageInMap.newBuilder()
                    .putImportedSubmessage(
                        0, ExampleImportedMessage.newBuilder().setHexString("zyx").build())
                    .build())
            .build();
    assertThat(
            ValidatorFactory.newBuilder()
                .build()
                .validate(invalid)
                .toProto()
                .getViolationsList()
                .size())
        .isEqualTo(1);
  }
}
