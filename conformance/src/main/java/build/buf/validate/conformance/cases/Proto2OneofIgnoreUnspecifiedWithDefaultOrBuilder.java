// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: buf/validate/conformance/cases/ignore_proto2.proto

package build.buf.validate.conformance.cases;

public interface Proto2OneofIgnoreUnspecifiedWithDefaultOrBuilder extends
    // @@protoc_insertion_point(interface_extends:buf.validate.conformance.cases.Proto2OneofIgnoreUnspecifiedWithDefault)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>int32 val = 1 [default = -42, json_name = "val", (.buf.validate.field) = { ... }</code>
   * @return Whether the val field is set.
   */
  boolean hasVal();
  /**
   * <code>int32 val = 1 [default = -42, json_name = "val", (.buf.validate.field) = { ... }</code>
   * @return The val.
   */
  int getVal();

  build.buf.validate.conformance.cases.Proto2OneofIgnoreUnspecifiedWithDefault.OCase getOCase();
}