// Copyright 2023-2026 Buf Technologies, Inc.
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

package build.buf.protovalidate.rules;

import build.buf.protovalidate.Evaluator;
import build.buf.protovalidate.RuleViolation;
import build.buf.protovalidate.Value;
import build.buf.protovalidate.exceptions.ExecutionException;
import build.buf.validate.BytesRules;
import build.buf.validate.FieldRules;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Native evaluator for the standard bytes rules: {@code const}, {@code len}, {@code min_len},
 * {@code max_len}, {@code pattern}, {@code prefix}, {@code suffix}, {@code contains}, {@code in},
 * {@code not_in}, plus the well-known size-only formats {@code ip}, {@code ipv4}, {@code ipv6},
 * {@code uuid}. Mirrors {@code nativeBytesEval} in protovalidate-go's {@code native_bytes.go}.
 */
final class BytesRulesEvaluator implements Evaluator {

  /** Well-known bytes format constraint — purely size-based per protovalidate spec. */
  private enum WellKnown {
    IP(
        "bytes.ip",
        "must be a valid IP address",
        "bytes.ip_empty",
        "value is empty, which is not a valid IP address",
        Arrays.asList(4, 16),
        BytesRules.IP_FIELD_NUMBER),
    IPV4(
        "bytes.ipv4",
        "must be a valid IPv4 address",
        "bytes.ipv4_empty",
        "value is empty, which is not a valid IPv4 address",
        Collections.singletonList(4),
        BytesRules.IPV4_FIELD_NUMBER),
    IPV6(
        "bytes.ipv6",
        "must be a valid IPv6 address",
        "bytes.ipv6_empty",
        "value is empty, which is not a valid IPv6 address",
        Collections.singletonList(16),
        BytesRules.IPV6_FIELD_NUMBER),
    UUID(
        "bytes.uuid",
        "must be a valid UUID",
        "bytes.uuid_empty",
        "value is empty, which is not a valid UUID",
        Collections.singletonList(16),
        BytesRules.UUID_FIELD_NUMBER);

    final RuleSite site;
    final RuleSite emptySite;
    final List<Integer> validSizes;
    final FieldDescriptor field;

    WellKnown(
        String ruleId,
        String message,
        String emptyRuleId,
        String emptyMessage,
        List<Integer> validSizes,
        int fieldNumber) {
      FieldDescriptor leaf = BytesRules.getDescriptor().findFieldByNumber(fieldNumber);
      this.field = leaf;
      this.site = RuleSite.of(BYTES_RULES_DESC, leaf, ruleId, message);
      this.emptySite = RuleSite.of(BYTES_RULES_DESC, leaf, emptyRuleId, emptyMessage);
      this.validSizes = Collections.unmodifiableList(validSizes);
    }

    boolean sizeIsValid(int size) {
      return validSizes.contains(size);
    }
  }

  private static final FieldDescriptor BYTES_RULES_DESC =
      FieldRules.getDescriptor().findFieldByNumber(FieldRules.BYTES_FIELD_NUMBER);

