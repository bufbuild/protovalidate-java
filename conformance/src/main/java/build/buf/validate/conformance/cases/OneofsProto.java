// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: buf/validate/conformance/cases/oneofs.proto
// Protobuf Java Version: 4.30.1

package build.buf.validate.conformance.cases;

public final class OneofsProto {
  private OneofsProto() {}
  static {
    com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(
      com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,
      /* major= */ 4,
      /* minor= */ 30,
      /* patch= */ 1,
      /* suffix= */ "",
      OneofsProto.class.getName());
  }
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_buf_validate_conformance_cases_TestOneofMsg_descriptor;
  static final 
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_buf_validate_conformance_cases_TestOneofMsg_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_buf_validate_conformance_cases_OneofNone_descriptor;
  static final 
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_buf_validate_conformance_cases_OneofNone_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_buf_validate_conformance_cases_Oneof_descriptor;
  static final 
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_buf_validate_conformance_cases_Oneof_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_buf_validate_conformance_cases_OneofRequired_descriptor;
  static final 
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_buf_validate_conformance_cases_OneofRequired_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_buf_validate_conformance_cases_OneofRequiredWithRequiredField_descriptor;
  static final 
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_buf_validate_conformance_cases_OneofRequiredWithRequiredField_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n+buf/validate/conformance/cases/oneofs." +
      "proto\022\036buf.validate.conformance.cases\032\033b" +
      "uf/validate/validate.proto\")\n\014TestOneofM" +
      "sg\022\031\n\003val\030\001 \001(\010B\007\272H\004j\002\010\001R\003val\"0\n\tOneofNo" +
      "ne\022\016\n\001x\030\001 \001(\tH\000R\001x\022\016\n\001y\030\002 \001(\005H\000R\001yB\003\n\001o\"" +
      "\177\n\005Oneof\022\032\n\001x\030\001 \001(\tB\n\272H\007r\005:\003fooH\000R\001x\022\027\n\001" +
      "y\030\002 \001(\005B\007\272H\004\032\002 \000H\000R\001y\022<\n\001z\030\003 \001(\0132,.buf.v" +
      "alidate.conformance.cases.TestOneofMsgH\000" +
      "R\001zB\003\n\001o\"\240\001\n\rOneofRequired\022\016\n\001x\030\001 \001(\tH\000R" +
      "\001x\022\016\n\001y\030\002 \001(\005H\000R\001y\0224\n\025name_with_undersco" +
      "res\030\003 \001(\005H\000R\023nameWithUnderscores\022-\n\022unde" +
      "r_and_1_number\030\004 \001(\005H\000R\017underAnd1NumberB" +
      "\n\n\001o\022\005\272H\002\010\001\"T\n\036OneofRequiredWithRequired" +
      "Field\022\026\n\001a\030\001 \001(\tB\006\272H\003\310\001\001H\000R\001a\022\016\n\001b\030\002 \001(\t" +
      "H\000R\001bB\n\n\001o\022\005\272H\002\010\001B\317\001\n$build.buf.validate" +
      ".conformance.casesB\013OneofsProtoP\001\242\002\004BVCC" +
      "\252\002\036Buf.Validate.Conformance.Cases\312\002\036Buf\\" +
      "Validate\\Conformance\\Cases\342\002*Buf\\Validat" +
      "e\\Conformance\\Cases\\GPBMetadata\352\002!Buf::V" +
      "alidate::Conformance::Casesb\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          build.buf.validate.ValidateProto.getDescriptor(),
        });
    internal_static_buf_validate_conformance_cases_TestOneofMsg_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_buf_validate_conformance_cases_TestOneofMsg_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessage.FieldAccessorTable(
        internal_static_buf_validate_conformance_cases_TestOneofMsg_descriptor,
        new java.lang.String[] { "Val", });
    internal_static_buf_validate_conformance_cases_OneofNone_descriptor =
      getDescriptor().getMessageTypes().get(1);
    internal_static_buf_validate_conformance_cases_OneofNone_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessage.FieldAccessorTable(
        internal_static_buf_validate_conformance_cases_OneofNone_descriptor,
        new java.lang.String[] { "X", "Y", "O", });
    internal_static_buf_validate_conformance_cases_Oneof_descriptor =
      getDescriptor().getMessageTypes().get(2);
    internal_static_buf_validate_conformance_cases_Oneof_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessage.FieldAccessorTable(
        internal_static_buf_validate_conformance_cases_Oneof_descriptor,
        new java.lang.String[] { "X", "Y", "Z", "O", });
    internal_static_buf_validate_conformance_cases_OneofRequired_descriptor =
      getDescriptor().getMessageTypes().get(3);
    internal_static_buf_validate_conformance_cases_OneofRequired_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessage.FieldAccessorTable(
        internal_static_buf_validate_conformance_cases_OneofRequired_descriptor,
        new java.lang.String[] { "X", "Y", "NameWithUnderscores", "UnderAnd1Number", "O", });
    internal_static_buf_validate_conformance_cases_OneofRequiredWithRequiredField_descriptor =
      getDescriptor().getMessageTypes().get(4);
    internal_static_buf_validate_conformance_cases_OneofRequiredWithRequiredField_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessage.FieldAccessorTable(
        internal_static_buf_validate_conformance_cases_OneofRequiredWithRequiredField_descriptor,
        new java.lang.String[] { "A", "B", "O", });
    descriptor.resolveAllFeaturesImmutable();
    build.buf.validate.ValidateProto.getDescriptor();
    com.google.protobuf.ExtensionRegistry registry =
        com.google.protobuf.ExtensionRegistry.newInstance();
    registry.add(build.buf.validate.ValidateProto.field);
    registry.add(build.buf.validate.ValidateProto.oneof);
    com.google.protobuf.Descriptors.FileDescriptor
        .internalUpdateFileDescriptor(descriptor, registry);
  }

  // @@protoc_insertion_point(outer_class_scope)
}
