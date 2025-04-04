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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;

final class Uri {
  private String str;
  private int index;
  private boolean pctEncodedFound;

  Uri(String str) {
    this.str = str;
  }

  /**
   * Reports whether string is a valid URI.
   *
   * <p>Method parses the rule:
   *
   * <pre>URI = scheme ":" hier-part [ "?" query ] [ "#" fragment ]
   */
  boolean uri() {
    int start = this.index;

    if (!(this.scheme() && this.take(':') && this.hierPart())) {
      this.index = start;
      return false;
    }

    if (this.take('?') && !this.query()) {
      return false;
    }

    if (this.take('#') && !this.fragment()) {
      return false;
    }

    if (this.index != this.str.length()) {
      this.index = start;
      return false;
    }

    return true;
  }

  /**
   * Reports whether string contains a valid hier-part.
   *
   * <p>Method parses the rule:
   *
   * <pre>hier-part = "//" authority path-abempty
   *                / path-absolute
   *                / path-rootless
   *                / path-empty
   */
  private boolean hierPart() {
    int start = this.index;

    if (this.takeDoubleSlash() && this.authority() && this.pathAbempty()) {
      return true;
    }

    this.index = start;

    return this.pathAbsolute() || this.pathRootless() || this.pathEmpty();
  }

  /**
   * Reports whether string is a valid URI reference.
   *
   * <p>Method parses the rule:
   *
   * <pre>URI-reference = URI / relative-ref
   */
  boolean uriReference() {
    return this.uri() || this.relativeRef();
  }

  /**
   * Reports whether string contains a valid relative reference.
   *
   * <p>Method parses the rule:
   *
   * <pre>relative-ref = relative-part [ "?" query ] [ "#" fragment ].
   */
  private boolean relativeRef() {
    int start = this.index;

    if (!this.relativePart()) {
      return false;
    }

    if (this.take('?') && !this.query()) {
      this.index = start;
      return false;
    }

    if (this.take('#') && !this.fragment()) {
      this.index = start;
      return false;
    }

    if (this.index != this.str.length()) {
      this.index = start;
      return false;
    }

    return true;
  }

  /**
   * Reports whether string contains a valid relative part.
   *
   * <p>Method parses the rule:
   *
   * <pre>relative-part = "//" authority path-abempty
   *                    / path-absolute
   *                    / path-noscheme
   *                    / path-empty
   */
  private boolean relativePart() {
    int start = this.index;

    if (this.takeDoubleSlash() && this.authority() && this.pathAbempty()) {
      return true;
    }

    this.index = start;

    return this.pathAbsolute() || this.pathNoscheme() || this.pathEmpty();
  }

  private boolean takeDoubleSlash() {
      boolean isSlash = take('/');

      return isSlash && take('/');
  }

  /**
   * Reports whether string contains a valid scheme.
   *
   * <p>Method parses the rule:
   *
   * <pre>scheme = ALPHA *( ALPHA / DIGIT / "+" / "-" / "." )
   */
  private boolean scheme() {
    int start = this.index;

    if (this.alpha()) {
      while (this.alpha() || this.digit() || this.take('+') || this.take('-') || this.take('.')) {}

      if (this.str.charAt(this.index) == ':') {
        return true;
      }
    }

    this.index = start;

    return false;
  }

  /**
   * Reports whether string contains a valid authority.
   *
   * <p>Method parses the rule:
   *
   * <pre>authority = [ userinfo "@" ] host [ ":" port ]
   *
   * Lead by double slash ("") and terminated by "/", "?", "#", or end of URI.
   */
  private boolean authority() {
    int start = this.index;

    if (this.userinfo()) {
      if (!this.take('@')) {
        this.index = start;
        return false;
      }
    }

    if (!this.host()) {
      this.index = start;
      return false;
    }

    if (this.take(':')) {
      if (!this.port()) {
        this.index = start;
        return false;
      }
    }

    if (!this.isAuthorityEnd()) {
      this.index = start;
      return false;
    }

    return true;
  }

