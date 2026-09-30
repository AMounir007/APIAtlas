package tests;

import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.List;

public class NegativeTests extends BaseTest {
    @Test(dataProvider = "endpoints", groups = "negative")
    public void unauthenticatedCallIsRejected(EndpointSpec s) {
        if ("NONE".equals(s.authType())) throw new SkipException(s + " is anonymous");
        Response r = call(s, false, "1");
        Assert.assertTrue(List.of(401, 403).contains(r.statusCode()), s + " accepted an unauthenticated call: " + r.statusCode());
    }

    @Test(dataProvider = "endpoints", groups = "negative")
    public void malformedIdentifierDoesNotCauseServerError(EndpointSpec s) {
        if (!hasPathParams(s)) throw new SkipException(s + " has no path parameters");
        requireTokenIfNeeded(s);
        for (String bad : List.of("abc", "%00", "'")) {
            Response r = call(s, true, bad);
            Assert.assertTrue(r.statusCode() < 500, s + " with '" + bad + "' returned " + r.statusCode());
        }
    }
}
