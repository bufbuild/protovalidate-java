// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: buf/validate/conformance/cases/ignore_proto_editions.proto
// Protobuf Java Version: 4.27.3

package build.buf.validate.conformance.cases;

/**
 * Protobuf type {@code buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault}
 */
public final class EditionsScalarImplicitPresenceIgnoreDefault extends
    com.google.protobuf.GeneratedMessage implements
    // @@protoc_insertion_point(message_implements:buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault)
    EditionsScalarImplicitPresenceIgnoreDefaultOrBuilder {
private static final long serialVersionUID = 0L;
  static {
    com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(
      com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,
      /* major= */ 4,
      /* minor= */ 27,
      /* patch= */ 3,
      /* suffix= */ "",
      EditionsScalarImplicitPresenceIgnoreDefault.class.getName());
  }
  // Use EditionsScalarImplicitPresenceIgnoreDefault.newBuilder() to construct.
  private EditionsScalarImplicitPresenceIgnoreDefault(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
    super(builder);
  }
  private EditionsScalarImplicitPresenceIgnoreDefault() {
  }

  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return build.buf.validate.conformance.cases.IgnoreProtoEditionsProto.internal_static_buf_validate_conformance_cases_EditionsScalarImplicitPresenceIgnoreDefault_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return build.buf.validate.conformance.cases.IgnoreProtoEditionsProto.internal_static_buf_validate_conformance_cases_EditionsScalarImplicitPresenceIgnoreDefault_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault.class, build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault.Builder.class);
  }

  public static final int VAL_FIELD_NUMBER = 1;
  private int val_ = 0;
  /**
   * <code>int32 val = 1 [json_name = "val", features = { ... }</code>
   * @return The val.
   */
  @java.lang.Override
  public int getVal() {
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
    if (val_ != 0) {
      output.writeInt32(1, val_);
    }
    getUnknownFields().writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (val_ != 0) {
      size += com.google.protobuf.CodedOutputStream
        .computeInt32Size(1, val_);
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
    if (!(obj instanceof build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault)) {
      return super.equals(obj);
    }
    build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault other = (build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault) obj;

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
    hash = (53 * hash) + getVal();
    hash = (29 * hash) + getUnknownFields().hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessage
        .parseWithIOException(PARSER, input);
  }
  public static build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessage
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  public static build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessage
        .parseDelimitedWithIOException(PARSER, input);
  }

  public static build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessage
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessage
        .parseWithIOException(PARSER, input);
  }
  public static build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault parseFrom(
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
  public static Builder newBuilder(build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault prototype) {
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
   * Protobuf type {@code buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessage.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault)
      build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefaultOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return build.buf.validate.conformance.cases.IgnoreProtoEditionsProto.internal_static_buf_validate_conformance_cases_EditionsScalarImplicitPresenceIgnoreDefault_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return build.buf.validate.conformance.cases.IgnoreProtoEditionsProto.internal_static_buf_validate_conformance_cases_EditionsScalarImplicitPresenceIgnoreDefault_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault.class, build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault.Builder.class);
    }

    // Construct using build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault.newBuilder()
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
      return build.buf.validate.conformance.cases.IgnoreProtoEditionsProto.internal_static_buf_validate_conformance_cases_EditionsScalarImplicitPresenceIgnoreDefault_descriptor;
    }

    @java.lang.Override
    public build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault getDefaultInstanceForType() {
      return build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault.getDefaultInstance();
    }

    @java.lang.Override
    public build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault build() {
      build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault buildPartial() {
      build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault result = new build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault(this);
      if (bitField0_ != 0) { buildPartial0(result); }
      onBuilt();
      return result;
    }

    private void buildPartial0(build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault result) {
      int from_bitField0_ = bitField0_;
      if (((from_bitField0_ & 0x00000001) != 0)) {
        result.val_ = val_;
      }
    }

    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault) {
        return mergeFrom((build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault other) {
      if (other == build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault.getDefaultInstance()) return this;
      if (other.getVal() != 0) {
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
              val_ = input.readInt32();
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

    private int val_ ;
    /**
     * <code>int32 val = 1 [json_name = "val", features = { ... }</code>
     * @return The val.
     */
    @java.lang.Override
    public int getVal() {
      return val_;
    }
    /**
     * <code>int32 val = 1 [json_name = "val", features = { ... }</code>
     * @param value The val to set.
     * @return This builder for chaining.
     */
    public Builder setVal(int value) {

      val_ = value;
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }
    /**
     * <code>int32 val = 1 [json_name = "val", features = { ... }</code>
     * @return This builder for chaining.
     */
    public Builder clearVal() {
      bitField0_ = (bitField0_ & ~0x00000001);
      val_ = 0;
      onChanged();
      return this;
    }

    // @@protoc_insertion_point(builder_scope:buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault)
  }

  // @@protoc_insertion_point(class_scope:buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault)
  private static final build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault();
  }

  public static build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<EditionsScalarImplicitPresenceIgnoreDefault>
      PARSER = new com.google.protobuf.AbstractParser<EditionsScalarImplicitPresenceIgnoreDefault>() {
    @java.lang.Override
    public EditionsScalarImplicitPresenceIgnoreDefault parsePartialFrom(
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

  public static com.google.protobuf.Parser<EditionsScalarImplicitPresenceIgnoreDefault> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<EditionsScalarImplicitPresenceIgnoreDefault> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public build.buf.validate.conformance.cases.EditionsScalarImplicitPresenceIgnoreDefault getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}
