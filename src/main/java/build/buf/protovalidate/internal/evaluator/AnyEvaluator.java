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

import build.buf.gen.buf.validate.Violation;
import build.buf.protovalidate.results.ExecutionException;
import build.buf.protovalidate.results.ValidationResult;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A specialized evaluator for applying {@link build.buf.gen.buf.validate.AnyRules} to an {@link
 * com.google.protobuf.Any} message. This is handled outside CEL which attempts to hydrate {@link
 * com.google.protobuf.Any}'s within an expression, breaking evaluation if the type is unknown at
 * runtime.
 */
class AnyEvaluator implements Evaluator {
  private final Descriptors.FieldDescriptor typeURLDescriptor;
  private final Map<String, Object> in;
  private final Map<String, Object> notIn;

  /** Constructs a new evaluator for {@link build.buf.gen.buf.validate.AnyRules} messages. */
  AnyEvaluator(Descriptors.FieldDescriptor typeURLDescriptor, String[] in, String[] notIn) {
    this.typeURLDescriptor = typeURLDescriptor;
    this.in = stringsToMap(in);
    this.notIn = stringsToMap(notIn);
  }

  @Override
  public ValidationResult evaluate(Value val, boolean failFast) throws ExecutionException {
    Message o = val.messageValue();
    List<Violation> violationList = new ArrayList<>();
    String typeURL = (String) o.getField(typeURLDescriptor);
    if (in != null && in.size() > 0) {
      if (!in.containsKey(typeURL)) {
        Violation.Builder violation = Violation.newBuilder();
        violation.setConstraintId("any.in");
        violation.setMessage("type URL must be in the allow list");
        violationList.add(violation.build());
        if (failFast) {
          return new ValidationResult(violationList);
        }
      }
    }
    if (notIn != null && notIn.size() > 0) {
      if (notIn.containsKey(typeURL)) {
        Violation.Builder violation = Violation.newBuilder();
        violation.setConstraintId("any.not_in");
        violation.setMessage("type URL must not be in the block list");
        violationList.add(violation.build());
      }
    }
    return new ValidationResult(violationList);
  }

  @Override
  public void append(Evaluator eval) {
    throw new UnsupportedOperationException("append not supported for Any");
  }

  @Override
  public boolean tautology() {
    return (in == null || in.size() == 0) && (notIn == null || notIn.size() == 0);
  }

  /** stringsToMap converts a string slice to a map for fast lookup. */
  private static Map<String, Object> stringsToMap(String[] strings) {
    if (strings == null || strings.length == 0) {
      return null;
    }
    Map<String, Object> map = new HashMap<>();
    for (String s : strings) {
      map.put(s, new Object());
    }
    return map;
  }
}
