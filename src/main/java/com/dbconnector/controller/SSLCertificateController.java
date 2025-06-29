package com.dbconnector.controller;

import com.dbconnector.service.SSLCertificateService;
import com.dbconnector.service.LoggingService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/ssl-certificates")
public class SSLCertificateController {

    @Autowired
    private SSLCertificateService sslCertificateService;
    
    @Autowired
    private LoggingService loggingService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadCertificate(
            @RequestParam("connectionId") String connectionId,
            @RequestParam("certificateType") String certificateType,
            @RequestParam("certificateFile") MultipartFile certificateFile) {
        
        loggingService.logAudit("Uploading SSL certificate: " + certificateType + " for connection: " + connectionId);
        
        try {
            SSLCertificateService.CertificateInfo certInfo = sslCertificateService.uploadCertificate(
                connectionId, certificateType, certificateFile);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Certificate uploaded successfully",
                "certificate", Map.of(
                    "type", certInfo.getCertificateType(),
                    "subject", certInfo.getSubject() != null ? certInfo.getSubject() : "N/A",
                    "issuer", certInfo.getIssuer() != null ? certInfo.getIssuer() : "N/A",
                    "validFrom", certInfo.getValidFrom() != null ? certInfo.getValidFrom().toString() : "N/A",
                    "validTo", certInfo.getValidTo() != null ? certInfo.getValidTo().toString() : "N/A",
                    "fingerprint", certInfo.getFingerprint() != null ? certInfo.getFingerprint() : "N/A",
                    "valid", certInfo.isValid(),
                    "validationError", certInfo.getValidationError()
                )
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            loggingService.logError("Error uploading SSL certificate", e);
            
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Failed to upload certificate: " + e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{connectionId}")
    public ResponseEntity<?> getConnectionCertificates(@PathVariable String connectionId) {
        loggingService.logAccess("Retrieving SSL certificates for connection: " + connectionId);
        
        try {
            Map<String, SSLCertificateService.CertificateInfo> certificates = 
                sslCertificateService.getConnectionCertificates(connectionId);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "certificates", certificates
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            loggingService.logError("Error retrieving SSL certificates", e);
            
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Error retrieving certificates: " + e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{connectionId}/{certificateType}")
    public ResponseEntity<?> deleteCertificate(@PathVariable String connectionId, 
                                              @PathVariable String certificateType) {
        loggingService.logAudit("Deleting SSL certificate: " + certificateType + " for connection: " + connectionId);
        
        try {
            boolean deleted = sslCertificateService.deleteCertificate(connectionId, certificateType);
            
            if (deleted) {
                Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Certificate deleted successfully"
                );
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = Map.of(
                    "success", false,
                    "message", "Certificate not found"
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            loggingService.logError("Error deleting SSL certificate", e);
            
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Error deleting certificate: " + e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateCertificate(@RequestParam("certificateFile") MultipartFile certificateFile) {
        loggingService.logAccess("Validating SSL certificate");
        
        try {
            // Create a temporary certificate info for validation
            SSLCertificateService.CertificateInfo certInfo = sslCertificateService.uploadCertificate(
                "temp", "validation", certificateFile);
            
            // Clean up temporary certificate
            sslCertificateService.deleteCertificate("temp", "validation");
            
            Map<String, Object> response = Map.of(
                "success", true,
                "valid", certInfo.isValid(),
                "certificate", Map.of(
                    "subject", certInfo.getSubject() != null ? certInfo.getSubject() : "N/A",
                    "issuer", certInfo.getIssuer() != null ? certInfo.getIssuer() : "N/A",
                    "validFrom", certInfo.getValidFrom() != null ? certInfo.getValidFrom().toString() : "N/A",
                    "validTo", certInfo.getValidTo() != null ? certInfo.getValidTo().toString() : "N/A",
                    "fingerprint", certInfo.getFingerprint() != null ? certInfo.getFingerprint() : "N/A"
                ),
                "validationError", certInfo.getValidationError()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            loggingService.logError("Error validating SSL certificate", e);
            
            Map<String, Object> response = Map.of(
                "success", false,
                "valid", false,
                "message", "Certificate validation failed: " + e.getMessage()
            );
            
            return ResponseEntity.ok(response);
        }
    }
}