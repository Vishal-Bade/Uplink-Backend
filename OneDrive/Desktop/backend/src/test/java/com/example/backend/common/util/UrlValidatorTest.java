package com.example.backend.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UrlValidatorTest {

    @Test
    void testSafePublicUrls() {
        assertTrue(UrlValidator.isSafeUrl("https://example.com/research-paper"));
        assertTrue(UrlValidator.isSafeUrl("https://en.wikipedia.org/wiki/Semiconductor"));
        assertTrue(UrlValidator.isSafeUrl("http://nature.com/articles/s41586"));
    }

    @Test
    void testSsrfLoopbackBlocked() {
        assertFalse(UrlValidator.isSafeUrl("http://localhost:8080/admin"));
        assertFalse(UrlValidator.isSafeUrl("http://127.0.0.1:5432"));
        assertFalse(UrlValidator.isSafeUrl("http://127.0.0.2"));
        assertFalse(UrlValidator.isSafeUrl("http://[::1]:8080"));
    }

    @Test
    void testSsrfCloudMetadataBlocked() {
        // AWS / GCP / Azure Instance Metadata endpoint
        assertFalse(UrlValidator.isSafeUrl("http://169.254.169.254/latest/meta-data/"));
        assertFalse(UrlValidator.isSafeUrl("http://169.254.1.1"));
    }

    @Test
    void testSsrfPrivateNetworkBlocked() {
        assertFalse(UrlValidator.isSafeUrl("http://10.0.0.1/internal"));
        assertFalse(UrlValidator.isSafeUrl("http://192.168.1.1/router"));
    }

    @Test
    void testNonHttpSchemesBlocked() {
        assertFalse(UrlValidator.isSafeUrl("file:///etc/passwd"));
        assertFalse(UrlValidator.isSafeUrl("ftp://files.example.com"));
        assertFalse(UrlValidator.isSafeUrl("gopher://old.net"));
        assertFalse(UrlValidator.isSafeUrl(null));
        assertFalse(UrlValidator.isSafeUrl(""));
    }
}
