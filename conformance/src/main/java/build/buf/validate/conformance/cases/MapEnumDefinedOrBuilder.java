// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: buf/validate/conformance/cases/enums.proto
// Protobuf Java Version: 4.30.1

package build.buf.validate.conformance.cases;

public interface MapEnumDefinedOrBuilder extends
    // @@protoc_insertion_point(interface_extends:buf.validate.conformance.cases.MapEnumDefined)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>map&lt;string, .buf.validate.conformance.cases.TestEnum&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  int getValCount();
  /**
   * <code>map&lt;string, .buf.validate.conformance.cases.TestEnum&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  boolean containsVal(
      java.lang.String key);
  /**
   * Use {@link #getValMap()} instead.
   */
  @java.lang.Deprecated
  java.util.Map<java.lang.String, build.buf.validate.conformance.cases.TestEnum>
  getVal();
  /**
   * <code>map&lt;string, .buf.validate.conformance.cases.TestEnum&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  java.util.Map<java.lang.String, build.buf.validate.conformance.cases.TestEnum>
  getValMap();
  /**
   * <code>map&lt;string, .buf.validate.conformance.cases.TestEnum&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  /* nullable */
build.buf.validate.conformance.cases.TestEnum getValOrDefault(
      java.lang.String key,
      /* nullable */
build.buf.validate.conformance.cases.TestEnum         defaultValue);
  /**
   * <code>map&lt;string, .buf.validate.conformance.cases.TestEnum&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  build.buf.validate.conformance.cases.TestEnum getValOrThrow(
      java.lang.String key);
  /**
   * Use {@link #getValValueMap()} instead.
   */
  @java.lang.Deprecated
  java.util.Map<java.lang.String, java.lang.Integer>
  getValValue();
  /**
   * <code>map&lt;string, .buf.validate.conformance.cases.TestEnum&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  java.util.Map<java.lang.String, java.lang.Integer>
  getValValueMap();
  /**
   * <code>map&lt;string, .buf.validate.conformance.cases.TestEnum&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  int getValValueOrDefault(
      java.lang.String key,
      int defaultValue);
  /**
   * <code>map&lt;string, .buf.validate.conformance.cases.TestEnum&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  int getValValueOrThrow(
      java.lang.String key);
}
