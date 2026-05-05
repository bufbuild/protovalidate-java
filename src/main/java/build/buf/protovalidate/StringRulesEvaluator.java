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

package build.buf.protovalidate;

import build.buf.validate.FieldRules;
import build.buf.validate.KnownRegex;
import build.buf.validate.StringRules;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.re2j.Pattern;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Native evaluator for the standard string rules: scalar rules (`const`, `len`, `min_len`,
 * `max_len`, `len_bytes`, `min_bytes`, `max_bytes`, `pattern`, `prefix`, `suffix`, `contains`,
 * `not_contains`, `in`, `not_in`), well-known formats (`email`, `hostname`, `ip`, `ipv4`, `ipv6`,
 * `uri`, `uri_ref`, `address`, `uuid`, `tuuid`, `ulid`, `host_and_port`, the {@code _prefix} /
 * {@code _with_prefixlen} variants), and the {@code well_known_regex} oneof case (HTTP header
 * name/value with optional strict mode).
 *
 * <p>Mirrors {@code nativeStringEval} in protovalidate-go's {@code native_string.go}. Format
 * helpers from {@link CustomOverload} are reused so native and CEL paths share the same
 * email/hostname/IP/URI parsers.
 */
final class StringRulesEvaluator implements Evaluator {

  // --- Static descriptors and rule sites ---

  private static final FieldDescriptor STRING_RULES_DESC =
      FieldRules.getDescriptor().findFieldByNumber(FieldRules.STRING_FIELD_NUMBER);

  private static final RuleSite CONST_SITE = site(StringRules.CONST_FIELD_NUMBER, "string.const");
  private static final RuleSite LEN_SITE = site(StringRules.LEN_FIELD_NUMBER, "string.len");
  private static final RuleSite MIN_LEN_SITE =
      site(StringRules.MIN_LEN_FIELD_NUMBER, "string.min_len");
  private static final RuleSite MAX_LEN_SITE =
      site(StringRules.MAX_LEN_FIELD_NUMBER, "string.max_len");
  private static final RuleSite LEN_BYTES_SITE =
      site(StringRules.LEN_BYTES_FIELD_NUMBER, "string.len_bytes");
  private static final RuleSite MIN_BYTES_SITE =
      site(StringRules.MIN_BYTES_FIELD_NUMBER, "string.min_bytes");
  private static final RuleSite MAX_BYTES_SITE =
      site(StringRules.MAX_BYTES_FIELD_NUMBER, "string.max_bytes");
  private static final RuleSite PATTERN_SITE =
      site(StringRules.PATTERN_FIELD_NUMBER, "string.pattern");
  private static final RuleSite PREFIX_SITE =
      site(StringRules.PREFIX_FIELD_NUMBER, "string.prefix");
  private static final RuleSite SUFFIX_SITE =
      site(StringRules.SUFFIX_FIELD_NUMBER, "string.suffix");
  private static final RuleSite CONTAINS_SITE =
      site(StringRules.CONTAINS_FIELD_NUMBER, "string.contains");
  private static final RuleSite NOT_CONTAINS_SITE =
      site(StringRules.NOT_CONTAINS_FIELD_NUMBER, "string.not_contains");
  private static final RuleSite IN_SITE = site(StringRules.IN_FIELD_NUMBER, "string.in");
  private static final RuleSite NOT_IN_SITE =
      site(StringRules.NOT_IN_FIELD_NUMBER, "string.not_in");
  private static final FieldDescriptor WELL_KNOWN_REGEX_DESC =
      StringRules.getDescriptor().findFieldByNumber(StringRules.WELL_KNOWN_REGEX_FIELD_NUMBER);