  private static final RuleSite CONST_SITE =
      RuleSite.of(
          BYTES_RULES_DESC,
          BytesRules.getDescriptor().findFieldByNumber(BytesRules.CONST_FIELD_NUMBER),
          "bytes.const",
          null);
  private static final RuleSite LEN_SITE =
      RuleSite.of(
          BYTES_RULES_DESC,
          BytesRules.getDescriptor().findFieldByNumber(BytesRules.LEN_FIELD_NUMBER),
          "bytes.len",
          null);
  private static final RuleSite MIN_LEN_SITE =
      RuleSite.of(
          BYTES_RULES_DESC,
          BytesRules.getDescriptor().findFieldByNumber(BytesRules.MIN_LEN_FIELD_NUMBER),
          "bytes.min_len",
          null);
  private static final RuleSite MAX_LEN_SITE =
      RuleSite.of(
          BYTES_RULES_DESC,
          BytesRules.getDescriptor().findFieldByNumber(BytesRules.MAX_LEN_FIELD_NUMBER),
          "bytes.max_len",
          null);
  private static final RuleSite PATTERN_SITE =
      RuleSite.of(
          BYTES_RULES_DESC,
          BytesRules.getDescriptor().findFieldByNumber(BytesRules.PATTERN_FIELD_NUMBER),
          "bytes.pattern",
          null);
  private static final RuleSite PREFIX_SITE =
      RuleSite.of(
          BYTES_RULES_DESC,
          BytesRules.getDescriptor().findFieldByNumber(BytesRules.PREFIX_FIELD_NUMBER),
          "bytes.prefix",
          null);
  private static final RuleSite SUFFIX_SITE =
      RuleSite.of(
          BYTES_RULES_DESC,
          BytesRules.getDescriptor().findFieldByNumber(BytesRules.SUFFIX_FIELD_NUMBER),
          "bytes.suffix",
          null);
  private static final RuleSite CONTAINS_SITE =
      RuleSite.of(
          BYTES_RULES_DESC,
          BytesRules.getDescriptor().findFieldByNumber(BytesRules.CONTAINS_FIELD_NUMBER),
          "bytes.contains",
          null);
  private static final RuleSite IN_SITE =
      RuleSite.of(
          BYTES_RULES_DESC,
          BytesRules.getDescriptor().findFieldByNumber(BytesRules.IN_FIELD_NUMBER),
          "bytes.in",
          null);
  private static final RuleSite NOT_IN_SITE =
      RuleSite.of(
          BYTES_RULES_DESC,
          BytesRules.getDescriptor().findFieldByNumber(BytesRules.NOT_IN_FIELD_NUMBER),
          "bytes.not_in",
          null);

  private final RuleBase base;
  private final @Nullable ByteString constVal;
  private final @Nullable Long exactLen;
  private final @Nullable Long minLen;
  private final @Nullable Long maxLen;
  private final @Nullable Pattern pattern;
  private final @Nullable String patternStr;
  private final @Nullable ByteString prefix;
  private final @Nullable ByteString suffix;
  private final @Nullable ByteString contains;
  private final List<ByteString> inVals;
  private final List<ByteString> notInVals;
  private final @Nullable WellKnown wellKnown;

  private BytesRulesEvaluator(
      RuleBase base,
      @Nullable ByteString constVal,
      @Nullable Long exactLen,
      @Nullable Long minLen,
      @Nullable Long maxLen,
      @Nullable Pattern pattern,
      @Nullable String patternStr,
      @Nullable ByteString prefix,
      @Nullable ByteString suffix,
      @Nullable ByteString contains,
      List<ByteString> inVals,
      List<ByteString> notInVals,
      @Nullable WellKnown wellKnown) {
    this.base = base;
    this.constVal = constVal;
    this.exactLen = exactLen;
    this.minLen = minLen;
    this.maxLen = maxLen;
    this.pattern = pattern;
    this.patternStr = patternStr;
    this.prefix = prefix;
    this.suffix = suffix;
    this.contains = contains;
    this.inVals = inVals;
    this.notInVals = notInVals;
    this.wellKnown = wellKnown;
  }

