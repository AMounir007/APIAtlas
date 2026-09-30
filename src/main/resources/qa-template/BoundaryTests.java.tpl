package tests;

import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.List;

public class BoundaryTests extends BaseTest {
    @Test(dataProvider = "endpoints", groups = "boundary")
    public void extremeIdentifiersDoNotCauseServerError(EndpointSpec s) {
        if (!hasPathParams(s)) throw new SkipException(s + " has no path parameters");
        requireTokenIfNeeded(s);
        List<String> values = List.of("0", "-1", "2147483648", "9".repeat(40), "a".repeat(512));
        for (String v : values) {
            Response r = call(s, true, v);
            Assert.assertTrue(r.statusCode() < 500, s + " with boundary value returned " + r.statusCode());
        }
    }
}
