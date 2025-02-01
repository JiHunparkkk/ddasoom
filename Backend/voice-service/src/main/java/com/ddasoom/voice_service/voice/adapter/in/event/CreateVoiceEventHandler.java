package com.ddasoom.voice_service.voice.adapter.in.event;

import com.ddasoom.voice_service.common.annotation.TimeTrace;
import com.ddasoom.voice_service.voice.application.port.in.ConvertTextScriptToSoundUseCase;
import com.ddasoom.voice_service.voice.application.port.in.CreateVoiceCommand;
import com.ddasoom.voice_service.voice.application.port.in.CreateVoiceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateVoiceEventHandler {

    private final CreateVoiceUseCase createVoiceUseCase;
    private final ConvertTextScriptToSoundUseCase convertTextScriptToSoundUseCase;

    @Async
    @EventListener
    @TimeTrace  //모든 시간
    public void createVoice(CreateVoiceEvent event) {
        String voiceKey = createVoiceUseCase.createVoice(
                new CreateVoiceCommand(event.userId(), event.voiceName(), event.voices())
        );

        convertTextScriptToSoundUseCase.convertTextScriptToSoundUseCase(voiceKey);
    }
}
