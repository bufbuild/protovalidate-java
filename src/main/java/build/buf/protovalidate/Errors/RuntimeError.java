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

package build.buf.protovalidate.Errors;

public class RuntimeError extends Exception {

    private final Exception cause;

    public RuntimeError(Exception cause) {
        super("runtime error: " + cause.getMessage());
        this.cause = cause;
    }

    public static RuntimeError newRuntimeError(Exception cause) {
        return new RuntimeError(cause);
    }

    public static RuntimeError newRuntimeErrorf(String format, Object... args) {
        return new RuntimeError(new Exception(String.format(format, args)));
    }

    @Override
    public synchronized Throwable getCause() {
        return this.cause;
    }
}
