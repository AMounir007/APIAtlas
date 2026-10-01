package com.apiatlas.utility;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** SSRF protection: rejects hosts that resolve to loopback, private, link-local or metadata addresses. */
public final class UrlGuard {
    private UrlGuard() {}

    /** @return true when the host is, or resolves to, an internal address (or cannot be resolved). */
    public static boolean isInternal(String host) {
        if (host == null || host.isBlank()) return true;
        try {
            for (InetAddress a : InetAddress.getAllByName(host)) {
                if (isInternal(a)) return true;
            }
            return false;
        } catch (UnknownHostException ex) {
            return true;
        }
    }

    static boolean isInternal(InetAddress a) {
        if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
                || a.isSiteLocalAddress() || a.isMulticastAddress()) {
            return true;
        }
        byte[] b = a.getAddress();
        if (b.length == 16) {
            return (b[0] & 0xfe) == 0xfc;                       // IPv6 unique local fc00::/7
        }
        int first = b[0] & 0xff;
        int second = b[1] & 0xff;
        return first == 0                                      // 0.0.0.0/8
                || (first == 100 && second >= 64 && second <= 127) // carrier-grade NAT 100.64.0.0/10
                || (first == 169 && second == 254);            // link-local / cloud metadata
    }
}
