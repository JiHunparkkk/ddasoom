package com.ddasoom.voice_service.voice.adapter.out;

import static com.ddasoom.voice_service.voice.adapter.out.elevenlabs.SpeechScript.speechScripts;

import com.ddasoom.voice_service.voice.adapter.out.elevenlabs.ElevenLabsAiVoiceAdapter;
import com.ddasoom.voice_service.voice.adapter.out.elevenlabs.Script;
import com.ddasoom.voice_service.voice.adapter.out.storage.DiskStorageUtils;
import com.ddasoom.voice_service.voice.adapter.out.storage.S3FileStorageUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceRecoveryScheduler {

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private final VoiceRepository voiceRepository;
    private final S3FileStorageUtils s3Storage;
    private final ElevenLabsAiVoiceAdapter elevenLabsAiVoiceAdapter;
    private final DiskStorageUtils diskStorage;

    @Scheduled(cron = "0 0 3 * * *")
    private void checkVoiceConcurrency() {
        log.info("음성 생성 및 저장 일관성 검사 시각: {}", LocalDateTime.now());

        List<String> voiceKeys = voiceRepository.findAll().stream()
                .map(VoiceJpaEntity::getVoiceKey)
                .toList();

        Map<String, List<Script>> remainScript = new HashMap<>();

        for (String voiceKey : voiceKeys) {
            speechScripts().stream()
                    .map(script -> String.format("%s-%s.mp3", voiceKey, script.code()))
                    .forEach(fileName -> {
                        if (doesExistInS3(fileName)) {
                            return;
                        }

                        diskStorage.readFromDisk(fileName).ifPresentOrElse(
                                bytes -> uploadDiskToS3(fileName, bytes),
                                () -> convertTextToSpeech(voiceKey, fileName, remainScript)
                        );
                    });
        }
        elevenLabsAiVoiceAdapter.convertTextScriptToSoundPort(remainScript);

        log.info("음성 생성 및 저장 일관성 검사 완료 시각: {}", LocalDateTime.now());
    }

    private boolean doesExistInS3(String fileName) {
        return s3Storage.doesObjectExist(fileName);
    }

    private void uploadDiskToS3(String fileName, byte[] bytes) {
        s3Storage.uploadSoundFile(fileName, bytes);
        diskStorage.deleteFromDisk(fileName);
        log.info("✅디스크 -> S3 업로드 완료!");
    }

    private void convertTextToSpeech(String voiceKey, String fileName,
            Map<String, List<Script>> remainScript) {
        remainScript.getOrDefault(voiceKey, new ArrayList<>())
                .add(new Script(fileName.split("-")[0], fileName.split("-")[1]));
    }
}
