// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: buf/validate/conformance/cases/predefined_rules_proto3.proto
// Protobuf Java Version: 4.30.1

package build.buf.validate.conformance.cases;

/**
 * Protobuf type {@code buf.validate.conformance.cases.PredefinedEnumRuleProto3}
 */
public final class PredefinedEnumRuleProto3 extends
    com.google.protobuf.GeneratedMessage implements
    // @@protoc_insertion_point(message_implements:buf.validate.conformance.cases.PredefinedEnumRuleProto3)
    PredefinedEnumRuleProto3OrBuilder {
private static final long serialVersionUID = 0L;
  static {
    com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(
      com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,
      /* major= */ 4,
      /* minor= */ 30,
      /* patch= */ 1,
      /* suffix= */ "",
      PredefinedEnumRuleProto3.class.getName());
  }
  // Use PredefinedEnumRuleProto3.newBuilder() to construct.
  private PredefinedEnumRuleProto3(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
    super(builder);
  }
  private PredefinedEnumRuleProto3() {
    val_ = 0;
  }

  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return build.buf.validate.conformance.cases.PredefinedRulesProto3Proto.internal_static_buf_validate_conformance_cases_PredefinedEnumRuleProto3_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return build.buf.validate.conformance.cases.PredefinedRulesProto3Proto.internal_static_buf_validate_conformance_cases_PredefinedEnumRuleProto3_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            build.buf.validate.conformance.cases.PredefinedEnumRuleProto3.class, build.buf.validate.conformance.cases.PredefinedEnumRuleProto3.Builder.class);
  }

  /**
   * Protobuf enum {@code buf.validate.conformance.cases.PredefinedEnumRuleProto3.EnumProto3}
   */
  public enum EnumProto3
      implements com.google.protobuf.ProtocolMessageEnum {
    /**
     * <code>ENUM_PROTO3_ZERO_UNSPECIFIED = 0;</code>
     */
    ENUM_PROTO3_ZERO_UNSPECIFIED(0),
    /**
     * <code>ENUM_PROTO3_ONE = 1;</code>
     */
    ENUM_PROTO3_ONE(1),
    UNRECOGNIZED(-1),
    ;

    static {
      com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(
        com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,
        /* major= */ 4,
        /* minor= */ 30,
        /* patch= */ 1,
        /* suffix= */ "",
        EnumProto3.class.getName());
    }
    /**
     * <code>ENUM_PROTO3_ZERO_UNSPECIFIED = 0;</code>
     */
    public static final int ENUM_PROTO3_ZERO_UNSPECIFIED_VALUE = 0;
    /**
     * <code>ENUM_PROTO3_ONE = 1;</code>
     */
    public static final int ENUM_PROTO3_ONE_VALUE = 1;


    public final int getNumber() {
      if (this == UNRECOGNIZED) {
        throw new java.lang.IllegalArgumentException(
            "Can't get the number of an unknown enum value.");
      }
      return value;
    }

    /**
     * @param value The numeric wire value of the corresponding enum entry.
     * @return The enum associated with the given numeric wire value.
     * @deprecated Use {@link #forNumber(int)} instead.
     */
    @java.lang.Deprecated
    public static EnumProto3 valueOf(int value) {
      return forNumber(value);
    }

    /**
     * @param value The numeric wire value of the corresponding enum entry.
     * @return The enum associated with the given numeric wire value.
     */
    public static EnumProto3 forNumber(int value) {
      switch (value) {
        case 0: return ENUM_PROTO3_ZERO_UNSPECIFIED;
        case 1: return ENUM_PROTO3_ONE;
        default: return null;
      }
    }

    public static com.google.protobuf.Internal.EnumLiteMap<EnumProto3>
        internalGetValueMap() {
      return internalValueMap;
    }
    private static final com.google.protobuf.Internal.EnumLiteMap<
        EnumProto3> internalValueMap =
          new com.google.protobuf.Internal.EnumLiteMap<EnumProto3>() {
            public EnumProto3 findValueByNumber(int number) {
              return EnumProto3.forNumber(number);
            }
          };

    public final com.google.protobuf.Descriptors.EnumValueDescriptor
        getValueDescriptor() {
      if (this == UNRECOGNIZED) {
        throw new java.lang.IllegalStateException(
            "Can't get the descriptor of an unrecognized enum value.");
      }
      return getDescriptor().getValues().get(ordinal());
    }
    public final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptorForType() {
      return getDescriptor();
    }
    public static final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptor() {
      return build.buf.validate.conformance.cases.PredefinedEnumRuleProto3.getDescriptor().getEnumTypes().get(0);
    }

    private static final EnumProto3[] VALUES = values();

    public static EnumProto3 valueOf(
        com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
      if (desc.getType() != getDescriptor()) {
        throw new java.lang.IllegalArgumentException(
          "EnumValueDescriptor is not for this type.");
      }
      if (desc.getIndex() == -1) {
        return UNRECOGNIZED;
      }
      return VALUES[desc.getIndex()];
    }

    private final int value;

    private EnumProto3(int value) {
      this.value = value;
    }

    // @@protoc_insertion_point(enum_scope:buf.validate.conformance.cases.PredefinedEnumRuleProto3.EnumProto3)
  }

  public static final int VAL_FIELD_NUMBER = 1;
  private int val_ = 0;
  /**
   * <code>.buf.validate.conformance.cases.PredefinedEnumRuleProto3.EnumProto3 val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   * @return The enum numeric value on the wire for val.
   */
  @java.lang.Override public int getValValue() {
    return val_;
  }
  /**
   * <code>.buf.validate.conformance.cases.PredefinedEnumRuleProto3.EnumProto3 val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   * @return The val.
   */
  @java.lang.Override public build.buf.validate.conformance.cases.PredefinedEnumRuleProto3.EnumProto3 getVal() {
    build.buf.validate.conformance.cases.PredefinedEnumRuleProto3.EnumProto3 result = build.buf.validate.conformance.cases.PredefinedEnumRuleProto3.EnumProto3.forNumber(val_);
    return result == null ? build.buf.validate.conformance.cases.PredefinedEnumRuleProto3.EnumProto3.UNRECOGNIZED : result;
  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (val_ != build.buf.validate.conformance.cases.PredefinedEnumRuleProto3.EnumProto3.ENUM_PROTO3_ZERO_UNSPECIFIED.getNumber()) {
      output.writeEnum(1, val_);
    }
    getUnknownFields().writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (val_ != build.buf.validate.conformance.cases.PredefinedEnumRuleProto3.EnumProto3.ENUM_PROTO3_ZERO_UNSPECIFIED.getNumber()) {
      size += com.google.protobuf.CodedOutputStream
        .computeEnumSize(1, val_);
    }
    size += getUnknownFields().getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof build.buf.validate.conformance.cases.PredefinedEnumRuleProto3)) {
      return super.equals(obj);
    }
    build.buf.validate.conformance.cases.PredefinedEnumRuleProto3 other = (build.buf.validate.conformance.cases.PredefinedEnumRuleProto3) obj;

    if (val_ != other.val_) return false;
    if (!getUnknownFields().equals(other.getUnknownFields())) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    hash = (37 * hash) + VAL_FIELD_NUMBER;
    hash = (53 * hash) + val_;
    hash = (29 * hash) + getUnknownFields().hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static build.buf.validate.conformance.cases.PredefinedEnumRuleProto3 parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static build.buf.validate.conformance.cases.PredefinedEnumRuleProto3 parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static build.buf.validate.conformance.cases.PredefinedEnumRuleProto3 parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static build.buf.validate.conformance.cases.PredefinedEnumRuleProto3 parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static build.buf.validate.conformance.cases.PredefinedEnumRuleProto3 parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static build.buf.validate.conformance.cases.PredefinedEnumRuleProto3 parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static build.buf.validate.conformance.cases.PredefinedEnumRuleProto3 parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessage
        .parseWithIOException(PARSER, input);
  }
  public static build.buf.validate.conformance.cases.PredefinedEnumRuleProto3 parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessage
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  public static build.buf.validate.conformance.cases.PredefinedEnumRuleProto3 parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessage
        .parseDelimitedWithIOException(PARSER, input);
  }

  public static build.buf.validate.conformance.cases.PredefinedEnumRuleProto3 parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessage
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static build.buf.validate.conformance.cases.PredefinedEnumRuleProto3 parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessage
        .parseWithIOException(PARSER, input);
  }
  public static build.buf.validate.conformance.cases.PredefinedEnumRuleProto3 parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessage
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(build.buf.validate.conformance.cases.PredefinedEnumRuleProto3 prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessage.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code buf.validate.conformance.cases.PredefinedEnumRuleProto3}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessage.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:buf.validate.conformance.cases.PredefinedEnumRuleProto3)
      build.buf.validate.conformance.cases.PredefinedEnumRuleProto3OrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return build.buf.validate.conformance.cases.PredefinedRulesProto3Proto.internal_static_buf_validate_conformance_cases_PredefinedEnumRuleProto3_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return build.buf.validate.conformance.cases.PredefinedRulesProto3Proto.internal_static_buf_validate_conformance_cases_PredefinedEnumRuleProto3_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              build.buf.validate.conformance.cases.PredefinedEnumRuleProto3.class, build.buf.validate.conformance.cases.PredefinedEnumRuleProto3.Builder.class);
    }

    // Construct using build.buf.validate.conformance.cases.PredefinedEnumRuleProto3.newBuilder()
    private Builder() {

    }

    private Builder(
        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
      super(parent);

    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      bitField0_ = 0;
      val_ = 0;
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return build.buf.validate.conformance.cases.PredefinedRulesProto3Proto.internal_static_buf_validate_conformance_cases_PredefinedEnumRuleProto3_descriptor;
    }

    @java.lang.Override
    public build.buf.validate.conformance.cases.PredefinedEnumRuleProto3 getDefaultInstanceForType() {
      return build.buf.validate.conformance.cases.PredefinedEnumRuleProto3.getDefaultInstance();
    }

    @java.lang.Override
    public build.buf.validate.conformance.cases.PredefinedEnumRuleProto3 build() {
      build.buf.validate.conformance.cases.PredefinedEnumRuleProto3 result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public build.buf.validate.conformance.cases.PredefinedEnumRuleProto3 buildPartial() {
      build.buf.validate.conformance.cases.PredefinedEnumRuleProto3 result = new build.buf.validate.conformance.cases.PredefinedEnumRuleProto3(this);
      if (bitField0_ != 0) { buildPartial0(result); }
      onBuilt();
      return result;
    }

    private void buildPartial0(build.buf.validate.conformance.cases.PredefinedEnumRuleProto3 result) {
      int from_bitField0_ = bitField0_;
      if (((from_bitField0_ & 0x00000001) != 0)) {
        result.val_ = val_;
      }
    }

    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof build.buf.validate.conformance.cases.PredefinedEnumRuleProto3) {
        return mergeFrom((build.buf.validate.conformance.cases.PredefinedEnumRuleProto3)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(build.buf.validate.conformance.cases.PredefinedEnumRuleProto3 other) {
      if (other == build.buf.validate.conformance.cases.PredefinedEnumRuleProto3.getDefaultInstance()) return this;
      if (other.val_ != 0) {
        setValValue(other.getValValue());
      }
      this.mergeUnknownFields(other.getUnknownFields());
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      if (extensionRegistry == null) {
        throw new java.lang.NullPointerException();
      }
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            case 8: {
              val_ = input.readEnum();
              bitField0_ |= 0x00000001;
              break;
            } // case 8
            default: {
              if (!super.parseUnknownField(input, extensionRegistry, tag)) {
                done = true; // was an endgroup tag
              }
              break;
            } // default:
          } // switch (tag)
        } // while (!done)
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.unwrapIOException();
      } finally {
        onChanged();
      } // finally
      return this;
    }
    private int bitField0_;

    private int val_ = 0;
    /**
     * <code>.buf.validate.conformance.cases.PredefinedEnumRuleProto3.EnumProto3 val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
     * @return The enum numeric value on the wire for val.
     */
    @java.lang.Override public int getValValue() {
      return val_;
    }
    /**
     * <code>.buf.validate.conformance.cases.PredefinedEnumRuleProto3.EnumProto3 val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
     * @param value The enum numeric value on the wire for val to set.
     * @return This builder for chaining.
     */
    public Builder setValValue(int value) {
      val_ = value;
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }
    /**
     * <code>.buf.validate.conformance.cases.PredefinedEnumRuleProto3.EnumProto3 val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
     * @return The val.
     */
    @java.lang.Override
    public build.buf.validate.conformance.cases.PredefinedEnumRuleProto3.EnumProto3 getVal() {
      build.buf.validate.conformance.cases.PredefinedEnumRuleProto3.EnumProto3 result = build.buf.validate.conformance.cases.PredefinedEnumRuleProto3.EnumProto3.forNumber(val_);
      return result == null ? build.buf.validate.conformance.cases.PredefinedEnumRuleProto3.EnumProto3.UNRECOGNIZED : result;
    }
    /**
     * <code>.buf.validate.conformance.cases.PredefinedEnumRuleProto3.EnumProto3 val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
     * @param value The val to set.
     * @return This builder for chaining.
     */
    public Builder setVal(build.buf.validate.conformance.cases.PredefinedEnumRuleProto3.EnumProto3 value) {
      if (value == null) {
        throw new NullPointerException();
      }
      bitField0_ |= 0x00000001;
      val_ = value.getNumber();
      onChanged();
      return this;
    }
    /**
     * <code>.buf.validate.conformance.cases.PredefinedEnumRuleProto3.EnumProto3 val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
     * @return This builder for chaining.
     */
    public Builder clearVal() {
      bitField0_ = (bitField0_ & ~0x00000001);
      val_ = 0;
      onChanged();
      return this;
    }

    // @@protoc_insertion_point(builder_scope:buf.validate.conformance.cases.PredefinedEnumRuleProto3)
  }

  // @@protoc_insertion_point(class_scope:buf.validate.conformance.cases.PredefinedEnumRuleProto3)
  private static final build.buf.validate.conformance.cases.PredefinedEnumRuleProto3 DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new build.buf.validate.conformance.cases.PredefinedEnumRuleProto3();
  }

  public static build.buf.validate.conformance.cases.PredefinedEnumRuleProto3 getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<PredefinedEnumRuleProto3>
      PARSER = new com.google.protobuf.AbstractParser<PredefinedEnumRuleProto3>() {
    @java.lang.Override
    public PredefinedEnumRuleProto3 parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      Builder builder = newBuilder();
      try {
        builder.mergeFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(builder.buildPartial());
      } catch (com.google.protobuf.UninitializedMessageException e) {
        throw e.asInvalidProtocolBufferException().setUnfinishedMessage(builder.buildPartial());
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e)
            .setUnfinishedMessage(builder.buildPartial());
      }
      return builder.buildPartial();
    }
  };

  public static com.google.protobuf.Parser<PredefinedEnumRuleProto3> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<PredefinedEnumRuleProto3> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public build.buf.validate.conformance.cases.PredefinedEnumRuleProto3 getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

