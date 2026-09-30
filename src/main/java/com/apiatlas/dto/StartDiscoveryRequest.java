package com.apiatlas.dto;

import com.apiatlas.model.Enums.SessionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record StartDiscoveryRequest(
        String name,
        @NotNull SessionType type,
        /** Web: start URL (http/https). Mobile: app path (.apk/.ipa) or package/bundle id. */
        @NotBlank String target,
        String browser,
        /** Mobile only: android | ios. */
        String platform,
        String deviceName,
        String udid,
        Integer maxDepth,
        Integer maxPages,
        Boolean submitForms,
        /** Extra HTTP headers for web crawling (e.g. Authorization). Never persisted. */
        Map<String, String> headers,
        /** Mobile only: accessibility ids to tap first (user flow). */
        List<String> flow,
        /** Mobile only: additional Appium capabilities. */
        Map<String, Object> capabilities) {
}
