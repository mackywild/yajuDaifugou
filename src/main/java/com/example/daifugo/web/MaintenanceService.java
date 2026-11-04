package com.example.daifugo.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 運営者のみが更新できるローカルファイルから稼働モードを読み込む。
 * 検証環境は本番とは別のファイルまたは未指定で起動する。
 */
@Service
public class MaintenanceService {
    public enum Mode { NORMAL, DRAINING, MAINTENANCE }

    private final Path modeFile;

    public MaintenanceService(@Value("${daifugo.maintenance-file:}") String filePath) {
        modeFile = filePath.isBlank() ? null : Path.of(filePath);
    }

    /** 設定ファイルが存在しない環境は通常稼働とする。 */
    public Mode mode() {
        if (modeFile == null || Files.notExists(modeFile)) {
            return Mode.NORMAL;
        }
        try {
            String content = Files.readString(modeFile, StandardCharsets.UTF_8).trim();
            return Mode.valueOf(content.toUpperCase(Locale.ROOT));
        } catch (IOException | IllegalArgumentException exception) {
            // 運用設定の破損時に不用意に新規対戦を解放しない。
            return Mode.MAINTENANCE;
        }
    }

    /** DRAININGでは新規ルーム・参加・開始のみ停止し、対戦は継続する。 */
    public boolean blocks(String method, String path) {
        Mode currentMode = mode();
        if (currentMode == Mode.NORMAL) {
            return false;
        }
        if (currentMode == Mode.MAINTENANCE) {
            return path.startsWith("/api/rooms/")
                    || path.equals("/api/rooms")
                    || path.startsWith("/api/users/");
        }
        if (!"POST".equalsIgnoreCase(method)) {
            return false;
        }
        return path.equals("/api/rooms")
                || path.matches("^/api/rooms/(cpu|challenge|multi-yaju)$")
                || path.matches("^/api/rooms/[^/]+/(join|start)$");
    }
}
