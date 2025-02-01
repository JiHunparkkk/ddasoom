package com.ddasoom.voice_service.voice.application.service;

import com.ddasoom.voice_service.common.annotation.TimeTrace;
import com.ddasoom.voice_service.common.annotation.UseCase;
import com.ddasoom.voice_service.voice.application.port.in.CreateVoiceCommand;
import com.ddasoom.voice_service.voice.application.port.in.CreateVoiceUseCase;
import com.ddasoom.voice_service.voice.application.port.out.CreateVoicePort;
import com.ddasoom.voice_service.voice.application.port.out.TrainAiVoicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@Transactional
@RequiredArgsConstructor
public class CreateVoiceService implements CreateVoiceUseCase {

    private final TrainAiVoicePort trainAiVoicePort;
    private final CreateVoicePort createVoicePort;

    @Override
    @TimeTrace  //key 생성 시간
    public String createVoice(CreateVoiceCommand command) {
        String voiceKey = trainAiVoicePort.trainAiVoice(command.voices());

        createVoicePort.createVoice(
                command.userId(),
                command.voiceName(),
                voiceKey
        );

        return voiceKey;
    }
}
