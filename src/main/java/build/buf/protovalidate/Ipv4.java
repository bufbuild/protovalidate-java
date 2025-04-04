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

import java.util.ArrayList;
import java.util.List;

/**
 * Ipv4 is a class used to parse a given string to determine if it is an IPv4 address or address prefix.
 */
final class Ipv4 {
  private String str;
  private int index;
  private List<Short> octets;
  private int prefixLen;

  Ipv4(String str) {
    this.str = str;
    this.octets = new ArrayList<Short>();
  }

  /**
   * Returns the 32-bit value of an address parsed through address() or addressPrefix().
   *
   * <p>Returns -1 if no address was parsed successfully.
   */
  int getBits() {
    if (this.octets.size() != 4) {
      return -1;
    }
    return (this.octets.get(0) << 24)
        | (this.octets.get(1) << 16)
        | (this.octets.get(2) << 8)
        | this.octets.get(3);
  }

  /**
   * Returns true if all bits to the right of the prefix-length are all zeros.
   *
   * <p>Behavior is undefined if addressPrefix() has not been called before, or has returned false.
   */
  boolean isPrefixOnly() {
    int bits = this.getBits();

    int mask = 0;
    if (this.prefixLen == 32) {
      mask = 0xffffffff;
    } else {
      mask = ~(0xffffffff >>> this.prefixLen);
    }

    int masked = bits & mask;

    return bits == masked;
  }

  // Parses an IPv4 Address in dotted decimal notation.
  boolean address() {
    return this.addressPart() && this.index == this.str.length();
  }

  // Parses an IPv4 Address prefix.
  boolean addressPrefix() {
    return this.addressPart()
        && this.take('/')
        && this.prefixLength()
        && this.index == this.str.length();
  }

  // Store value in prefixLen
  private boolean prefixLength() {
    int start = this.index;

    while (this.index < this.str.length() && this.digit()) {
      if (this.index - start > 2) {
        // max prefix-length is 32 bits, so anything more than 2 digits is invalid
        return false;
      }
    }

    String str = this.str.substring(start, this.index);
    if (str.isEmpty()) {
      // too short
      return false;
    }

    if (str.length() > 1 && str.charAt(0) == '0') {
      // bad leading 0
      return false;
    }

    try {
      int val = Integer.parseInt(str);

      if (val > 32) {
        // max 32 bits
        return false;
      }

      this.prefixLen = val;
      return true;
    } catch (NumberFormatException nfe) {
      return false;
    }
  }

  private boolean addressPart() {
    int start = this.index;

    if (this.decOctet()
        && this.take('.')
        && this.decOctet()
        && this.take('.')
        && this.decOctet()
        && this.take('.')
        && this.decOctet()) {
      return true;
    }

    this.index = start;

    return false;
  }

  private boolean decOctet() {
    int start = this.index;

    while (this.index < this.str.length() && this.digit()) {
      if (this.index - start > 3) {
        // decimal octet can be three characters at most
        return false;
      }
    }

    String str = this.str.substring(start, this.index);
    if (str.isEmpty()) {
      // too short
      return false;
    }

    if (str.length() > 1 && str.charAt(0) == '0') {
      // bad leading 0
      return false;
    }

    try {
      int val = Integer.parseInt(str);

      if (val > 255) {
        return false;
      }

      this.octets.add((short) val);

      return true;
    } catch (NumberFormatException nfe) {
      // Error converting to number
      return false;
    }
  }

  /**
   * Determines whether the current position is a digit.
   *
   * <p>Parses the rule:
   *
   * <pre>DIGIT = %x30-39 ; 0-9
   */
  private boolean digit() {
    char c = this.str.charAt(this.index);
    if ('0' <= c && c <= '9') {
      this.index++;
      return true;
    }
    return false;
  }

  /**
   * Take the given char at the current position, incrementing the index if necessary.
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
