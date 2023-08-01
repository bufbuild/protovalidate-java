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


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public final class Envelope {
    final byte[] bytes;
    final boolean endStream;

    public Envelope(byte[] bytes, boolean endStream) {
        this.bytes = bytes;
        this.endStream = endStream;
    }

    public static final class EnvelopeReader {
        public final BufferedInputStream inputStream;

        public EnvelopeReader(InputStream inputStream) {
            this.inputStream = new BufferedInputStream(inputStream);
        }

        public Envelope read() throws IOException {
            int endStream = inputStream.read();
            if (endStream == 1) {
                inputStream.close();
                return new Envelope(new byte[0], true);
            }
            byte[] bytes = inputStream.readNBytes(4);
            int payloadLength = ByteBuffer.wrap(bytes).getInt();
            byte[] payload = inputStream.readNBytes(payloadLength);
            return new Envelope(payload, false);
        }
    }

    public static final class EnvelopeWriter {
        public final BufferedOutputStream outputStream;

        public EnvelopeWriter(OutputStream outputStream) {
            this.outputStream = new BufferedOutputStream(outputStream);
        }

        public void write(byte[] bytes, boolean endStream) throws IOException {
            outputStream.write(endStream ? 1 : 0);
            outputStream.write(bytes.length);
            outputStream.write(bytes);
            outputStream.flush();
        }
    }
}
