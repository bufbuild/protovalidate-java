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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Ipv6 is a class used to parse a given string to determine if it is an IPv6 address or address
 * prefix.
 */
final class Ipv6 {
  private String str;
  private int index;
  // 16-bit pieces found
  private List<Integer> pieces;
  // number of 16-bit pieces found when double colon was found
  private int doubleColonAt;
  private boolean doubleColonSeen;
  // dotted notation for right-most 32 bits
  private String dottedRaw;
  // dotted notation successfully parsed as IPv4
  @Nullable private Ipv4 dottedAddr;
  private boolean zoneIDFound;
  // 0 - 128
  private int prefixLen;

  Ipv6(String str) {
    this.str = str;
    this.pieces = new ArrayList<Integer>();
    this.doubleColonAt = -1;
    this.dottedRaw = "";
  }

  /**
   * Returns the 128-bit value of an address parsed through address() or addressPrefix() as a
   * 2-element length array of 64-bit values.
   *
   * <p>Returns [0L, 0L] if no address was parsed successfully.
   */
  private long[] getBits() {
    List<Integer> p16 = this.pieces;

    // handle dotted decimal, add to p16
    if (this.dottedAddr != null) {
      // right-most 32 bits
      long dotted32 = this.dottedAddr.getBits();
      // high 16 bits
      p16.add((int) (dotted32 >> 16));
      // low 16 bits
      p16.add((int) dotted32);
    }

    // handle double colon, fill pieces with 0
    if (this.doubleColonSeen) {
      while (p16.size() < 8) {
        p16.add(this.doubleColonAt, 0x00000000);
      }
    }

    if (p16.size() != 8) {
      return new long[] {0L, 0L};
    }

    return new long[] {
      Long.valueOf(p16.get(0)) << 48
          | Long.valueOf(p16.get(1)) << 32
          | Long.valueOf(p16.get(2)) << 16
          | Long.valueOf(p16.get(3)),
      Long.valueOf(p16.get(4)) << 48
          | Long.valueOf(p16.get(5)) << 32
          | Long.valueOf(p16.get(6)) << 16
          | Long.valueOf(p16.get(7))
    };
  }

  boolean isPrefixOnly() {
    // For each 64-bit piece of the address, require that values to the right of the prefix are zero
    long[] bits = this.getBits();
    for (int i = 0; i < bits.length; i++) {
      long p64 = bits[i];
      long size = this.prefixLen - 64L * i;

      long mask = 0L;
      if (size >= 64) {
        mask = 0xFFFFFFFFFFFFFFFFL;
      } else if (size < 0) {
        mask = 0x0;
      } else {
        mask = ~(0xFFFFFFFFFFFFFFFFL >>> size);
      }
      long masked = p64 & mask;
      if (p64 != masked) {
        return false;
      }
    }

    return true;
  }

  // Parses an IPv6 Address following RFC 4291, with optional zone id following RFC 4007.
  boolean address() {
    return this.addressPart() && this.index == this.str.length();
  }

  // Parse IPv6 Address Prefix following RFC 4291. Zone id is not permitted.
  boolean addressPrefix() {
    return this.addressPart()
        && !this.zoneIDFound
        && this.take('/')
        && this.prefixLength()
        && this.index == this.str.length();
  }

