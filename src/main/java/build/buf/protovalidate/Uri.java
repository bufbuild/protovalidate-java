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
   * <p>URI = scheme ":" hier-part [ "?" query ] [ "#" fragment ]
   */
  boolean uri() {
    int start = this.index;

    if (!(this.scheme() && this.take(':') && this.hierPart())) {
      this.index = start;
      System.err.println("failed hier");
      return false;
    }

    if (this.take('?') && !this.query()) {
      System.err.println("failed cuir");
      return false;
    }

    if (this.take('#') && !this.fragment()) {
      System.err.println("failed fragomen");
      return false;
    }

    if (this.index != this.str.length()) {
      System.err.println("failed index");
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
   * <p>hier-part = "//" authority path-abempty / path-absolute / path-rootless / path-empty
   */
  // The multiple take('/') invocations are intended.
  @SuppressWarnings("IdentityBinaryExpression")
  private boolean hierPart() {
    int start = this.index;

    if (this.take('/') && this.take('/') && this.authority() && this.pathAbempty()) {
      System.err.println("trooby");
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
   * <p>URI-reference = URI / relative-ref
   */
  boolean uriReference() {
    return this.uri() || this.relativeRef();
  }

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

  // The multiple take('/') invocations are intended.
  @SuppressWarnings("IdentityBinaryExpression")
  private boolean relativePart() {
    int start = this.index;

    if (this.take('/') && this.take('/') && this.authority() && this.pathAbempty()) {
      return true;
    }

    this.index = start;

    return this.pathAbsolute() || this.pathNoscheme() || this.pathEmpty();
  }

  private boolean scheme() {
    int start = this.index;

    if (this.alpha()) {
      while (this.alpha() || this.digit() || this.take('+') || this.take('-') || this.take('.')) {
        // continue
      }

      if (this.str.charAt(this.index) == ':') {
        return true;
      }
    }

    this.index = start;

    return false;
  }

  private boolean authority() {
    int start = this.index;

    if (this.userinfo()) {
      if (!this.take('@')) {
        this.index = start;
        System.err.println("auth1 fail");
        return false;
      }
    }

    if (!this.host()) {
      this.index = start;
      System.err.println("auth2 fail");
      return false;
    }

    if (this.take(':')) {
      if (!this.port()) {
        this.index = start;
        System.err.println("auth3 fail");
        return false;
      }
    }

    if (!this.isAuthorityEnd()) {
      this.index = start;
      System.err.println("auth4 fail");
      return false;
    }

    return true;
  }

  private boolean isAuthorityEnd() {
    if (this.index >= this.str.length()) {
      return true;
    }
    char c = this.str.charAt(this.index);
    return (c == '?' || c == '#' || c == '/');
  }

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

  @FunctionalInterface
  interface UnhexOperation {
    int unhex(char c);
  }

  // private boolean checkHostPctEncoded(String str) {

  //   UnhexOperation fn =
  //       c -> {
  //         if ('0' <= c && c <= '9') {
  //           return c - '0';
  //         } else if ('a' <= c && c <= 'f') {
  //           return c - 'a' + 10;
  //         } else if ('A' <= c && c <= 'F') {
  //           return c - 'A' + 10;
  //         }

  //         return 0;
  //       };

  //   List<Integer> escaped = new ArrayList<Integer>();

  //   for (int i = 0; i < str.length(); ) {
  //     if (str.charAt(i) == '%') {
  //       escaped.add(fn.unhex(str.charAt(i + 1)) << 4 | fn.unhex(str.charAt(i + 2)));
  //       i += 3;
  //     } else {
  //       escaped.add((int) str.charAt(i));
  //       i++;
  //     }
  //   }

  //   CharsetDecoder decoder = Charset.forName(StandardCharsets.UTF_8.toString()).newDecoder();

  //   decoder.onMalformedInput(CodingErrorAction.REPORT); // Reject invalid input
  //   decoder.onUnmappableCharacter(CodingErrorAction.REPORT);

  //   try {
  //     decoder.decode(java.nio.ByteBuffer.wrap(escaped)); // Attempt to decode
  //     return true; // No errors means valid UTF-8
  //   } catch (Exception e) {
  //     return false; // Exception means invalid UTF-8
  //   }

  //   System.err.println(escaped);

  //   return true;
  // }

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

        if (!this.checkHostPctEncoded(rawHost)) {
          return false;
        }
        // try {
        //   String s = URLDecoder.decode(rawHost, StandardCharsets.UTF_8.toString());
        //   System.err.println(s);
        // } catch (IllegalArgumentException e) {
        //     System.err.println("CRASH");
        //     return false;

        // } catch (UnsupportedEncodingException e) {
        //     System.err.println("BOOM");
        //   return false;
        // }
      }

      return true;
    }

    return false;
  }

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

    System.err.println("litch fail");
    return false;
  }

  private boolean ipv6Address() {
    int start = this.index;

    while (this.hexDig() || this.take(':')) {
      // continue
    }

    if (CustomOverload.isIP(this.str.substring(start, this.index), 6)) {
      return true;
    }

    this.index = start;

    return false;
  }

  private boolean ipv6Addrz() {
    int start = this.index;

    if (this.ipv6Address() && this.take('%') && this.take('2') && this.take('5') && this.zoneID()) {
      return true;
    }

    this.index = start;

    return false;
  }

  private boolean zoneID() {
    int start = this.index;

    while (this.unreserved() || this.pctEncoded()) {
      // continue
    }

    if (this.index - start > 0) {
      return true;
    }

    this.index = start;

    return false;
  }

  private boolean ipvFuture() {
    int start = this.index;

    if (this.take('v') && this.hexDig()) {
      while (this.hexDig()) {
        // continue;
      }

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

  private boolean isPathEnd() {
    if (this.index >= this.str.length()) {
      return true;
    }

    char c = this.str.charAt(this.index);

    System.err.println("path ned fail");
    return (c == '?' || c == '#');
  }

  private boolean pathAbempty() {
    int start = this.index;

    while (this.take('/') && this.segment()) {
      // continue
    }

    if (this.isPathEnd()) {
      return true;
    }

    this.index = start;

    return false;
  }

  private boolean pathAbsolute() {
    int start = this.index;

    if (this.take('/')) {
      if (this.segmentNz()) {
        while (this.take('/') && this.segment()) {
          // continue
        }
      }

      if (this.isPathEnd()) {
        return true;
      }
    }

    this.index = start;

    System.err.println("path abs fail");
    return false;
  }

  private boolean pathNoscheme() {
    int start = this.index;

    if (this.segmentNzNc()) {
      while (this.take('/') && this.segment()) {
        // continue
      }

      if (this.isPathEnd()) {
        return true;
      }
    }

    this.index = start;

    return false;
  }

  private boolean pathRootless() {
    int start = this.index;

    if (this.segmentNz()) {
      while (this.take('/') && this.segment()) {
        // continue
      }

      if (this.isPathEnd()) {
        return true;
      }
    }

    this.index = start;

    System.err.println("path root fail");
    return false;
  }

  private boolean pathEmpty() {
    return this.isPathEnd();
  }

  private boolean segment() {
    while (this.pchar()) {
      // continue
    }

    return true;
  }

  private boolean segmentNz() {
    int start = this.index;

    if (this.pchar()) {
      while (this.pchar()) {
        // continue
      }
      return true;
    }

    this.index = start;

    return false;
  }

  private boolean segmentNzNc() {
    int start = this.index;

    while (this.unreserved() || this.pctEncoded() || this.subDelims() || this.take('@')) {
      // continue
    }

    if (this.index - start > 0) {
      return true;
    }

    this.index = start;

    return false;
  }

  private boolean pchar() {
    return (this.unreserved()
        || this.pctEncoded()
        || this.subDelims()
        || this.take(':')
        || this.take('@'));
  }

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

  private boolean pctEncoded() {
    int start = this.index;

    if (this.take('%') && this.hexDig() && this.hexDig()) {
      this.pctEncodedFound = true;

      return true;
    }

    this.index = start;

    return false;
  }

  private boolean unreserved() {
    return (this.alpha()
        || this.digit()
        || this.take('-')
        || this.take('_')
        || this.take('.')
        || this.take('~'));
  }

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
   * <p>HEXDIG = DIGIT / "A" / "B" / "C" / "D" / "E" / "F"
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
   * <p>DIGIT = %x30-39 ; 0-9
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
