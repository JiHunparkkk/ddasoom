package com.ddasoom.voice_service.voice.adapter.out.elevenlabs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;

import com.ddasoom.voice_service.voice.adapter.out.storage.DiskStorageUtils;
import com.ddasoom.voice_service.voice.adapter.out.storage.S3FileStorageUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ElevenLabsAiVoiceAdapterTest {

    @Mock
    private ElevenLabsRequestUtils elevenLabsRequestUtils;

    @Mock
    private S3FileStorageUtils s3Storage;

    @Mock
    private DiskStorageUtils diskStorageUtils;

    @InjectMocks
    private ElevenLabsAiVoiceAdapter adapter;

    @DisplayName("녹음 성공, S3실패 시 Disk에 저장된다.")
    @Test
    void convertTextScriptToSoundPort_uploadFails_thenFallbackToDisk() {
        //given
        String voiceKey = "temp_voice";

        Mockito.when(elevenLabsRequestUtils.sendRequest(eq(voiceKey), any()))
                .thenReturn(Mono.just("TEST-AUDIO".getBytes()));

        Mockito.doThrow(new RuntimeException("S3 upload fail"))
                .when(s3Storage).uploadSoundFile(any());

        Mockito.doNothing()
                .when(diskStorageUtils).saveToDisk(any());

        //when
        adapter.convertTextScriptToSoundPort(voiceKey);

        //then
        Mockito.verify(elevenLabsRequestUtils, atLeastOnce()).sendRequest(eq(voiceKey), any());
        Mockito.verify(s3Storage, atLeastOnce()).uploadSoundFile(any());
        Mockito.verify(diskStorageUtils, times(SpeechScript.speechScripts().size())).saveToDisk(any());
    }
}