  /**
   * Reports whether the current position is the end of the authority.
   *
   * <p>The authority component [...] is terminated by one of the following:
   *
   * <ul>
   *   <li>the next slash ("/")
   *   <li>question mark ("?")
   *   <li>number sign ("#") character
   *   <li>the end of the URI.
   * </ul>
   */
  private boolean isAuthorityEnd() {
    if (this.index >= this.str.length()) {
      return true;
    }
    char c = this.str.charAt(this.index);
    return (c == '?' || c == '#' || c == '/');
  }

  /**
   * Reports whether string contains a valid userinfo.
   *
   * <p>Method parses the rule:
   *
   * <pre>userinfo = *( unreserved / pct-encoded / sub-delims / ":" )
   *
   * Terminated by "@" in authority.
   */
  private boolean userinfo() {
    int start = this.index;

    while (true) {
      if (this.unreserved() || this.pctEncoded() || this.subDelims() || this.take(':')) {
        continue;
      }

      if (this.index < this.str.length()) {
        if (this.str.charAt(this.index) == '@') {
          return true;
        }
      }

      this.index = start;

      return false;
    }
  }

  private static int unhex(char c) {
    if ('0' <= c && c <= '9') {
      return c - '0';
    } else if ('a' <= c && c <= 'f') {
      return c - 'a' + 10;
    } else if ('A' <= c && c <= 'F') {
      return c - 'A' + 10;
    }

    return 0;
  }

  /**
   * Verifies that str is correctly percent-encoded.
   *
   * <p>Note that we essentially want to mimic the behavior of decodeURIComponent, which would fail
   * on malformed URLs. Java does have various methods for decoding URLs, but none behave
   * consistently with decodeURIComponent.
   *
   * <p>The code below is a combination of `checkHostPctEncoded` from the protovalidate-go
   * implementation and Java's java.net.URI#decode methods.
   */
  private boolean checkHostPctEncoded(String str) {
     CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();

    int strLen = str.length();
    ByteBuffer buffer = ByteBuffer.allocate(strLen);
    CharBuffer out = CharBuffer.allocate(strLen);

    // Unhex str and convert to a ByteBuffer.
    for (int i = 0; i < str.length(); ) {
      if (str.charAt(i) == '%') {
        // If we encounter a %, unhex the two following digits, extract their
        // last 4 bits, cast to a byte.
        byte b =
            (byte)
                (((unhex(str.charAt(i + 1)) & 0xf) << 4) | ((unhex(str.charAt(i + 2)) & 0xf) << 0));
        buffer.put(b);
        i += 3;
      } else {
        // Not percent encoded, extract the last 4 bits, convert to a byte
        // and add to the byte buffer.
        buffer.put((byte) (str.charAt(i) & 0xf));
        i++;
      }
    }

    // Attempt to decode the byte buffer as UTF-8.
    CoderResult f = decoder.decode((ByteBuffer) buffer.flip(), out, true);

    // If an error occurred, return false as invalid.
    if (f.isError()) {
      return false;
    }
    // Flush the buffer
    f = decoder.flush(out);

    // If an error occurred, return false as invalid.
    // Otherwise return true.
    return !f.isError();
  }

  /**
   * Reports whether string contains a valid host.
   *
   * <p>Method parses the rule:
   *
   * <pre>host = IP-literal / IPv4address / reg-name.
   */
  private boolean host() {
    if (this.index >= this.str.length()) {
      return true;
    }

    int start = this.index;
    this.pctEncodedFound = false;

    // Note: IPv4address is a subset of reg-name
    if ((this.str.charAt(this.index) == '[' && this.ipLiteral()) || this.regName()) {
      if (this.pctEncodedFound) {
        String rawHost = this.str.substring(start, this.index);
        // RFC 3986:
        // > URI producing applications must not use percent-encoding in host
        // > unless it is used to represent a UTF-8 character sequence.
        if (!this.checkHostPctEncoded(rawHost)) {
          return false;
        }
      }

      return true;
    }

    return false;
  }

