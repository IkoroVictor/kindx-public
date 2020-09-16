package io.kindx.backoffice.service;


import com.google.inject.Inject;
import io.kindx.backoffice.dto.events.MenuCrawlEvent;
import io.kindx.backoffice.dto.menu.HtmlMenuProcessRequestDto;
import io.kindx.backoffice.exception.CrawlerException;
import io.kindx.elasticsearch.ElasticSearchService;
import io.kindx.util.TextUtil;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.ExpandedTitleContentHandler;
import org.elasticsearch.client.indices.AnalyzeResponse;
import org.openqa.selenium.WebDriver;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static io.restassured.config.RedirectConfig.redirectConfig;

public class MenuCrawlerService {
    private static final Logger logger = LogManager.getLogger(MenuCrawlerService.class);


    private MenuProcessorService processorService;
    private WebDriver webDriver;
    private ElasticSearchService elasticSearchService;
    private PDFParser pdfParser;

    @Inject
    public MenuCrawlerService(MenuProcessorService processorService,
                              WebDriver webDriver,
                              ElasticSearchService elasticSearchService) {
        this.processorService = processorService;
        this.webDriver = webDriver;
        this.elasticSearchService = elasticSearchService;
        this.pdfParser = new PDFParser();
        RestAssured.config().redirect(redirectConfig().followRedirects(true));

    }

    public void processMenuCrawlEvent(MenuCrawlEvent crawlEvent) {
        switch (crawlEvent.getContentType()) {
            case HTML: processHtmlContent(crawlEvent); break;
            case PDF: processPdf(crawlEvent);  break;
            default: logger.error("'" + crawlEvent.getContentType() + "' not supported yet...ignoring...");
        }
    }

    private void processPdf(MenuCrawlEvent event) {
        String source = pdfToHtml(event.getUrl());
        processHtmlContent(event, source, true);
    }

    @SneakyThrows
    private String pdfToHtml(String url) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SAXTransformerFactory factory = (SAXTransformerFactory)
                SAXTransformerFactory.newInstance();
        TransformerHandler handler = factory.newTransformerHandler();
        handler.getTransformer().setOutputProperty(OutputKeys.METHOD, "html");
        handler.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes");
        handler.getTransformer().setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        handler.setResult(new StreamResult(out));
        ExpandedTitleContentHandler expandedTitleContentHandler = new ExpandedTitleContentHandler(handler);
        pdfParser.parse(RestAssured.given().when().get(url).asInputStream(),
                expandedTitleContentHandler, new Metadata(), new ParseContext());
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    @SneakyThrows
    private void processHtmlContent(MenuCrawlEvent crawlEvent) {
        String source = loadPageSource(crawlEvent.getUrl());
        processHtmlContent(crawlEvent, source, false);
    }

    @SneakyThrows
    private void processHtmlContent(MenuCrawlEvent crawlEvent, String source, boolean isPdf) {
        HtmlMenuProcessRequestDto requestDto = HtmlMenuProcessRequestDto.builder()
                .kitchenId(crawlEvent.getKitchenId())
                .menuConfigurationId(crawlEvent.getMenuConfigurationId())
                .url(crawlEvent.getUrl())
                .originalHtml(source)
                .pdfSource(isPdf)
                .strippedText(strippedHtmlText(source))
                .build();
        processorService.processHtmlMenu(requestDto);
    }

    @SneakyThrows
    private String loadPageSource(String url) {
        //TODO: Add retry capabilities with expo backoff or use another client that supports it
        //TODO: optimize by Check ETAG for content change
        try {
            Response response = RestAssured.get(new URL(url));
            if (response.statusCode() < 200 || response.statusCode() > 299) {
                throw new CrawlerException(String.format("Unexpected response code [%d] : for [%s]",
                        response.statusCode(), url), null);
            }
            webDriver.get(url);
            return webDriver.getPageSource();
        } catch (Exception  ex) {
            throw new CrawlerException("Could not load page source for " + url, ex);
        }

    }

    private String strippedHtmlText(String originalHtmlText) {
        AnalyzeResponse response = elasticSearchService.analyzeHtmlStrip(originalHtmlText);
        return TextUtil.cleanUpRedundantNewLines(response.getTokens().get(0).getTerm());
    }
}