  // well_known_regex sites: pre-built once. Header-name and header-value have a normal-failure
  // site and an empty-input site (header_name); pattern selection at evaluation time picks
  // between strict and loose matchers.
  private static final RuleSite HEADER_NAME_SITE =
      RuleSite.of(
          STRING_RULES_DESC,
          WELL_KNOWN_REGEX_DESC,
          "string.well_known_regex.header_name",
          "must be a valid HTTP header name");
  private static final RuleSite HEADER_NAME_EMPTY_SITE =
      RuleSite.of(
          STRING_RULES_DESC,
          WELL_KNOWN_REGEX_DESC,
          "string.well_known_regex.header_name_empty",
          "value is empty, which is not a valid HTTP header name");
  private static final RuleSite HEADER_VALUE_SITE =
      RuleSite.of(
          STRING_RULES_DESC,
          WELL_KNOWN_REGEX_DESC,
          "string.well_known_regex.header_value",
          "must be a valid HTTP header value");

  private static RuleSite site(int fieldNumber, String ruleId) {
    FieldDescriptor leaf = StringRules.getDescriptor().findFieldByNumber(fieldNumber);
    return RuleSite.of(STRING_RULES_DESC, leaf, ruleId, null);
  }

  // --- Static regexes (compile once) ---

  private static final Pattern UUID_REGEX =
      Pattern.compile(
          "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
  private static final Pattern TUUID_REGEX = Pattern.compile("^[0-9a-fA-F]{32}$");
  private static final Pattern ULID_REGEX =
      Pattern.compile("^[0-7][0-9A-HJKMNP-TV-Za-hjkmnp-tv-z]{25}$");
  private static final Pattern HEADER_NAME_REGEX =
      Pattern.compile("^:?[0-9a-zA-Z!#$%&\\\\'*+\\-.\\^_|~`]+$");
  private static final Pattern HEADER_VALUE_REGEX =
      Pattern.compile("^[^\\x00-\\x08\\x0A-\\x1F\\x7F]*$");
  private static final Pattern LOOSE_REGEX = Pattern.compile("^[^\\x00\\x0A\\x0D]+$");

  // --- Well-known string formats ---

  /**
   * Each constant carries the rule id, the main violation message, the empty-value variant message,
   * and the validation. Empty messages are stored verbatim from the proto spec rather than derived
   * by string substitution: {@code host_and_port}, for example, has a main message of {@code "must
   * be a valid host (hostname or IP address) and port pair"} but an empty message of {@code "value
   * is empty, which is not a valid host and port pair"} (without the parenthetical) — substring
   * derivation produced the wrong text.
   */
  @SuppressWarnings("ImmutableEnumChecker") // RuleSite is logically immutable; not annotated.
  enum WellKnownFormat {
    EMAIL(
        StringRules.EMAIL_FIELD_NUMBER,
        "email",
        "must be a valid email address",
        "value is empty, which is not a valid email address") {
      @Override
      boolean validate(String s) {
        return CustomOverload.isEmail(s);
      }
    },
    HOSTNAME(
        StringRules.HOSTNAME_FIELD_NUMBER,
        "hostname",
        "must be a valid hostname",
        "value is empty, which is not a valid hostname") {
      @Override
      boolean validate(String s) {
        return CustomOverload.isHostname(s);
      }
    },
    IP(
        StringRules.IP_FIELD_NUMBER,
        "ip",
        "must be a valid IP address",
        "value is empty, which is not a valid IP address") {
      @Override
      boolean validate(String s) {
        return CustomOverload.isIp(s, 0);
      }
    },
    IPV4(
        StringRules.IPV4_FIELD_NUMBER,
        "ipv4",
        "must be a valid IPv4 address",
        "value is empty, which is not a valid IPv4 address") {
      @Override
      boolean validate(String s) {
        return CustomOverload.isIp(s, 4);
      }
    },
    IPV6(
        StringRules.IPV6_FIELD_NUMBER,
        "ipv6",
        "must be a valid IPv6 address",
        "value is empty, which is not a valid IPv6 address") {
      @Override
      boolean validate(String s) {
        return CustomOverload.isIp(s, 6);
      }
    },
    URI(
        StringRules.URI_FIELD_NUMBER,
        "uri",
        "must be a valid URI",
        "value is empty, which is not a valid URI") {
      @Override
      boolean validate(String s) {
        return CustomOverload.isUri(s);
      }
    },
    URI_REF(StringRules.URI_REF_FIELD_NUMBER, "uri_ref", "must be a valid URI Reference", null) {
      @Override
      boolean validate(String s) {
        return CustomOverload.isUriRef(s);
      }

      @Override
      boolean checksEmpty() {
        return false;
      }
    },
    ADDRESS(
        StringRules.ADDRESS_FIELD_NUMBER,
        "address",
        "must be a valid hostname, or ip address",
        "value is empty, which is not a valid hostname, or ip address") {
      @Override
      boolean validate(String s) {
        return CustomOverload.isHostname(s) || CustomOverload.isIp(s, 0);
      }
    },
    UUID(
        StringRules.UUID_FIELD_NUMBER,
        "uuid",
        "must be a valid UUID",
        "value is empty, which is not a valid UUID") {
      @Override
      boolean validate(String s) {
        return UUID_REGEX.matches(s);
      }
    },
    TUUID(
        StringRules.TUUID_FIELD_NUMBER,
        "tuuid",
        "must be a valid trimmed UUID",
        "value is empty, which is not a valid trimmed UUID") {
      @Override
      boolean validate(String s) {
        return TUUID_REGEX.matches(s);
      }
    },
    IP_WITH_PREFIXLEN(
        StringRules.IP_WITH_PREFIXLEN_FIELD_NUMBER,
        "ip_with_prefixlen",
        "must be a valid IP prefix",
        "value is empty, which is not a valid IP prefix") {
      @Override
      boolean validate(String s) {
        return CustomOverload.isIpPrefix(s, 0, false);
      }
    },
    IPV4_WITH_PREFIXLEN(
        StringRules.IPV4_WITH_PREFIXLEN_FIELD_NUMBER,
        "ipv4_with_prefixlen",
        "must be a valid IPv4 address with prefix length",
        "value is empty, which is not a valid IPv4 address with prefix length") {
      @Override
      boolean validate(String s) {
        return CustomOverload.isIpPrefix(s, 4, false);
      }
    },
    IPV6_WITH_PREFIXLEN(
        StringRules.IPV6_WITH_PREFIXLEN_FIELD_NUMBER,
        "ipv6_with_prefixlen",
        "must be a valid IPv6 address with prefix length",
        "value is empty, which is not a valid IPv6 address with prefix length") {
      @Override
      boolean validate(String s) {
        return CustomOverload.isIpPrefix(s, 6, false);
      }
    },
    IP_PREFIX(
        StringRules.IP_PREFIX_FIELD_NUMBER,
        "ip_prefix",
        "must be a valid IP prefix",
        "value is empty, which is not a valid IP prefix") {
      @Override
      boolean validate(String s) {
        return CustomOverload.isIpPrefix(s, 0, true);
      }
    },
    IPV4_PREFIX(
        StringRules.IPV4_PREFIX_FIELD_NUMBER,
        "ipv4_prefix",
        "must be a valid IPv4 prefix",
        "value is empty, which is not a valid IPv4 prefix") {
      @Override
      boolean validate(String s) {
        return CustomOverload.isIpPrefix(s, 4, true);
      }
    },
    IPV6_PREFIX(
        StringRules.IPV6_PREFIX_FIELD_NUMBER,
        "ipv6_prefix",
        "must be a valid IPv6 prefix",
        "value is empty, which is not a valid IPv6 prefix") {
      @Override
      boolean validate(String s) {
        return CustomOverload.isIpPrefix(s, 6, true);
      }
    },
    HOST_AND_PORT(
        StringRules.HOST_AND_PORT_FIELD_NUMBER,
        "host_and_port",
        "must be a valid host (hostname or IP address) and port pair",
        "value is empty, which is not a valid host and port pair") {
      @Override
      boolean validate(String s) {
        return CustomOverload.isHostAndPort(s, true);
      }
    },
    ULID(
        StringRules.ULID_FIELD_NUMBER,
        "ulid",
        "must be a valid ULID",
        "value is empty, which is not a valid ULID") {
      @Override
      boolean validate(String s) {
        return ULID_REGEX.matches(s);
      }
    };

    final FieldDescriptor field;
    final RuleSite site;
    final @Nullable RuleSite emptySite;

    WellKnownFormat(
        int fieldNumber, String ruleSuffix, String message, @Nullable String emptyMessage) {
      FieldDescriptor leaf = StringRules.getDescriptor().findFieldByNumber(fieldNumber);
      this.field = leaf;
      this.site = RuleSite.of(STRING_RULES_DESC, leaf, "string." + ruleSuffix, message);
      this.emptySite =
          emptyMessage == null
              ? null
              : RuleSite.of(
                  STRING_RULES_DESC, leaf, "string." + ruleSuffix + "_empty", emptyMessage);
    }

    /** Whether this format reports an empty-value violation distinctly from the format failure. */
    boolean checksEmpty() {
      return true;
    }

    abstract boolean validate(String s);
  }

  // --- Fields ---

  private final RuleBase base;
  private final @Nullable String constVal;
  private final @Nullable Long exactLen;
  private final @Nullable Long minLen;
  private final @Nullable Long maxLen;
  private final @Nullable Long exactBytes;
  private final @Nullable Long minBytes;
  private final @Nullable Long maxBytes;
  private final @Nullable Pattern pattern;
  private final @Nullable String patternStr;
  private final @Nullable String prefix;
  private final @Nullable String suffix;
  private final @Nullable String contains;
  private final @Nullable String notContains;
  private final List<String> inVals;
  private final List<String> notInVals;
  private final @Nullable WellKnownFormat wellKnown;
  private final KnownRegex knownRegex;
  private final boolean knownRegexStrict;

  private StringRulesEvaluator(
      RuleBase base,
      @Nullable String constVal,
      @Nullable Long exactLen,
      @Nullable Long minLen,
      @Nullable Long maxLen,
      @Nullable Long exactBytes,
      @Nullable Long minBytes,
      @Nullable Long maxBytes,
      @Nullable Pattern pattern,
      @Nullable String patternStr,
      @Nullable String prefix,
      @Nullable String suffix,
      @Nullable String contains,
      @Nullable String notContains,
      List<String> inVals,
      List<String> notInVals,
      @Nullable WellKnownFormat wellKnown,
      KnownRegex knownRegex,
      boolean knownRegexStrict) {
    this.base = base;
    this.constVal = constVal;
    this.exactLen = exactLen;
    this.minLen = minLen;
    this.maxLen = maxLen;
    this.exactBytes = exactBytes;
    this.minBytes = minBytes;
    this.maxBytes = maxBytes;
    this.pattern = pattern;
    this.patternStr = patternStr;
    this.prefix = prefix;
    this.suffix = suffix;
    this.contains = contains;
    this.notContains = notContains;
    this.inVals = inVals;
    this.notInVals = notInVals;
    this.wellKnown = wellKnown;
    this.knownRegex = knownRegex;
    this.knownRegexStrict = knownRegexStrict;
  }

  static @Nullable Evaluator tryBuild(RuleBase base, FieldRules.Builder rulesBuilder) {
    if (!rulesBuilder.hasString()) {
      return null;
    }
    StringRules rules = rulesBuilder.getString();
    if (!rules.getUnknownFields().isEmpty()) {
      return null;
    }

    StringRules.Builder sb = rules.toBuilder();
    boolean hasRule = false;

    // Well-known oneof: at most one of the format fields, OR well_known_regex, can be set.
    WellKnownFormat wellKnown = null;
    KnownRegex knownRegex = KnownRegex.KNOWN_REGEX_UNSPECIFIED;
    boolean knownRegexStrict = false;
    for (WellKnownFormat fmt : WellKnownFormat.values()) {
      if (rules.hasField(fmt.field)) {
        boolean enabled = (Boolean) rules.getField(fmt.field);
        if (enabled) {
          wellKnown = fmt;
          sb.clearField(fmt.field);
          hasRule = true;
        }
        break;
      }
    }
    if (wellKnown == null && rules.hasWellKnownRegex()) {
      knownRegex = rules.getWellKnownRegex();
      // strict defaults to true when not explicitly set.
      knownRegexStrict = !rules.hasStrict() || rules.getStrict();
      if (knownRegex != KnownRegex.KNOWN_REGEX_UNSPECIFIED) {
        sb.clearWellKnownRegex();
        if (rules.hasStrict()) {
          sb.clearStrict();
        }
        hasRule = true;
      }
    }

    String constVal = null;
    if (rules.hasConst()) {
      constVal = rules.getConst();
      sb.clearConst();
      hasRule = true;
    }
    Long exactLen = null;
    if (rules.hasLen()) {
      exactLen = rules.getLen();
      sb.clearLen();
      hasRule = true;
    }
    Long minLen = null;
    if (rules.hasMinLen()) {
      minLen = rules.getMinLen();
      sb.clearMinLen();
      hasRule = true;
    }
    Long maxLen = null;
    if (rules.hasMaxLen()) {
      maxLen = rules.getMaxLen();
      sb.clearMaxLen();
      hasRule = true;
    }
    Long exactBytes = null;
    if (rules.hasLenBytes()) {
      exactBytes = rules.getLenBytes();
      sb.clearLenBytes();
      hasRule = true;
    }
    Long minBytes = null;
    if (rules.hasMinBytes()) {
      minBytes = rules.getMinBytes();
      sb.clearMinBytes();
      hasRule = true;
    }
    Long maxBytes = null;
    if (rules.hasMaxBytes()) {
      maxBytes = rules.getMaxBytes();
      sb.clearMaxBytes();
      hasRule = true;
    }

    Pattern compiledPattern = null;
    String patternStr = null;
    if (rules.hasPattern()) {
      patternStr = rules.getPattern();
      try {
        compiledPattern = Pattern.compile(patternStr);
      } catch (com.google.re2j.PatternSyntaxException e) {
        return null; // bail to CEL — same compilation error
      }
      sb.clearPattern();
      hasRule = true;
    }

    String prefix = null;
    if (rules.hasPrefix()) {
      prefix = rules.getPrefix();
      sb.clearPrefix();
      hasRule = true;
    }
    String suffix = null;
    if (rules.hasSuffix()) {
      suffix = rules.getSuffix();
      sb.clearSuffix();
      hasRule = true;
    }
    String contains = null;
    if (rules.hasContains()) {
      contains = rules.getContains();
      sb.clearContains();
      hasRule = true;
    }
    String notContains = null;
    if (rules.hasNotContains()) {
      notContains = rules.getNotContains();
      sb.clearNotContains();
      hasRule = true;
    }

    // getInList()/getNotInList() return immutable views from the proto runtime; we only read
    // them, so no defensive copy is needed.
    List<String> inVals = rules.getInList();
    if (!inVals.isEmpty()) {
      sb.clearIn();
      hasRule = true;
    }

    List<String> notInVals = rules.getNotInList();
    if (!notInVals.isEmpty()) {
      sb.clearNotIn();
      hasRule = true;
    }

    if (!hasRule) {
      return null;
    }
    rulesBuilder.setString(sb.build());
    return new StringRulesEvaluator(
        base,
        constVal,
        exactLen,
        minLen,
        maxLen,
        exactBytes,
        minBytes,
        maxBytes,
        compiledPattern,
        patternStr,
        prefix,
        suffix,
        contains,
        notContains,
        inVals,
        notInVals,
        wellKnown,
        knownRegex,
        knownRegexStrict);
  }

  @Override
  public boolean tautology() {
    return false;
  }

  @Override
  public List<RuleViolation.Builder> evaluate(Value val, boolean failFast) {
    String strVal = (String) val.rawValue();
    List<RuleViolation.Builder> violations = null;

    if (exactLen != null || minLen != null || maxLen != null) {
      long runeCount = strVal.codePointCount(0, strVal.length());
      violations = applyLength(violations, val, runeCount, failFast);
      if (failFast && violations != null) {
        return base.done(violations);
      }
    }

    if (exactBytes != null || minBytes != null || maxBytes != null) {
      long byteCount = utf8ByteLength(strVal);
      violations = applyByteLength(violations, val, byteCount, failFast);
      if (failFast && violations != null) {
        return base.done(violations);
      }
    }

    if (constVal != null && !strVal.equals(constVal)) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  CONST_SITE, null, "must equal `" + constVal + "`", val, constVal));
      if (failFast) return base.done(violations);
    }

    if (pattern != null && !pattern.matches(strVal)) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  PATTERN_SITE,
                  null,
                  "does not match regex pattern `" + patternStr + "`",
                  val,
                  patternStr));
      if (failFast) return base.done(violations);
    }

    if (prefix != null && !strVal.startsWith(prefix)) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  PREFIX_SITE, null, "does not have prefix `" + prefix + "`", val, prefix));
      if (failFast) return base.done(violations);
    }

    if (suffix != null && !strVal.endsWith(suffix)) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  SUFFIX_SITE, null, "does not have suffix `" + suffix + "`", val, suffix));
      if (failFast) return base.done(violations);
    }

    if (contains != null && !strVal.contains(contains)) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  CONTAINS_SITE,
                  null,
                  "does not contain substring `" + contains + "`",
                  val,
                  contains));
      if (failFast) return base.done(violations);
    }

    if (notContains != null && strVal.contains(notContains)) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  NOT_CONTAINS_SITE,
                  null,
                  "contains substring `" + notContains + "`",
                  val,
                  notContains));
      if (failFast) return base.done(violations);
    }

    if (!inVals.isEmpty() && !inVals.contains(strVal)) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  IN_SITE, null, "must be in list " + RuleBase.formatList(inVals), val, strVal));
      if (failFast) return base.done(violations);
    }

    if (!notInVals.isEmpty() && notInVals.contains(strVal)) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  NOT_IN_SITE,
                  null,
                  "must not be in list " + RuleBase.formatList(notInVals),
                  val,
                  strVal));
      if (failFast) return base.done(violations);
    }

    if (wellKnown != null) {
      RuleViolation.Builder wkv = checkWellKnown(strVal, val);
      if (wkv != null) {
        violations = RuleBase.add(violations, wkv);
        if (failFast) return base.done(violations);
      }
    } else if (knownRegex != KnownRegex.KNOWN_REGEX_UNSPECIFIED) {
      RuleViolation.Builder krv = checkKnownRegex(strVal, val);
      if (krv != null) {
        violations = RuleBase.add(violations, krv);
        if (failFast) return base.done(violations);
      }
    }

    return base.done(violations);
  }

  // --- Length checks ---

  private @Nullable List<RuleViolation.Builder> applyLength(
      @Nullable List<RuleViolation.Builder> violations,
      Value val,
      long runeCount,
      boolean failFast) {
    if (exactLen != null && runeCount != exactLen) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  LEN_SITE, null, "must be " + exactLen + " characters", val, exactLen));
      if (failFast) return violations;
    }
    if (minLen != null && runeCount < minLen) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  MIN_LEN_SITE, null, "must be at least " + minLen + " characters", val, minLen));
      if (failFast) return violations;
    }
    if (maxLen != null && runeCount > maxLen) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  MAX_LEN_SITE, null, "must be at most " + maxLen + " characters", val, maxLen));
      if (failFast) return violations;
    }
    return violations;
  }

  private @Nullable List<RuleViolation.Builder> applyByteLength(
      @Nullable List<RuleViolation.Builder> violations,
      Value val,
      long byteCount,
      boolean failFast) {
    if (exactBytes != null && byteCount != exactBytes) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  LEN_BYTES_SITE, null, "must be " + exactBytes + " bytes", val, exactBytes));
      if (failFast) return violations;
    }
    if (minBytes != null && byteCount < minBytes) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  MIN_BYTES_SITE, null, "must be at least " + minBytes + " bytes", val, minBytes));
      if (failFast) return violations;
    }
    if (maxBytes != null && byteCount > maxBytes) {
      violations =
          RuleBase.add(
              violations,
              NativeViolations.newViolation(
                  MAX_BYTES_SITE, null, "must be at most " + maxBytes + " bytes", val, maxBytes));
      if (failFast) return violations;
    }
    return violations;
  }

  // --- Well-known format check ---

  private RuleViolation.@Nullable Builder checkWellKnown(String strVal, Value val) {
    WellKnownFormat fmt = wellKnown;
    if (fmt == null) {
      return null;
    }
    if (fmt.checksEmpty() && strVal.isEmpty()) {
      // checksEmpty() returning true implies emptySite is non-null (the WellKnownFormat
      // constructor's contract).
      RuleSite emptySite = Objects.requireNonNull(fmt.emptySite);
      return NativeViolations.newViolation(emptySite, null, null, val, true);
    }
    if (fmt.validate(strVal)) {
      return null;
    }
    return NativeViolations.newViolation(fmt.site, null, null, val, true);
  }

  private RuleViolation.@Nullable Builder checkKnownRegex(String strVal, Value val) {
    Pattern matcher;
    RuleSite site;
    switch (knownRegex) {
      case KNOWN_REGEX_HTTP_HEADER_NAME:
        if (strVal.isEmpty()) {
          return NativeViolations.newViolation(
              HEADER_NAME_EMPTY_SITE, null, null, val, knownRegex.getNumber());
        }
        matcher = HEADER_NAME_REGEX;
        site = HEADER_NAME_SITE;
        break;
      case KNOWN_REGEX_HTTP_HEADER_VALUE:
        matcher = HEADER_VALUE_REGEX;
        site = HEADER_VALUE_SITE;
        break;
      default:
        return null;
    }
    if (!knownRegexStrict) {
      matcher = LOOSE_REGEX;
    }
    if (!matcher.matches(strVal)) {
      return NativeViolations.newViolation(site, null, null, val, knownRegex.getNumber());
    }
    return null;
  }

  // --- Helpers ---

  /**
   * Returns the number of bytes in the UTF-8 encoding of {@code s} without allocating the byte
   * array. Counts each code point's encoded byte length: 1 for U+0000–U+007F, 2 for U+0080–U+07FF,
   * 3 for U+0800–U+FFFF (excluding the surrogate range, which is consumed as a pair), 4 for
   * supplementary characters (U+10000–U+10FFFF).
   */
  private static long utf8ByteLength(String s) {
    long count = 0;
    int i = 0;
    int len = s.length();
    while (i < len) {
      char c = s.charAt(i);
      if (c < 0x80) {
        count += 1;
        i += 1;
      } else if (c < 0x800) {
        count += 2;
        i += 1;
      } else if (Character.isHighSurrogate(c)
          && i + 1 < len
          && Character.isLowSurrogate(s.charAt(i + 1))) {
        count += 4;
        i += 2;
      } else {
        count += 3;
        i += 1;
      }
    }
    return count;
  }
}
