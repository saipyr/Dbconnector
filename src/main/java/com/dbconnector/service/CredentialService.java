package com.dbconnector.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

@Service
public class CredentialService {
    // In-memory credential storage for demo; replace with secure storage in production
    private final Map<String, Map<String, Object>> credentialsStore = new ConcurrentHashMap<>();

    public void saveCredentials(String profileName, Map<String, Object> credentials) {
        credentialsStore.put(profileName, credentials);
    }

    public Map<String, Object> loadCredentials(String profileName) {
        return credentialsStore.get(profileName);
    }

    public Set<String> listCredentialProfiles() {
        return credentialsStore.keySet();
    }

    public boolean deleteCredentials(String profileName) {
        return credentialsStore.remove(profileName) != null;
    }

    // TODO: Use encryption/secure storage for credentials in production
} 