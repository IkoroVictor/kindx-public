package io.kindx.constants;

public class Defaults {
    public static final String SYSTEM_USER_ID = "system";
    public static final String USER_LOCALE = "en-gb";
    public static final int PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 50;

    public static final String KINDX_APP_FACEBOOK_PAGE_USERNAME = "kindx.io";

    public static final String LINE_DELIMITER_REGEX =  "\n";
    public static final String WORD_DELIMITER_REGEX =  "\\s|, ";
    public static final String SYSTEM_TEXT_DELIMITER_REGEX =  " ";
    public static final long POST_TIME_WINDOW_SECONDS = 7200;
    public static final long BROWSER_LOAD_TIMEOUT_SECONDS = 5;
    public static final long READER_WAIT_TIMEOUT_SECONDS = 10;
    public static final float SCORE_NOTIFICATION_THRESHOLD = 1;
    public static final long MAX_GEO_DISTANCE_KM = 200;
    public static final int MAX_LOCATION_SEARCH_RADIUS_METERS = 50000;
    public static final long REPROCESS_RADIUS_METERS = 50000;
    public static final long PLACES_CACHE_TTL_SECONDS = 432000; //5 days
    public static final int MAX_RADIUS_FACTOR_METERS = 4;
    public static final int DEFAULT_PLACES_POLL_SEARCH_RADIUS_METERS = 1000;
    public static final long PAGE_TOKEN_TTL_SECONDS = 600 ;
    public static final String ELASTIC_SEARCH_HOSTS = "http://docker.for.mac.localhost:9200,http://docker.for.mac.localhost:9300";
    public static final String CHROME_REMOTE_DRIVER_URL = "https://chrome.browserless.io/webdriver";
    public static final String FIREFOX_REMOTE_DRIVER_URL = "https://firefox.browserless.io/webdriver";
    public static final String[] CHROME_CAPABILITIES = new String[] {
            "--disable-background-timer-throttling",
            "--disable-backgrounding-occluded-windows",
            "--disable-breakpad",
            "--disable-component-extensions-with-background-pages",
            "--disable-dev-shm-usage",
            "--disable-extensions",
            "--disable-features=TranslateUI,BlinkGenPropertyTrees",
            "--disable-ipc-flooding-protection",
            "--disable-renderer-backgrounding",
            "--enable-features=NetworkService,NetworkServiceInProcess",
            "--force-color-profile=srgb",
            "--hide-scrollbars",
            "--metrics-recording-only",
            "--mute-audio",
            "--headless",
            "--no-sandbox",
           "--enable-reader-mode=true"
    };

    public static final String[] FIREFOX_CAPABILITIES = new String[]{};

}
