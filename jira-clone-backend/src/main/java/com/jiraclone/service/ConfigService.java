package com.jiraclone.service;

import com.jiraclone.model.AppConfig;
import com.jiraclone.repository.AppConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConfigService {

    @Autowired
    private AppConfigRepository appConfigRepository;

    public AppConfig get() {
        return appConfigRepository.findById(1L).orElseGet(this::defaultConfig);
    }

    @Transactional
    public AppConfig update(AppConfig config) {
        config.setId(1L);
        return appConfigRepository.save(config);
    }

    private AppConfig defaultConfig() {
        AppConfig cfg = new AppConfig();
        cfg.setId(1L);
        cfg.setLabels(List.of("bug", "feature", "improvement", "documentation"));
        cfg.setTeamMembers(List.of());
        return cfg;
    }
}
