// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: buf/validate/conformance/cases/maps.proto
// Protobuf Java Version: 4.30.1

package build.buf.validate.conformance.cases;

public interface MapRecursiveOrBuilder extends
    // @@protoc_insertion_point(interface_extends:buf.validate.conformance.cases.MapRecursive)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>map&lt;uint32, .buf.validate.conformance.cases.MapRecursive.Msg&gt; val = 1 [json_name = "val"];</code>
   */
  int getValCount();
  /**
   * <code>map&lt;uint32, .buf.validate.conformance.cases.MapRecursive.Msg&gt; val = 1 [json_name = "val"];</code>
   */
  boolean containsVal(
      int key);
  /**
   * Use {@link #getValMap()} instead.
   */
  @java.lang.Deprecated
  java.util.Map<java.lang.Integer, build.buf.validate.conformance.cases.MapRecursive.Msg>
  getVal();
  /**
   * <code>map&lt;uint32, .buf.validate.conformance.cases.MapRecursive.Msg&gt; val = 1 [json_name = "val"];</code>
   */
  java.util.Map<java.lang.Integer, build.buf.validate.conformance.cases.MapRecursive.Msg>
  getValMap();
  /**
   * <code>map&lt;uint32, .buf.validate.conformance.cases.MapRecursive.Msg&gt; val = 1 [json_name = "val"];</code>
   */
  /* nullable */
build.buf.validate.conformance.cases.MapRecursive.Msg getValOrDefault(
      int key,
      /* nullable */
build.buf.validate.conformance.cases.MapRecursive.Msg defaultValue);
  /**
   * <code>map&lt;uint32, .buf.validate.conformance.cases.MapRecursive.Msg&gt; val = 1 [json_name = "val"];</code>
   */
  build.buf.validate.conformance.cases.MapRecursive.Msg getValOrThrow(
      int key);
}
