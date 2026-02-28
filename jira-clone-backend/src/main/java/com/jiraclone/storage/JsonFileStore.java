package com.jiraclone.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class JsonFileStore {

    @Value("${app.data.dir:./data}")
    private String dataDir;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StoreLock storeLock;

    private Path getPath(String filename) {
        return Path.of(dataDir, filename);
    }

    public <T> List<T> readList(String filename, TypeReference<List<T>> typeRef) {
        ReentrantReadWriteLock.ReadLock lock = storeLock.getLock(filename).readLock();
        lock.lock();
        try {
            Path path = getPath(filename);
            if (!Files.exists(path)) return List.of();
            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length == 0) return List.of();
            return objectMapper.readValue(bytes, typeRef);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + filename, e);
        } finally {
            lock.unlock();
        }
    }

    public <T> T readObject(String filename, TypeReference<T> typeRef) {
        ReentrantReadWriteLock.ReadLock lock = storeLock.getLock(filename).readLock();
        lock.lock();
        try {
            Path path = getPath(filename);
            if (!Files.exists(path)) return null;
            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length == 0) return null;
            return objectMapper.readValue(bytes, typeRef);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + filename, e);
        } finally {
            lock.unlock();
        }
    }

    public <T> void writeList(String filename, List<T> data) {
        ReentrantReadWriteLock.WriteLock lock = storeLock.getLock(filename).writeLock();
        lock.lock();
        try {
            Path dir = Path.of(dataDir);
            Files.createDirectories(dir);
            Path target = getPath(filename);
            Path tmp = Path.of(dataDir, filename + ".tmp");
            objectMapper.writeValue(tmp.toFile(), data);
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write " + filename, e);
        } finally {
            lock.unlock();
        }
    }

    public <T> void writeObject(String filename, T data) {
        ReentrantReadWriteLock.WriteLock lock = storeLock.getLock(filename).writeLock();
        lock.lock();
        try {
            Path dir = Path.of(dataDir);
            Files.createDirectories(dir);
            Path target = getPath(filename);
            Path tmp = Path.of(dataDir, filename + ".tmp");
            objectMapper.writeValue(tmp.toFile(), data);
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write " + filename, e);
        } finally {
            lock.unlock();
        }
    }

    public void ensureFile(String filename, String defaultContent) {
        Path path = getPath(filename);
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                Files.writeString(path, defaultContent);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize " + filename, e);
        }
    }
}
