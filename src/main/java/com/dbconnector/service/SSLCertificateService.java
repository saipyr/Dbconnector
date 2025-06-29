package com.dbconnector.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SSLCertificateService {
    
    @Autowired
    private LoggingService loggingService;
    
    private final String certificatesDirectory;
    private final Map<String, CertificateInfo> certificates = new ConcurrentHashMap<>();
    
    public SSLCertificateService() {
        this.certificatesDirectory = System.getProperty("user.dir") + "/certificates";
        
        // Create certificates directory if it doesn't exist
        File dir = new File(certificatesDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        loadCertificates();
    }
    
    /**
     * Upload and store SSL certificate
     */
    public CertificateInfo uploadCertificate(String connectionId, String certificateType, 
                                           MultipartFile certificateFile) throws Exception {
        
        loggingService.logInfo("Uploading SSL certificate: " + certificateType + " for connection: " + connectionId);
        
        // Validate certificate file
        validateCertificateFile(certificateFile);
        
        // Generate unique filename
        String fileName = connectionId + "_" + certificateType + "_" + System.currentTimeMillis() + ".pem";
        Path filePath = Paths.get(certificatesDirectory, fileName);
        
        // Save certificate file
        Files.write(filePath, certificateFile.getBytes());
        
        // Parse certificate information
        CertificateInfo certInfo = parseCertificate(certificateFile.getBytes(), certificateType);
        certInfo.setConnectionId(connectionId);
        certInfo.setFilePath(filePath.toString());
        certInfo.setFileName(fileName);
        
        // Store certificate info
        String certKey = connectionId + "_" + certificateType;
        certificates.put(certKey, certInfo);
        
        // Persist certificate metadata
        persistCertificateMetadata();
        
        loggingService.logInfo("SSL certificate uploaded successfully: " + fileName);
        
        return certInfo;
    }
    
    /**
     * Get certificate information
     */
    public CertificateInfo getCertificate(String connectionId, String certificateType) {
        String certKey = connectionId + "_" + certificateType;
        return certificates.get(certKey);
    }
    
    /**
     * Get all certificates for a connection
     */
    public Map<String, CertificateInfo> getConnectionCertificates(String connectionId) {
        Map<String, CertificateInfo> connectionCerts = new HashMap<>();
        
        certificates.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(connectionId + "_"))
            .forEach(entry -> {
                String certType = entry.getKey().substring(connectionId.length() + 1);
                connectionCerts.put(certType, entry.getValue());
            });
        
        return connectionCerts;
    }
    
    /**
     * Delete certificate
     */
    public boolean deleteCertificate(String connectionId, String certificateType) {
        String certKey = connectionId + "_" + certificateType;
        CertificateInfo certInfo = certificates.remove(certKey);
        
        if (certInfo != null) {
            try {
                // Delete certificate file
                Files.deleteIfExists(Paths.get(certInfo.getFilePath()));
                
                // Persist changes
                persistCertificateMetadata();
                
                loggingService.logInfo("SSL certificate deleted: " + certInfo.getFileName());
                return true;
            } catch (IOException e) {
                loggingService.logError("Error deleting certificate file", e);
            }
        }
        
        return false;
    }
    
    /**
     * Validate certificate file
     */
    private void validateCertificateFile(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Certificate file is empty");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".pem") && !filename.endsWith(".crt") && !filename.endsWith(".cer"))) {
            throw new IllegalArgumentException("Invalid certificate file format. Only .pem, .crt, and .cer files are supported");
        }
        
        // Validate file content
        String content = new String(file.getBytes());
        if (!content.contains("-----BEGIN CERTIFICATE-----") && !content.contains("-----BEGIN PRIVATE KEY-----")) {
            throw new IllegalArgumentException("Invalid certificate file content");
        }
    }
    
    /**
     * Parse certificate information
     */
    private CertificateInfo parseCertificate(byte[] certificateData, String certificateType) throws Exception {
        CertificateInfo certInfo = new CertificateInfo();
        certInfo.setCertificateType(certificateType);
        certInfo.setUploadDate(new Date());
        
        String content = new String(certificateData);
        
        if (content.contains("-----BEGIN CERTIFICATE-----")) {
            // Parse X.509 certificate
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                ByteArrayInputStream bis = new ByteArrayInputStream(certificateData);
                X509Certificate cert = (X509Certificate) cf.generateCertificate(bis);
                
                certInfo.setSubject(cert.getSubjectDN().toString());
                certInfo.setIssuer(cert.getIssuerDN().toString());
                certInfo.setValidFrom(cert.getNotBefore());
                certInfo.setValidTo(cert.getNotAfter());
                certInfo.setSerialNumber(cert.getSerialNumber().toString());
                certInfo.setFingerprint(generateFingerprint(cert.getEncoded()));
                
                // Check if certificate is valid
                try {
                    cert.checkValidity();
                    certInfo.setValid(true);
                } catch (Exception e) {
                    certInfo.setValid(false);
                    certInfo.setValidationError(e.getMessage());
                }
                
            } catch (Exception e) {
                loggingService.logError("Error parsing X.509 certificate", e);
                certInfo.setValid(false);
                certInfo.setValidationError("Failed to parse certificate: " + e.getMessage());
            }
        } else if (content.contains("-----BEGIN PRIVATE KEY-----") || content.contains("-----BEGIN RSA PRIVATE KEY-----")) {
            // Handle private key
            certInfo.setSubject("Private Key");
            certInfo.setIssuer("N/A");
            certInfo.setValid(true);
        }
        
        return certInfo;
    }
    
    /**
     * Generate certificate fingerprint
     */
    private String generateFingerprint(byte[] certData) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(certData);
        
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        
        return "SHA256:" + sb.toString().toUpperCase();
    }
    
    /**
     * Load certificates metadata
     */
    private void loadCertificates() {
        File metadataFile = new File(certificatesDirectory, "certificates.dat");
        if (!metadataFile.exists()) {
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(metadataFile))) {
            @SuppressWarnings("unchecked")
            Map<String, CertificateInfo> loadedCertificates = (Map<String, CertificateInfo>) ois.readObject();
            certificates.putAll(loadedCertificates);
            loggingService.logInfo("Loaded " + certificates.size() + " SSL certificates");
        } catch (Exception e) {
            loggingService.logError("Error loading SSL certificates metadata", e);
        }
    }
    
    /**
     * Persist certificates metadata
     */
    private void persistCertificateMetadata() {
        File metadataFile = new File(certificatesDirectory, "certificates.dat");
        
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(metadataFile))) {
            oos.writeObject(new HashMap<>(certificates));
            loggingService.logInfo("Saved SSL certificates metadata");
        } catch (Exception e) {
            loggingService.logError("Error saving SSL certificates metadata", e);
        }
    }
    
    /**
     * Certificate information class
     */
    public static class CertificateInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String connectionId;
        private String certificateType;
        private String fileName;
        private String filePath;
        private String subject;
        private String issuer;
        private Date validFrom;
        private Date validTo;
        private String serialNumber;
        private String fingerprint;
        private boolean valid;
        private String validationError;
        private Date uploadDate;
        
        // Getters and setters
        public String getConnectionId() { return connectionId; }
        public void setConnectionId(String connectionId) { this.connectionId = connectionId; }
        
        public String getCertificateType() { return certificateType; }
        public void setCertificateType(String certificateType) { this.certificateType = certificateType; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        
        public Date getValidFrom() { return validFrom; }
        public void setValidFrom(Date validFrom) { this.validFrom = validFrom; }
        
        public Date getValidTo() { return validTo; }
        public void setValidTo(Date validTo) { this.validTo = validTo; }
        
        public String getSerialNumber() { return serialNumber; }
        public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
        
        public String getFingerprint() { return fingerprint; }
        public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public String getValidationError() { return validationError; }
        public void setValidationError(String validationError) { this.validationError = validationError; }
        
        public Date getUploadDate() { return uploadDate; }
        public void setUploadDate(Date uploadDate) { this.uploadDate = uploadDate; }
    }
}