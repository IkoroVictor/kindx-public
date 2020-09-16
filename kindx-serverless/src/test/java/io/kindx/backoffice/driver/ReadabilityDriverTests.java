package io.kindx.backoffice.driver;

import com.amazonaws.services.s3.AmazonS3;
import com.google.inject.Binder;
import io.kindx.BaseTests;
import io.kindx.backoffice.service.EventService;
import io.kindx.backoffice.service.QueueService;
import io.kindx.lambda.LambdaFunctions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

import static org.mockito.Mockito.mock;

public class ReadabilityDriverTests extends BaseTests {
    @Before
    public void setup() {
        super.setup();
    }

    @Test
    @Ignore
    public void testReadabilityDriver() {
        WebDriver driver = new ReadabilityLambdaInvocationWebDriver(injector.getInstance(LambdaFunctions.class),
                injector.getInstance(AmazonS3.class));
        driver.get("http://www.gianni.ee/restoran/");
        Assert.assertNotNull(driver.getPageSource());

    }
    @Override
    protected void bindMocks(Binder binder) {
        binder.bind(EventService.class).toInstance(mock(EventService.class));
        binder.bind(QueueService.class).toInstance(mock(QueueService.class));
    }
}
