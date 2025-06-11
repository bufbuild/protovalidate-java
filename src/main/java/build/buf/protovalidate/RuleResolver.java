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
import build.buf.validate.FieldRules;
import build.buf.validate.MessageRules;
import build.buf.validate.OneofRules;
import build.buf.validate.ValidateProto;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;

/** Manages the resolution of protovalidate rules. */
final class RuleResolver {
  private static final ExtensionRegistry EXTENSION_REGISTRY = ExtensionRegistry.newInstance();

  static {
    EXTENSION_REGISTRY.add(ValidateProto.message);
    EXTENSION_REGISTRY.add(ValidateProto.oneof);
    EXTENSION_REGISTRY.add(ValidateProto.field);
  }

  /**
   * Resolves the rules for a message descriptor.
   *
   * @param desc the message descriptor.
   * @return the resolved {@link MessageRules}.
   */
  MessageRules resolveMessageRules(Descriptor desc)
      throws InvalidProtocolBufferException, CompilationException {
    DescriptorProtos.MessageOptions options = desc.getOptions();
    // If the protovalidate message extension is unknown, reparse using extension registry.
    if (options.getUnknownFields().hasField(ValidateProto.message.getNumber())) {
      options =
          DescriptorProtos.MessageOptions.parseFrom(options.toByteString(), EXTENSION_REGISTRY);
    }
    if (!options.hasExtension(ValidateProto.message)) {
      return MessageRules.getDefaultInstance();
    }
    // Don't use getExtension here to avoid exception if descriptor types don't match.
    // This can occur if the extension is generated to a different Java package.
    Object value = options.getField(ValidateProto.message.getDescriptor());
    if (value instanceof MessageRules) {
      return ((MessageRules) value);
    }
    if (value instanceof MessageLite) {
      // Possible that this represents the same rule type, just generated to a different
      // java_package.
      return MessageRules.parseFrom(((MessageLite) value).toByteString());
    }
    throw new CompilationException("unexpected message rule option type: " + value);
  }

  /**
   * Resolves the rules for a oneof descriptor.
   *
   * @param desc the oneof descriptor.
   * @return the resolved {@link OneofRules}.
   */
  OneofRules resolveOneofRules(OneofDescriptor desc)
      throws InvalidProtocolBufferException, CompilationException {
    DescriptorProtos.OneofOptions options = desc.getOptions();
    // If the protovalidate oneof extension is unknown, reparse using extension registry.
    if (options.getUnknownFields().hasField(ValidateProto.oneof.getNumber())) {
      options = DescriptorProtos.OneofOptions.parseFrom(options.toByteString(), EXTENSION_REGISTRY);
    }
    if (!options.hasExtension(ValidateProto.oneof)) {
      return OneofRules.getDefaultInstance();
    }
    // Don't use getExtension here to avoid exception if descriptor types don't match.
    // This can occur if the extension is generated to a different Java package.
    Object value = options.getField(ValidateProto.oneof.getDescriptor());
    if (value instanceof OneofRules) {
      return ((OneofRules) value);
    }
    if (value instanceof MessageLite) {
      // Possible that this represents the same rule type, just generated to a different
      // java_package.
      return OneofRules.parseFrom(((MessageLite) value).toByteString());
    }
    throw new CompilationException("unexpected oneof rule option type: " + value);
  }

  /**
   * Resolves the rules for a field descriptor.
   *
   * @param desc the field descriptor.
   * @return the resolved {@link FieldRules}.
   */
  FieldRules resolveFieldRules(FieldDescriptor desc)
      throws InvalidProtocolBufferException, CompilationException {
    DescriptorProtos.FieldOptions options = desc.getOptions();
    // If the protovalidate field option is unknown, reparse using extension registry.
    if (options.getUnknownFields().hasField(ValidateProto.field.getNumber())) {
      options = DescriptorProtos.FieldOptions.parseFrom(options.toByteString(), EXTENSION_REGISTRY);
    }
    if (!options.hasExtension(ValidateProto.field)) {
      return FieldRules.getDefaultInstance();
    }
    // Don't use getExtension here to avoid exception if descriptor types don't match.
    // This can occur if the extension is generated to a different Java package.
    Object value = options.getField(ValidateProto.field.getDescriptor());
    if (value instanceof FieldRules) {
      return ((FieldRules) value);
    }
    if (value instanceof MessageLite) {
      // Possible that this represents the same rule type, just generated to a different
      // java_package.
      return FieldRules.parseFrom(((MessageLite) value).toByteString());
    }
    throw new CompilationException("unexpected field rule option type: " + value);
  }
}
