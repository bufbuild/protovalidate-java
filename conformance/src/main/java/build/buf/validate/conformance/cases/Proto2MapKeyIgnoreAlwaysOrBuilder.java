// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: buf/validate/conformance/cases/ignore_proto2.proto
// Protobuf Java Version: 4.30.1

package build.buf.validate.conformance.cases;

public interface Proto2MapKeyIgnoreAlwaysOrBuilder extends
    // @@protoc_insertion_point(interface_extends:buf.validate.conformance.cases.Proto2MapKeyIgnoreAlways)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>map&lt;int32, int32&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  int getValCount();
  /**
   * <code>map&lt;int32, int32&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  boolean containsVal(
      int key);
  /**
   * Use {@link #getValMap()} instead.
   */
  @java.lang.Deprecated
  java.util.Map<java.lang.Integer, java.lang.Integer>
  getVal();
  /**
   * <code>map&lt;int32, int32&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  java.util.Map<java.lang.Integer, java.lang.Integer>
  getValMap();
  /**
   * <code>map&lt;int32, int32&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  int getValOrDefault(
      int key,
      int defaultValue);
  /**
   * <code>map&lt;int32, int32&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  int getValOrThrow(
      int key);
}
