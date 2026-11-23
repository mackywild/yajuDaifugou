package com.example.daifugo.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** ログイン前にも閲覧できる稼働状態。更新時はキャッシュしない。 */
@RestController
@RequestMapping("/api/system")
public class MaintenanceStatusController {
    private final MaintenanceService maintenanceService;

    public MaintenanceStatusController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping("/status")
    public Map<String, String> status() {
        return Map.of("mode", maintenanceService.mode().name());
    }
}
