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

package build.buf.protovalidate.exceptions;

/** CompilationException is returned when a rule fails to compile. This is a fatal error. */
public class CompilationException extends ValidationException {
  /**
   * Creates a CompilationException with the specified message.
   *
   * @param message Exception message.
   */
  public CompilationException(String message) {
    super(message);
  }

  /**
   * Creates a CompilationException with the specified message and cause.
   *
   * @param message Exception message.
   * @param cause Underlying cause of the exception.
   */
  public CompilationException(String message, Throwable cause) {
    super(message, cause);
  }
}
