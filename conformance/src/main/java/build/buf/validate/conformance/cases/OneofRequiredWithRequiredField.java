// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: buf/validate/conformance/cases/oneofs.proto

package build.buf.validate.conformance.cases;

/**
 * Protobuf type {@code buf.validate.conformance.cases.OneofRequiredWithRequiredField}
 */
public final class OneofRequiredWithRequiredField extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:buf.validate.conformance.cases.OneofRequiredWithRequiredField)
    OneofRequiredWithRequiredFieldOrBuilder {
private static final long serialVersionUID = 0L;
  // Use OneofRequiredWithRequiredField.newBuilder() to construct.
  private OneofRequiredWithRequiredField(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private OneofRequiredWithRequiredField() {
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new OneofRequiredWithRequiredField();
  }

  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return build.buf.validate.conformance.cases.OneofsProto.internal_static_buf_validate_conformance_cases_OneofRequiredWithRequiredField_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return build.buf.validate.conformance.cases.OneofsProto.internal_static_buf_validate_conformance_cases_OneofRequiredWithRequiredField_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            build.buf.validate.conformance.cases.OneofRequiredWithRequiredField.class, build.buf.validate.conformance.cases.OneofRequiredWithRequiredField.Builder.class);
  }

  private int oCase_ = 0;
  @SuppressWarnings("serial")
  private java.lang.Object o_;
  public enum OCase
      implements com.google.protobuf.Internal.EnumLite,
          com.google.protobuf.AbstractMessage.InternalOneOfEnum {
    A(1),
    B(2),
    O_NOT_SET(0);
    private final int value;
    private OCase(int value) {
      this.value = value;
    }
    /**
     * @param value The number of the enum to look for.
     * @return The enum associated with the given number.
     * @deprecated Use {@link #forNumber(int)} instead.
     */
    @java.lang.Deprecated
    public static OCase valueOf(int value) {
      return forNumber(value);
    }

    public static OCase forNumber(int value) {
      switch (value) {
        case 1: return A;
        case 2: return B;
        case 0: return O_NOT_SET;
        default: return null;
      }
    }
    public int getNumber() {
      return this.value;
    }
  };

  public OCase
  getOCase() {
    return OCase.forNumber(
        oCase_);
  }

  public static final int A_FIELD_NUMBER = 1;
  /**
   * <code>string a = 1 [json_name = "a", (.buf.validate.field) = { ... }</code>
   * @return Whether the a field is set.
   */
  public boolean hasA() {
    return oCase_ == 1;
  }
  /**
   * <code>string a = 1 [json_name = "a", (.buf.validate.field) = { ... }</code>
   * @return The a.
   */
  public java.lang.String getA() {
    java.lang.Object ref = "";
    if (oCase_ == 1) {
      ref = o_;
    }
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      if (oCase_ == 1) {
        o_ = s;
      }
      return s;
    }
  }
  /**
   * <code>string a = 1 [json_name = "a", (.buf.validate.field) = { ... }</code>
   * @return The bytes for a.
   */
  public com.google.protobuf.ByteString
      getABytes() {
    java.lang.Object ref = "";
    if (oCase_ == 1) {
      ref = o_;
    }
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      if (oCase_ == 1) {
        o_ = b;
      }
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int B_FIELD_NUMBER = 2;
  /**
   * <code>string b = 2 [json_name = "b"];</code>
   * @return Whether the b field is set.
   */
  public boolean hasB() {
    return oCase_ == 2;
  }
  /**
   * <code>string b = 2 [json_name = "b"];</code>
   * @return The b.
   */
  public java.lang.String getB() {
    java.lang.Object ref = "";
    if (oCase_ == 2) {
      ref = o_;
    }
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      if (oCase_ == 2) {
        o_ = s;
      }
      return s;
    }
  }
  /**
   * <code>string b = 2 [json_name = "b"];</code>
   * @return The bytes for b.
   */
  public com.google.protobuf.ByteString
      getBBytes() {
    java.lang.Object ref = "";
    if (oCase_ == 2) {
      ref = o_;
    }
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      if (oCase_ == 2) {
        o_ = b;
      }
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
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
    if (oCase_ == 1) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, o_);
    }
    if (oCase_ == 2) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, o_);
    }
    getUnknownFields().writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (oCase_ == 1) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, o_);
    }
    if (oCase_ == 2) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, o_);
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
    if (!(obj instanceof build.buf.validate.conformance.cases.OneofRequiredWithRequiredField)) {
      return super.equals(obj);
    }
    build.buf.validate.conformance.cases.OneofRequiredWithRequiredField other = (build.buf.validate.conformance.cases.OneofRequiredWithRequiredField) obj;

    if (!getOCase().equals(other.getOCase())) return false;
    switch (oCase_) {
      case 1:
        if (!getA()
            .equals(other.getA())) return false;
        break;
      case 2:
        if (!getB()
            .equals(other.getB())) return false;
        break;
      case 0:
      default:
    }
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
    switch (oCase_) {
      case 1:
        hash = (37 * hash) + A_FIELD_NUMBER;
        hash = (53 * hash) + getA().hashCode();
        break;
      case 2:
        hash = (37 * hash) + B_FIELD_NUMBER;
        hash = (53 * hash) + getB().hashCode();
        break;
      case 0:
      default:
    }
    hash = (29 * hash) + getUnknownFields().hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static build.buf.validate.conformance.cases.OneofRequiredWithRequiredField parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static build.buf.validate.conformance.cases.OneofRequiredWithRequiredField parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static build.buf.validate.conformance.cases.OneofRequiredWithRequiredField parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static build.buf.validate.conformance.cases.OneofRequiredWithRequiredField parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static build.buf.validate.conformance.cases.OneofRequiredWithRequiredField parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static build.buf.validate.conformance.cases.OneofRequiredWithRequiredField parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static build.buf.validate.conformance.cases.OneofRequiredWithRequiredField parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static build.buf.validate.conformance.cases.OneofRequiredWithRequiredField parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  public static build.buf.validate.conformance.cases.OneofRequiredWithRequiredField parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }

  public static build.buf.validate.conformance.cases.OneofRequiredWithRequiredField parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static build.buf.validate.conformance.cases.OneofRequiredWithRequiredField parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static build.buf.validate.conformance.cases.OneofRequiredWithRequiredField parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(build.buf.validate.conformance.cases.OneofRequiredWithRequiredField prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code buf.validate.conformance.cases.OneofRequiredWithRequiredField}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:buf.validate.conformance.cases.OneofRequiredWithRequiredField)
      build.buf.validate.conformance.cases.OneofRequiredWithRequiredFieldOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return build.buf.validate.conformance.cases.OneofsProto.internal_static_buf_validate_conformance_cases_OneofRequiredWithRequiredField_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return build.buf.validate.conformance.cases.OneofsProto.internal_static_buf_validate_conformance_cases_OneofRequiredWithRequiredField_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              build.buf.validate.conformance.cases.OneofRequiredWithRequiredField.class, build.buf.validate.conformance.cases.OneofRequiredWithRequiredField.Builder.class);
    }

    // Construct using build.buf.validate.conformance.cases.OneofRequiredWithRequiredField.newBuilder()
    private Builder() {

    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);

    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      bitField0_ = 0;
      oCase_ = 0;
      o_ = null;
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return build.buf.validate.conformance.cases.OneofsProto.internal_static_buf_validate_conformance_cases_OneofRequiredWithRequiredField_descriptor;
    }

    @java.lang.Override
    public build.buf.validate.conformance.cases.OneofRequiredWithRequiredField getDefaultInstanceForType() {
      return build.buf.validate.conformance.cases.OneofRequiredWithRequiredField.getDefaultInstance();
    }

    @java.lang.Override
    public build.buf.validate.conformance.cases.OneofRequiredWithRequiredField build() {
      build.buf.validate.conformance.cases.OneofRequiredWithRequiredField result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public build.buf.validate.conformance.cases.OneofRequiredWithRequiredField buildPartial() {
      build.buf.validate.conformance.cases.OneofRequiredWithRequiredField result = new build.buf.validate.conformance.cases.OneofRequiredWithRequiredField(this);
      if (bitField0_ != 0) { buildPartial0(result); }
      buildPartialOneofs(result);
      onBuilt();
      return result;
    }

    private void buildPartial0(build.buf.validate.conformance.cases.OneofRequiredWithRequiredField result) {
      int from_bitField0_ = bitField0_;
    }

    private void buildPartialOneofs(build.buf.validate.conformance.cases.OneofRequiredWithRequiredField result) {
      result.oCase_ = oCase_;
      result.o_ = this.o_;
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof build.buf.validate.conformance.cases.OneofRequiredWithRequiredField) {
        return mergeFrom((build.buf.validate.conformance.cases.OneofRequiredWithRequiredField)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(build.buf.validate.conformance.cases.OneofRequiredWithRequiredField other) {
      if (other == build.buf.validate.conformance.cases.OneofRequiredWithRequiredField.getDefaultInstance()) return this;
      switch (other.getOCase()) {
        case A: {
          oCase_ = 1;
          o_ = other.o_;
          onChanged();
          break;
        }
        case B: {
          oCase_ = 2;
          o_ = other.o_;
          onChanged();
          break;
        }
        case O_NOT_SET: {
          break;
        }
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
            case 10: {
              java.lang.String s = input.readStringRequireUtf8();
              oCase_ = 1;
              o_ = s;
              break;
            } // case 10
            case 18: {
              java.lang.String s = input.readStringRequireUtf8();
              oCase_ = 2;
              o_ = s;
              break;
            } // case 18
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
    private int oCase_ = 0;
    private java.lang.Object o_;
    public OCase
        getOCase() {
      return OCase.forNumber(
          oCase_);
    }

    public Builder clearO() {
      oCase_ = 0;
      o_ = null;
      onChanged();
      return this;
    }

    private int bitField0_;

    /**
     * <code>string a = 1 [json_name = "a", (.buf.validate.field) = { ... }</code>
     * @return Whether the a field is set.
     */
    @java.lang.Override
    public boolean hasA() {
      return oCase_ == 1;
    }
    /**
     * <code>string a = 1 [json_name = "a", (.buf.validate.field) = { ... }</code>
     * @return The a.
     */
    @java.lang.Override
    public java.lang.String getA() {
      java.lang.Object ref = "";
      if (oCase_ == 1) {
        ref = o_;
      }
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        if (oCase_ == 1) {
          o_ = s;
        }
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string a = 1 [json_name = "a", (.buf.validate.field) = { ... }</code>
     * @return The bytes for a.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getABytes() {
      java.lang.Object ref = "";
      if (oCase_ == 1) {
        ref = o_;
      }
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        if (oCase_ == 1) {
          o_ = b;
        }
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string a = 1 [json_name = "a", (.buf.validate.field) = { ... }</code>
     * @param value The a to set.
     * @return This builder for chaining.
     */
    public Builder setA(
        java.lang.String value) {
      if (value == null) { throw new NullPointerException(); }
      oCase_ = 1;
      o_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string a = 1 [json_name = "a", (.buf.validate.field) = { ... }</code>
     * @return This builder for chaining.
     */
    public Builder clearA() {
      if (oCase_ == 1) {
        oCase_ = 0;
        o_ = null;
        onChanged();
      }
      return this;
    }
    /**
     * <code>string a = 1 [json_name = "a", (.buf.validate.field) = { ... }</code>
     * @param value The bytes for a to set.
     * @return This builder for chaining.
     */
    public Builder setABytes(
        com.google.protobuf.ByteString value) {
      if (value == null) { throw new NullPointerException(); }
      checkByteStringIsUtf8(value);
      oCase_ = 1;
      o_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>string b = 2 [json_name = "b"];</code>
     * @return Whether the b field is set.
     */
    @java.lang.Override
    public boolean hasB() {
      return oCase_ == 2;
    }
    /**
     * <code>string b = 2 [json_name = "b"];</code>
     * @return The b.
     */
    @java.lang.Override
    public java.lang.String getB() {
      java.lang.Object ref = "";
      if (oCase_ == 2) {
        ref = o_;
      }
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        if (oCase_ == 2) {
          o_ = s;
        }
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <code>string b = 2 [json_name = "b"];</code>
     * @return The bytes for b.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getBBytes() {
      java.lang.Object ref = "";
      if (oCase_ == 2) {
        ref = o_;
      }
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        if (oCase_ == 2) {
          o_ = b;
        }
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string b = 2 [json_name = "b"];</code>
     * @param value The b to set.
     * @return This builder for chaining.
     */
    public Builder setB(
        java.lang.String value) {
      if (value == null) { throw new NullPointerException(); }
      oCase_ = 2;
      o_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string b = 2 [json_name = "b"];</code>
     * @return This builder for chaining.
     */
    public Builder clearB() {
      if (oCase_ == 2) {
        oCase_ = 0;
        o_ = null;
        onChanged();
      }
      return this;
    }
    /**
     * <code>string b = 2 [json_name = "b"];</code>
     * @param value The bytes for b to set.
     * @return This builder for chaining.
     */
    public Builder setBBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) { throw new NullPointerException(); }
      checkByteStringIsUtf8(value);
      oCase_ = 2;
      o_ = value;
      onChanged();
      return this;
    }
    @java.lang.Override
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:buf.validate.conformance.cases.OneofRequiredWithRequiredField)
  }

  // @@protoc_insertion_point(class_scope:buf.validate.conformance.cases.OneofRequiredWithRequiredField)
  private static final build.buf.validate.conformance.cases.OneofRequiredWithRequiredField DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new build.buf.validate.conformance.cases.OneofRequiredWithRequiredField();
  }

  public static build.buf.validate.conformance.cases.OneofRequiredWithRequiredField getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<OneofRequiredWithRequiredField>
      PARSER = new com.google.protobuf.AbstractParser<OneofRequiredWithRequiredField>() {
    @java.lang.Override
    public OneofRequiredWithRequiredField parsePartialFrom(
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

  public static com.google.protobuf.Parser<OneofRequiredWithRequiredField> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<OneofRequiredWithRequiredField> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public build.buf.validate.conformance.cases.OneofRequiredWithRequiredField getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}
