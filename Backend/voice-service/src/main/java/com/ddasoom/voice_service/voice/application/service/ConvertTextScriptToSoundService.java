package com.ddasoom.voice_service.voice.application.service;

import com.ddasoom.voice_service.common.annotation.TimeTrace;
import com.ddasoom.voice_service.common.annotation.UseCase;
import com.ddasoom.voice_service.voice.application.port.in.ConvertTextScriptToSoundUseCase;
import com.ddasoom.voice_service.voice.application.port.out.ConvertTextScriptToSoundPort;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@Transactional
@RequiredArgsConstructor
public class ConvertTextScriptToSoundService implements ConvertTextScriptToSoundUseCase {

    private final ConvertTextScriptToSoundPort convertTextScriptToSoundPort;

    @Override
    @TimeTrace
    public void convertTextScriptToSoundUseCase(String voiceKey) {
        convertTextScriptToSoundPort.convertTextScriptToSoundPort(voiceKey);
    }
}
