package com.apiatlas.crawler;

import com.apiatlas.config.AtlasProperties;
import com.apiatlas.dto.StartDiscoveryRequest;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;

/**
 * Appium based explorer. Traffic itself is captured by mitmproxy (see docker/mitm/atlas_addon.py), which posts
 * transactions to the ingest endpoint with this session's id; this class only drives the UI.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MobileExplorer {
    private final AtlasProperties props;
    private final Random random = new Random();

    /** @return number of UI actions performed */
    public int explore(Long sessionId, StartDiscoveryRequest req, BooleanSupplier cancelled) throws MalformedURLException, InterruptedException {
        boolean ios = "ios".equalsIgnoreCase(req.platform());
        var url = URI.create(props.discovery().mobile().appiumUrl()).toURL();
        AppiumDriver driver = ios ? new IOSDriver(url, iosOptions(req)) : new AndroidDriver(url, androidOptions(req));
        int actions = 0;
        try {
            if (req.flow() != null) {
                for (String id : req.flow()) {
                    if (cancelled.getAsBoolean()) return actions;
                    driver.findElement(AppiumBy.accessibilityId(id)).click();
                    actions++;
                    Thread.sleep(800);
                }
            }
            String xpath = ios ? "//XCUIElementTypeButton | //XCUIElementTypeCell" : "//*[@clickable='true']";
            int max = props.discovery().mobile().maxActions();
            while (actions < max && !cancelled.getAsBoolean()) {
                try {
                    List<WebElement> elements = driver.findElements(By.xpath(xpath));
                    if (elements.isEmpty()) {
                        driver.navigate().back();
                    } else {
                        elements.get(random.nextInt(elements.size())).click();
                    }
                } catch (RuntimeException ex) {
                    log.debug("UI action failed: {}", ex.getMessage());
                    try {
                        driver.navigate().back();
                    } catch (RuntimeException ignored) {
                        // nothing more to try this round
                    }
                }
                actions++;
                Thread.sleep(800);
            }
        } finally {
            driver.quit();
        }
        return actions;
    }

    private UiAutomator2Options androidOptions(StartDiscoveryRequest req) {
        UiAutomator2Options o = new UiAutomator2Options();
        common(o, req);
        String t = req.target();
        o.setCapability(t.endsWith(".apk") ? "appium:app" : "appium:appPackage", t);
        return o;
    }

    private XCUITestOptions iosOptions(StartDiscoveryRequest req) {
        XCUITestOptions o = new XCUITestOptions();
        common(o, req);
        String t = req.target();
        o.setCapability(t.endsWith(".ipa") || t.endsWith(".app") ? "appium:app" : "appium:bundleId", t);
        return o;
    }

    private static void common(MutableCapabilities o, StartDiscoveryRequest req) {
        if (req.deviceName() != null) o.setCapability("appium:deviceName", req.deviceName());
        if (req.udid() != null) o.setCapability("appium:udid", req.udid());
        if (req.capabilities() != null) req.capabilities().forEach(o::setCapability);
    }
}
