package com.ddasoom.voice_service.voice.adapter.out.elevenlabs;

import static lombok.AccessLevel.PRIVATE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

import com.ddasoom.voice_service.voice.adapter.out.elevenlabs.request.TextToSpeechRequest;
import com.ddasoom.voice_service.voice.adapter.out.elevenlabs.request.TrainAiVoiceRequest;
import com.ddasoom.voice_service.voice.adapter.out.elevenlabs.response.TrainAiVoiceResponse;
import lombok.NoArgsConstructor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@NoArgsConstructor(access = PRIVATE)
public abstract class ElevenLabsRequestUtils {

    private static final RestClient restClient;
    private static final WebClient webClient;

    static {
        restClient = RestClient.builder()
                .baseUrl("https://api.elevenlabs.io")
                .defaultHeader("xi-api-key", "-")
                .build();

        webClient = WebClient.builder()
                .baseUrl("https://api.elevenlabs.io")
                .defaultHeader("xi-api-key", "-")
                .build();
    }

    public static TrainAiVoiceResponse sendRequest(TrainAiVoiceRequest request) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("name", request.getName());
        request.getFiles().forEach(file -> body.add("files", file));

        return restClient.post()
                .uri("/v1/voices/add")
                .contentType(MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(TrainAiVoiceResponse.class);
    }

//    public static byte[] sendRequest(String voiceKey, TextToSpeechRequest request) {
//        return restClient.post()
//                .uri("/v1/text-to-speech/" + voiceKey)
//                .contentType(APPLICATION_JSON)
//                .body(request)
//                .retrieve()
//                .body(byte[].class);
//    }

    public static Mono<byte[]> sendRequest(String voiceKey, TextToSpeechRequest request) {
        return webClient.post()
                .uri("/v1/text-to-speech/" + voiceKey)
                .contentType(APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(byte[].class);
    }
}
