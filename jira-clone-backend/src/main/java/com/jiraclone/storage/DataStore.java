package com.jiraclone.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jiraclone.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataStore {

    @Autowired
    private JsonFileStore store;

    // Projects
    public List<Project> readProjects() {
        return store.readList("projects.json", new TypeReference<>() {});
    }

    public void writeProjects(List<Project> projects) {
        store.writeList("projects.json", projects);
    }

    // Issues
    public List<Issue> readIssues() {
        return store.readList("issues.json", new TypeReference<>() {});
    }

    public void writeIssues(List<Issue> issues) {
        store.writeList("issues.json", issues);
    }

    // Sprints
    public List<Sprint> readSprints() {
        return store.readList("sprints.json", new TypeReference<>() {});
    }

    public void writeSprints(List<Sprint> sprints) {
        store.writeList("sprints.json", sprints);
    }

    // Comments
    public List<Comment> readComments() {
        return store.readList("comments.json", new TypeReference<>() {});
    }

    public void writeComments(List<Comment> comments) {
        store.writeList("comments.json", comments);
    }

    // Activity
    public List<ActivityLog> readActivity() {
        return store.readList("activity.json", new TypeReference<>() {});
    }

    public void writeActivity(List<ActivityLog> activity) {
        store.writeList("activity.json", activity);
    }

    // Config
    public AppConfig readConfig() {
        AppConfig cfg = store.readObject("config.json", new TypeReference<>() {});
        if (cfg == null) {
            cfg = new AppConfig();
            cfg.setLabels(List.of("bug", "feature", "improvement", "documentation"));
            cfg.setTeamMembers(List.of());
        }
        return cfg;
    }

    public void writeConfig(AppConfig config) {
        store.writeObject("config.json", config);
    }
}
