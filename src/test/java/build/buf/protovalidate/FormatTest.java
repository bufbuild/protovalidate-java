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

import org.junit.jupiter.api.Test;
import org.projectnessie.cel.common.types.ListT;
import org.projectnessie.cel.common.types.UintT;
import org.projectnessie.cel.common.types.ref.Val;

class FormatTest {
  @Test
  void largeDecimalValuesAreProperlyFormatted() {
    UintT largeDecimal = UintT.uintOf(999999999999L);
    ListT val = (ListT) ListT.newValArrayList(null, new Val[] {largeDecimal});
    String formatted = Format.format("%s", val);
    assertThat(formatted).isEqualTo("999999999999");
  }
}
