package build.buf;


import build.buf.gen.buf.validate.conformance.harness.ConformanceServiceGrpc;
import build.buf.gen.buf.validate.conformance.harness.TestConformanceRequest;
import build.buf.gen.buf.validate.conformance.harness.TestConformanceResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MainTest {
//    @Test
    public void asdf() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 1234)
                .usePlaintext() // Use clear text (no encryption)
                .build();
        ConformanceServiceGrpc.ConformanceServiceBlockingStub stub = ConformanceServiceGrpc.newBlockingStub(channel);
        TestConformanceResponse testConformanceResponse = stub.testConformance(TestConformanceRequest.newBuilder().build());
        assertEquals(testConformanceResponse, testConformanceResponse);
    }

}