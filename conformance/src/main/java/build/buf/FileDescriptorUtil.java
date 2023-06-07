package build.buf;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileDescriptorUtil {

    public static Map<String, Descriptors.Descriptor> parse(DescriptorProtos.FileDescriptorSet fileDescriptorSet) throws Descriptors.DescriptorValidationException {
        Map<String, Descriptors.Descriptor> descriptorMap = new HashMap<>();
        Map<String, Descriptors.FileDescriptor> fileDescriptorMap = parseFileDescriptors(fileDescriptorSet);
        for (Descriptors.FileDescriptor fileDescriptor : fileDescriptorMap.values()) {
            for (Descriptors.Descriptor messageType : fileDescriptor.getMessageTypes()) {
                descriptorMap.put(messageType.getFullName(), messageType);
            }
            for (Descriptors.EnumDescriptor enumType : fileDescriptor.getEnumTypes()) {

            }
        }
        return descriptorMap;
    }
    public static Map<String, Descriptors.FileDescriptor> parseFileDescriptors(DescriptorProtos.FileDescriptorSet fileDescriptorSet) throws Descriptors.DescriptorValidationException {
        Map<String, DescriptorProtos.FileDescriptorProto> fileDescriptorProtoMap = new HashMap<>();
        for (DescriptorProtos.FileDescriptorProto fileDescriptorProto : fileDescriptorSet.getFileList()) {
            fileDescriptorProtoMap.put(fileDescriptorProto.getName(), fileDescriptorProto);
        }
        Map<String, Descriptors.FileDescriptor> fileDescriptorMap = new HashMap<>();
        for (DescriptorProtos.FileDescriptorProto fileDescriptorProto : fileDescriptorSet.getFileList()) {
            if (fileDescriptorProto.getDependencyList().isEmpty()) {
                fileDescriptorMap.put(fileDescriptorProto.getName(), Descriptors.FileDescriptor.buildFrom(fileDescriptorProto, new Descriptors.FileDescriptor[0], false));
                continue;
            }
            List<Descriptors.FileDescriptor> dependencies = new ArrayList<>();
            for (String dependency : fileDescriptorProto.getDependencyList()) {
                if (fileDescriptorMap.get(dependency) != null) {
                    dependencies.add(fileDescriptorMap.get(dependency));
                }
            }
            fileDescriptorMap.put(fileDescriptorProto.getName(), Descriptors.FileDescriptor.buildFrom(fileDescriptorProto, dependencies.toArray(new Descriptors.FileDescriptor[0]), false));
        }
        return fileDescriptorMap;
    }
}
