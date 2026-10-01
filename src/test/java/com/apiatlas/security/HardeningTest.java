package com.apiatlas.security;

import com.apiatlas.config.ProductionSecretsGuard;
import com.apiatlas.crawler.MobileExplorer;
import com.apiatlas.dto.StartDiscoveryRequest;
import com.apiatlas.model.Enums.SessionType;
import com.apiatlas.utility.Masker;
import com.apiatlas.utility.UrlGuard;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HardeningTest {

    @Test
    void urlGuardBlocksInternalAddresses() {
        for (String h : new String[]{"localhost", "127.0.0.1", "10.0.0.5", "192.168.1.1", "172.16.0.1",
                "169.254.169.254", "0.0.0.0", "100.64.0.1", "::1"}) {
            assertTrue(UrlGuard.isInternal(h), h);
        }
        assertTrue(UrlGuard.isInternal(null));
        assertFalse(UrlGuard.isInternal("8.8.8.8"));
    }

    @Test
    void productionGuardRejectsDefaults() {
        assertThrows(IllegalStateException.class, () -> new ProductionSecretsGuard("change-me-x", "tok-123456", "pw-123456"));
        assertThrows(IllegalStateException.class, () -> new ProductionSecretsGuard("a".repeat(40), "change-me", "pw-123456"));
        assertThrows(IllegalStateException.class, () -> new ProductionSecretsGuard("a".repeat(40), "tok-123456", "admin123"));
        assertDoesNotThrow(() -> new ProductionSecretsGuard("a".repeat(40), "tok-123456", "pw-123456"));
    }

    private static StartDiscoveryRequest mobile(String target, Map<String, Object> caps) {
        return new StartDiscoveryRequest("n", SessionType.MOBILE, target, null, "android", null, null,
                null, null, null, null, null, caps);
    }

    @Test
    void mobileTargetMustBePackageOrPlainFile() {
        assertDoesNotThrow(() -> MobileExplorer.validate(mobile("com.example.app", null)));
        assertDoesNotThrow(() -> MobileExplorer.validate(mobile("app-debug.apk", null)));
        for (String bad : new String[]{"http://169.254.169.254/x.apk", "file:///etc/passwd", "../../x.apk", "C:\\x.apk", "/data/x.apk"}) {
            assertThrows(IllegalArgumentException.class, () -> MobileExplorer.validate(mobile(bad, null)), bad);
        }
    }

    @Test
    void mobileCapabilitiesAreAllowListed() {
        assertDoesNotThrow(() -> MobileExplorer.validate(mobile("com.example.app", Map.of("noReset", true))));
        assertThrows(IllegalArgumentException.class,
                () -> MobileExplorer.validate(mobile("com.example.app", Map.of("appium:directConnect", true))));
        assertThrows(IllegalArgumentException.class,
                () -> MobileExplorer.validate(mobile("com.example.app", Map.of("chromedriverExecutable", "/tmp/x"))));
    }

    @Test
    void secretsAreMaskedEvenWhenBodyIsLongerThanLimit() {
        String body = "{\"id\":1,\"pad\":\"" + "x".repeat(50) + "\",\"password\":\"hunter2\"}";
        String stored = Masker.truncate(Masker.maskBody(body), 80);
        assertFalse(stored.contains("hunter2"));
    }
}

