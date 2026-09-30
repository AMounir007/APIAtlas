package tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.SkipException;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public abstract class BaseTest {
    public record EndpointSpec(String method, String baseUrl, String path, String authType, int expectedStatus,
                               String requestBody, String contentType, String responseContentType) {
        @Override
        public String toString() {
            return method + " " + path;
        }
    }

    protected static final Properties CONFIG = new Properties();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<String> SAFE_METHODS = List.of("GET", "HEAD");

    @BeforeSuite(alwaysRun = true)
    public void loadConfig() throws Exception {
        String env = System.getProperty("env", "dev");
        try (InputStream in = BaseTest.class.getResourceAsStream("/config/" + env + ".properties")) {
            if (in != null) CONFIG.load(in);
        }
        if (Boolean.parseBoolean(System.getProperty("relaxedHttps", CONFIG.getProperty("relaxedHttps", "false")))) {
            RestAssured.useRelaxedHTTPSValidation();
        }
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
    }

    @DataProvider(name = "endpoints", parallel = true)
    public static Object[][] endpoints() throws Exception {
        boolean mutating = Boolean.getBoolean("allow.mutating");
        try (InputStream in = BaseTest.class.getResourceAsStream("/endpoints.json")) {
            return Arrays.stream(MAPPER.readValue(in, EndpointSpec[].class))
                    .filter(s -> mutating || SAFE_METHODS.contains(s.method()))
                    .map(s -> new Object[]{s})
                    .toArray(Object[][]::new);
        }
    }

    protected static String token() {
        String t = System.getProperty("token", CONFIG.getProperty("token"));
        return t == null || t.isBlank() ? null : t;
    }

    protected static void requireTokenIfNeeded(EndpointSpec s) {
        if (!"NONE".equals(s.authType()) && token() == null) {
            throw new SkipException("No -Dtoken provided for authenticated endpoint " + s);
        }
    }

    protected static String resolve(String path, String value) {
        return path.replaceAll("\\{uuid\\d*}", "00000000-0000-0000-0000-000000000001").replaceAll("\\{[^}]+}", value);
    }

    protected static boolean hasPathParams(EndpointSpec s) {
        return s.path().contains("{");
    }

    protected static RequestSpecification request(EndpointSpec s, boolean authenticated) {
        String base = System.getProperty("baseUrl", CONFIG.getProperty("baseUrl", s.baseUrl()));
        RequestSpecification r = RestAssured.given().baseUri(base).accept("*/*");
        if (authenticated && token() != null && !"NONE".equals(s.authType())) {
            r.header("Authorization", "Bearer " + token());
        }
        if (s.requestBody() != null && s.contentType() != null) {
            r.contentType(s.contentType()).body(s.requestBody());
        }
        return r;
    }

    protected static Response call(EndpointSpec s, boolean authenticated, String pathValue) {
        return request(s, authenticated).request(s.method(), resolve(s.path(), pathValue));
    }
}
