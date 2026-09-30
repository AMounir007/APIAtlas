package com.apiatlas.controller;

import com.apiatlas.exporter.*;
import com.apiatlas.model.SecurityFinding;
import com.apiatlas.repository.SecurityFindingRepository;
import com.apiatlas.service.CatalogService;
import com.apiatlas.service.StatisticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Export center: every format can be restricted to a single service with {@code ?service=host}. */
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {
    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final CatalogService catalog;
    private final OpenApiExporter openApi;
    private final PostmanExporter postman;
    private final TabularExporter tabular;
    private final HtmlExporter html;
    private final TestSuiteGenerator tests;
    private final StatisticsService statistics;
    private final SecurityFindingRepository findings;
    private final ObjectMapper mapper;

    @GetMapping("/swagger")
    public ResponseEntity<byte[]> swagger(@RequestParam(required = false) String service) throws Exception {
        return file(mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(
                openApi.build(catalog.listAll(service), "API Atlas - " + (service == null ? "All services" : service))),
                MediaType.APPLICATION_JSON, "openapi.json", false);
    }

    @GetMapping("/postman")
    public ResponseEntity<byte[]> postman(@RequestParam(required = false) String service) throws Exception {
        return file(mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(
                postman.build(catalog.listAll(service), "API Atlas - " + (service == null ? "All services" : service))),
                MediaType.APPLICATION_JSON, "postman_collection.json", true);
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> excel(@RequestParam(required = false) String service) throws Exception {
        List<SecurityFinding> open = findings.findByResolvedFalseOrderByDetectedAtDesc();
        return file(tabular.excel(catalog.listAll(service), open), XLSX, "api-atlas.xlsx", true);
    }

    @GetMapping("/csv")
    public ResponseEntity<byte[]> csv(@RequestParam(required = false) String service) throws Exception {
        return file(tabular.csv(catalog.listAll(service)), MediaType.parseMediaType("text/csv"), "api-atlas.csv", true);
    }

    @GetMapping("/json")
    public ResponseEntity<byte[]> json(@RequestParam(required = false) String service) throws Exception {
        return file(mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(catalog.listAll(service)),
                MediaType.APPLICATION_JSON, "api-atlas.json", true);
    }

    @GetMapping("/html")
    public ResponseEntity<byte[]> html(@RequestParam(required = false) String service) {
        return file(html.documentation(catalog.listAll(service), "API Atlas Documentation").getBytes(StandardCharsets.UTF_8),
                MediaType.TEXT_HTML, "api-atlas.html", true);
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf(@RequestParam(required = false) String service) throws Exception {
        return file(tabular.pdf(catalog.listAll(service)), MediaType.APPLICATION_PDF, "api-atlas.pdf", true);
    }

    @GetMapping("/security-report")
    public ResponseEntity<byte[]> securityReport() {
        return file(html.securityReport(statistics.securityReport()).getBytes(StandardCharsets.UTF_8),
                MediaType.TEXT_HTML, "security-report.html", true);
    }

    @GetMapping("/tests")
    public ResponseEntity<byte[]> tests(@RequestParam(required = false) String service) throws Exception {
        return file(tests.generate(catalog.listAll(service)), MediaType.parseMediaType("application/zip"), "api-atlas-tests.zip", true);
    }

    private static ResponseEntity<byte[]> file(byte[] body, MediaType type, String name, boolean attachment) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(type);
        h.setContentDisposition((attachment ? ContentDisposition.attachment() : ContentDisposition.inline()).filename(name).build());
        h.set("X-Content-Type-Options", "nosniff");
        return ResponseEntity.ok().headers(h).body(body);
    }
}