  /**
   * Reports whether string contains a valid port.
   *
   * <p>Method parses the rule:
   *
   * <pre>port = *DIGIT
   *
   * Terminated by end of authority.
   */
  private boolean port() {
    int start = this.index;

    while (true) {
      if (this.digit()) {
        continue;
      }

      if (this.isAuthorityEnd()) {
        return true;
      }

      this.index = start;

      return false;
    }
  }

  /**
   * Reports whether string contains a valid IP literal.
   *
   * <p>Method parses the rule from RFC 6874:
   *
   * <pre>IP-literal = "[" ( IPv6address / IPv6addrz / IPvFuture  ) "]"
   */
  private boolean ipLiteral() {
    int start = this.index;

    if (this.take('[')) {
      int j = this.index;

      if (this.ipv6Address() && this.take(']')) {
        return true;
      }

      this.index = j;

      if (this.ipv6Addrz() && this.take(']')) {
        return true;
      }

      this.index = j;

      if (this.ipvFuture() && this.take(']')) {
        return true;
      }
    }

    this.index = start;

    return false;
  }

  /**
   * Reports whether string contains a valid ipv6 address.
   *
   * <p>ipv6Address parses the rule "IPv6address".
   *
   * <p>Relies on the implementation of isIp.
   */
  private boolean ipv6Address() {
    int start = this.index;

    while (this.hexDig() || this.take(':')) {}

    if (CustomOverload.isIp(this.str.substring(start, this.index), 6)) {
      return true;
    }

    this.index = start;

    return false;
  }

  /**
   * Reports whether string contains a valid IPv6addrz.
   *
   * Method parses the rule:
   *
   * <pre>IPv6addrz = IPv6address "%25" ZoneID
   */
  private boolean ipv6Addrz() {
    int start = this.index;

    if (this.ipv6Address() && this.take('%') && this.take('2') && this.take('5') && this.zoneID()) {
      return true;
    }

    this.index = start;

    return false;
  }

  /**
   * Reports whether string contains a valid zone ID.
   *
   * Method parses the rule:
   *
   * <pre>ZoneID = 1*( unreserved / pct-encoded )
   */
  private boolean zoneID() {
    int start = this.index;

    while (this.unreserved() || this.pctEncoded()) {}

    if (this.index - start > 0) {
      return true;
    }

    this.index = start;

    return false;
  }

  /**
   * Reports whether string contains a valid IPvFuture.
   *
   * Method parses the rule:
   *
   * <pre>IPvFuture  = "v" 1*HEXDIG "." 1*( unreserved / sub-delims / ":" )
   */
  private boolean ipvFuture() {
    int start = this.index;

    if (this.take('v') && this.hexDig()) {
      while (this.hexDig()) {}

      if (this.take('.')) {
        int j = 0;

        while (this.unreserved() || this.subDelims() || this.take(':')) {
          j++;
        }

        if (j >= 1) {
          return true;
        }
      }
    }

    this.index = start;

    return false;
  }

  /**
   * Reports whether string contains a valid reg-name.
   *
   * Method parses the rule:
   *
   * <pre>reg-name = *( unreserved / pct-encoded / sub-delims )
   *
   * Terminates on start of port (":") or end of authority.
   */
  private boolean regName() {
    int start = this.index;

    while (true) {
      if (this.unreserved() || this.pctEncoded() || this.subDelims()) {
        continue;
      }

      if (this.isAuthorityEnd()) {
        // End of authority
        return true;
      }

      if (this.str.charAt(this.index) == ':') {
        return true;
      }

      this.index = start;

      return false;
    }
  }

  /**
   * Reports whether the current position is the end of the path.
   *
   * <p>The path is terminated by one of the following:
   *
   * <ul>
   *   <li>the first question mark ("?")
   *   <li>number sign ("#") character
   *   <li>the end of the URI.
   * </ul>
   */
  private boolean isPathEnd() {
    if (this.index >= this.str.length()) {
      return true;
    }

    char c = this.str.charAt(this.index);

    return (c == '?' || c == '#');
  }

