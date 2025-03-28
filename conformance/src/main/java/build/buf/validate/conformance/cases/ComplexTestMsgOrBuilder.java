// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: buf/validate/conformance/cases/kitchen_sink.proto
// Protobuf Java Version: 4.30.1

package build.buf.validate.conformance.cases;

public interface ComplexTestMsgOrBuilder extends
    // @@protoc_insertion_point(interface_extends:buf.validate.conformance.cases.ComplexTestMsg)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>string const = 1 [json_name = "const", (.buf.validate.field) = { ... }</code>
   * @return The const.
   */
  java.lang.String getConst();
  /**
   * <code>string const = 1 [json_name = "const", (.buf.validate.field) = { ... }</code>
   * @return The bytes for const.
   */
  com.google.protobuf.ByteString
      getConstBytes();

  /**
   * <code>.buf.validate.conformance.cases.ComplexTestMsg nested = 2 [json_name = "nested"];</code>
   * @return Whether the nested field is set.
   */
  boolean hasNested();
  /**
   * <code>.buf.validate.conformance.cases.ComplexTestMsg nested = 2 [json_name = "nested"];</code>
   * @return The nested.
   */
  build.buf.validate.conformance.cases.ComplexTestMsg getNested();
  /**
   * <code>.buf.validate.conformance.cases.ComplexTestMsg nested = 2 [json_name = "nested"];</code>
   */
  build.buf.validate.conformance.cases.ComplexTestMsgOrBuilder getNestedOrBuilder();

  /**
   * <code>int32 int_const = 3 [json_name = "intConst", (.buf.validate.field) = { ... }</code>
   * @return The intConst.
   */
  int getIntConst();

  /**
   * <code>bool bool_const = 4 [json_name = "boolConst", (.buf.validate.field) = { ... }</code>
   * @return The boolConst.
   */
  boolean getBoolConst();

  /**
   * <code>.google.protobuf.FloatValue float_val = 5 [json_name = "floatVal", (.buf.validate.field) = { ... }</code>
   * @return Whether the floatVal field is set.
   */
  boolean hasFloatVal();
  /**
   * <code>.google.protobuf.FloatValue float_val = 5 [json_name = "floatVal", (.buf.validate.field) = { ... }</code>
   * @return The floatVal.
   */
  com.google.protobuf.FloatValue getFloatVal();
  /**
   * <code>.google.protobuf.FloatValue float_val = 5 [json_name = "floatVal", (.buf.validate.field) = { ... }</code>
   */
  com.google.protobuf.FloatValueOrBuilder getFloatValOrBuilder();

  /**
   * <code>.google.protobuf.Duration dur_val = 6 [json_name = "durVal", (.buf.validate.field) = { ... }</code>
   * @return Whether the durVal field is set.
   */
  boolean hasDurVal();
  /**
   * <code>.google.protobuf.Duration dur_val = 6 [json_name = "durVal", (.buf.validate.field) = { ... }</code>
   * @return The durVal.
   */
  com.google.protobuf.Duration getDurVal();
  /**
   * <code>.google.protobuf.Duration dur_val = 6 [json_name = "durVal", (.buf.validate.field) = { ... }</code>
   */
  com.google.protobuf.DurationOrBuilder getDurValOrBuilder();

  /**
   * <code>.google.protobuf.Timestamp ts_val = 7 [json_name = "tsVal", (.buf.validate.field) = { ... }</code>
   * @return Whether the tsVal field is set.
   */
  boolean hasTsVal();
  /**
   * <code>.google.protobuf.Timestamp ts_val = 7 [json_name = "tsVal", (.buf.validate.field) = { ... }</code>
   * @return The tsVal.
   */
  com.google.protobuf.Timestamp getTsVal();
  /**
   * <code>.google.protobuf.Timestamp ts_val = 7 [json_name = "tsVal", (.buf.validate.field) = { ... }</code>
   */
  com.google.protobuf.TimestampOrBuilder getTsValOrBuilder();

  /**
   * <code>.buf.validate.conformance.cases.ComplexTestMsg another = 8 [json_name = "another"];</code>
   * @return Whether the another field is set.
   */
  boolean hasAnother();
  /**
   * <code>.buf.validate.conformance.cases.ComplexTestMsg another = 8 [json_name = "another"];</code>
   * @return The another.
   */
  build.buf.validate.conformance.cases.ComplexTestMsg getAnother();
  /**
   * <code>.buf.validate.conformance.cases.ComplexTestMsg another = 8 [json_name = "another"];</code>
   */
  build.buf.validate.conformance.cases.ComplexTestMsgOrBuilder getAnotherOrBuilder();

  /**
   * <code>float float_const = 9 [json_name = "floatConst", (.buf.validate.field) = { ... }</code>
   * @return The floatConst.
   */
  float getFloatConst();

  /**
   * <code>double double_in = 10 [json_name = "doubleIn", (.buf.validate.field) = { ... }</code>
   * @return The doubleIn.
   */
  double getDoubleIn();

  /**
   * <code>.buf.validate.conformance.cases.ComplexTestEnum enum_const = 11 [json_name = "enumConst", (.buf.validate.field) = { ... }</code>
   * @return The enum numeric value on the wire for enumConst.
   */
  int getEnumConstValue();
  /**
   * <code>.buf.validate.conformance.cases.ComplexTestEnum enum_const = 11 [json_name = "enumConst", (.buf.validate.field) = { ... }</code>
   * @return The enumConst.
   */
  build.buf.validate.conformance.cases.ComplexTestEnum getEnumConst();

  /**
   * <code>.google.protobuf.Any any_val = 12 [json_name = "anyVal", (.buf.validate.field) = { ... }</code>
   * @return Whether the anyVal field is set.
   */
  boolean hasAnyVal();
  /**
   * <code>.google.protobuf.Any any_val = 12 [json_name = "anyVal", (.buf.validate.field) = { ... }</code>
   * @return The anyVal.
   */
  com.google.protobuf.Any getAnyVal();
  /**
   * <code>.google.protobuf.Any any_val = 12 [json_name = "anyVal", (.buf.validate.field) = { ... }</code>
   */
  com.google.protobuf.AnyOrBuilder getAnyValOrBuilder();

  /**
   * <code>repeated .google.protobuf.Timestamp rep_ts_val = 13 [json_name = "repTsVal", (.buf.validate.field) = { ... }</code>
   */
  java.util.List<com.google.protobuf.Timestamp> 
      getRepTsValList();
  /**
   * <code>repeated .google.protobuf.Timestamp rep_ts_val = 13 [json_name = "repTsVal", (.buf.validate.field) = { ... }</code>
   */
  com.google.protobuf.Timestamp getRepTsVal(int index);
  /**
   * <code>repeated .google.protobuf.Timestamp rep_ts_val = 13 [json_name = "repTsVal", (.buf.validate.field) = { ... }</code>
   */
  int getRepTsValCount();
  /**
   * <code>repeated .google.protobuf.Timestamp rep_ts_val = 13 [json_name = "repTsVal", (.buf.validate.field) = { ... }</code>
   */
  java.util.List<? extends com.google.protobuf.TimestampOrBuilder> 
      getRepTsValOrBuilderList();
  /**
   * <code>repeated .google.protobuf.Timestamp rep_ts_val = 13 [json_name = "repTsVal", (.buf.validate.field) = { ... }</code>
   */
  com.google.protobuf.TimestampOrBuilder getRepTsValOrBuilder(
      int index);

  /**
   * <code>map&lt;sint32, string&gt; map_val = 14 [json_name = "mapVal", (.buf.validate.field) = { ... }</code>
   */
  int getMapValCount();
  /**
   * <code>map&lt;sint32, string&gt; map_val = 14 [json_name = "mapVal", (.buf.validate.field) = { ... }</code>
   */
  boolean containsMapVal(
      int key);
  /**
   * Use {@link #getMapValMap()} instead.
   */
  @java.lang.Deprecated
  java.util.Map<java.lang.Integer, java.lang.String>
  getMapVal();
  /**
   * <code>map&lt;sint32, string&gt; map_val = 14 [json_name = "mapVal", (.buf.validate.field) = { ... }</code>
   */
  java.util.Map<java.lang.Integer, java.lang.String>
  getMapValMap();
  /**
   * <code>map&lt;sint32, string&gt; map_val = 14 [json_name = "mapVal", (.buf.validate.field) = { ... }</code>
   */
  /* nullable */