  // Stores value in prefixLen
  private boolean prefixLength() {
    int start = this.index;

    while (this.index < this.str.length() && this.digit()) {
      if (this.index - start > 3) {
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

      if (val > 128) {
        // max 128 bits
        return false;
      }

      this.prefixLen = val;
      return true;
    } catch (NumberFormatException nfe) {
      // Error converting to number
      return false;
    }
  }

  // Stores dotted notation for right-most 32 bits in dottedRaw / dottedAddr if found.
  private boolean addressPart() {
    while (this.index < this.str.length()) {
      // dotted notation for right-most 32 bits, e.g. 0:0:0:0:0:ffff:192.1.56.10
      if ((this.doubleColonSeen || this.pieces.size() == 6) && this.dotted()) {
        Ipv4 dotted = new Ipv4(this.dottedRaw);
        if (dotted.address()) {
          this.dottedAddr = dotted;
          return true;
        }
        return false;
      }

      try {
        if (this.h16()) {
          continue;
        }
      } catch (IllegalStateException | NumberFormatException e) {
        return false;
      }

      if (this.take(':')) {
        if (this.take(':')) {
          if (this.doubleColonSeen) {
            return false;
          }

          this.doubleColonSeen = true;
          this.doubleColonAt = this.pieces.size();
          if (this.take(':')) {
            return false;
          }
        } else if (this.index == 1 || this.index == this.str.length()) {
          // invalid - string cannot start or end on single colon
          return false;
        }
        continue;
      }

      if (this.str.charAt(this.index) == '%' && !this.zoneID()) {
        return false;
      }

      break;
    }

    return this.doubleColonSeen || this.pieces.size() == 8;
  }

  /**
   * There is no definition for the character set allowed in the zone identifier. RFC 4007 permits
   * basically any non-null string.
   *
   * <pre>RFC 6874: ZoneID = 1*( unreserved / pct-encoded )
   */
  private boolean zoneID() {
    int start = this.index;

    if (this.take('%')) {
      if (this.str.length() - this.index > 0) {
        // permit any non-null string
        this.index = this.str.length();
        this.zoneIDFound = true;

        return true;
      }
    }

    this.index = start;
    this.zoneIDFound = false;

    return false;
  }

  /**
   * Determines whether the current position is a dotted address.
   *
   * <p>Parses the rule:
   *
   * <pre>1*3DIGIT "." 1*3DIGIT "." 1*3DIGIT "." 1*3DIGIT
   *
   * <p>Stores match in dottedRaw.
   */
  private boolean dotted() {
    int start = this.index;

    this.dottedRaw = "";

    while (this.index < this.str.length() && (this.digit() || this.take('.'))) {}

    if (this.index - start >= 7) {
      this.dottedRaw = this.str.substring(start, this.index);

      return true;
    }

    this.index = start;

    return false;
  }

  /**
   * Determines whether the current position is an h16.
   *
   * <p>Parses the rule:
   *
   * <pre>h16 = 1*4HEXDIG
   *
   * If 1-4 hex digits are found, the parsed 16-bit unsigned integer is stored
   * in pieces and true is returned.
   * If 0 hex digits are found, returns false.
   * If more than 4 hex digits are found, an IllegalStateException is thrown.
   * If the found hex digits cannot be converted to an int, a NumberFormatException is raised.
   */
  private boolean h16() throws IllegalStateException, NumberFormatException {
    int start = this.index;

    while (this.index < this.str.length() && this.hexDig()) {}

    String str = this.str.substring(start, this.index);

    if (str.isEmpty()) {
      // too short, just return false
      // this is not an error condition, it just means we didn't find any
      // hex digits at the current position.
      return false;
    }

    if (str.length() > 4) {
      // too long
      // this is an error condition, it means we found a string of more than
      // four valid hex digits, which is invalid in ipv6 addresses.
      throw new IllegalStateException("invalid hex");
    }

    // Note that this will throw a NumberFormatException if string cannot be
    // converted to an int.
    int val = Integer.parseInt(str, 16);

    this.pieces.add(val);
    return true;
  }

  /**
   * Determines whether the current position is a hex digit.
   *
   * <p>Parses the rule:
   *
   * <pre>HEXDIG = DIGIT / "A" / "B" / "C" / "D" / "E" / "F"
   */
  private boolean hexDig() {
    char c = this.str.charAt(this.index);

    if (('0' <= c && c <= '9') || ('a' <= c && c <= 'f') || ('A' <= c && c <= 'F')) {
      this.index++;

      return true;
    }

    return false;
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

  /** Take the given char at the current position, incrementing the index if necessary. */
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
