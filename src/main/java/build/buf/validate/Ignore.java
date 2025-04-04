// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: buf/validate/validate.proto
// Protobuf Java Version: 4.30.1

package build.buf.validate;

/**
 * <pre>
 * Specifies how FieldConstraints.ignore behaves. See the documentation for
 * FieldConstraints.required for definitions of "populated" and "nullable".
 * </pre>
 *
 * Protobuf enum {@code buf.validate.Ignore}
 */
public enum Ignore
    implements com.google.protobuf.ProtocolMessageEnum {
  /**
   * <pre>
   * Validation is only skipped if it's an unpopulated nullable fields.
   *
   * ```proto
   * syntax="proto3";
   *
   * message Request {
   * // The uri rule applies to any value, including the empty string.
   * string foo = 1 [
   * (buf.validate.field).string.uri = true
   * ];
   *
   * // The uri rule only applies if the field is set, including if it's
   * // set to the empty string.
   * optional string bar = 2 [
   * (buf.validate.field).string.uri = true
   * ];
   *
   * // The min_items rule always applies, even if the list is empty.
   * repeated string baz = 3 [
   * (buf.validate.field).repeated.min_items = 3
   * ];
   *
   * // The custom CEL rule applies only if the field is set, including if
   * // it's the "zero" value of that message.
   * SomeMessage quux = 4 [
   * (buf.validate.field).cel = {/&#42; ... *&#47;}
   * ];
   * }
   * ```
   * </pre>
   *
   * <code>IGNORE_UNSPECIFIED = 0;</code>
   */
  IGNORE_UNSPECIFIED(0),
  /**
   * <pre>
   * Validation is skipped if the field is unpopulated. This rule is redundant
   * if the field is already nullable.
   *
   * ```proto
   * syntax="proto3
   *
   * message Request {
   * // The uri rule applies only if the value is not the empty string.
   * string foo = 1 [
   * (buf.validate.field).string.uri = true,
   * (buf.validate.field).ignore = IGNORE_IF_UNPOPULATED
   * ];
   *
   * // IGNORE_IF_UNPOPULATED is equivalent to IGNORE_UNSPECIFIED in this
   * // case: the uri rule only applies if the field is set, including if
   * // it's set to the empty string.
   * optional string bar = 2 [
   * (buf.validate.field).string.uri = true,
   * (buf.validate.field).ignore = IGNORE_IF_UNPOPULATED
   * ];
   *
   * // The min_items rule only applies if the list has at least one item.
   * repeated string baz = 3 [
   * (buf.validate.field).repeated.min_items = 3,
   * (buf.validate.field).ignore = IGNORE_IF_UNPOPULATED
   * ];
   *
   * // IGNORE_IF_UNPOPULATED is equivalent to IGNORE_UNSPECIFIED in this
   * // case: the custom CEL rule applies only if the field is set, including
   * // if it's the "zero" value of that message.
   * SomeMessage quux = 4 [
   * (buf.validate.field).cel = {/&#42; ... *&#47;},
   * (buf.validate.field).ignore = IGNORE_IF_UNPOPULATED
   * ];
   * }
   * ```
   * </pre>
   *
   * <code>IGNORE_IF_UNPOPULATED = 1;</code>
   */
  IGNORE_IF_UNPOPULATED(1),
  /**
   * <pre>
   * Validation is skipped if the field is unpopulated or if it is a nullable
   * field populated with its default value. This is typically the zero or
   * empty value, but proto2 scalars support custom defaults. For messages, the
   * default is a non-null message with all its fields unpopulated.
   *
   * ```proto
   * syntax="proto3
   *
   * message Request {
   * // IGNORE_IF_DEFAULT_VALUE is equivalent to IGNORE_IF_UNPOPULATED in
   * // this case; the uri rule applies only if the value is not the empty
   * // string.
   * string foo = 1 [
   * (buf.validate.field).string.uri = true,
   * (buf.validate.field).ignore = IGNORE_IF_DEFAULT_VALUE
   * ];
   *
   * // The uri rule only applies if the field is set to a value other than
   * // the empty string.
   * optional string bar = 2 [
   * (buf.validate.field).string.uri = true,
   * (buf.validate.field).ignore = IGNORE_IF_DEFAULT_VALUE
   * ];
   *
   * // IGNORE_IF_DEFAULT_VALUE is equivalent to IGNORE_IF_UNPOPULATED in
   * // this case; the min_items rule only applies if the list has at least
   * // one item.
   * repeated string baz = 3 [
   * (buf.validate.field).repeated.min_items = 3,
   * (buf.validate.field).ignore = IGNORE_IF_DEFAULT_VALUE
   * ];
   *
   * // The custom CEL rule only applies if the field is set to a value other
   * // than an empty message (i.e., fields are unpopulated).
   * SomeMessage quux = 4 [
   * (buf.validate.field).cel = {/&#42; ... *&#47;},
   * (buf.validate.field).ignore = IGNORE_IF_DEFAULT_VALUE
   * ];
   * }
   * ```
   *
   * This rule is affected by proto2 custom default values:
   *
   * ```proto
   * syntax="proto2";
   *
   * message Request {
   * // The gt rule only applies if the field is set and it's value is not
   * the default (i.e., not -42). The rule even applies if the field is set
   * to zero since the default value differs.
   * optional int32 value = 1 [
   * default = -42,
   * (buf.validate.field).int32.gt = 0,
   * (buf.validate.field).ignore = IGNORE_IF_DEFAULT_VALUE
   * ];
   * }
   * </pre>
   *
   * <code>IGNORE_IF_DEFAULT_VALUE = 2;</code>
   */
  IGNORE_IF_DEFAULT_VALUE(2),
  /**
   * <pre>
   * The validation rules of this field will be skipped and not evaluated. This
   * is useful for situations that necessitate turning off the rules of a field
   * containing a message that may not make sense in the current context, or to
   * temporarily disable constraints during development.
   *
   * ```proto
   * message MyMessage {
   * // The field's rules will always be ignored, including any validation's
   * // on value's fields.
   * MyOtherMessage value = 1 [
   * (buf.validate.field).ignore = IGNORE_ALWAYS];
   * }
   * ```
   * </pre>
   *
   * <code>IGNORE_ALWAYS = 3;</code>
   */
  IGNORE_ALWAYS(3),
  ;

  static {
    com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(
      com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,
      /* major= */ 4,
      /* minor= */ 30,
      /* patch= */ 1,
      /* suffix= */ "",
      Ignore.class.getName());
  }
  /**
   * <pre>
   * Validation is only skipped if it's an unpopulated nullable fields.
   *
   * ```proto
   * syntax="proto3";
   *
   * message Request {
   * // The uri rule applies to any value, including the empty string.
   * string foo = 1 [
   * (buf.validate.field).string.uri = true
   * ];
   *
   * // The uri rule only applies if the field is set, including if it's
   * // set to the empty string.
   * optional string bar = 2 [
   * (buf.validate.field).string.uri = true
   * ];
   *
   * // The min_items rule always applies, even if the list is empty.
   * repeated string baz = 3 [
   * (buf.validate.field).repeated.min_items = 3
   * ];
   *
   * // The custom CEL rule applies only if the field is set, including if
   * // it's the "zero" value of that message.
   * SomeMessage quux = 4 [
   * (buf.validate.field).cel = {/&#42; ... *&#47;}
   * ];
   * }
   * ```
   * </pre>
   *
   * <code>IGNORE_UNSPECIFIED = 0;</code>
   */
  public static final int IGNORE_UNSPECIFIED_VALUE = 0;
  /**
   * <pre>
   * Validation is skipped if the field is unpopulated. This rule is redundant
   * if the field is already nullable.
   *
   * ```proto
   * syntax="proto3
   *
   * message Request {
   * // The uri rule applies only if the value is not the empty string.
   * string foo = 1 [
   * (buf.validate.field).string.uri = true,
   * (buf.validate.field).ignore = IGNORE_IF_UNPOPULATED
   * ];
   *
   * // IGNORE_IF_UNPOPULATED is equivalent to IGNORE_UNSPECIFIED in this
   * // case: the uri rule only applies if the field is set, including if
   * // it's set to the empty string.
   * optional string bar = 2 [
   * (buf.validate.field).string.uri = true,
   * (buf.validate.field).ignore = IGNORE_IF_UNPOPULATED
   * ];
   *
   * // The min_items rule only applies if the list has at least one item.
   * repeated string baz = 3 [
   * (buf.validate.field).repeated.min_items = 3,
   * (buf.validate.field).ignore = IGNORE_IF_UNPOPULATED
   * ];
   *
   * // IGNORE_IF_UNPOPULATED is equivalent to IGNORE_UNSPECIFIED in this
   * // case: the custom CEL rule applies only if the field is set, including
   * // if it's the "zero" value of that message.
   * SomeMessage quux = 4 [
   * (buf.validate.field).cel = {/&#42; ... *&#47;},
   * (buf.validate.field).ignore = IGNORE_IF_UNPOPULATED
   * ];
   * }
   * ```
   * </pre>
   *
   * <code>IGNORE_IF_UNPOPULATED = 1;</code>
   */
  public static final int IGNORE_IF_UNPOPULATED_VALUE = 1;
  /**
   * <pre>
   * Validation is skipped if the field is unpopulated or if it is a nullable
   * field populated with its default value. This is typically the zero or
   * empty value, but proto2 scalars support custom defaults. For messages, the
   * default is a non-null message with all its fields unpopulated.
   *
   * ```proto
   * syntax="proto3
   *
   * message Request {
   * // IGNORE_IF_DEFAULT_VALUE is equivalent to IGNORE_IF_UNPOPULATED in
   * // this case; the uri rule applies only if the value is not the empty
   * // string.
   * string foo = 1 [
   * (buf.validate.field).string.uri = true,
   * (buf.validate.field).ignore = IGNORE_IF_DEFAULT_VALUE
   * ];
   *
   * // The uri rule only applies if the field is set to a value other than
   * // the empty string.
   * optional string bar = 2 [
   * (buf.validate.field).string.uri = true,
   * (buf.validate.field).ignore = IGNORE_IF_DEFAULT_VALUE
   * ];
   *
   * // IGNORE_IF_DEFAULT_VALUE is equivalent to IGNORE_IF_UNPOPULATED in
   * // this case; the min_items rule only applies if the list has at least
   * // one item.
   * repeated string baz = 3 [
   * (buf.validate.field).repeated.min_items = 3,
   * (buf.validate.field).ignore = IGNORE_IF_DEFAULT_VALUE
   * ];
   *
   * // The custom CEL rule only applies if the field is set to a value other
   * // than an empty message (i.e., fields are unpopulated).
   * SomeMessage quux = 4 [
   * (buf.validate.field).cel = {/&#42; ... *&#47;},
   * (buf.validate.field).ignore = IGNORE_IF_DEFAULT_VALUE
   * ];
   * }
   * ```
   *
   * This rule is affected by proto2 custom default values:
   *
   * ```proto
   * syntax="proto2";
   *
   * message Request {
   * // The gt rule only applies if the field is set and it's value is not
   * the default (i.e., not -42). The rule even applies if the field is set
   * to zero since the default value differs.
   * optional int32 value = 1 [
   * default = -42,
   * (buf.validate.field).int32.gt = 0,
   * (buf.validate.field).ignore = IGNORE_IF_DEFAULT_VALUE
   * ];
   * }
   * </pre>
   *
   * <code>IGNORE_IF_DEFAULT_VALUE = 2;</code>
   */
  public static final int IGNORE_IF_DEFAULT_VALUE_VALUE = 2;
  /**
   * <pre>
   * The validation rules of this field will be skipped and not evaluated. This
   * is useful for situations that necessitate turning off the rules of a field
   * containing a message that may not make sense in the current context, or to
   * temporarily disable constraints during development.
   *
   * ```proto
   * message MyMessage {
   * // The field's rules will always be ignored, including any validation's
   * // on value's fields.
   * MyOtherMessage value = 1 [
   * (buf.validate.field).ignore = IGNORE_ALWAYS];
   * }
   * ```
   * </pre>
   *
   * <code>IGNORE_ALWAYS = 3;</code>
   */
  public static final int IGNORE_ALWAYS_VALUE = 3;


  public final int getNumber() {
    return value;
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   * @deprecated Use {@link #forNumber(int)} instead.
   */
  @java.lang.Deprecated
  public static Ignore valueOf(int value) {
    return forNumber(value);
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   */
  public static Ignore forNumber(int value) {
    switch (value) {
      case 0: return IGNORE_UNSPECIFIED;
      case 1: return IGNORE_IF_UNPOPULATED;
      case 2: return IGNORE_IF_DEFAULT_VALUE;
      case 3: return IGNORE_ALWAYS;
      default: return null;
    }
  }

  public static com.google.protobuf.Internal.EnumLiteMap<Ignore>
      internalGetValueMap() {
    return internalValueMap;
  }
  private static final com.google.protobuf.Internal.EnumLiteMap<
      Ignore> internalValueMap =
        new com.google.protobuf.Internal.EnumLiteMap<Ignore>() {
          public Ignore findValueByNumber(int number) {
            return Ignore.forNumber(number);
          }
        };

  public final com.google.protobuf.Descriptors.EnumValueDescriptor
      getValueDescriptor() {
    return getDescriptor().getValues().get(ordinal());
  }
  public final com.google.protobuf.Descriptors.EnumDescriptor
      getDescriptorForType() {
    return getDescriptor();
  }
  public static final com.google.protobuf.Descriptors.EnumDescriptor
      getDescriptor() {
    return build.buf.validate.ValidateProto.getDescriptor().getEnumTypes().get(0);
  }

  private static final Ignore[] VALUES = values();

  public static Ignore valueOf(
      com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
    if (desc.getType() != getDescriptor()) {
      throw new java.lang.IllegalArgumentException(
        "EnumValueDescriptor is not for this type.");
    }
    return VALUES[desc.getIndex()];
  }

  private final int value;

  private Ignore(int value) {
    this.value = value;
  }

  // @@protoc_insertion_point(enum_scope:buf.validate.Ignore)
}

