// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: buf/validate/conformance/cases/enums.proto
// Protobuf Java Version: 4.30.1

package build.buf.validate.conformance.cases;

/**
 * Protobuf type {@code buf.validate.conformance.cases.MapExternalEnumDefined}
 */
public final class MapExternalEnumDefined extends
    com.google.protobuf.GeneratedMessage implements
    // @@protoc_insertion_point(message_implements:buf.validate.conformance.cases.MapExternalEnumDefined)
    MapExternalEnumDefinedOrBuilder {
private static final long serialVersionUID = 0L;
  static {
    com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(
      com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,
      /* major= */ 4,
      /* minor= */ 30,
      /* patch= */ 1,
      /* suffix= */ "",
      MapExternalEnumDefined.class.getName());
  }
  // Use MapExternalEnumDefined.newBuilder() to construct.
  private MapExternalEnumDefined(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
    super(builder);
  }
  private MapExternalEnumDefined() {
  }

  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return build.buf.validate.conformance.cases.EnumsProto.internal_static_buf_validate_conformance_cases_MapExternalEnumDefined_descriptor;
  }

  @SuppressWarnings({"rawtypes"})
  @java.lang.Override
  protected com.google.protobuf.MapFieldReflectionAccessor internalGetMapFieldReflection(
      int number) {
    switch (number) {
      case 1:
        return internalGetVal();
      default:
        throw new RuntimeException(
            "Invalid map field number: " + number);
    }
  }
  @java.lang.Override
  protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return build.buf.validate.conformance.cases.EnumsProto.internal_static_buf_validate_conformance_cases_MapExternalEnumDefined_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            build.buf.validate.conformance.cases.MapExternalEnumDefined.class, build.buf.validate.conformance.cases.MapExternalEnumDefined.Builder.class);
  }

  public static final int VAL_FIELD_NUMBER = 1;
  private static final class ValDefaultEntryHolder {
    static final com.google.protobuf.MapEntry<
        java.lang.String, java.lang.Integer> defaultEntry =
            com.google.protobuf.MapEntry
            .<java.lang.String, java.lang.Integer>newDefaultInstance(
                build.buf.validate.conformance.cases.EnumsProto.internal_static_buf_validate_conformance_cases_MapExternalEnumDefined_ValEntry_descriptor, 
                com.google.protobuf.WireFormat.FieldType.STRING,
                "",
                com.google.protobuf.WireFormat.FieldType.ENUM,
                build.buf.validate.conformance.cases.other_package.Embed.Enumerated.ENUMERATED_UNSPECIFIED.getNumber());
  }
  @SuppressWarnings("serial")
  private com.google.protobuf.MapField<
      java.lang.String, java.lang.Integer> val_;
  private com.google.protobuf.MapField<java.lang.String, java.lang.Integer>
  internalGetVal() {
    if (val_ == null) {
      return com.google.protobuf.MapField.emptyMapField(
          ValDefaultEntryHolder.defaultEntry);
    }
    return val_;
  }
  private static final
  com.google.protobuf.Internal.MapAdapter.Converter<
      java.lang.Integer, build.buf.validate.conformance.cases.other_package.Embed.Enumerated> valValueConverter =
          com.google.protobuf.Internal.MapAdapter.newEnumConverter(
              build.buf.validate.conformance.cases.other_package.Embed.Enumerated.internalGetValueMap(),
              build.buf.validate.conformance.cases.other_package.Embed.Enumerated.UNRECOGNIZED);
  private static final java.util.Map<java.lang.String, build.buf.validate.conformance.cases.other_package.Embed.Enumerated>
  internalGetAdaptedValMap(
      java.util.Map<java.lang.String, java.lang.Integer> map) {
    return new com.google.protobuf.Internal.MapAdapter<
        java.lang.String, build.buf.validate.conformance.cases.other_package.Embed.Enumerated, java.lang.Integer>(
            map, valValueConverter);
  }
  public int getValCount() {
    return internalGetVal().getMap().size();
  }
  /**
   * <code>map&lt;string, .buf.validate.conformance.cases.other_package.Embed.Enumerated&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  @java.lang.Override
  public boolean containsVal(
      java.lang.String key) {
    if (key == null) { throw new NullPointerException("map key"); }
    return internalGetVal().getMap().containsKey(key);
  }
  /**
   * Use {@link #getValMap()} instead.
   */
  @java.lang.Override
  @java.lang.Deprecated
  public java.util.Map<java.lang.String, build.buf.validate.conformance.cases.other_package.Embed.Enumerated>
  getVal() {
    return getValMap();
  }
  /**
   * <code>map&lt;string, .buf.validate.conformance.cases.other_package.Embed.Enumerated&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  @java.lang.Override
  public java.util.Map<java.lang.String, build.buf.validate.conformance.cases.other_package.Embed.Enumerated>
  getValMap() {
    return internalGetAdaptedValMap(
        internalGetVal().getMap());}
  /**
   * <code>map&lt;string, .buf.validate.conformance.cases.other_package.Embed.Enumerated&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  @java.lang.Override
  public /* nullable */
