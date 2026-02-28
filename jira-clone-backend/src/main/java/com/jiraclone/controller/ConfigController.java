package com.jiraclone.controller;

import com.jiraclone.dto.ApiResponse;
import com.jiraclone.model.AppConfig;
import com.jiraclone.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Autowired
    private ConfigService configService;

    @GetMapping
    public ApiResponse<AppConfig> get() {
        return ApiResponse.success(configService.get());
    }

    @PutMapping
    public ApiResponse<AppConfig> update(@RequestBody AppConfig config) {
        return ApiResponse.success(configService.update(config));
    }
}
