package com.example.backend.common.util;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;

public final class UrlValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private UrlValidator() {}

    /**
     * Validates that a URL is safe from SSRF attacks:
     * - Only HTTP/HTTPS allowed
     * - Hostname must resolve
     * - Cannot resolve to loopback (127.0.0.1, ::1)
     * - Cannot resolve to site-local / private networks (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16)
     * - Cannot resolve to link-local (169.254.0.0/16 - AWS/cloud metadata)
     * - Cannot resolve to any local or multicast address
     */
    public static boolean isSafeUrl(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            return false;
        }

        try {
            URI uri = URI.create(urlString.trim());
            String scheme = uri.getScheme();
            if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
                return false;
            }

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }

            // Check for explicit local hostnames
            String lowerHost = host.toLowerCase();
            if (lowerHost.equals("localhost") || lowerHost.endsWith(".localhost") || lowerHost.endsWith(".local") || lowerHost.endsWith(".internal")) {
                return false;
            }

            // Resolve IP and check boundaries
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (address.isLoopbackAddress() ||
                    address.isSiteLocalAddress() ||
                    address.isLinkLocalAddress() ||
                    address.isAnyLocalAddress() ||
                    address.isMulticastAddress()) {
                    return false;
                }

                byte[] ipBytes = address.getAddress();
                // Check 169.254.x.x (AWS/GCP/Azure instance metadata)
                if (ipBytes.length == 4 && (ipBytes[0] & 0xFF) == 169 && (ipBytes[1] & 0xFF) == 254) {
                    return false;
                }
                // Check 0.0.0.0/8
                if (ipBytes.length == 4 && (ipBytes[0] & 0xFF) == 0) {
                    return false;
                }
            }

            return true;
        } catch (UnknownHostException e) {
            // Unresolvable host is unsafe to connect to
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