build.buf.validate.conformance.cases.other_package.Embed.Enumerated getValOrDefault(
      java.lang.String key,
      /* nullable */
build.buf.validate.conformance.cases.other_package.Embed.Enumerated defaultValue) {
    if (key == null) { throw new NullPointerException("map key"); }
    java.util.Map<java.lang.String, java.lang.Integer> map =
        internalGetVal().getMap();
    return map.containsKey(key)
           ? valValueConverter.doForward(map.get(key))
           : defaultValue;
  }
  /**
   * <code>map&lt;string, .buf.validate.conformance.cases.other_package.Embed.Enumerated&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  @java.lang.Override
  public build.buf.validate.conformance.cases.other_package.Embed.Enumerated getValOrThrow(
      java.lang.String key) {
    if (key == null) { throw new NullPointerException("map key"); }
    java.util.Map<java.lang.String, java.lang.Integer> map =
        internalGetVal().getMap();
    if (!map.containsKey(key)) {
      throw new java.lang.IllegalArgumentException();
    }
    return valValueConverter.doForward(map.get(key));
  }
  /**
   * Use {@link #getValValueMap()} instead.
   */
  @java.lang.Override
  @java.lang.Deprecated
  public java.util.Map<java.lang.String, java.lang.Integer>
  getValValue() {
    return getValValueMap();
  }
  /**
   * <code>map&lt;string, .buf.validate.conformance.cases.other_package.Embed.Enumerated&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  @java.lang.Override
  public java.util.Map<java.lang.String, java.lang.Integer>
  getValValueMap() {
    return internalGetVal().getMap();
  }
  /**
   * <code>map&lt;string, .buf.validate.conformance.cases.other_package.Embed.Enumerated&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  @java.lang.Override
  public int getValValueOrDefault(
      java.lang.String key,
      int defaultValue) {
    if (key == null) { throw new NullPointerException("map key"); }
    java.util.Map<java.lang.String, java.lang.Integer> map =
        internalGetVal().getMap();
    return map.containsKey(key) ? map.get(key) : defaultValue;
  }
  /**
   * <code>map&lt;string, .buf.validate.conformance.cases.other_package.Embed.Enumerated&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  @java.lang.Override
  public int getValValueOrThrow(
      java.lang.String key) {
    if (key == null) { throw new NullPointerException("map key"); }
    java.util.Map<java.lang.String, java.lang.Integer> map =
        internalGetVal().getMap();
    if (!map.containsKey(key)) {
      throw new java.lang.IllegalArgumentException();
    }
    return map.get(key);
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
    com.google.protobuf.GeneratedMessage
      .serializeStringMapTo(
        output,
        internalGetVal(),
        ValDefaultEntryHolder.defaultEntry,
        1);
    getUnknownFields().writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    for (java.util.Map.Entry<java.lang.String, java.lang.Integer> entry
         : internalGetVal().getMap().entrySet()) {
      com.google.protobuf.MapEntry<java.lang.String, java.lang.Integer>
      val__ = ValDefaultEntryHolder.defaultEntry.newBuilderForType()
          .setKey(entry.getKey())
          .setValue(entry.getValue())
          .build();
      size += com.google.protobuf.CodedOutputStream
          .computeMessageSize(1, val__);
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
    if (!(obj instanceof build.buf.validate.conformance.cases.MapExternalEnumDefined)) {
      return super.equals(obj);
    }
    build.buf.validate.conformance.cases.MapExternalEnumDefined other = (build.buf.validate.conformance.cases.MapExternalEnumDefined) obj;

    if (!internalGetVal().equals(
        other.internalGetVal())) return false;
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
    if (!internalGetVal().getMap().isEmpty()) {
      hash = (37 * hash) + VAL_FIELD_NUMBER;
      hash = (53 * hash) + internalGetVal().hashCode();
    }
    hash = (29 * hash) + getUnknownFields().hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static build.buf.validate.conformance.cases.MapExternalEnumDefined parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static build.buf.validate.conformance.cases.MapExternalEnumDefined parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static build.buf.validate.conformance.cases.MapExternalEnumDefined parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static build.buf.validate.conformance.cases.MapExternalEnumDefined parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static build.buf.validate.conformance.cases.MapExternalEnumDefined parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static build.buf.validate.conformance.cases.MapExternalEnumDefined parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static build.buf.validate.conformance.cases.MapExternalEnumDefined parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessage
        .parseWithIOException(PARSER, input);
  }
  public static build.buf.validate.conformance.cases.MapExternalEnumDefined parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessage
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  public static build.buf.validate.conformance.cases.MapExternalEnumDefined parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessage
        .parseDelimitedWithIOException(PARSER, input);
  }

  public static build.buf.validate.conformance.cases.MapExternalEnumDefined parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessage
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static build.buf.validate.conformance.cases.MapExternalEnumDefined parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessage
        .parseWithIOException(PARSER, input);
  }
  public static build.buf.validate.conformance.cases.MapExternalEnumDefined parseFrom(
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
  public static Builder newBuilder(build.buf.validate.conformance.cases.MapExternalEnumDefined prototype) {
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
   * Protobuf type {@code buf.validate.conformance.cases.MapExternalEnumDefined}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessage.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:buf.validate.conformance.cases.MapExternalEnumDefined)
      build.buf.validate.conformance.cases.MapExternalEnumDefinedOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return build.buf.validate.conformance.cases.EnumsProto.internal_static_buf_validate_conformance_cases_MapExternalEnumDefined_descriptor;
    }

    @SuppressWarnings({"rawtypes"})
    protected com.google.protobuf.MapFieldReflectionAccessor internalGetMapFieldReflection(
        int number) {
      switch (number) {
        case 1:
          return internalGetVal();
        default:
          throw new RuntimeException(
              "Invalid map field number: " + number);
      }
    }
    @SuppressWarnings({"rawtypes"})
    protected com.google.protobuf.MapFieldReflectionAccessor internalGetMutableMapFieldReflection(
        int number) {
      switch (number) {
        case 1:
          return internalGetMutableVal();
        default:
          throw new RuntimeException(
              "Invalid map field number: " + number);
      }
    }
    @java.lang.Override
    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return build.buf.validate.conformance.cases.EnumsProto.internal_static_buf_validate_conformance_cases_MapExternalEnumDefined_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              build.buf.validate.conformance.cases.MapExternalEnumDefined.class, build.buf.validate.conformance.cases.MapExternalEnumDefined.Builder.class);
    }

    // Construct using build.buf.validate.conformance.cases.MapExternalEnumDefined.newBuilder()
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
      internalGetMutableVal().clear();
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return build.buf.validate.conformance.cases.EnumsProto.internal_static_buf_validate_conformance_cases_MapExternalEnumDefined_descriptor;
    }

    @java.lang.Override
    public build.buf.validate.conformance.cases.MapExternalEnumDefined getDefaultInstanceForType() {
      return build.buf.validate.conformance.cases.MapExternalEnumDefined.getDefaultInstance();
    }

    @java.lang.Override
    public build.buf.validate.conformance.cases.MapExternalEnumDefined build() {
      build.buf.validate.conformance.cases.MapExternalEnumDefined result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public build.buf.validate.conformance.cases.MapExternalEnumDefined buildPartial() {
      build.buf.validate.conformance.cases.MapExternalEnumDefined result = new build.buf.validate.conformance.cases.MapExternalEnumDefined(this);
      if (bitField0_ != 0) { buildPartial0(result); }
      onBuilt();
      return result;
    }

    private void buildPartial0(build.buf.validate.conformance.cases.MapExternalEnumDefined result) {
      int from_bitField0_ = bitField0_;
      if (((from_bitField0_ & 0x00000001) != 0)) {
        result.val_ = internalGetVal();
        result.val_.makeImmutable();
      }
    }

    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof build.buf.validate.conformance.cases.MapExternalEnumDefined) {
        return mergeFrom((build.buf.validate.conformance.cases.MapExternalEnumDefined)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(build.buf.validate.conformance.cases.MapExternalEnumDefined other) {
      if (other == build.buf.validate.conformance.cases.MapExternalEnumDefined.getDefaultInstance()) return this;
      internalGetMutableVal().mergeFrom(
          other.internalGetVal());
      bitField0_ |= 0x00000001;
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
              com.google.protobuf.MapEntry<java.lang.String, java.lang.Integer>
              val__ = input.readMessage(
                  ValDefaultEntryHolder.defaultEntry.getParserForType(), extensionRegistry);
              internalGetMutableVal().getMutableMap().put(
                  val__.getKey(), val__.getValue());
              bitField0_ |= 0x00000001;
              break;
            } // case 10
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

    private com.google.protobuf.MapField<
        java.lang.String, java.lang.Integer> val_;
    private com.google.protobuf.MapField<java.lang.String, java.lang.Integer>
        internalGetVal() {
      if (val_ == null) {
        return com.google.protobuf.MapField.emptyMapField(
            ValDefaultEntryHolder.defaultEntry);
      }
      return val_;
    }
    private com.google.protobuf.MapField<java.lang.String, java.lang.Integer>
        internalGetMutableVal() {
      if (val_ == null) {
        val_ = com.google.protobuf.MapField.newMapField(
            ValDefaultEntryHolder.defaultEntry);
      }
      if (!val_.isMutable()) {
        val_ = val_.copy();
      }
      bitField0_ |= 0x00000001;
      onChanged();
      return val_;
    }
    public int getValCount() {
      return internalGetVal().getMap().size();
    }
    /**
     * <code>map&lt;string, .buf.validate.conformance.cases.other_package.Embed.Enumerated&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
     */
    @java.lang.Override
    public boolean containsVal(
        java.lang.String key) {
      if (key == null) { throw new NullPointerException("map key"); }
      return internalGetVal().getMap().containsKey(key);
    }
    /**
     * Use {@link #getValMap()} instead.
     */
    @java.lang.Override
    @java.lang.Deprecated
    public java.util.Map<java.lang.String, build.buf.validate.conformance.cases.other_package.Embed.Enumerated>
    getVal() {
      return getValMap();
    }
    /**
     * <code>map&lt;string, .buf.validate.conformance.cases.other_package.Embed.Enumerated&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
     */
    @java.lang.Override
    public java.util.Map<java.lang.String, build.buf.validate.conformance.cases.other_package.Embed.Enumerated>
    getValMap() {
      return internalGetAdaptedValMap(
          internalGetVal().getMap());}
    /**
     * <code>map&lt;string, .buf.validate.conformance.cases.other_package.Embed.Enumerated&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
     */
    @java.lang.Override
    public /* nullable */
