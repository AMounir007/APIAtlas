package tests;

import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

public class RegressionTests extends BaseTest {
    @Test(dataProvider = "endpoints", groups = "regression")
    public void statusMatchesRecordedBaseline(EndpointSpec s) {
        if (s.expectedStatus() <= 0) throw new SkipException("No baseline status recorded for " + s);
        requireTokenIfNeeded(s);
        Response r = call(s, true, "1");
        Assert.assertEquals(r.statusCode(), s.expectedStatus(), s + " status changed");
    }
}
