// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: simple.proto

package build.buf.validate.java;

public final class SimpleProto {
  private SimpleProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_buf_validate_java_Simple_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_buf_validate_java_Simple_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_buf_validate_java_AnotherSimple_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_buf_validate_java_AnotherSimple_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_buf_validate_java_AnotherMessage_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_buf_validate_java_AnotherMessage_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\014simple.proto\022\021buf.validate.java\032\033buf/v" +
      "alidate/validate.proto\"K\n\006Simple\022\014\n\001a\030\001 " +
      "\001(\005R\001a:3\372\367\030/\032-\n\010simple.a\022\024simple.a is no" +
      "n-zero\032\013this.a != 0\"D\n\rAnotherSimple\0223\n\003" +
      "msg\030\001 \001(\0132!.buf.validate.java.AnotherMes" +
      "sageR\003msg\",\n\016AnotherMessage\022\032\n\003str\030\001 \001(\t" +
      "B\010\372\367\030\004r\002\020\001R\003strB\214\001\n\027build.buf.validate.j" +
      "avaB\013SimpleProtoP\001\242\002\003BVJ\252\002\021Buf.Validate." +
      "Java\312\002\021Buf\\Validate\\Java\342\002\035Buf\\Validate\\" +
      "Java\\GPBMetadata\352\002\023Buf::Validate::Javab\006" +
      "proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          build.buf.validate.ValidateProto.getDescriptor(),
        });
    internal_static_buf_validate_java_Simple_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_buf_validate_java_Simple_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_buf_validate_java_Simple_descriptor,
        new java.lang.String[] { "A", });
    internal_static_buf_validate_java_AnotherSimple_descriptor =
      getDescriptor().getMessageTypes().get(1);
    internal_static_buf_validate_java_AnotherSimple_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_buf_validate_java_AnotherSimple_descriptor,
        new java.lang.String[] { "Msg", });
    internal_static_buf_validate_java_AnotherMessage_descriptor =
      getDescriptor().getMessageTypes().get(2);
    internal_static_buf_validate_java_AnotherMessage_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_buf_validate_java_AnotherMessage_descriptor,
        new java.lang.String[] { "Str", });
    com.google.protobuf.ExtensionRegistry registry =
        com.google.protobuf.ExtensionRegistry.newInstance();
    registry.add(build.buf.validate.ValidateProto.field);
    registry.add(build.buf.validate.ValidateProto.message);
    com.google.protobuf.Descriptors.FileDescriptor
        .internalUpdateFileDescriptor(descriptor, registry);
    build.buf.validate.ValidateProto.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}