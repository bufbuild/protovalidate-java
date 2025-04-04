// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: buf/validate/validate.proto
// Protobuf Java Version: 4.30.1

package build.buf.validate;

public interface DoubleRulesOrBuilder extends
    // @@protoc_insertion_point(interface_extends:buf.validate.DoubleRules)
    com.google.protobuf.GeneratedMessage.
        ExtendableMessageOrBuilder<DoubleRules> {

  /**
   * <pre>
   * `const` requires the field value to exactly match the specified value. If
   * the field value doesn't match, an error message is generated.
   *
   * ```proto
   * message MyDouble {
   * // value must equal 42.0
   * double value = 1 [(buf.validate.field).double.const = 42.0];
   * }
   * ```
   * </pre>
   *
   * <code>optional double const = 1 [json_name = "const", (.buf.validate.predefined) = { ... }</code>
   * @return Whether the const field is set.
   */
  boolean hasConst();
  /**
   * <pre>
   * `const` requires the field value to exactly match the specified value. If
   * the field value doesn't match, an error message is generated.
   *
   * ```proto
   * message MyDouble {
   * // value must equal 42.0
   * double value = 1 [(buf.validate.field).double.const = 42.0];
   * }
   * ```
   * </pre>
   *
   * <code>optional double const = 1 [json_name = "const", (.buf.validate.predefined) = { ... }</code>
   * @return The const.
   */
  double getConst();

  /**
   * <pre>
   * `lt` requires the field value to be less than the specified value (field &lt;
   * value). If the field value is equal to or greater than the specified
   * value, an error message is generated.
   *
   * ```proto
   * message MyDouble {
   * // value must be less than 10.0
   * double value = 1 [(buf.validate.field).double.lt = 10.0];
   * }
   * ```
   * </pre>
   *
   * <code>double lt = 2 [json_name = "lt", (.buf.validate.predefined) = { ... }</code>
   * @return Whether the lt field is set.
   */
  boolean hasLt();
  /**
   * <pre>
   * `lt` requires the field value to be less than the specified value (field &lt;
   * value). If the field value is equal to or greater than the specified
   * value, an error message is generated.
   *
   * ```proto
   * message MyDouble {
   * // value must be less than 10.0
   * double value = 1 [(buf.validate.field).double.lt = 10.0];
   * }
   * ```
   * </pre>
   *
   * <code>double lt = 2 [json_name = "lt", (.buf.validate.predefined) = { ... }</code>
   * @return The lt.
   */
  double getLt();

  /**
   * <pre>
   * `lte` requires the field value to be less than or equal to the specified value
   * (field &lt;= value). If the field value is greater than the specified value,
   * an error message is generated.
   *
   * ```proto
   * message MyDouble {
   * // value must be less than or equal to 10.0
   * double value = 1 [(buf.validate.field).double.lte = 10.0];
   * }
   * ```
   * </pre>
   *
   * <code>double lte = 3 [json_name = "lte", (.buf.validate.predefined) = { ... }</code>
   * @return Whether the lte field is set.
   */
  boolean hasLte();
  /**
   * <pre>
   * `lte` requires the field value to be less than or equal to the specified value
   * (field &lt;= value). If the field value is greater than the specified value,
   * an error message is generated.
   *
   * ```proto
   * message MyDouble {
   * // value must be less than or equal to 10.0
   * double value = 1 [(buf.validate.field).double.lte = 10.0];
   * }
   * ```
   * </pre>
   *
   * <code>double lte = 3 [json_name = "lte", (.buf.validate.predefined) = { ... }</code>
   * @return The lte.
   */
  double getLte();

  /**
   * <pre>
   * `gt` requires the field value to be greater than the specified value
   * (exclusive). If the value of `gt` is larger than a specified `lt` or `lte`,
   * the range is reversed, and the field value must be outside the specified
   * range. If the field value doesn't meet the required conditions, an error
   * message is generated.
   *
   * ```proto
   * message MyDouble {
   * // value must be greater than 5.0 [double.gt]
   * double value = 1 [(buf.validate.field).double.gt = 5.0];
   *
   * // value must be greater than 5 and less than 10.0 [double.gt_lt]
   * double other_value = 2 [(buf.validate.field).double = { gt: 5.0, lt: 10.0 }];
   *
   * // value must be greater than 10 or less than 5.0 [double.gt_lt_exclusive]
   * double another_value = 3 [(buf.validate.field).double = { gt: 10.0, lt: 5.0 }];
   * }
   * ```
   * </pre>
   *
   * <code>double gt = 4 [json_name = "gt", (.buf.validate.predefined) = { ... }</code>
   * @return Whether the gt field is set.
   */
  boolean hasGt();
  /**
   * <pre>
   * `gt` requires the field value to be greater than the specified value
   * (exclusive). If the value of `gt` is larger than a specified `lt` or `lte`,
   * the range is reversed, and the field value must be outside the specified
   * range. If the field value doesn't meet the required conditions, an error
   * message is generated.
   *
   * ```proto
   * message MyDouble {
   * // value must be greater than 5.0 [double.gt]
   * double value = 1 [(buf.validate.field).double.gt = 5.0];
   *
   * // value must be greater than 5 and less than 10.0 [double.gt_lt]
   * double other_value = 2 [(buf.validate.field).double = { gt: 5.0, lt: 10.0 }];
   *
   * // value must be greater than 10 or less than 5.0 [double.gt_lt_exclusive]
   * double another_value = 3 [(buf.validate.field).double = { gt: 10.0, lt: 5.0 }];
   * }
   * ```
   * </pre>
   *
   * <code>double gt = 4 [json_name = "gt", (.buf.validate.predefined) = { ... }</code>
   * @return The gt.
   */
  double getGt();

  /**
   * <pre>
   * `gte` requires the field value to be greater than or equal to the specified
   * value (exclusive). If the value of `gte` is larger than a specified `lt` or
   * `lte`, the range is reversed, and the field value must be outside the
   * specified range. If the field value doesn't meet the required conditions,
   * an error message is generated.
   *
   * ```proto
   * message MyDouble {
   * // value must be greater than or equal to 5.0 [double.gte]
   * double value = 1 [(buf.validate.field).double.gte = 5.0];
   *
   * // value must be greater than or equal to 5.0 and less than 10.0 [double.gte_lt]
   * double other_value = 2 [(buf.validate.field).double = { gte: 5.0, lt: 10.0 }];
   *
   * // value must be greater than or equal to 10.0 or less than 5.0 [double.gte_lt_exclusive]
   * double another_value = 3 [(buf.validate.field).double = { gte: 10.0, lt: 5.0 }];
   * }
   * ```
   * </pre>
   *
   * <code>double gte = 5 [json_name = "gte", (.buf.validate.predefined) = { ... }</code>
   * @return Whether the gte field is set.
   */
  boolean hasGte();
  /**
   * <pre>
   * `gte` requires the field value to be greater than or equal to the specified
   * value (exclusive). If the value of `gte` is larger than a specified `lt` or
   * `lte`, the range is reversed, and the field value must be outside the
   * specified range. If the field value doesn't meet the required conditions,
   * an error message is generated.
   *
   * ```proto
   * message MyDouble {
   * // value must be greater than or equal to 5.0 [double.gte]
   * double value = 1 [(buf.validate.field).double.gte = 5.0];
   *
   * // value must be greater than or equal to 5.0 and less than 10.0 [double.gte_lt]
   * double other_value = 2 [(buf.validate.field).double = { gte: 5.0, lt: 10.0 }];
   *
   * // value must be greater than or equal to 10.0 or less than 5.0 [double.gte_lt_exclusive]
   * double another_value = 3 [(buf.validate.field).double = { gte: 10.0, lt: 5.0 }];
   * }
   * ```
   * </pre>
   *
   * <code>double gte = 5 [json_name = "gte", (.buf.validate.predefined) = { ... }</code>
   * @return The gte.
   */
  double getGte();

  /**
   * <pre>
   * `in` requires the field value to be equal to one of the specified values.
   * If the field value isn't one of the specified values, an error message is
   * generated.
   *
   * ```proto
   * message MyDouble {
   * // value must be in list [1.0, 2.0, 3.0]
   * double value = 1 [(buf.validate.field).double = { in: [1.0, 2.0, 3.0] }];
   * }
   * ```
   * </pre>
   *
   * <code>repeated double in = 6 [json_name = "in", (.buf.validate.predefined) = { ... }</code>
   * @return A list containing the in.
   */
  java.util.List<java.lang.Double> getInList();
  /**
   * <pre>
   * `in` requires the field value to be equal to one of the specified values.
   * If the field value isn't one of the specified values, an error message is
   * generated.
   *
   * ```proto
   * message MyDouble {
   * // value must be in list [1.0, 2.0, 3.0]
   * double value = 1 [(buf.validate.field).double = { in: [1.0, 2.0, 3.0] }];
   * }
   * ```
   * </pre>
   *
   * <code>repeated double in = 6 [json_name = "in", (.buf.validate.predefined) = { ... }</code>
   * @return The count of in.
   */
  int getInCount();
  /**
   * <pre>
   * `in` requires the field value to be equal to one of the specified values.
   * If the field value isn't one of the specified values, an error message is
   * generated.
   *
   * ```proto
   * message MyDouble {
   * // value must be in list [1.0, 2.0, 3.0]
   * double value = 1 [(buf.validate.field).double = { in: [1.0, 2.0, 3.0] }];
   * }
   * ```
   * </pre>
   *
   * <code>repeated double in = 6 [json_name = "in", (.buf.validate.predefined) = { ... }</code>
   * @param index The index of the element to return.
   * @return The in at the given index.
   */
  double getIn(int index);

  /**
   * <pre>
   * `not_in` requires the field value to not be equal to any of the specified
   * values. If the field value is one of the specified values, an error
   * message is generated.
   *
   * ```proto
   * message MyDouble {
   * // value must not be in list [1.0, 2.0, 3.0]
   * double value = 1 [(buf.validate.field).double = { not_in: [1.0, 2.0, 3.0] }];
   * }
   * ```
   * </pre>
   *
   * <code>repeated double not_in = 7 [json_name = "notIn", (.buf.validate.predefined) = { ... }</code>
   * @return A list containing the notIn.
   */
  java.util.List<java.lang.Double> getNotInList();
  /**
   * <pre>
   * `not_in` requires the field value to not be equal to any of the specified
   * values. If the field value is one of the specified values, an error
   * message is generated.
   *
   * ```proto
   * message MyDouble {
   * // value must not be in list [1.0, 2.0, 3.0]
   * double value = 1 [(buf.validate.field).double = { not_in: [1.0, 2.0, 3.0] }];
   * }
   * ```
   * </pre>
   *
   * <code>repeated double not_in = 7 [json_name = "notIn", (.buf.validate.predefined) = { ... }</code>
   * @return The count of notIn.
   */
  int getNotInCount();
  /**
   * <pre>
   * `not_in` requires the field value to not be equal to any of the specified
   * values. If the field value is one of the specified values, an error
   * message is generated.
   *
   * ```proto
   * message MyDouble {
   * // value must not be in list [1.0, 2.0, 3.0]
   * double value = 1 [(buf.validate.field).double = { not_in: [1.0, 2.0, 3.0] }];
   * }
   * ```
   * </pre>
   *
   * <code>repeated double not_in = 7 [json_name = "notIn", (.buf.validate.predefined) = { ... }</code>
   * @param index The index of the element to return.
   * @return The notIn at the given index.
   */
  double getNotIn(int index);

  /**
   * <pre>
   * `finite` requires the field value to be finite. If the field value is
   * infinite or NaN, an error message is generated.
   * </pre>
   *
   * <code>optional bool finite = 8 [json_name = "finite", (.buf.validate.predefined) = { ... }</code>
   * @return Whether the finite field is set.
   */
  boolean hasFinite();
  /**
   * <pre>
   * `finite` requires the field value to be finite. If the field value is
   * infinite or NaN, an error message is generated.
   * </pre>
   *
   * <code>optional bool finite = 8 [json_name = "finite", (.buf.validate.predefined) = { ... }</code>
   * @return The finite.
   */
  boolean getFinite();

  /**
   * <pre>
   * `example` specifies values that the field may have. These values SHOULD
   * conform to other constraints. `example` values will not impact validation
   * but may be used as helpful guidance on how to populate the given field.
   *
   * ```proto
   * message MyDouble {
   * double value = 1 [
   * (buf.validate.field).double.example = 1.0,
   * (buf.validate.field).double.example = "Infinity"
   * ];
   * }
   * ```
   * </pre>
   *
   * <code>repeated double example = 9 [json_name = "example", (.buf.validate.predefined) = { ... }</code>
   * @return A list containing the example.
   */
  java.util.List<java.lang.Double> getExampleList();
  /**
   * <pre>
   * `example` specifies values that the field may have. These values SHOULD
   * conform to other constraints. `example` values will not impact validation
   * but may be used as helpful guidance on how to populate the given field.
   *
   * ```proto
   * message MyDouble {
   * double value = 1 [
   * (buf.validate.field).double.example = 1.0,
   * (buf.validate.field).double.example = "Infinity"
   * ];
   * }
   * ```
   * </pre>
   *
   * <code>repeated double example = 9 [json_name = "example", (.buf.validate.predefined) = { ... }</code>
   * @return The count of example.
   */
  int getExampleCount();
  /**
   * <pre>
   * `example` specifies values that the field may have. These values SHOULD
   * conform to other constraints. `example` values will not impact validation
   * but may be used as helpful guidance on how to populate the given field.
   *
   * ```proto
   * message MyDouble {
   * double value = 1 [
   * (buf.validate.field).double.example = 1.0,
   * (buf.validate.field).double.example = "Infinity"
   * ];
   * }
   * ```
   * </pre>
   *
   * <code>repeated double example = 9 [json_name = "example", (.buf.validate.predefined) = { ... }</code>
   * @param index The index of the element to return.
   * @return The example at the given index.
   */
  double getExample(int index);

  build.buf.validate.DoubleRules.LessThanCase getLessThanCase();

  build.buf.validate.DoubleRules.GreaterThanCase getGreaterThanCase();
}
