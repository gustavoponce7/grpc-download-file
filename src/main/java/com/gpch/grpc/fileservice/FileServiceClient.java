package com.gpch.grpc.fileservice;

import com.gpch.grpc.protobuf.DataChunk;
import com.gpch.grpc.protobuf.DownloadFileRequest;
import com.gpch.grpc.protobuf.FileServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class FileServiceClient {

    private final FileServiceGrpc.FileServiceBlockingStub blockingStub;
    private final FileServiceGrpc.FileServiceStub nonBlockingStub;

    private ManagedChannel channel;

    @Autowired
    public FileServiceClient(@Value("${fileservice.grpc.host:localhost}") String host, @Value("${fileservice.grpc.port:7000}") int port){
        this(ManagedChannelBuilder.forAddress(host, port)
                .keepAliveTime(60, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .usePlaintext());
    }

    private FileServiceClient(ManagedChannelBuilder channelBuilder){
        channel = channelBuilder.build();
        blockingStub = FileServiceGrpc.newBlockingStub(channel);
        nonBlockingStub = FileServiceGrpc.newStub(channel);
    }

    public ByteArrayOutputStream downloadFie(String fileName) {

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final CountDownLatch finishLatch = new CountDownLatch(1);
        final AtomicBoolean completed = new AtomicBoolean(false);

        // observer to process each chunk of data the comes over the wire
        StreamObserver<DataChunk> streamObserver = new StreamObserver<DataChunk>() {
            @Override
            public void onNext(DataChunk dataChunk) {
                try {
                    baos.write(dataChunk.getData().toByteArray());
                } catch (IOException e) {
                    log.error("error on write to byte array stream", e);
                    onError(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.info("acquireImage.onError()", t);
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                log.info("acquireImage.onCompleted() successful!");
                completed.compareAndSet(false, true);
                finishLatch.countDown();
            }
        };

        try {

            DownloadFileRequest.Builder builder = DownloadFileRequest
                    .newBuilder()
                    .setFileName(fileName);

            nonBlockingStub.downloadFile(builder.build(), streamObserver);

            finishLatch.await(5, TimeUnit.MINUTES);

            if (!completed.get()) {
                throw new Exception("The acquireImage method did not complete");
            }

        } catch (Exception e) {
            log.error("The acquireImage method did not complete");
        }

        return baos;
    }
}
