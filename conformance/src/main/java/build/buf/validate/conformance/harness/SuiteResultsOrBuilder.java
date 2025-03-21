// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: buf/validate/conformance/harness/results.proto
// Protobuf Java Version: 4.30.1

package build.buf.validate.conformance.harness;

public interface SuiteResultsOrBuilder extends
    // @@protoc_insertion_point(interface_extends:buf.validate.conformance.harness.SuiteResults)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * The suite name.
   * </pre>
   *
   * <code>string name = 1 [json_name = "name"];</code>
   * @return The name.
   */
  java.lang.String getName();
  /**
   * <pre>
   * The suite name.
   * </pre>
   *
   * <code>string name = 1 [json_name = "name"];</code>
   * @return The bytes for name.
   */
  com.google.protobuf.ByteString
      getNameBytes();

  /**
   * <pre>
   * Count of successes.
   * </pre>
   *
   * <code>int32 successes = 2 [json_name = "successes"];</code>
   * @return The successes.
   */
  int getSuccesses();

  /**
   * <pre>
   * Count of failures.
   * </pre>
   *
   * <code>int32 failures = 3 [json_name = "failures"];</code>
   * @return The failures.
   */
  int getFailures();

  /**
   * <pre>
   * List of case results.
   * </pre>
   *
   * <code>repeated .buf.validate.conformance.harness.CaseResult cases = 4 [json_name = "cases"];</code>
   */
  java.util.List<build.buf.validate.conformance.harness.CaseResult> 
      getCasesList();
  /**
   * <pre>
   * List of case results.
   * </pre>
   *
   * <code>repeated .buf.validate.conformance.harness.CaseResult cases = 4 [json_name = "cases"];</code>
   */
  build.buf.validate.conformance.harness.CaseResult getCases(int index);
  /**
   * <pre>
   * List of case results.
   * </pre>
   *
   * <code>repeated .buf.validate.conformance.harness.CaseResult cases = 4 [json_name = "cases"];</code>
   */
  int getCasesCount();
  /**
   * <pre>
   * List of case results.
   * </pre>
   *
   * <code>repeated .buf.validate.conformance.harness.CaseResult cases = 4 [json_name = "cases"];</code>
   */
  java.util.List<? extends build.buf.validate.conformance.harness.CaseResultOrBuilder> 
      getCasesOrBuilderList();
  /**
   * <pre>
   * List of case results.
   * </pre>
   *
   * <code>repeated .buf.validate.conformance.harness.CaseResult cases = 4 [json_name = "cases"];</code>
   */
  build.buf.validate.conformance.harness.CaseResultOrBuilder getCasesOrBuilder(
      int index);

  /**
   * <pre>
   * The file descriptor set used to generate this result.
   * </pre>
   *
   * <code>.google.protobuf.FileDescriptorSet fdset = 5 [json_name = "fdset"];</code>
   * @return Whether the fdset field is set.
   */
  boolean hasFdset();
  /**
   * <pre>
   * The file descriptor set used to generate this result.
   * </pre>
   *
   * <code>.google.protobuf.FileDescriptorSet fdset = 5 [json_name = "fdset"];</code>
   * @return The fdset.
   */
  com.google.protobuf.DescriptorProtos.FileDescriptorSet getFdset();
  /**
   * <pre>
   * The file descriptor set used to generate this result.
   * </pre>
   *
   * <code>.google.protobuf.FileDescriptorSet fdset = 5 [json_name = "fdset"];</code>
   */
  com.google.protobuf.DescriptorProtos.FileDescriptorSetOrBuilder getFdsetOrBuilder();

  /**
   * <pre>
   * Count of expected failures.
   * </pre>
   *
   * <code>int32 expected_failures = 6 [json_name = "expectedFailures"];</code>
   * @return The expectedFailures.
   */
  int getExpectedFailures();
}