  /**
   * Reports whether string contains a valid path-abempty.
   *
   * Method parses the rule:
   *
   * <pre>path-abempty = *( "/" segment )
   *
   * Terminated by end of path: "?", "#", or end of URI.
   */
  private boolean pathAbempty() {
    int start = this.index;

    while (this.take('/') && this.segment()) {}

    if (this.isPathEnd()) {
      return true;
    }

    this.index = start;

    return false;
  }

  /**
   * Reports whether string contains a valid path-absolute.
   *
   * Method parses the rule:
   *
   * <pre>path-absolute = "/" [ segment-nz *( "/" segment ) ]
   *
   * Terminated by end of path: "?", "#", or end of URI.
   */
  private boolean pathAbsolute() {
    int start = this.index;

    if (this.take('/')) {
      if (this.segmentNz()) {
        while (this.take('/') && this.segment()) {}
      }

      if (this.isPathEnd()) {
        return true;
      }
    }

    this.index = start;

    return false;
  }

  /**
   * Reports whether string contains a valid path-noscheme.
   *
   * Method parses the rule:
   *
   * <pre>path-noscheme = segment-nz-nc *( "/" segment )
   *
   * Terminated by end of path: "?", "#", or end of URI.
   */
  private boolean pathNoscheme() {
    int start = this.index;

    if (this.segmentNzNc()) {
      while (this.take('/') && this.segment()) {}

      if (this.isPathEnd()) {
        return true;
      }
    }

    this.index = start;

    return false;
  }

  /**
   * Reports whether string contains a valid path-rootless.
   *
   * Method parses the rule:
   *
   * <pre>path-rootless = segment-nz *( "/" segment )
   *
   * Terminated by end of path: "?", "#", or end of URI.
   */
  private boolean pathRootless() {
    int start = this.index;

    if (this.segmentNz()) {
      while (this.take('/') && this.segment()) {}

      if (this.isPathEnd()) {
        return true;
      }
    }

    this.index = start;

    return false;
  }

  /**
   * Reports whether string contains a valid path-empty.
   *
   * Method parses the rule:
   *
   * <pre>path-empty = 0<pchar>
   *
   * Terminated by end of path: "?", "#", or end of URI.
   */
  private boolean pathEmpty() {
    return this.isPathEnd();
  }

  /**
   * Reports whether string contains a valid segment.
   *
   * Method parses the rule:
   *
   * <pre>segment = *pchar
   */
  private boolean segment() {
    while (this.pchar()) {}

    return true;
  }

  /**
   * Reports whether string contains a valid segment-nz.
   *
   * Method parses the rule:
   *
   * <pre>segment-nz = 1*pchar
   */
  private boolean segmentNz() {
    int start = this.index;

    if (this.pchar()) {
      while (this.pchar()) {}
      return true;
    }

    this.index = start;

    return false;
  }

  /**
   * Reports whether string contains a valid segment-nz-nc.
   *
   * Method parses the rule:
   *
   * <pre>segment-nz-nc = 1*( unreserved / pct-encoded / sub-delims / "@" )
   *                   ; non-zero-length segment without any colon ":"
   */
  private boolean segmentNzNc() {
    int start = this.index;

    while (this.unreserved() || this.pctEncoded() || this.subDelims() || this.take('@')) {}

    if (this.index - start > 0) {
      return true;
    }

    this.index = start;

    return false;
  }

  /**
   * Reports whether string contains a valid pchar.
   *
   * Method parses the rule:
   *
   * <pre>pchar = unreserved / pct-encoded / sub-delims / ":" / "@"
   */
  private boolean pchar() {
    return (this.unreserved()
        || this.pctEncoded()
        || this.subDelims()
        || this.take(':')
        || this.take('@'));
  }

