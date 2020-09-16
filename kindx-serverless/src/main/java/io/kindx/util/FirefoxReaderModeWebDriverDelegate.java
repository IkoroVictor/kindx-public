package io.kindx.util;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URLEncoder;
import java.util.List;
import java.util.Set;


@AllArgsConstructor
public class FirefoxReaderModeWebDriverDelegate implements WebDriver {

    private WebDriver driver;
    private long readerWaitTimeoutSeconds;

    @Override
    @SneakyThrows
    public void get(String url) {
        driver.get("about:reader?url=" + URLEncoder.encode(url,"UTF-8"));
        new WebDriverWait(driver, readerWaitTimeoutSeconds)
                .until(ExpectedConditions.or(
                        ExpectedConditions.presenceOfElementLocated(By.className("page")),
                        ExpectedConditions.attributeToBe(By.xpath("/*"), "data-is-error", "true")));
    }

    @Override
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    @Override
    public String getTitle() {
        return driver.getTitle();
    }

    @Override
    public List<WebElement> findElements(By by) {
        return driver.findElements(by);
    }

    @Override
    public WebElement findElement(By by) {
        return driver.findElement(by);
    }

    @Override
    public String getPageSource() {
        try {
            driver.findElement(By.className("page"));
        } catch (NoSuchElementException e) {
            throw new WebDriverException("Reader mode page not loaded for " + driver.getCurrentUrl(), e);
        }
        return driver.getPageSource();
    }

    @Override
    public void close() {
        driver.close();
    }

    @Override
    public void quit() {
        driver.quit();
    }

    @Override
    public Set<String> getWindowHandles() {
        return driver.getWindowHandles();
    }

    @Override
    public String getWindowHandle() {
        return driver.getWindowHandle();
    }

    @Override
    public TargetLocator switchTo() {
        return driver.switchTo();
    }

    @Override
    public Navigation navigate() {
        return driver.navigate();
    }

    @Override
    public Options manage() {
        return driver.manage();
    }
}
