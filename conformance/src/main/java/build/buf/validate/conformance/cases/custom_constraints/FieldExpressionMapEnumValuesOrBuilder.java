// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: buf/validate/conformance/cases/custom_constraints/custom_constraints.proto
// Protobuf Java Version: 4.30.1

package build.buf.validate.conformance.cases.custom_constraints;

public interface FieldExpressionMapEnumValuesOrBuilder extends
    // @@protoc_insertion_point(interface_extends:buf.validate.conformance.cases.custom_constraints.FieldExpressionMapEnumValues)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>map&lt;int32, .buf.validate.conformance.cases.custom_constraints.Enum&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  int getValCount();
  /**
   * <code>map&lt;int32, .buf.validate.conformance.cases.custom_constraints.Enum&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  boolean containsVal(
      int key);
  /**
   * Use {@link #getValMap()} instead.
   */
  @java.lang.Deprecated
  java.util.Map<java.lang.Integer, build.buf.validate.conformance.cases.custom_constraints.Enum>
  getVal();
  /**
   * <code>map&lt;int32, .buf.validate.conformance.cases.custom_constraints.Enum&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  java.util.Map<java.lang.Integer, build.buf.validate.conformance.cases.custom_constraints.Enum>
  getValMap();
  /**
   * <code>map&lt;int32, .buf.validate.conformance.cases.custom_constraints.Enum&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  /* nullable */
build.buf.validate.conformance.cases.custom_constraints.Enum getValOrDefault(
      int key,
      /* nullable */
build.buf.validate.conformance.cases.custom_constraints.Enum         defaultValue);
  /**
   * <code>map&lt;int32, .buf.validate.conformance.cases.custom_constraints.Enum&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  build.buf.validate.conformance.cases.custom_constraints.Enum getValOrThrow(
      int key);
  /**
   * Use {@link #getValValueMap()} instead.
   */
  @java.lang.Deprecated
  java.util.Map<java.lang.Integer, java.lang.Integer>
  getValValue();
  /**
   * <code>map&lt;int32, .buf.validate.conformance.cases.custom_constraints.Enum&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  java.util.Map<java.lang.Integer, java.lang.Integer>
  getValValueMap();
  /**
   * <code>map&lt;int32, .buf.validate.conformance.cases.custom_constraints.Enum&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  int getValValueOrDefault(
      int key,
      int defaultValue);
  /**
   * <code>map&lt;int32, .buf.validate.conformance.cases.custom_constraints.Enum&gt; val = 1 [json_name = "val", (.buf.validate.field) = { ... }</code>
   */
  int getValValueOrThrow(
      int key);
}
