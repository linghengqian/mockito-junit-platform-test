package io.etcd.jetcd.impl;

import com.lingh.TestUtil;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.api.WatchGrpc.WatchImplBase;
import io.etcd.jetcd.api.WatchRequest;
import io.etcd.jetcd.api.WatchResponse;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.test.GrpcServerExtension;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static com.lingh.TestUtil.bytesOf;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SuppressWarnings({"SameParameterValue"})
@ExtendWith(MockitoExtension.class)
public class WatchUnitTest {
    private static final ByteSequence KEY = bytesOf("test_key");
    @RegisterExtension
    public final GrpcServerExtension grpcServerRule = new GrpcServerExtension().directExecutor();
    private Watch watchClient;
    private ExecutorService executor = Executors.newFixedThreadPool(2);
    @Mock
    private StreamObserver<WatchRequest> requestStreamObserverMock;

    private static ArgumentMatcher<WatchRequest> hasCreateKey(ByteSequence key) {
        return o -> Arrays.equals(o.getCreateRequest().getKey().toByteArray(), key.getBytes());
    }

    private static WatchImplBase createWatchImpBase(AtomicReference<StreamObserver<WatchResponse>> responseObserverRef,
                                                    StreamObserver<WatchRequest> requestStreamObserver) {
        return new WatchImplBase() {
            @Override
            public StreamObserver<WatchRequest> watch(StreamObserver<WatchResponse> responseObserver) {
                responseObserverRef.set(responseObserver);
                return requestStreamObserver;
            }
        };
    }

    @BeforeEach
    public void setUp() {
        this.executor = Executors.newSingleThreadExecutor();
        AtomicReference<StreamObserver<WatchResponse>> responseObserverRef = new AtomicReference<>();
        this.grpcServerRule.getServiceRegistry().addService(createWatchImpBase(responseObserverRef, requestStreamObserverMock));
        this.watchClient = new WatchImpl(new ClientConnectionManager(Client.builder(), this.grpcServerRule.getChannel()));
    }

    @AfterEach
    public void tearDown() {
        watchClient.close();
        grpcServerRule.getChannel().shutdownNow();
        executor.shutdownNow();
    }

    @Test
    public void testWatchOnSendingWatchCreateRequest() {
        try (Watch.Watcher ignored = watchClient.watch(
                KEY,
                WatchOption.DEFAULT,
                Watch.listener(TestUtil::noOpWatchResponseConsumer))) {
            verify(this.requestStreamObserverMock, timeout(100).times(1)).onNext(argThat(hasCreateKey(KEY)));
        }
    }
}
