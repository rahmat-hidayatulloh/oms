package com.oms.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Resolves the client identifier for rate limiting.
 * <p>
 * Strategy: userId for authenticated users, IP address for anonymous.
 * Trusted proxy headers (X-Forwarded-For) are resolved when the direct
 * connection originates from a configured trusted proxy.
 */
@Slf4j
@Component
public class IpResolver {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private final RateLimitProperties properties;

    public IpResolver(RateLimitProperties properties) {
        this.properties = properties;
    }

    /**
     * Resolves the rate limit key for the current request.
     *
     * @param request the HTTP servlet request
     * @return a unique client identifier (e.g., "user:john" or "ip:192.168.1.1")
     */
    public String resolve(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof String principal
                && !"anonymousUser".equals(principal)) {
            return "user:" + auth.getName();
        }

        String remoteAddr = request.getRemoteAddr();
        String clientIp = remoteAddr != null ? remoteAddr : "unknown";

        // Only trust X-Forwarded-For if the direct connection is from a trusted proxy
        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
        if (StringUtils.hasText(forwardedFor) && isTrustedProxy(remoteAddr)) {
            clientIp = forwardedFor.split(",")[0].trim();
        }

        return "ip:" + clientIp;
    }

    /**
     * Checks if the given IP address belongs to a trusted proxy.
     * Supports exact match and CIDR notation.
     */
    private boolean isTrustedProxy(String ipAddress) {
        if (ipAddress == null || properties.getTrustedProxies().length == 0) {
            return false;
        }
        for (String trusted : properties.getTrustedProxies()) {
            if (trusted.contains("/")) {
                if (isIpInCidr(ipAddress, trusted)) {
                    return true;
                }
            } else if (trusted.equals(ipAddress)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if an IP address is within a CIDR range.
     * Supports both IPv4 and IPv6.
     */
    private boolean isIpInCidr(String ipAddress, String cidr) {
        try {
            String[] parts = cidr.split("/");
            byte[] address = InetAddress.getByName(ipAddress).getAddress();
            byte[] network = InetAddress.getByName(parts[0]).getAddress();
            int prefixLength = Integer.parseInt(parts[1]);

            if (address.length != network.length) {
                return false;
            }

            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (address[i] != network[i]) {
                    return false;
                }
            }

            if (remainingBits > 0) {
                int mask = (0xFF << (8 - remainingBits)) & 0xFF;
                if ((address[fullBytes] & mask) != (network[fullBytes] & mask)) {
                    return false;
                }
            }

            return true;
        } catch (UnknownHostException | NumberFormatException e) {
            log.warn("Invalid IP or CIDR: ip={}, cidr={}", ipAddress, cidr);
            return false;
        }
    }
}
