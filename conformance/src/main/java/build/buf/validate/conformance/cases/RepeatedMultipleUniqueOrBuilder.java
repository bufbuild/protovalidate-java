// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: buf/validate/conformance/cases/repeated.proto
// Protobuf Java Version: 4.30.1

package build.buf.validate.conformance.cases;

public interface RepeatedMultipleUniqueOrBuilder extends
    // @@protoc_insertion_point(interface_extends:buf.validate.conformance.cases.RepeatedMultipleUnique)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>repeated string a = 1 [json_name = "a", (.buf.validate.field) = { ... }</code>
   * @return A list containing the a.
   */
  java.util.List<java.lang.String>
      getAList();
  /**
   * <code>repeated string a = 1 [json_name = "a", (.buf.validate.field) = { ... }</code>
   * @return The count of a.
   */
  int getACount();
  /**
   * <code>repeated string a = 1 [json_name = "a", (.buf.validate.field) = { ... }</code>
   * @param index The index of the element to return.
   * @return The a at the given index.
   */
  java.lang.String getA(int index);
  /**
   * <code>repeated string a = 1 [json_name = "a", (.buf.validate.field) = { ... }</code>
   * @param index The index of the value to return.
   * @return The bytes of the a at the given index.
   */
  com.google.protobuf.ByteString
      getABytes(int index);

  /**
   * <code>repeated int32 b = 2 [json_name = "b", (.buf.validate.field) = { ... }</code>
   * @return A list containing the b.
   */
  java.util.List<java.lang.Integer> getBList();
  /**
   * <code>repeated int32 b = 2 [json_name = "b", (.buf.validate.field) = { ... }</code>
   * @return The count of b.
   */
  int getBCount();
  /**
   * <code>repeated int32 b = 2 [json_name = "b", (.buf.validate.field) = { ... }</code>
   * @param index The index of the element to return.
   * @return The b at the given index.
   */
  int getB(int index);
}
