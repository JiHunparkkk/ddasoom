package com.ddasoom.voice_service.voice.adapter.out.elevenlabs;

import static com.ddasoom.voice_service.voice.adapter.out.elevenlabs.ElevenLabsRequestUtils.sendRequest;
import static com.ddasoom.voice_service.voice.adapter.out.elevenlabs.SpeechScript.speechScripts;

import com.ddasoom.voice_service.common.annotation.TimeTrace;
import com.ddasoom.voice_service.voice.adapter.out.elevenlabs.request.TextToSpeechRequest;
import com.ddasoom.voice_service.voice.adapter.out.elevenlabs.request.TrainAiVoiceRequest;
import com.ddasoom.voice_service.voice.adapter.out.elevenlabs.response.TrainAiVoiceResponse;
import com.ddasoom.voice_service.voice.application.domain.SoundFile;
import com.ddasoom.voice_service.voice.application.domain.Voice;
import com.ddasoom.voice_service.voice.application.port.out.ConvertTextScriptToSoundPort;
import com.ddasoom.voice_service.voice.application.port.out.TrainAiVoicePort;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
@RequiredArgsConstructor
public class ElevenLabsAiVoiceAdapter implements TrainAiVoicePort, ConvertTextScriptToSoundPort {

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

//    @TimeTrace
//    @Override
//    public List<SoundFile> convertTextScriptToSoundPort(String voiceKey) {
//        return speechScripts().stream()
//                .map(script -> getSoundFile(voiceKey, script))
//                .toList();
//    }

    @TimeTrace
    @Override
    public List<SoundFile> convertTextScriptToSoundPort(String voiceKey) {
        List<Mono<Pair<Script, byte[]>>> requests = speechScripts().stream()
                .map(script ->
                        sendRequest(voiceKey, new TextToSpeechRequest(script.message()))
                                .map(bytes -> Pair.of(script, bytes))
                )
                .toList();

        List<Pair<Script, byte[]>> responses = Flux.fromIterable(requests)
                .flatMap(request -> request
                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))), 3)
                .collectList()
                .block();

        return responses.stream()
                .map(pair -> getSoundFile(voiceKey, pair.getFirst().code(), pair.getSecond()))
                .toList();
    }

    private ByteArrayResource convertByteArrayToResource(Voice voice) {
        return new ByteArrayResource(voice.bytes()) {

            @Override
            public String getFilename() {
                return UUID.randomUUID().toString();
            }
        };
    }

//    private SoundFile getSoundFile(String voiceKey, Script script) {
//        byte[] bytes = sendRequest(
//                voiceKey,
//                new TextToSpeechRequest(script.message())
//        );
//
//        return new SoundFile(
//                String.format("%s-%s.mp3", voiceKey, script.code()),
//                bytes
//        );
//    }

    private SoundFile getSoundFile(String voiceKey, String scriptCode, byte[] bytes) {
        return new SoundFile(
                String.format("%s-%s.mp3", voiceKey, scriptCode),
                bytes
        );
    }
}