  /**
   * Reports whether string contains a valid query.
   *
   * Method parses the rule:
   *
   * <pre>query = *( pchar / "/" / "?" )
   *
   * Terminated by "#" or end of URI.
   */
  private boolean query() {
    int start = this.index;

    while (true) {
      if (this.pchar() || this.take('/') || this.take('?')) {
        continue;
      }

      if (this.index == this.str.length() || this.str.charAt(this.index) == '#') {
        return true;
      }

      this.index = start;

      return false;
    }
  }

  /**
   * Reports whether string contains a valid fragment.
   *
   * Method parses the rule:
   *
   * <pre>fragment = *( pchar / "/" / "?" )
   *
   * Terminated by end of URI.
   */
  private boolean fragment() {
    int start = this.index;

    while (true) {
      if (this.pchar() || this.take('/') || this.take('?')) {
        continue;
      }

      if (this.index == this.str.length()) {
        return true;
      }

      this.index = start;

      return false;
    }
  }

  /**
   * Reports whether string contains a valid pct-encoded.
   *
   * Method parses the rule:
   *
   * <pre>pct-encoded = "%"+HEXDIG+HEXDIG
   *
   * Sets `pctEncodedFound` to true if a valid triplet was found.
   */
  private boolean pctEncoded() {
    int start = this.index;

    if (this.take('%') && this.hexDig() && this.hexDig()) {
      this.pctEncodedFound = true;

      return true;
    }

    this.index = start;

    return false;
  }

  /**
   * Reports whether current position is an unreserved character.
   *
   * Method parses the rule:
   *
   * <pre>unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
   */
  private boolean unreserved() {
    return (this.alpha()
        || this.digit()
        || this.take('-')
        || this.take('_')
        || this.take('.')
        || this.take('~'));
  }

  /**
   * Reports whether current position is a sub-delim.
   *
   * Method parses the rule:
   *
   * <pre>sub-delims  = "!" / "$" / "&" / "'" / "(" / ")"
   *                  / "*" / "+" / "," / ";" / "="
   */
  private boolean subDelims() {
    return (this.take('!')
        || this.take('$')
        || this.take('&')
        || this.take('\'')
        || this.take('(')
        || this.take(')')
        || this.take('*')
        || this.take('+')
        || this.take(',')
        || this.take(';')
        || this.take('='));
  }

  /**
   * Reports whether current position is an alpha character.
   *
   * Method parses the rule:
   *
   * <pre>ALPHA =  %x41-5A / %x61-7A ; A-Z / a-z
   */
  private boolean alpha() {
    if (this.index >= this.str.length()) {
      return false;
    }

    char c = this.str.charAt(this.index);

    if (('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z')) {
      this.index++;
      return true;
    }

    return false;
  }

  /**
   * Reports whether the current position is a hex digit.
   *
   * <p>Method parses the rule:
   *
   * <pre>HEXDIG = DIGIT / "A" / "B" / "C" / "D" / "E" / "F"
   */
  private boolean hexDig() {
    if (this.index >= this.str.length()) {
      return false;
    }

    char c = this.str.charAt(this.index);

    if (('0' <= c && c <= '9') || ('a' <= c && c <= 'f') || ('A' <= c && c <= 'F')) {
      this.index++;
      return true;
    }

    return false;
  }

  /**
   * Reports whether the current position is a digit.
   *
   * <p>Method parses the rule:
   *
   * <pre>DIGIT = %x30-39 ; 0-9
   */
  private boolean digit() {
    if (this.index >= this.str.length()) {
      return false;
    }

    char c = this.str.charAt(this.index);
    if ('0' <= c && c <= '9') {
      this.index++;
      return true;
    }
    return false;
  }

  /**
   * Take the given char at the current index.
   *
   * <p>If char is at the current index, increment the index.
   */
  private boolean take(char c) {
    if (this.index >= this.str.length()) {
      return false;
    }

    if (this.str.charAt(this.index) == c) {
      this.index++;
      return true;
    }

    return false;
  }
}