  static @Nullable Evaluator tryBuild(RuleBase base, FieldRules.Builder rulesBuilder) {
    if (!rulesBuilder.hasBytes()) {
      return null;
    }
    BytesRules rules = rulesBuilder.getBytes();
    if (!rules.getUnknownFields().isEmpty()) {
      return null;
    }

    BytesRules.Builder bb = rules.toBuilder();
    boolean hasRule = false;

    WellKnown wellKnown = null;
    // Mirror Go's switch — earlier cases win. Setting ip=true takes precedence over
    // ipv4/ipv6/uuid if multiple are set; protovalidate considers that a misconfiguration but
    // we follow Go's order to keep behavior identical.
    if (rules.getIp()) {
      wellKnown = WellKnown.IP;
      bb.clearIp();
      hasRule = true;
    } else if (rules.getIpv4()) {
      wellKnown = WellKnown.IPV4;
      bb.clearIpv4();
      hasRule = true;
    } else if (rules.getIpv6()) {
      wellKnown = WellKnown.IPV6;
      bb.clearIpv6();
      hasRule = true;
    } else if (rules.getUuid()) {
      wellKnown = WellKnown.UUID;
      bb.clearUuid();
      hasRule = true;
    }

    ByteString constVal = null;
    if (rules.hasConst()) {
      constVal = rules.getConst();
      bb.clearConst();
      hasRule = true;
    }

    Long exactLen = null;
    if (rules.hasLen()) {
      exactLen = rules.getLen();
      bb.clearLen();
      hasRule = true;
    }

    Long minLen = null;
    if (rules.hasMinLen()) {
      minLen = rules.getMinLen();
      bb.clearMinLen();
      hasRule = true;
    }

    Long maxLen = null;
    if (rules.hasMaxLen()) {
      maxLen = rules.getMaxLen();
      bb.clearMaxLen();
      hasRule = true;
    }

    Pattern compiledPattern = null;
    String patternStr = null;
    if (rules.hasPattern()) {
      patternStr = rules.getPattern();
      try {
        compiledPattern = Pattern.compile(patternStr);
      } catch (PatternSyntaxException e) {
        // Bail to CEL — it produces the same compilation error.
        return null;
      }
      bb.clearPattern();
      hasRule = true;
    }

    ByteString prefix = null;
    if (rules.hasPrefix()) {
      prefix = rules.getPrefix();
      bb.clearPrefix();
      hasRule = true;
    }

    ByteString suffix = null;
    if (rules.hasSuffix()) {
      suffix = rules.getSuffix();
      bb.clearSuffix();
      hasRule = true;
    }

    ByteString contains = null;
    if (rules.hasContains()) {
      contains = rules.getContains();
      bb.clearContains();
      hasRule = true;
    }

    // Proto returns immutable views; we only read them.
    List<ByteString> inVals = rules.getInList();
    if (!inVals.isEmpty()) {
      bb.clearIn();
      hasRule = true;
    }

    List<ByteString> notInVals = rules.getNotInList();
    if (!notInVals.isEmpty()) {
      bb.clearNotIn();
      hasRule = true;
    }

    if (!hasRule) {
      return null;
    }
    rulesBuilder.setBytes(bb.build());
    return new BytesRulesEvaluator(
        base,
        constVal,
        exactLen,
        minLen,
        maxLen,
        compiledPattern,
        patternStr,
        prefix,
        suffix,
        contains,
        inVals,
        notInVals,
        wellKnown);
  }

  @Override
  public boolean tautology() {
    return false;
  }

