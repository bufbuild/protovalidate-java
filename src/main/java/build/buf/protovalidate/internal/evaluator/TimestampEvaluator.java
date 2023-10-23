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

package build.buf.protovalidate.internal.evaluator;

import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.exceptions.ExecutionException;
import build.buf.validate.Violation;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import java.util.ArrayList;
import java.util.List;

/**
 * A specialized evaluator for applying some {@link build.buf.validate.TimestampRules} (only the
 * `valid` rule currently) to an {@link com.google.protobuf.Timestamp} message. This is handled
 * outside CEL which handles {@link com.google.protobuf.Timestamp} as an abstract type, thus not
 * allowing access to the message fields.
 */
class TimestampEvaluator implements Evaluator {
  private final long maxTimestamp = +253402300799L;
  private final long minTimestamp = -62135596800L;

  private final Descriptors.FieldDescriptor secondsDescriptor;
  private final Descriptors.FieldDescriptor nanosDescriptor;
  private final boolean valid;

  /** Constructs a new evaluator for {@link build.buf.validate.TimestampRules} messages. */
  TimestampEvaluator(
      Descriptors.FieldDescriptor secondsDescriptor,
      Descriptors.FieldDescriptor nanosDescriptor,
      boolean valid) {
    this.secondsDescriptor = secondsDescriptor;
    this.nanosDescriptor = nanosDescriptor;
    this.valid = valid;
  }

  @Override
  public ValidationResult evaluate(Value val, boolean failFast) throws ExecutionException {
    Message timestampValue = val.messageValue();
    if (timestampValue == null) {
      return ValidationResult.EMPTY;
    }
    List<Violation> violationList = new ArrayList<>();
    if (valid) {
      long seconds = (long) timestampValue.getField(secondsDescriptor);
      int nanos = (int) timestampValue.getField(nanosDescriptor);

      String errorMessage = "";
      if (seconds < minTimestamp) {
        errorMessage = "timestamp before 0001-01-01";
      } else if (seconds > maxTimestamp) {
        errorMessage = "timestamp after 9999-12-31";
      } else if (nanos < 0 || nanos >= 1e9) {
        errorMessage = "timestamp has out-of-range nanos";
      }

      if (errorMessage.length() != 0) {
        Violation violation =
            Violation.newBuilder()
                .setConstraintId("timestamp.valid")
                .setMessage(errorMessage)
                .build();
        violationList.add(violation);
        if (failFast) {
          return new ValidationResult(violationList);
        }
      }
    }
    return new ValidationResult(violationList);
  }

  @Override
  public boolean tautology() {
    return !valid;
  }
}
