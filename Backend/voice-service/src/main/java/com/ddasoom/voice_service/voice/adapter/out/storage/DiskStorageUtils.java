package com.ddasoom.voice_service.voice.adapter.out.storage;

import com.ddasoom.voice_service.voice.application.domain.SoundFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DiskStorageUtils {

    private static final Path DIR_PATH = Paths.get(
            "/Users/jihunpark/Desktop/SSAFY/refactoring/ddasoom/Backend/voice-service/backup-sounds");

    public void saveToDisk(SoundFile soundFile) {
        Path filePath = DIR_PATH.resolve(soundFile.fileName());

        try {
            Files.createDirectories(DIR_PATH);
            Files.write(filePath, soundFile.bytes());
            System.out.println("Saved to: " + filePath.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file", e);
        }
    }

    public boolean fileExistsInDir(String fileName) {
        Path filePath = DIR_PATH.resolve(fileName);

        return Files.exists(filePath);
    }

    public Optional<byte[]> readFromDisk(String fileName) {
        Path filePath = DIR_PATH.resolve(fileName);

        try {
            return Optional.of(Files.readAllBytes(filePath));
        } catch (IOException e) {
            log.error("⚠️Failed to read from disk or File does not exist");
        }
        return Optional.empty();
    }

    public void deleteFromDisk(String fileName) {
        Path filePath = DIR_PATH.resolve(fileName);

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("⚠️Failed to delete from disk or File does not exist");
        }
    }
}
