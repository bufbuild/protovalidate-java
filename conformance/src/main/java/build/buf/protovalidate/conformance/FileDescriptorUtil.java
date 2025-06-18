// Copyright 2023-2025 Buf Technologies, Inc.
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

package build.buf.protovalidate.conformance;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.TypeRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class FileDescriptorUtil {
  static Map<String, Descriptors.Descriptor> parse(
      DescriptorProtos.FileDescriptorSet fileDescriptorSet)
      throws Descriptors.DescriptorValidationException {
    Map<String, Descriptors.Descriptor> descriptorMap = new HashMap<>();
    Map<String, Descriptors.FileDescriptor> fileDescriptorMap =
        parseFileDescriptors(fileDescriptorSet);
    for (Descriptors.FileDescriptor fileDescriptor : fileDescriptorMap.values()) {
      for (Descriptors.Descriptor messageType : fileDescriptor.getMessageTypes()) {
        descriptorMap.put(messageType.getFullName(), messageType);
      }
    }
    return descriptorMap;
  }

  static Map<String, Descriptors.FileDescriptor> parseFileDescriptors(
      DescriptorProtos.FileDescriptorSet fileDescriptorSet)
      throws Descriptors.DescriptorValidationException {
    Map<String, DescriptorProtos.FileDescriptorProto> fileDescriptorProtoMap = new HashMap<>();
    for (DescriptorProtos.FileDescriptorProto fileDescriptorProto :
        fileDescriptorSet.getFileList()) {
      if (fileDescriptorProtoMap.containsKey(fileDescriptorProto.getName())) {
        throw new RuntimeException("duplicate files found.");
      }
      fileDescriptorProtoMap.put(fileDescriptorProto.getName(), fileDescriptorProto);
    }
    Map<String, Descriptors.FileDescriptor> fileDescriptorMap = new HashMap<>();
    for (DescriptorProtos.FileDescriptorProto fileDescriptorProto :
        fileDescriptorSet.getFileList()) {
      if (fileDescriptorProto.getDependencyList().isEmpty()) {
        fileDescriptorMap.put(
            fileDescriptorProto.getName(),
            Descriptors.FileDescriptor.buildFrom(
                fileDescriptorProto, new Descriptors.FileDescriptor[0], false));
        continue;
      }
      List<Descriptors.FileDescriptor> dependencies = new ArrayList<>();
      for (String dependency : fileDescriptorProto.getDependencyList()) {
        if (fileDescriptorMap.get(dependency) != null) {
          dependencies.add(fileDescriptorMap.get(dependency));
        }
      }
      fileDescriptorMap.put(
          fileDescriptorProto.getName(),
          Descriptors.FileDescriptor.buildFrom(
              fileDescriptorProto, dependencies.toArray(new Descriptors.FileDescriptor[0]), false));
    }
    return fileDescriptorMap;
  }

  static TypeRegistry createTypeRegistry(
      Iterable<? extends Descriptors.FileDescriptor> fileDescriptors) {
    TypeRegistry.Builder registryBuilder = TypeRegistry.newBuilder();
    for (Descriptors.FileDescriptor fileDescriptor : fileDescriptors) {
      registryBuilder.add(fileDescriptor.getMessageTypes());
    }
    return registryBuilder.build();
  }

  static ExtensionRegistry createExtensionRegistry(
      Iterable<? extends Descriptors.FileDescriptor> fileDescriptors) {
    ExtensionRegistry registry = ExtensionRegistry.newInstance();
    for (Descriptors.FileDescriptor fileDescriptor : fileDescriptors) {
      registerFileExtensions(registry, fileDescriptor);
    }
    return registry;
  }

  private static void registerFileExtensions(
      ExtensionRegistry registry, Descriptors.FileDescriptor fileDescriptor) {
    registerExtensions(registry, fileDescriptor.getExtensions());
    for (Descriptors.Descriptor descriptor : fileDescriptor.getMessageTypes()) {
      registerMessageExtensions(registry, descriptor);
    }
  }

  private static void registerMessageExtensions(
      ExtensionRegistry registry, Descriptors.Descriptor descriptor) {
    registerExtensions(registry, descriptor.getExtensions());
    for (Descriptors.Descriptor nestedDescriptor : descriptor.getNestedTypes()) {
      registerMessageExtensions(registry, nestedDescriptor);
    }
  }

  private static void registerExtensions(
      ExtensionRegistry registry, List<Descriptors.FieldDescriptor> extensions) {
    for (Descriptors.FieldDescriptor fieldDescriptor : extensions) {
      if (fieldDescriptor.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
        registry.add(
            fieldDescriptor, DynamicMessage.getDefaultInstance(fieldDescriptor.getMessageType()));
      } else {
        registry.add(fieldDescriptor);
      }
    }
  }
}
