package com.apiatlas.crawler;

import com.apiatlas.config.AtlasProperties;
import com.apiatlas.dto.CapturedTraffic;
import com.apiatlas.dto.StartDiscoveryRequest;
import com.apiatlas.model.Enums.Protocol;
import com.apiatlas.service.TrafficIngestionService;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

/** Playwright based crawler: navigates pages, interacts with the UI and captures every XHR/fetch/WebSocket call. */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebCrawler {
    private static final Set<String> API_RESOURCE_TYPES = Set.of("xhr", "fetch", "eventsource");
    private static final Pattern DESTRUCTIVE = Pattern.compile("(?i)delete|remove|logout|log out|sign out|cancel|deactivate|unsubscribe|pay|purchase|confirm");
    private static final String FORM_INPUTS =
            "input:not([type=hidden]):not([type=password]):not([type=submit]):not([type=button]):not([type=checkbox]):not([type=radio]):not([type=file])";

    private final AtlasProperties props;
    private final TrafficIngestionService ingestion;

    private record Node(String url, int depth) {}

    /** @return number of pages visited */
    public int crawl(Long sessionId, StartDiscoveryRequest req, BooleanSupplier cancelled) {
        var web = props.discovery().web();
        int maxDepth = req.maxDepth() != null ? req.maxDepth() : web.maxDepth();
        int maxPages = req.maxPages() != null ? req.maxPages() : web.maxPages();
        boolean submitForms = Boolean.TRUE.equals(req.submitForms());
        double timeoutMs = web.navigationTimeout().toMillis();
        URI start = URI.create(req.target());

        Set<String> visited = new HashSet<>();
        Deque<Node> queue = new ArrayDeque<>();
        queue.add(new Node(req.target(), 0));

        try (Playwright pw = Playwright.create()) {
            Browser browser = launch(pw, req.browser() != null ? req.browser() : web.defaultBrowser(), web.headless());
            try (BrowserContext ctx = browser.newContext()) {
                if (req.headers() != null && !req.headers().isEmpty()) {
                    // Caller-supplied headers (e.g. Authorization) go to the start origin only, never to third parties.
                    Map<String, String> extra = req.headers();
                    ctx.route("**/*", route -> {
                        if (sameOrigin(start, route.request().url())) {
                            Map<String, String> merged = new HashMap<>(route.request().headers());
                            merged.putAll(extra);
                            route.resume(new Route.ResumeOptions().setHeaders(merged));
                        } else {
                            route.resume();
                        }
                    });
                }
                ctx.onResponse(r -> capture(sessionId, r));
                Page page = ctx.newPage();
                page.setDefaultTimeout(timeoutMs);
                page.onWebSocket(ws -> ingestion.ingest(new CapturedTraffic(sessionId, ws.url(), "GET", Map.of(), null, 101,
                        Map.of(), null, null, 0L, Protocol.WEBSOCKET)));

                while (!queue.isEmpty() && visited.size() < maxPages && !cancelled.getAsBoolean()) {
                    Node n = queue.poll();
                    if (!visited.add(stripFragment(n.url()))) continue;
                    try {
                        page.navigate(n.url());
                        try {
                            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(5000));
                        } catch (PlaywrightException ignored) {
                            // long-polling pages never become idle
                        }
                        if (n.depth() < maxDepth) {
                            for (String link : links(page)) {
                                if (allowed(start, link, web.sameOriginOnly()) && !visited.contains(stripFragment(link))) {
                                    queue.add(new Node(link, n.depth() + 1));
                                }
                            }
                        }
                        interact(page, submitForms);
                    } catch (PlaywrightException ex) {
                        log.debug("Navigation failed for {}: {}", n.url(), ex.getMessage());
                    }
                }
            } finally {
                browser.close();
            }
        }
        return visited.size();
    }

    private void capture(Long sessionId, Response r) {
        try {
            Request q = r.request();
            if (!API_RESOURCE_TYPES.contains(q.resourceType())) return;
            Map<String, String> respHeaders = r.allHeaders();
            String ct = respHeaders.get("content-type");
            String body = null;
            if (ct == null || !ct.contains("text/event-stream")) {
                try {
                    body = r.text();
                } catch (PlaywrightException ignored) {
                    // body unavailable (redirect, aborted request ...)
                }
            }
            long ms = 0;
            var timing = q.timing();
            if (timing != null && timing.responseEnd > 0) ms = (long) timing.responseEnd;
            ingestion.ingest(new CapturedTraffic(sessionId, q.url(), q.method(), q.allHeaders(), q.postData(),
                    r.status(), respHeaders, body, ct, ms, null));
        } catch (Exception ex) {
            log.debug("Capture failed: {}", ex.getMessage());
        }
    }

    private static Browser launch(Playwright pw, String name, boolean headless) {
        BrowserType.LaunchOptions o = new BrowserType.LaunchOptions().setHeadless(headless);
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "firefox" -> pw.firefox().launch(o);
            case "webkit", "safari" -> pw.webkit().launch(o);
            case "msedge", "edge" -> pw.chromium().launch(o.setChannel("msedge"));
            case "chrome" -> pw.chromium().launch(o.setChannel("chrome"));
            default -> pw.chromium().launch(o);
        };
    }

    @SuppressWarnings("unchecked")
    private static List<String> links(Page page) {
        Object res = page.evalOnSelectorAll("a[href]", "els => els.map(e => e.href)");
        return res instanceof List<?> l ? (List<String>) l : List.of();
    }

    private static boolean allowed(URI start, String link, boolean sameOriginOnly) {
        try {
            URI u = URI.create(link);
            if (u.getScheme() == null || !(u.getScheme().equals("http") || u.getScheme().equals("https"))) return false;
            return !sameOriginOnly || Objects.equals(u.getHost(), start.getHost());
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static boolean sameOrigin(URI start, String url) {
        try {
            URI u = URI.create(url);
            return u.getScheme() != null && u.getHost() != null
                    && u.getScheme().equalsIgnoreCase(start.getScheme())
                    && u.getHost().equalsIgnoreCase(start.getHost())
                    && effectivePort(u) == effectivePort(start);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static int effectivePort(URI u) {
        if (u.getPort() > 0) return u.getPort();
        return "https".equalsIgnoreCase(u.getScheme()) ? 443 : 80;
    }

    private static String stripFragment(String url) {
        int i = url.indexOf('#');
        return i < 0 ? url : url.substring(0, i);
    }

    private void interact(Page page, boolean submitForms) {
        String before = page.url();
        try {
            Locator buttons = page.locator("button:visible, [role=button]:visible");
            int count = Math.min(buttons.count(), 8);
            for (int i = 0; i < count; i++) {
                Locator b = buttons.nth(i);
                String label = Objects.toString(b.innerText(), "");
                if (DESTRUCTIVE.matcher(label).find()) continue;
                b.click(new Locator.ClickOptions().setTimeout(1500));
                page.waitForTimeout(300);
                if (!page.url().equals(before)) break;
            }
            if (submitForms && page.url().equals(before)) {
                submitForms(page);
            }
        } catch (PlaywrightException ex) {
            log.debug("Interaction skipped: {}", ex.getMessage());
        }
    }

    private void submitForms(Page page) {
        Locator forms = page.locator("form:visible");
        int count = Math.min(forms.count(), 3);
        for (int i = 0; i < count; i++) {
            Locator form = forms.nth(i);
            if (form.locator("input[type=password]").count() > 0) continue; // never touch login forms
            Locator inputs = form.locator(FORM_INPUTS);
            for (int j = 0; j < inputs.count(); j++) {
                Locator in = inputs.nth(j);
                String type = Objects.toString(in.getAttribute("type"), "text").toLowerCase(Locale.ROOT);
                in.fill(switch (type) {
                    case "email" -> "test@example.com";
                    case "number" -> "1";
                    case "tel" -> "5551234567";
                    default -> "test";
                }, new Locator.FillOptions().setTimeout(1000));
            }
            Locator submit = form.locator("[type=submit]");
            if (submit.count() > 0) {
                submit.first().click(new Locator.ClickOptions().setTimeout(1500));
                page.waitForTimeout(500);
            }
        }
    }
}
