package com.ddasoom.voice_service.voice.adapter.out.elevenlabs;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class ElevenLabsRequestUtilsTest {

    @DisplayName("환율 API를 활용한 비동기 요청")
    @Test
    void requestWebClientTest() {
        WebClient webClient = WebClient.create("https://open.er-api.com");

        long startTime = System.currentTimeMillis();

        List<Mono<Double>> requests = IntStream.range(0, 10)
                .mapToObj(i -> webClient.get()
                        .uri("/v6/latest")
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                        })
                        .delayElement(Duration.ofSeconds(1))
                        .map(response -> {
                            Map<String, Double> rates = (Map<String, Double>) response.get("rates");
                            return rates.get("KRW");
                        })
                ).toList();

        List<Double> krwRate = Flux.merge(requests).collectList().block();

        long endTime = System.currentTimeMillis();
        long totalTime = (endTime - startTime) / 1000;

        // 결과 출력
        System.out.println("총 실행 시간: " + totalTime + "초");
        System.out.println("KRW 환율 응답값: " + krwRate);

        assertThat(totalTime).isLessThan(2L);
    }


    @ValueSource(ints = {10, 100, 10000})
    @DisplayName("환율 API를 활용한 비동기 요청 시 Flux merge 시 List 동시성 확인")
    @ParameterizedTest
    void nonConcurrentWhenParallelRequests(int count) {
        WebClient webClient = WebClient.create("https://open.er-api.com");

        List<Mono<Double>> requests = IntStream.range(0, count)
                .mapToObj(i -> webClient.get()
                        .uri("/v6/latest")
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                        })
                        .map(response -> {
                            Map<String, Double> rates = (Map<String, Double>) response.get("rates");
                            return rates.get("KRW");
                        })
                ).toList();

        List<Double> krwRate = Flux.merge(requests).collectList().block();

        assertThat(krwRate.size()).isEqualTo(count);
    }

    @ValueSource(ints = {10, 100, 1000})
    @DisplayName("환율 API를 활용한 비동기 요청 시 직접 List 에 추가할 때 List 동시성 확인")
    @ParameterizedTest
    void concurrentWhenParallelRequests(int count) {
        //given
        List<Double> krwRates = new ArrayList<>();
        WebClient webClient = WebClient.create("https://open.er-api.com");

        //when
        List<Mono<Void>> requests = IntStream.range(0, count)
                .mapToObj(i -> webClient.get()
                        .uri("/v6/latest")
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                        })
                        .map(response -> {
                            Map<String, Double> rates = (Map<String, Double>) response.get("rates");
                            return rates.get("KRW");
                        })
                        .doOnNext(krwRates::add)    //리스트에 직접 추가
                        .then()
                ).toList();

        Flux.merge(requests).blockFirst();

        //then
        assertThat(krwRates.size()).isEqualTo(count);
    }
}