java.lang.String getMapValOrDefault(
      int key,
      /* nullable */
java.lang.String defaultValue);
  /**
   * <code>map&lt;sint32, string&gt; map_val = 14 [json_name = "mapVal", (.buf.validate.field) = { ... }</code>
   */
  java.lang.String getMapValOrThrow(
      int key);

  /**
   * <code>bytes bytes_val = 15 [json_name = "bytesVal", (.buf.validate.field) = { ... }</code>
   * @return The bytesVal.
   */
  com.google.protobuf.ByteString getBytesVal();

  /**
   * <code>string x = 16 [json_name = "x"];</code>
   * @return Whether the x field is set.
   */
  boolean hasX();
  /**
   * <code>string x = 16 [json_name = "x"];</code>
   * @return The x.
   */
  java.lang.String getX();
  /**
   * <code>string x = 16 [json_name = "x"];</code>
   * @return The bytes for x.
   */
  com.google.protobuf.ByteString
      getXBytes();

  /**
   * <code>int32 y = 17 [json_name = "y"];</code>
   * @return Whether the y field is set.
   */
  boolean hasY();
  /**
   * <code>int32 y = 17 [json_name = "y"];</code>
   * @return The y.
   */
  int getY();

  build.buf.validate.conformance.cases.ComplexTestMsg.OCase getOCase();
}
