// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: buf/validate/validate.proto
// Protobuf Java Version: 4.30.1

package build.buf.validate;

public interface OneofConstraintsOrBuilder extends
    // @@protoc_insertion_point(interface_extends:buf.validate.OneofConstraints)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * If `required` is true, exactly one field of the oneof must be present. A
   * validation error is returned if no fields in the oneof are present. The
   * field itself may still be a default value; further constraints
   * should be placed on the fields themselves to ensure they are valid values,
   * such as `min_len` or `gt`.
   *
   * ```proto
   * message MyMessage {
   * oneof value {
   * // Either `a` or `b` must be set. If `a` is set, it must also be
   * // non-empty; whereas if `b` is set, it can still be an empty string.
   * option (buf.validate.oneof).required = true;
   * string a = 1 [(buf.validate.field).string.min_len = 1];
   * string b = 2;
   * }
   * }
   * ```
   * </pre>
   *
   * <code>optional bool required = 1 [json_name = "required"];</code>
   * @return Whether the required field is set.
   */
  boolean hasRequired();
  /**
   * <pre>
   * If `required` is true, exactly one field of the oneof must be present. A
   * validation error is returned if no fields in the oneof are present. The
   * field itself may still be a default value; further constraints
   * should be placed on the fields themselves to ensure they are valid values,
   * such as `min_len` or `gt`.
   *
   * ```proto
   * message MyMessage {
   * oneof value {
   * // Either `a` or `b` must be set. If `a` is set, it must also be
   * // non-empty; whereas if `b` is set, it can still be an empty string.
   * option (buf.validate.oneof).required = true;
   * string a = 1 [(buf.validate.field).string.min_len = 1];
   * string b = 2;
   * }
   * }
   * ```
   * </pre>
   *
   * <code>optional bool required = 1 [json_name = "required"];</code>
   * @return The required.
   */
  boolean getRequired();
}
