// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: buf/validate/conformance/cases/oneofs.proto

package build.buf.validate.conformance.cases;

public final class OneofsProto {
  private OneofsProto() {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }

  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_buf_validate_conformance_cases_TestOneofMsg_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_buf_validate_conformance_cases_TestOneofMsg_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_buf_validate_conformance_cases_OneofNone_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_buf_validate_conformance_cases_OneofNone_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_buf_validate_conformance_cases_Oneof_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_buf_validate_conformance_cases_Oneof_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_buf_validate_conformance_cases_OneofRequired_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_buf_validate_conformance_cases_OneofRequired_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
      internal_static_buf_validate_conformance_cases_OneofIgnoreEmpty_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_buf_validate_conformance_cases_OneofIgnoreEmpty_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }

  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;

  static {
    java.lang.String[] descriptorData = {
      "\n+buf/validate/conformance/cases/oneofs."
          + "proto\022\036buf.validate.conformance.cases\032\033b"
          + "uf/validate/validate.proto\")\n\014TestOneofM"
          + "sg\022\031\n\003val\030\001 \001(\010B\007\272H\004j\002\010\001R\003val\"0\n\tOneofNo"
          + "ne\022\016\n\001x\030\001 \001(\tH\000R\001x\022\016\n\001y\030\002 \001(\005H\000R\001yB\003\n\001o\""
          + "\177\n\005Oneof\022\032\n\001x\030\001 \001(\tB\n\272H\007r\005:\003fooH\000R\001x\022\027\n\001"
          + "y\030\002 \001(\005B\007\272H\004\032\002 \000H\000R\001y\022<\n\001z\030\003 \001(\0132,.buf.v"
          + "alidate.conformance.cases.TestOneofMsgH\000"
          + "R\001zB\003\n\001o\"\240\001\n\rOneofRequired\022\016\n\001x\030\001 \001(\tH\000R"
          + "\001x\022\016\n\001y\030\002 \001(\005H\000R\001y\0224\n\025name_with_undersco"
          + "res\030\003 \001(\005H\000R\023nameWithUnderscores\022-\n\022unde"
          + "r_and_1_number\030\004 \001(\005H\000R\017underAnd1NumberB"
          + "\n\n\001o\022\005\272H\002\010\001\"s\n\020OneofIgnoreEmpty\022\034\n\001x\030\001 \001"
          + "(\tB\014\272H\tr\004\020\003\030\005\320\001\001H\000R\001x\022\034\n\001y\030\002 \001(\014B\014\272H\tz\004\020"
          + "\003\030\005\320\001\001H\000R\001y\022\036\n\001z\030\003 \001(\005B\016\272H\013\032\006\030\200\001(\200\002\320\001\001H\000"
          + "R\001zB\003\n\001oB\317\001\n$build.buf.validate.conforma"
          + "nce.casesB\013OneofsProtoP\001\242\002\004BVCC\252\002\036Buf.Va"
          + "lidate.Conformance.Cases\312\002\036Buf\\Validate\\"
          + "Conformance\\Cases\342\002*Buf\\Validate\\Conform"
          + "ance\\Cases\\GPBMetadata\352\002!Buf::Validate::"
          + "Conformance::Casesb\006proto3"
    };
    descriptor =
        com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(
            descriptorData,
            new com.google.protobuf.Descriptors.FileDescriptor[] {
              build.buf.validate.ValidateProto.getDescriptor(),
            });
    internal_static_buf_validate_conformance_cases_TestOneofMsg_descriptor =
        getDescriptor().getMessageTypes().get(0);
    internal_static_buf_validate_conformance_cases_TestOneofMsg_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_buf_validate_conformance_cases_TestOneofMsg_descriptor,
            new java.lang.String[] {
              "Val",
            });
    internal_static_buf_validate_conformance_cases_OneofNone_descriptor =
        getDescriptor().getMessageTypes().get(1);
    internal_static_buf_validate_conformance_cases_OneofNone_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_buf_validate_conformance_cases_OneofNone_descriptor,
            new java.lang.String[] {
              "X", "Y", "O",
            });
    internal_static_buf_validate_conformance_cases_Oneof_descriptor =
        getDescriptor().getMessageTypes().get(2);
    internal_static_buf_validate_conformance_cases_Oneof_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_buf_validate_conformance_cases_Oneof_descriptor,
            new java.lang.String[] {
              "X", "Y", "Z", "O",
            });
    internal_static_buf_validate_conformance_cases_OneofRequired_descriptor =
        getDescriptor().getMessageTypes().get(3);
    internal_static_buf_validate_conformance_cases_OneofRequired_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_buf_validate_conformance_cases_OneofRequired_descriptor,
            new java.lang.String[] {
              "X", "Y", "NameWithUnderscores", "UnderAnd1Number", "O",
            });
    internal_static_buf_validate_conformance_cases_OneofIgnoreEmpty_descriptor =
        getDescriptor().getMessageTypes().get(4);
    internal_static_buf_validate_conformance_cases_OneofIgnoreEmpty_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_buf_validate_conformance_cases_OneofIgnoreEmpty_descriptor,
            new java.lang.String[] {
              "X", "Y", "Z", "O",
            });
    com.google.protobuf.ExtensionRegistry registry =
        com.google.protobuf.ExtensionRegistry.newInstance();
    registry.add(build.buf.validate.ValidateProto.field);
    registry.add(build.buf.validate.ValidateProto.oneof);
    com.google.protobuf.Descriptors.FileDescriptor.internalUpdateFileDescriptor(
        descriptor, registry);
    build.buf.validate.ValidateProto.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}