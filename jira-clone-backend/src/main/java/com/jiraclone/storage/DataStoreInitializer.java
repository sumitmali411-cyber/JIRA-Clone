package com.jiraclone.storage;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DataStoreInitializer {

    @Autowired
    private JsonFileStore store;

    @PostConstruct
    public void init() {
        store.ensureFile("projects.json", "[]");
        store.ensureFile("issues.json", "[]");
        store.ensureFile("sprints.json", "[]");
        store.ensureFile("comments.json", "[]");
        store.ensureFile("activity.json", "[]");
        store.ensureFile("config.json", "{\"labels\":[\"bug\",\"feature\",\"improvement\",\"documentation\"],\"teamMembers\":[]}");
    }
}
