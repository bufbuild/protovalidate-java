// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: buf/validate/conformance/cases/strings.proto
// Protobuf Java Version: 4.30.1

package build.buf.validate.conformance.cases;

public interface StringInOneofOrBuilder extends
    // @@protoc_insertion_point(interface_extends:buf.validate.conformance.cases.StringInOneof)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>string bar = 1 [json_name = "bar", (.buf.validate.field) = { ... }</code>
   * @return Whether the bar field is set.
   */
  boolean hasBar();
  /**
   * <code>string bar = 1 [json_name = "bar", (.buf.validate.field) = { ... }</code>
   * @return The bar.
   */
  java.lang.String getBar();
  /**
   * <code>string bar = 1 [json_name = "bar", (.buf.validate.field) = { ... }</code>
   * @return The bytes for bar.
   */
  com.google.protobuf.ByteString
      getBarBytes();

  build.buf.validate.conformance.cases.StringInOneof.FooCase getFooCase();
}
