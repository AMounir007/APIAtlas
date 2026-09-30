package tests;

import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SmokeTests extends BaseTest {
    @Test(dataProvider = "endpoints", groups = "smoke")
    public void endpointRespondsWithoutServerError(EndpointSpec s) {
        Response r = call(s, true, "1");
        Assert.assertTrue(r.statusCode() < 500, s + " returned " + r.statusCode());
    }
}