build.buf.validate.conformance.cases.other_package.Embed.Enumerated getValOrDefault(
        java.lang.String key,
        /* nullable */
build.buf.validate.conformance.cases.other_package.Embed.Enumerated defaultValue) {
      if (key == null) { throw new NullPointerException("map key"); }
      java.util.Map<java.lang.String, java.lang.Integer> map =
          internalGetVal().getMap();
      return map.containsKey(key)
             ? valValueConverter.doForward(map.get(key))
             : defaultValue;
    }
    /**
     * <code>map&lt;string, .buf.validate.conformance.cases.other_package.Embed.Enumerated&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
     */
    @java.lang.Override
    public build.buf.validate.conformance.cases.other_package.Embed.Enumerated getValOrThrow(
        java.lang.String key) {
      if (key == null) { throw new NullPointerException("map key"); }
      java.util.Map<java.lang.String, java.lang.Integer> map =
          internalGetVal().getMap();
      if (!map.containsKey(key)) {
        throw new java.lang.IllegalArgumentException();
      }
      return valValueConverter.doForward(map.get(key));
    }
    /**
     * Use {@link #getValValueMap()} instead.
     */
    @java.lang.Override
    @java.lang.Deprecated
    public java.util.Map<java.lang.String, java.lang.Integer>
    getValValue() {
      return getValValueMap();
    }
    /**
     * <code>map&lt;string, .buf.validate.conformance.cases.other_package.Embed.Enumerated&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
     */
    @java.lang.Override
    public java.util.Map<java.lang.String, java.lang.Integer>
    getValValueMap() {
      return internalGetVal().getMap();
    }
    /**
     * <code>map&lt;string, .buf.validate.conformance.cases.other_package.Embed.Enumerated&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
     */
    @java.lang.Override
    public int getValValueOrDefault(
        java.lang.String key,
        int defaultValue) {
      if (key == null) { throw new NullPointerException("map key"); }
      java.util.Map<java.lang.String, java.lang.Integer> map =
          internalGetVal().getMap();
      return map.containsKey(key) ? map.get(key) : defaultValue;
    }
    /**
     * <code>map&lt;string, .buf.validate.conformance.cases.other_package.Embed.Enumerated&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
     */
    @java.lang.Override
    public int getValValueOrThrow(
        java.lang.String key) {
      if (key == null) { throw new NullPointerException("map key"); }
      java.util.Map<java.lang.String, java.lang.Integer> map =
          internalGetVal().getMap();
      if (!map.containsKey(key)) {
        throw new java.lang.IllegalArgumentException();
      }
      return map.get(key);
    }
    public Builder clearVal() {
      bitField0_ = (bitField0_ & ~0x00000001);
      internalGetMutableVal().getMutableMap()
          .clear();
      return this;
    }
    /**
     * <code>map&lt;string, .buf.validate.conformance.cases.other_package.Embed.Enumerated&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
     */
    public Builder removeVal(
        java.lang.String key) {
      if (key == null) { throw new NullPointerException("map key"); }
      internalGetMutableVal().getMutableMap()
          .remove(key);
      return this;
    }
    /**
     * Use alternate mutation accessors instead.
     */
    @java.lang.Deprecated
    public java.util.Map<java.lang.String, build.buf.validate.conformance.cases.other_package.Embed.Enumerated>
        getMutableVal() {
      bitField0_ |= 0x00000001;
      return internalGetAdaptedValMap(
           internalGetMutableVal().getMutableMap());
    }
    /**
     * <code>map&lt;string, .buf.validate.conformance.cases.other_package.Embed.Enumerated&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
     */
    public Builder putVal(
        java.lang.String key,
        build.buf.validate.conformance.cases.other_package.Embed.Enumerated value) {
      if (key == null) { throw new NullPointerException("map key"); }

      internalGetMutableVal().getMutableMap()
          .put(key, valValueConverter.doBackward(value));
      bitField0_ |= 0x00000001;
      return this;
    }
    /**
     * <code>map&lt;string, .buf.validate.conformance.cases.other_package.Embed.Enumerated&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
     */
    public Builder putAllVal(
        java.util.Map<java.lang.String, build.buf.validate.conformance.cases.other_package.Embed.Enumerated> values) {
      internalGetAdaptedValMap(
          internalGetMutableVal().getMutableMap())
              .putAll(values);
      bitField0_ |= 0x00000001;
      return this;
    }
    /**
     * Use alternate mutation accessors instead.
     */
    @java.lang.Deprecated
    public java.util.Map<java.lang.String, java.lang.Integer>
    getMutableValValue() {
      bitField0_ |= 0x00000001;
      return internalGetMutableVal().getMutableMap();
    }
    /**
     * <code>map&lt;string, .buf.validate.conformance.cases.other_package.Embed.Enumerated&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
     */
    public Builder putValValue(
        java.lang.String key,
        int value) {
      if (key == null) { throw new NullPointerException("map key"); }

      internalGetMutableVal().getMutableMap()
          .put(key, value);
      bitField0_ |= 0x00000001;
      return this;
    }
    /**
     * <code>map&lt;string, .buf.validate.conformance.cases.other_package.Embed.Enumerated&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
     */
    public Builder putAllValValue(
        java.util.Map<java.lang.String, java.lang.Integer> values) {
      internalGetMutableVal().getMutableMap()
          .putAll(values);
      bitField0_ |= 0x00000001;
      return this;
    }

    // @@protoc_insertion_point(builder_scope:buf.validate.conformance.cases.MapExternalEnumDefined)
  }

  // @@protoc_insertion_point(class_scope:buf.validate.conformance.cases.MapExternalEnumDefined)
  private static final build.buf.validate.conformance.cases.MapExternalEnumDefined DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new build.buf.validate.conformance.cases.MapExternalEnumDefined();
  }

  public static build.buf.validate.conformance.cases.MapExternalEnumDefined getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<MapExternalEnumDefined>
      PARSER = new com.google.protobuf.AbstractParser<MapExternalEnumDefined>() {
    @java.lang.Override
    public MapExternalEnumDefined parsePartialFrom(
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

  public static com.google.protobuf.Parser<MapExternalEnumDefined> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<MapExternalEnumDefined> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public build.buf.validate.conformance.cases.MapExternalEnumDefined getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

