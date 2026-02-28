package com.jiraclone.storage;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class StoreLock {
    private final ConcurrentHashMap<String, ReentrantReadWriteLock> locks = new ConcurrentHashMap<>();

    public ReentrantReadWriteLock getLock(String filename) {
        return locks.computeIfAbsent(filename, k -> new ReentrantReadWriteLock());
    }

    public void readLock(String filename, Runnable action) {
        ReentrantReadWriteLock.ReadLock lock = getLock(filename).readLock();
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }

    public void writeLock(String filename, Runnable action) {
        ReentrantReadWriteLock.WriteLock lock = getLock(filename).writeLock();
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }
}
