package io.kindx.backoffice.driver;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import io.kindx.backoffice.exception.CrawlerException;
import io.kindx.dto.function.ReadabilityRequest;
import io.kindx.dto.function.ReadabilityResponse;
import io.kindx.lambda.LambdaFunctions;
import io.kindx.util.IDUtil;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

public class ReadabilityLambdaInvocationWebDriver implements WebDriver {

    private String url;

    private static final String READABILITY_S3_BUCKET = "readability-html-files";

    private LambdaFunctions functions;
    private AmazonS3 amazonS3;

    public ReadabilityLambdaInvocationWebDriver(LambdaFunctions functions,
                                                AmazonS3 amazonS3) {
        this.functions = functions;
        this.amazonS3 = amazonS3;
    }

    @Override
    public void get(String url) {
        this.url = url;
    }

    @Override
    public String getCurrentUrl() {
        return url;
    }

    @Override
    public String getTitle() {
        throw new UnsupportedOperationException("");
    }

    @Override
    public List<WebElement> findElements(By by) {
        throw new UnsupportedOperationException("");
    }

    @Override
    public WebElement findElement(By by) {
        throw new UnsupportedOperationException("");
    }

    @Override
    @SneakyThrows
    public String getPageSource() {
        ReadabilityResponse response = functions.execReadabilityProcessor(ReadabilityRequest
                .builder().url(url)
                .sanitize(true)
                .key(IDUtil.generateGenericId())
                .build());
        if (!response.isSuccess()) {
            throw new CrawlerException(String.format("Could not load readable webpage for '%s' : [%s]",
                    url, response.toString()), null);
        }
        S3ObjectInputStream stream = amazonS3.getObject(READABILITY_S3_BUCKET, response.getContentKey())
                .getObjectContent();
        return IOUtils.toString(stream, StandardCharsets.UTF_8);
    }

    @Override
    public void close() {

    }

    @Override
    public void quit() {

    }

    @Override
    public Set<String> getWindowHandles() {
        throw new UnsupportedOperationException("");
    }

    @Override
    public String getWindowHandle() {
        throw new UnsupportedOperationException("");
    }

    @Override
    public TargetLocator switchTo() {
        throw new UnsupportedOperationException("");
    }

    @Override
    public Navigation navigate() {
        throw new UnsupportedOperationException("");
    }

    @Override
    public Options manage() {
        throw new UnsupportedOperationException("");
    }
}
