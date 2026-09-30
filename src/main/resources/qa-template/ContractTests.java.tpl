package tests;

import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

public class ContractTests extends BaseTest {
    @Test(dataProvider = "endpoints", groups = "contract")
    public void responseMatchesRecordedContract(EndpointSpec s) {
        if (s.responseContentType() == null || s.expectedStatus() < 200 || s.expectedStatus() >= 300) {
            throw new SkipException("No successful baseline response recorded for " + s);
        }
        requireTokenIfNeeded(s);
        Response r = call(s, true, "1");
        if (r.statusCode() >= 200 && r.statusCode() < 300) {
            String expected = s.responseContentType().split(";")[0].trim();
            String actual = r.contentType() == null ? "" : r.contentType().split(";")[0].trim();
            Assert.assertEquals(actual, expected, s + " content type changed");
            if (expected.contains("json")) {
                Assert.assertNotNull(r.jsonPath().get("$"), s + " body is not valid JSON");
            }
        }
    }
}
