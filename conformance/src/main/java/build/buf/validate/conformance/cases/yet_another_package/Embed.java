// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: buf/validate/conformance/cases/yet_another_package/embed2.proto
// Protobuf Java Version: 4.30.1

package build.buf.validate.conformance.cases.yet_another_package;

/**
 * <pre>
 * Validate message embedding across packages.
 * </pre>
 *
 * Protobuf type {@code buf.validate.conformance.cases.yet_another_package.Embed}
 */
public final class Embed extends
    com.google.protobuf.GeneratedMessage implements
    // @@protoc_insertion_point(message_implements:buf.validate.conformance.cases.yet_another_package.Embed)
    EmbedOrBuilder {
private static final long serialVersionUID = 0L;
  static {
    com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(
      com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,
      /* major= */ 4,
      /* minor= */ 30,
      /* patch= */ 1,
      /* suffix= */ "",
      Embed.class.getName());
  }
  // Use Embed.newBuilder() to construct.
  private Embed(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
    super(builder);
  }
  private Embed() {
  }

  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return build.buf.validate.conformance.cases.yet_another_package.Embed2Proto.internal_static_buf_validate_conformance_cases_yet_another_package_Embed_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return build.buf.validate.conformance.cases.yet_another_package.Embed2Proto.internal_static_buf_validate_conformance_cases_yet_another_package_Embed_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            build.buf.validate.conformance.cases.yet_another_package.Embed.class, build.buf.validate.conformance.cases.yet_another_package.Embed.Builder.class);
  }

  /**
   * Protobuf enum {@code buf.validate.conformance.cases.yet_another_package.Embed.Enumerated}
   */
  public enum Enumerated
      implements com.google.protobuf.ProtocolMessageEnum {
    /**
     * <code>ENUMERATED_UNSPECIFIED = 0;</code>
     */
    ENUMERATED_UNSPECIFIED(0),
    /**
     * <code>ENUMERATED_VALUE = 1;</code>
     */
    ENUMERATED_VALUE(1),
    UNRECOGNIZED(-1),
    ;

    static {
      com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(
        com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,
        /* major= */ 4,
        /* minor= */ 30,
        /* patch= */ 1,
        /* suffix= */ "",
        Enumerated.class.getName());
    }
    /**
     * <code>ENUMERATED_UNSPECIFIED = 0;</code>
     */
    public static final int ENUMERATED_UNSPECIFIED_VALUE = 0;
    /**
     * <code>ENUMERATED_VALUE = 1;</code>
     */
    public static final int ENUMERATED_VALUE_VALUE = 1;


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
    public static Enumerated valueOf(int value) {
      return forNumber(value);
    }

    /**
     * @param value The numeric wire value of the corresponding enum entry.
     * @return The enum associated with the given numeric wire value.
     */
    public static Enumerated forNumber(int value) {
      switch (value) {
        case 0: return ENUMERATED_UNSPECIFIED;
        case 1: return ENUMERATED_VALUE;
        default: return null;
      }
    }

    public static com.google.protobuf.Internal.EnumLiteMap<Enumerated>
        internalGetValueMap() {
      return internalValueMap;
    }
    private static final com.google.protobuf.Internal.EnumLiteMap<
        Enumerated> internalValueMap =
          new com.google.protobuf.Internal.EnumLiteMap<Enumerated>() {
            public Enumerated findValueByNumber(int number) {
              return Enumerated.forNumber(number);
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
      return build.buf.validate.conformance.cases.yet_another_package.Embed.getDescriptor().getEnumTypes().get(0);
    }

    private static final Enumerated[] VALUES = values();

    public static Enumerated valueOf(
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

    private Enumerated(int value) {
      this.value = value;
    }

    // @@protoc_insertion_point(enum_scope:buf.validate.conformance.cases.yet_another_package.Embed.Enumerated)
  }

  public static final int VAL_FIELD_NUMBER = 1;
  private long val_ = 0L;
  /**
   * <code>int64 val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   * @return The val.
   */
  @java.lang.Override
  public long getVal() {
    return val_;
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
    if (val_ != 0L) {
      output.writeInt64(1, val_);
    }
    getUnknownFields().writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (val_ != 0L) {
      size += com.google.protobuf.CodedOutputStream
        .computeInt64Size(1, val_);
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
    if (!(obj instanceof build.buf.validate.conformance.cases.yet_another_package.Embed)) {
      return super.equals(obj);
    }
    build.buf.validate.conformance.cases.yet_another_package.Embed other = (build.buf.validate.conformance.cases.yet_another_package.Embed) obj;

    if (getVal()
        != other.getVal()) return false;
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
    hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
        getVal());
    hash = (29 * hash) + getUnknownFields().hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static build.buf.validate.conformance.cases.yet_another_package.Embed parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static build.buf.validate.conformance.cases.yet_another_package.Embed parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static build.buf.validate.conformance.cases.yet_another_package.Embed parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static build.buf.validate.conformance.cases.yet_another_package.Embed parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static build.buf.validate.conformance.cases.yet_another_package.Embed parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static build.buf.validate.conformance.cases.yet_another_package.Embed parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static build.buf.validate.conformance.cases.yet_another_package.Embed parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessage
        .parseWithIOException(PARSER, input);
  }
  public static build.buf.validate.conformance.cases.yet_another_package.Embed parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessage
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  public static build.buf.validate.conformance.cases.yet_another_package.Embed parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessage
        .parseDelimitedWithIOException(PARSER, input);
  }

  public static build.buf.validate.conformance.cases.yet_another_package.Embed parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessage
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static build.buf.validate.conformance.cases.yet_another_package.Embed parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessage
        .parseWithIOException(PARSER, input);
  }
  public static build.buf.validate.conformance.cases.yet_another_package.Embed parseFrom(
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
  public static Builder newBuilder(build.buf.validate.conformance.cases.yet_another_package.Embed prototype) {
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
   * <pre>
   * Validate message embedding across packages.
   * </pre>
   *
   * Protobuf type {@code buf.validate.conformance.cases.yet_another_package.Embed}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessage.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:buf.validate.conformance.cases.yet_another_package.Embed)
      build.buf.validate.conformance.cases.yet_another_package.EmbedOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return build.buf.validate.conformance.cases.yet_another_package.Embed2Proto.internal_static_buf_validate_conformance_cases_yet_another_package_Embed_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return build.buf.validate.conformance.cases.yet_another_package.Embed2Proto.internal_static_buf_validate_conformance_cases_yet_another_package_Embed_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              build.buf.validate.conformance.cases.yet_another_package.Embed.class, build.buf.validate.conformance.cases.yet_another_package.Embed.Builder.class);
    }

    // Construct using build.buf.validate.conformance.cases.yet_another_package.Embed.newBuilder()
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
      val_ = 0L;
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return build.buf.validate.conformance.cases.yet_another_package.Embed2Proto.internal_static_buf_validate_conformance_cases_yet_another_package_Embed_descriptor;
    }

    @java.lang.Override
    public build.buf.validate.conformance.cases.yet_another_package.Embed getDefaultInstanceForType() {
      return build.buf.validate.conformance.cases.yet_another_package.Embed.getDefaultInstance();
    }

    @java.lang.Override
    public build.buf.validate.conformance.cases.yet_another_package.Embed build() {
      build.buf.validate.conformance.cases.yet_another_package.Embed result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public build.buf.validate.conformance.cases.yet_another_package.Embed buildPartial() {
      build.buf.validate.conformance.cases.yet_another_package.Embed result = new build.buf.validate.conformance.cases.yet_another_package.Embed(this);
      if (bitField0_ != 0) { buildPartial0(result); }
      onBuilt();
      return result;
    }

    private void buildPartial0(build.buf.validate.conformance.cases.yet_another_package.Embed result) {
      int from_bitField0_ = bitField0_;
      if (((from_bitField0_ & 0x00000001) != 0)) {
        result.val_ = val_;
      }
    }

    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof build.buf.validate.conformance.cases.yet_another_package.Embed) {
        return mergeFrom((build.buf.validate.conformance.cases.yet_another_package.Embed)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(build.buf.validate.conformance.cases.yet_another_package.Embed other) {
      if (other == build.buf.validate.conformance.cases.yet_another_package.Embed.getDefaultInstance()) return this;
      if (other.getVal() != 0L) {
        setVal(other.getVal());
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
              val_ = input.readInt64();
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

    private long val_ ;
    /**
     * <code>int64 val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
     * @return The val.
     */
    @java.lang.Override
    public long getVal() {
      return val_;
    }
    /**
     * <code>int64 val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
     * @param value The val to set.
     * @return This builder for chaining.
     */
    public Builder setVal(long value) {

      val_ = value;
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }
    /**
     * <code>int64 val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
     * @return This builder for chaining.
     */
    public Builder clearVal() {
      bitField0_ = (bitField0_ & ~0x00000001);
      val_ = 0L;
      onChanged();
      return this;
    }

    // @@protoc_insertion_point(builder_scope:buf.validate.conformance.cases.yet_another_package.Embed)
  }

  // @@protoc_insertion_point(class_scope:buf.validate.conformance.cases.yet_another_package.Embed)
  private static final build.buf.validate.conformance.cases.yet_another_package.Embed DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new build.buf.validate.conformance.cases.yet_another_package.Embed();
  }

  public static build.buf.validate.conformance.cases.yet_another_package.Embed getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<Embed>
      PARSER = new com.google.protobuf.AbstractParser<Embed>() {
    @java.lang.Override
    public Embed parsePartialFrom(
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

  public static com.google.protobuf.Parser<Embed> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<Embed> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public build.buf.validate.conformance.cases.yet_another_package.Embed getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

