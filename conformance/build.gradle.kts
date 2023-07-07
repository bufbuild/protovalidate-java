plugins {
    java
}

tasks {
    jar {
        manifest {
            attributes(mapOf("Main-Class" to "build.buf.Main"))
        }
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        // This line of code recursively collects and copies all of a project's files
        // and adds them to the JAR itself. One can extend this task, to skip certain
        // files or particular types at will
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) } +
                sourcesMain.output
        from(contents)
    }
}

repositories {
    mavenCentral()
    maven {
        name = "buf"
        url = uri("https://buf.build/gen/maven")
    }
}

dependencies {
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.java.util)
    implementation(project(":"))
    implementation("build.buf.gen:bufbuild_protovalidate_protocolbuffers_java:23.3.0.1.20230704214709.336f5dd89681")
    implementation("build.buf.gen:bufbuild_protovalidate-testing_protocolbuffers_java:23.3.0.1.20230704214710.61d5e0152a75")
}
