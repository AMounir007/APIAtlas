package com.apiatlas.exporter;

import com.apiatlas.model.ApiEndpoint;
import com.apiatlas.model.ApiResponse;
import com.apiatlas.utility.PathNormalizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generates a ready-to-run Maven project (Java 21, REST Assured, TestNG) with data-driven smoke, regression,
 * contract, negative and boundary tests. Endpoint data is exported to {@code endpoints.json}; the test classes are
 * generic templates, so nothing user-controlled is ever compiled as code.
 */
@Component
@RequiredArgsConstructor
public class TestSuiteGenerator {
    private static final Map<String, String> TEMPLATES = new LinkedHashMap<>();

    static {
        TEMPLATES.put("pom.xml.tpl", "pom.xml");
        TEMPLATES.put("testng.xml.tpl", "testng.xml");
        TEMPLATES.put("README.md.tpl", "README.md");
        TEMPLATES.put("dev.properties.tpl", "src/test/resources/config/dev.properties");
        for (String n : List.of("BaseTest", "SmokeTests", "RegressionTests", "ContractTests", "NegativeTests", "BoundaryTests")) {
            TEMPLATES.put(n + ".java.tpl", "src/test/java/tests/" + n + ".java");
        }
    }

    private final EndpointDetails details;
    private final ObjectMapper mapper;

    public record EndpointSpec(String method, String baseUrl, String path, String authType, int expectedStatus,
                               String requestBody, String contentType, String responseContentType) {}

    public byte[] generate(List<ApiEndpoint> endpoints) throws IOException {
        List<EndpointSpec> specs = new ArrayList<>();
        for (ApiEndpoint e : endpoints) {
            Optional<ApiResponse> latest = details.responsesByStatus(e.getId()).values().stream().findFirst();
            specs.add(new EndpointSpec(e.getMethod(), PathNormalizer.baseUrl(e.getUrl(), e.getPath()), e.getPath(),
                    e.getAuthType().name(), latest.map(ApiResponse::getStatusCode).orElse(0), e.getSampleRequest(),
                    details.requestHeaders(e.getId()).get("content-type"), latest.map(ApiResponse::getContentType).orElse(null)));
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> t : TEMPLATES.entrySet()) {
                try (InputStream in = new ClassPathResource("qa-template/" + t.getKey()).getInputStream()) {
                    put(zip, t.getValue(), in.readAllBytes());
                }
            }
            put(zip, "src/test/resources/endpoints.json", mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(specs));
            zip.finish();
            return out.toByteArray();
        }
    }

    private static void put(ZipOutputStream zip, String name, byte[] data) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }
}
