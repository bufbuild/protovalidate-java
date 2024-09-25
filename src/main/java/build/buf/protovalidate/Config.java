// Copyright 2023-2024 Buf Technologies, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package build.buf.protovalidate;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.TypeRegistry;

/** Config is the configuration for a Validator. */
public final class Config {
  private static final TypeRegistry DEFAULT_TYPE_REGISTRY = TypeRegistry.getEmptyTypeRegistry();
  private static final ExtensionRegistry DEFAULT_EXTENSION_REGISTRY =
      ExtensionRegistry.getEmptyRegistry();

  private final boolean failFast;
  private final boolean disableLazy;
  private final TypeRegistry typeRegistry;
  private final ExtensionRegistry extensionRegistry;
  private final boolean allowUnknownFields;

  private Config(
      boolean failFast,
      boolean disableLazy,
      TypeRegistry typeRegistry,
      ExtensionRegistry extensionRegistry,
      boolean allowUnknownFields) {
    this.failFast = failFast;
    this.disableLazy = disableLazy;
    this.typeRegistry = typeRegistry;
    this.extensionRegistry = extensionRegistry;
    this.allowUnknownFields = allowUnknownFields;
  }

  /**
   * Create a new Configuration builder.
   *
   * @return a new Configuration builder.
   */
  public static Builder newBuilder() {
    return new Builder();
  }

  /**
   * Checks if the configuration for failing fast is enabled.
   *
   * @return if failing fast is enabled
   */
  public boolean isFailFast() {
    return failFast;
  }

  /**
   * Checks if the configuration for disabling lazy evaluation is enabled.
   *
   * @return if disabling lazy evaluation is enabled
   */
  public boolean isDisableLazy() {
    return disableLazy;
  }

  /**
   * Gets the type registry used for reparsing protobuf messages.
   *
   * @return a type registry
   */
  public TypeRegistry getTypeRegistry() {
    return typeRegistry;
  }

  /**
   * Gets the extension registry used for resolving unknown protobuf extensions.
   *
   * @return an extension registry
   */
  public ExtensionRegistry getExtensionRegistry() {
    return extensionRegistry;
  }

  /**
   * Checks if the configuration for allowing unknown constraint fields is enabled.
   *
   * @return if allowing unknown constraint fields is enabled
   */
  public boolean isAllowingUnknownFields() {
    return allowUnknownFields;
  }

  /** Builder for configuration. Provides a forward compatible API for users. */
  public static final class Builder {
    private boolean failFast;
    private boolean disableLazy;
    private TypeRegistry typeRegistry = DEFAULT_TYPE_REGISTRY;
    private ExtensionRegistry extensionRegistry = DEFAULT_EXTENSION_REGISTRY;
    private boolean allowUnknownFields;

    private Builder() {}

    /**
     * Set the configuration for failing fast.
     *
     * @param failFast the boolean for enabling
     * @return this builder
     */
    public Builder setFailFast(boolean failFast) {
      this.failFast = failFast;
      return this;
    }

    /**
     * Set the configuration for disabling lazy evaluation.
     *
     * @param disableLazy the boolean for enabling
     * @return this builder
     */
    public Builder setDisableLazy(boolean disableLazy) {
      this.disableLazy = disableLazy;
      return this;
    }

    /**
     * Set the type registry for reparsing protobuf messages. This option should be set alongside
     * setExtensionRegistry to allow dynamic resolution of predefined rule extensions. It should be
     * set to a TypeRegistry with all the message types from your file descriptor set registered. By
     * default, if any unknown field constraints are found, compilation of the constraints will
     * fail; use setAllowUnknownFields to control this behavior.
     *
     * <p>Note that the message types for any extensions in setExtensionRegistry must be present in
     * the typeRegistry, and have an exactly-equal Descriptor. If the type registry is not set, the
     * extension types in the extension registry must have exactly-equal Descriptor types to the
     * protovalidate built-in messages. If these conditions are not met, extensions will not be
     * resolved as expected. These conditions will be met when constructing a TypeRegistry and
     * ExtensionRegistry using information from the same file descriptor sets.
     *
     * @param typeRegistry the type registry to use
     * @return this builder
     */
    public Builder setTypeRegistry(TypeRegistry typeRegistry) {
      this.typeRegistry = typeRegistry;
      return this;
    }

    /**
     * Set the extension registry for resolving unknown extensions. This option should be set
     * alongside setTypeRegistry to allow dynamic resolution of predefined rule extensions. It
     * should be set to an ExtensionRegistry with all the extension types from your file descriptor
     * set registered. By default, if any unknown field constraints are found, compilation of the
     * constraints will fail; use setAllowUnknownFields to control this behavior.
     *
     * @param extensionRegistry the extension registry to use
     * @return this builder
     */
    public Builder setExtensionRegistry(ExtensionRegistry extensionRegistry) {
      this.extensionRegistry = extensionRegistry;
      return this;
    }

    /**
     * Set whether unknown constraint fields are allowed. If this setting is set to true, unknown
     * standard predefined field constraints and predefined field constraint extensions will be
     * ignored. This setting defaults to false, which will result in a CompilationException being
     * thrown whenever an unknown field constraint is encountered. Setting this to true will cause
     * some field constraints to be ignored; if the descriptor is dynamic, you can instead use
     * setExtensionRegistry to provide dynamic type information that protovalidate can use to
     * resolve the unknown fields.
     *
     * @param allowUnknownFields setting to apply
     * @return this builder
     */
    public Builder setAllowUnknownFields(boolean allowUnknownFields) {
      this.allowUnknownFields = allowUnknownFields;
      return this;
    }

    /**
     * Build the corresponding {@link Config}.
     *
     * @return the configuration.
     */
    public Config build() {
      return new Config(failFast, disableLazy, typeRegistry, extensionRegistry, allowUnknownFields);
    }
  }
}
