package com.ddasoom.voice_service.voice.adapter.out.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.ddasoom.voice_service.common.annotation.TimeTrace;
import com.ddasoom.voice_service.voice.application.domain.SoundFile;
import com.ddasoom.voice_service.voice.application.port.out.UploadSoundFilePort;
import java.io.ByteArrayInputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class S3FileStorageAdapter implements UploadSoundFilePort {

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private final AmazonS3 amazonS3;

    @TimeTrace
    @Override
    public void uploadSoundFiles(List<SoundFile> files) {
        files.forEach(this::uploadSoundFile);
    }

    public void uploadSoundFile(SoundFile file) {
        amazonS3.putObject(
                bucketName,
                file.fileName(),
                new ByteArrayInputStream(file.bytes()),
                getMetadata(file)
        );
    }

    private ObjectMetadata getMetadata(SoundFile file) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(file.size());
        objectMetadata.setContentType("audio/mpeg");
        return objectMetadata;
    }
}
