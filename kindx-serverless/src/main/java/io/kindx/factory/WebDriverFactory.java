package io.kindx.factory;

import com.amazonaws.services.s3.AmazonS3;
import io.kindx.backoffice.driver.ReadabilityLambdaInvocationWebDriver;
import io.kindx.constants.Defaults;
import io.kindx.lambda.LambdaFunctions;
import io.kindx.util.FirefoxReaderModeWebDriverDelegate;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;
import java.util.Arrays;

import static io.kindx.util.EnvUtil.getEnvLongOrDefault;
import static io.kindx.util.EnvUtil.getEnvOrDefault;
import static java.util.concurrent.TimeUnit.SECONDS;

public class WebDriverFactory {

    private static final String BROWSER_TIMEOUT_KEY = "BROWSER_LOAD_TIMEOUT_SECONDS";
    private static final String READER_TIMEOUT_KEY = "READER_WAIT_TIMEOUT_SECONDS";
    private static final String DRIVER_URL_KEY = "REMOTE_DRIVER_URL";

    public static WebDriver getRemoteChromeDriver() throws Exception {
        long timeout = getEnvLongOrDefault(BROWSER_TIMEOUT_KEY,
                Defaults.BROWSER_LOAD_TIMEOUT_SECONDS);
        String server = getEnvOrDefault(DRIVER_URL_KEY, Defaults.CHROME_REMOTE_DRIVER_URL);

        ChromeOptions chromeOptions =  new ChromeOptions();
        chromeOptions.setHeadless(true);
        chromeOptions.addArguments(Arrays.asList(Defaults.CHROME_CAPABILITIES));
        WebDriver driver = new RemoteWebDriver(new URL(server), chromeOptions);
        driver.manage().timeouts().implicitlyWait(timeout, SECONDS);
        return driver;
    }

    public static WebDriver getReadabilityLambdaWebDriver(LambdaFunctions lambdaFunctions, AmazonS3 s3) {
        return new ReadabilityLambdaInvocationWebDriver(lambdaFunctions, s3);
    }

    public static WebDriver getRemoteFirefoxDriver() throws Exception {
        long timeout = getEnvLongOrDefault(BROWSER_TIMEOUT_KEY,
                Defaults.BROWSER_LOAD_TIMEOUT_SECONDS);
        String server = getEnvOrDefault(DRIVER_URL_KEY, Defaults.FIREFOX_REMOTE_DRIVER_URL);

        FirefoxOptions firefoxOptions =  new FirefoxOptions();
        firefoxOptions.setHeadless(true);
        firefoxOptions.addArguments(Arrays.asList(Defaults.FIREFOX_CAPABILITIES));
        WebDriver driver = new RemoteWebDriver(new URL(server), firefoxOptions);
        driver.manage().timeouts().implicitlyWait(timeout, SECONDS);
        return driver;
    }


    public static WebDriver getReaderModeRemoteFirefoxDriver() throws Exception {
        long timeout = getEnvLongOrDefault(READER_TIMEOUT_KEY,
                Defaults.READER_WAIT_TIMEOUT_SECONDS);
       return new FirefoxReaderModeWebDriverDelegate(getRemoteFirefoxDriver(), timeout);
    }
}
