package com.ddasoom.voice_service.voice.adapter.out.elevenlabs;

import static com.ddasoom.voice_service.voice.adapter.out.elevenlabs.ElevenLabsRequestUtils.sendRequest;
import static com.ddasoom.voice_service.voice.adapter.out.elevenlabs.SpeechScript.speechScripts;
import static com.ddasoom.voice_service.voice.adapter.out.storage.DiskStorageUtils.saveToDisk;

import com.amazonaws.services.s3.AmazonS3;
import com.ddasoom.voice_service.common.annotation.TimeTrace;
import com.ddasoom.voice_service.voice.adapter.out.elevenlabs.request.TextToSpeechRequest;
import com.ddasoom.voice_service.voice.adapter.out.elevenlabs.request.TrainAiVoiceRequest;
import com.ddasoom.voice_service.voice.adapter.out.elevenlabs.response.TrainAiVoiceResponse;
import com.ddasoom.voice_service.voice.adapter.out.storage.S3FileStorageUtils;
import com.ddasoom.voice_service.voice.application.domain.SoundFile;
import com.ddasoom.voice_service.voice.application.domain.Voice;
import com.ddasoom.voice_service.voice.application.port.out.ConvertTextScriptToSoundPort;
import com.ddasoom.voice_service.voice.application.port.out.TrainAiVoicePort;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElevenLabsAiVoiceAdapter implements TrainAiVoicePort, ConvertTextScriptToSoundPort {

    private final S3FileStorageUtils s3Storage;
    private final AmazonS3 amazonS3;

    @Override
    public String trainAiVoice(List<Voice> voices) {
        List<ByteArrayResource> voiceFiles = voices.stream()
                .map(this::convertByteArrayToResource)
                .toList();

        TrainAiVoiceResponse response = sendRequest(
                new TrainAiVoiceRequest(UUID.randomUUID().toString(), voiceFiles)
        );

        return response.voiceId();
    }

    @TimeTrace
    @Override
    public void convertTextScriptToSoundPort(String voiceKey) {
        List<Mono<Pair<Script, byte[]>>> requests = getMonos(voiceKey);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger uploadFailCount = new AtomicInteger(0);

        Flux.fromIterable(requests)
                .flatMap(request -> request
                                .doOnNext(pair -> {
                                    successCount.incrementAndGet();
                                    uploadSoundFile(voiceKey, pair, uploadFailCount);
                                })
                                .onErrorResume(e -> errorGuide(e, failCount))
                                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                        , 3)
                .doOnComplete(() -> {
                    log.info("음성 복제 ✅성공 : {}개, ❌실패 : {}개", successCount.get(), failCount.get());
                    log.warn("❌업로드 실패 : {}개", uploadFailCount.get());
                })
                .subscribe();
    }

    public void convertTextScriptToSoundPort(Map<String, List<Script>> remainScript) {
        List<Mono<ScriptRequest>> requests = getMonos(remainScript);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger uploadFailCount = new AtomicInteger(0);

        Flux.fromIterable(requests)
                .flatMap(request -> request
                                .doOnNext(scriptRequest -> {
                                    successCount.incrementAndGet();
                                    uploadSoundFile(
                                            scriptRequest.voiceKey(),
                                            Pair.of(scriptRequest.script(), scriptRequest.bytes()),
                                            uploadFailCount
                                    );
                                })
                                .onErrorResume(e -> errorGuide2(e, failCount))
                                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                        , 3)
                .doOnComplete(() -> {
                    log.info("음성 복제 ✅성공 : {}개, ❌실패 : {}개", successCount.get(), failCount.get());
                    log.warn("❌업로드 실패 : {}개", uploadFailCount.get());
                })
                .blockLast();
    }

    private List<Mono<Pair<Script, byte[]>>> getMonos(String voiceKey) {
        return speechScripts().stream()
                .map(script ->
                        sendRequest(voiceKey, new TextToSpeechRequest(script.message()))
                                .map(bytes -> Pair.of(script, bytes))
                )
                .toList();
    }

    private List<Mono<ScriptRequest>> getMonos(Map<String, List<Script>> remainScript) {
        return remainScript.entrySet().stream()
                .flatMap(entry -> {
                    String voiceKey = entry.getKey();
                    return entry.getValue().stream()
                            .map(script ->
                                    sendRequest(voiceKey, new TextToSpeechRequest(script.message()))
                                            .map(bytes -> new ScriptRequest(voiceKey, script, bytes))
                            );
                })
                .toList();
    }

    private static Mono<Pair<Script, byte[]>> errorGuide(Throwable e, AtomicInteger failCount) {
        log.warn("Sound file failed after retry: " + e.getMessage());
        failCount.incrementAndGet();
        return Mono.empty();
    }

    private static Mono<ScriptRequest> errorGuide2(Throwable e, AtomicInteger failCount) {
        log.warn("Sound file failed after retry: " + e.getMessage());
        failCount.incrementAndGet();
        return Mono.empty();
    }

    private void uploadSoundFile(String voiceKey, Pair<Script, byte[]> pair, AtomicInteger uploadFailCount) {
        SoundFile soundFile = getSoundFile(voiceKey, pair.getFirst().code(), pair.getSecond());

        try {
            s3Storage.uploadSoundFile(soundFile);
        } catch (Exception e) {
            saveToDisk(soundFile);
            uploadFailCount.incrementAndGet();
        }
    }

    private ByteArrayResource convertByteArrayToResource(Voice voice) {
        return new ByteArrayResource(voice.bytes()) {

            @Override
            public String getFilename() {
                return UUID.randomUUID().toString();
            }
        };
    }

    private SoundFile getSoundFile(String voiceKey, String scriptCode, byte[] bytes) {
        return new SoundFile(
                String.format("%s-%s.mp3", voiceKey, scriptCode),
                bytes
        );
    }

    private record ScriptRequest(String voiceKey, Script script, byte[] bytes) {

    }

}
