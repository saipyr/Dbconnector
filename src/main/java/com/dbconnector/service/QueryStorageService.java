package com.dbconnector.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

@Service
public class QueryStorageService {
    // In-memory query storage for demo; replace with persistent storage in production
    private final Map<String, String> queryStore = new ConcurrentHashMap<>();

    public void saveQuery(String queryName, String query) {
        queryStore.put(queryName, query);
    }

    public String loadQuery(String queryName) {
        return queryStore.get(queryName);
    }

    public Set<String> listQueryNames() {
        return queryStore.keySet();
    }

    public boolean deleteQuery(String queryName) {
        return queryStore.remove(queryName) != null;
    }

    // TODO: Use persistent storage for queries in production
} 