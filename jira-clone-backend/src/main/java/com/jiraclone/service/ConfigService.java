package com.jiraclone.service;

import com.jiraclone.model.AppConfig;
import com.jiraclone.storage.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConfigService {

    @Autowired
    private DataStore dataStore;

    public AppConfig get() {
        return dataStore.readConfig();
    }

    public AppConfig update(AppConfig config) {
        dataStore.writeConfig(config);
        return config;
    }
}
