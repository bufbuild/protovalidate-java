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

import build.buf.protovalidate.exceptions.CompilationException;
import build.buf.validate.FieldConstraints;
import build.buf.validate.MessageConstraints;
import build.buf.validate.OneofConstraints;
import build.buf.validate.ValidateProto;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;

/** Manages the resolution of protovalidate constraints. */
class ConstraintResolver {
  private static final ExtensionRegistry EXTENSION_REGISTRY = ExtensionRegistry.newInstance();

  static {
    EXTENSION_REGISTRY.add(ValidateProto.message);
    EXTENSION_REGISTRY.add(ValidateProto.oneof);
    EXTENSION_REGISTRY.add(ValidateProto.field);
  }

  /**
   * Resolves the constraints for a message descriptor.
   *
   * @param desc the message descriptor.
   * @return the resolved {@link MessageConstraints}.
   */
  MessageConstraints resolveMessageConstraints(Descriptor desc)
      throws InvalidProtocolBufferException, CompilationException {
    DescriptorProtos.MessageOptions options = desc.getOptions();
    // If the protovalidate message extension is unknown, reparse using extension registry.
    if (options.getUnknownFields().hasField(ValidateProto.message.getNumber())) {
      options =
          DescriptorProtos.MessageOptions.parseFrom(options.toByteString(), EXTENSION_REGISTRY);
    }
    if (!options.hasExtension(ValidateProto.message)) {
      return MessageConstraints.getDefaultInstance();
    }
    // Don't use getExtension here to avoid exception if descriptor types don't match.
    // This can occur if the extension is generated to a different Java package.
    Object value = options.getField(ValidateProto.message.getDescriptor());
    if (value instanceof MessageConstraints) {
      return ((MessageConstraints) value);
    }
    if (value instanceof MessageLite) {
      // Possible that this represents the same constraint type, just generated to a different
      // java_package.
      return MessageConstraints.parseFrom(((MessageLite) value).toByteString());
    }
    throw new CompilationException("unexpected message constraint option type: " + value);
  }

  /**
   * Resolves the constraints for a oneof descriptor.
   *
   * @param desc the oneof descriptor.
   * @return the resolved {@link OneofConstraints}.
   */
  OneofConstraints resolveOneofConstraints(OneofDescriptor desc)
      throws InvalidProtocolBufferException, CompilationException {
    DescriptorProtos.OneofOptions options = desc.getOptions();
    // If the protovalidate oneof extension is unknown, reparse using extension registry.
    if (options.getUnknownFields().hasField(ValidateProto.oneof.getNumber())) {
      options = DescriptorProtos.OneofOptions.parseFrom(options.toByteString(), EXTENSION_REGISTRY);
    }
    if (!options.hasExtension(ValidateProto.oneof)) {
      return OneofConstraints.getDefaultInstance();
    }
    // Don't use getExtension here to avoid exception if descriptor types don't match.
    // This can occur if the extension is generated to a different Java package.
    Object value = options.getField(ValidateProto.oneof.getDescriptor());
    if (value instanceof OneofConstraints) {
      return ((OneofConstraints) value);
    }
    if (value instanceof MessageLite) {
      // Possible that this represents the same constraint type, just generated to a different
      // java_package.
      return OneofConstraints.parseFrom(((MessageLite) value).toByteString());
    }
    throw new CompilationException("unexpected oneof constraint option type: " + value);
  }

  /**
   * Resolves the constraints for a field descriptor.
   *
   * @param desc the field descriptor.
   * @return the resolved {@link FieldConstraints}.
   */
  FieldConstraints resolveFieldConstraints(FieldDescriptor desc)
      throws InvalidProtocolBufferException, CompilationException {
    DescriptorProtos.FieldOptions options = desc.getOptions();
    // If the protovalidate field option is unknown, reparse using extension registry.
    if (options.getUnknownFields().hasField(ValidateProto.field.getNumber())) {
      options = DescriptorProtos.FieldOptions.parseFrom(options.toByteString(), EXTENSION_REGISTRY);
    }
    if (!options.hasExtension(ValidateProto.field)) {
      return FieldConstraints.getDefaultInstance();
    }
    // Don't use getExtension here to avoid exception if descriptor types don't match.
    // This can occur if the extension is generated to a different Java package.
    Object value = options.getField(ValidateProto.field.getDescriptor());
    if (value instanceof FieldConstraints) {
      return ((FieldConstraints) value);
    }
    if (value instanceof MessageLite) {
      // Possible that this represents the same constraint type, just generated to a different
      // java_package.
      return FieldConstraints.parseFrom(((MessageLite) value).toByteString());
    }
    throw new CompilationException("unexpected field constraint option type: " + value);
  }
}