  @Override
  public List<RuleViolation.Builder> evaluate(Value val, boolean failFast)
      throws ExecutionException {
    ByteString bytesVal = (ByteString) val.rawValue();
    long byteLen = bytesVal.size();
    List<RuleViolation.Builder> violations = null;

    if (constVal != null && !bytesVal.equals(constVal)) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  CONST_SITE, null, "must be " + hex(constVal), val, constVal));
      if (failFast) return base.done(violations);
    }

    if (exactLen != null && byteLen != exactLen) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  LEN_SITE, null, "must be " + exactLen + " bytes", val, exactLen));
      if (failFast) return base.done(violations);
    }

    if (minLen != null && byteLen < minLen) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  MIN_LEN_SITE, null, "must be at least " + minLen + " bytes", val, minLen));
      if (failFast) return base.done(violations);
    }

    if (maxLen != null && byteLen > maxLen) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  MAX_LEN_SITE, null, "must be at most " + maxLen + " bytes", val, maxLen));
      if (failFast) return base.done(violations);
    }

    if (pattern != null) {
      if (!bytesVal.isValidUtf8()) {
        // Match Go: surface this as an execution error rather than a violation. The conformance
        // suite expects pattern checks to fail loudly on non-UTF-8 input.
        throw new ExecutionException("must be valid UTF-8 to apply regexp");
      }
      if (!pattern.matches(bytesVal.toStringUtf8())) {
        violations =
            RuleBase.add(
                violations,
                NativeViolations.newViolation(
                    PATTERN_SITE,
                    null,
                    "must match regex pattern `" + patternStr + "`",
                    val,
                    patternStr));
        if (failFast) return base.done(violations);
      }
    }

    if (prefix != null && !bytesVal.startsWith(prefix)) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  PREFIX_SITE, null, "does not have prefix " + hex(prefix), val, prefix));
      if (failFast) return base.done(violations);
    }

    if (suffix != null && !bytesVal.endsWith(suffix)) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  SUFFIX_SITE, null, "does not have suffix " + hex(suffix), val, suffix));
      if (failFast) return base.done(violations);
    }

    if (contains != null && !containsBytes(bytesVal, contains)) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  CONTAINS_SITE, null, "does not contain " + hex(contains), val, contains));
      if (failFast) return base.done(violations);
    }

    if (!inVals.isEmpty() && !inVals.contains(bytesVal)) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  IN_SITE, null, "must be in list " + formatList(inVals), val, bytesVal));
      if (failFast) return base.done(violations);
    }

    if (!notInVals.isEmpty() && notInVals.contains(bytesVal)) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  NOT_IN_SITE,
                  null,
                  "must not be in list " + formatList(notInVals),
                  val,
                  bytesVal));
      if (failFast) return base.done(violations);
    }

    if (wellKnown != null) {
      RuleViolation.Builder wkViolation = evaluateWellKnown(bytesVal, val);
      if (wkViolation != null) {
        violations = RuleBase.add(violations, wkViolation);
        if (failFast) return base.done(violations);
      }
    }

    return base.done(violations);
  }

  private RuleViolation.@Nullable Builder evaluateWellKnown(ByteString bytesVal, Value val) {
    int size = bytesVal.size();
    WellKnown wk = wellKnown;
    if (wk == null) {
      return null;
    }
    if (size == 0) {
      // Rule value is the bool 'true' (the rule was enabled). Site has the rule id and message
      // pre-baked.
      return NativeViolations.newViolation(wk.emptySite, null, null, val, true);
    }
    if (wk.sizeIsValid(size)) {
      return null;
    }
    return NativeViolations.newViolation(wk.site, null, null, val, true);
  }

  /** {@code ByteString} doesn't have a {@code contains} method; implement it directly. */
  private static boolean containsBytes(ByteString haystack, ByteString needle) {
    int hLen = haystack.size();
    int nLen = needle.size();
    if (nLen == 0) {
      return true;
    }
    if (nLen > hLen) {
      return false;
    }
    outer:
    for (int i = 0; i <= hLen - nLen; i++) {
      for (int j = 0; j < nLen; j++) {
        if (haystack.byteAt(i + j) != needle.byteAt(j)) {
          continue outer;
        }
      }
      return true;
    }
    return false;
  }

  private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

  /** Lowercase hex encoding to match Go's {@code fmt.Sprintf("%x", ...)} for byte slices. */
  private static String hex(ByteString bs) {
    int len = bs.size();
    char[] out = new char[len * 2];
    for (int i = 0; i < len; i++) {
      int b = bs.byteAt(i) & 0xff;
      out[i * 2] = HEX_DIGITS[b >>> 4];
      out[i * 2 + 1] = HEX_DIGITS[b & 0xf];
    }
    return new String(out);
  }

  /**
   * Formats a list of bytes the way CEL does — each element rendered as its raw string (UTF-8).
   * Mirrors Go's {@code formatBytesList}.
   */
  private static String formatList(List<ByteString> vals) {
    return RuleBase.formatList(vals, ByteString::toStringUtf8);
  }

}
