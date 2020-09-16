package io.kindx;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.invoke.LambdaInvokerFactory;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import com.google.maps.GeoApiContext;
import feign.Feign;
import feign.Retryer;
import feign.jackson.JacksonDecoder;
import io.kindx.backoffice.processor.notification.NotificationProcessor;
import io.kindx.backoffice.processor.notification.TwilioNotificationProcessor;
import io.kindx.backoffice.processor.template.FileTemplateProcessor;
import io.kindx.backoffice.processor.template.TemplateProcessor;
import io.kindx.client.FacebookClient;
import io.kindx.constants.Defaults;
import io.kindx.constants.DriverType;
import io.kindx.factory.WebDriverFactory;
import io.kindx.lambda.EnvLambdaNameFunctionResolver;
import io.kindx.lambda.LambdaFunctions;
import io.kindx.util.EnvUtil;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.openqa.selenium.WebDriver;

import java.util.Arrays;

import static java.util.concurrent.TimeUnit.SECONDS;

public class Application extends AbstractModule {

    @Override
    protected void configure() {

        bindConstant().annotatedWith(Names.named("facebookToken"))
                .to(EnvUtil.getEnv("FACEBOOK_TOKEN"));
        bindConstant().annotatedWith(Names.named("facebookWebhookVerifyToken"))
                .to(EnvUtil.getEnv("FACEBOOK_WEBHOOK_VERIFY_TOKEN"));
        bindConstant().annotatedWith(Names.named("notificationThreshold"))
                .to(EnvUtil.getEnvFloatOrDefault("SCORE_NOTIFICATION_THRESHOLD", Defaults.SCORE_NOTIFICATION_THRESHOLD));
        bindConstant().annotatedWith(Names.named("postTimeWindowInSeconds"))
                .to(EnvUtil.getEnvLongOrDefault("POST_TIME_WINDOW_SECONDS", Defaults.POST_TIME_WINDOW_SECONDS));
        bindConstant().annotatedWith(Names.named("queryMaxGeoDistanceInKm"))
                .to(EnvUtil.getEnvLongOrDefault("MAX_GEO_DISTANCE_KM", Defaults.MAX_GEO_DISTANCE_KM));
        bindConstant().annotatedWith(Names.named("keepPageTokenAlive"))
                .to(EnvUtil.getEnvLongOrDefault("PAGE_TOKEN_TTL_SECONDS", Defaults.PAGE_TOKEN_TTL_SECONDS));
        bindConstant().annotatedWith(Names.named("reprocessRadiusInMeters"))
                .to(EnvUtil.getEnvLongOrDefault("REPROCESS_RADIUS_METERS", Defaults.REPROCESS_RADIUS_METERS));
        bindConstant().annotatedWith(Names.named("placesCacheTtlSeconds"))
                .to(EnvUtil.getEnvLongOrDefault("PLACES_CACHE_TTL_SECONDS", Defaults.PLACES_CACHE_TTL_SECONDS));
    }


    @Provides
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
    }


    @Provides
    @SneakyThrows
    public WebDriver webDriver(LambdaFunctions lambdaFunctions, AmazonS3 amazonS3) {
        String driverType = EnvUtil.getEnvOrDefault("WEB_DRIVER_TYPE", DriverType.LAMBDA.name());
        switch (DriverType.valueOf(driverType.toUpperCase())) {
            case CHROME:
                return WebDriverFactory.getRemoteChromeDriver();
            case FIREFOX:
                return WebDriverFactory.getRemoteFirefoxDriver();
            case FIREFOX_READER_MODE:
                return WebDriverFactory.getReaderModeRemoteFirefoxDriver();
            case LAMBDA:
            default:
                return WebDriverFactory.getReadabilityLambdaWebDriver(lambdaFunctions,  amazonS3);
        }
    }


    @Provides
    @SneakyThrows
    public GeoApiContext geoApiContext() {
        String apiKey = EnvUtil.getEnv("GOOGLE_PLACES_API_KEY");
        return new GeoApiContext.Builder()
                .apiKey(apiKey)
                .maxRetries(3)
                .build();
    }

    @Provides
    public  NotificationProcessor twilioNotificationProcessor() {
        String authToken = EnvUtil.getEnv("TWILIO_AUTH_TOKEN");
        String accountSid = EnvUtil.getEnv("TWILIO_ACCOUNT_SID");
        String sender = EnvUtil.getEnv("TWILIO_WHATSAPP_SENDER");
        return new TwilioNotificationProcessor(accountSid, authToken, "whatsapp:" + sender);
    }

    @Provides
    public TemplateProcessor templateProcessor() {
        return new FileTemplateProcessor();
    }

    @Provides
    public RestHighLevelClient elasticSearchClient() {
        String elasticSearchHosts = EnvUtil.getEnvOrDefault("ELASTIC_SEARCH_HOSTS", Defaults.ELASTIC_SEARCH_HOSTS);
        String elasticSearchCredentials = EnvUtil.getEnv("ELASTIC_SEARCH_CREDENTIALS");

        RestClientBuilder builder = RestClient.builder(Arrays.stream(elasticSearchHosts.split(","))
                .map(String::trim)
                .map(HttpHost::create)
                .toArray(HttpHost[]::new));

        if (StringUtils.isNotBlank(elasticSearchCredentials)) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            String[] cred = elasticSearchCredentials.split(":");
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(cred[0], cred[1]));
            builder.setHttpClientConfigCallback((asyncBuilder) -> asyncBuilder
                    .setDefaultCredentialsProvider(credentialsProvider));
        }
        return new RestHighLevelClient(builder);
    }

    @Provides
    public AmazonDynamoDB dynamoDbClient() {

        String region = System.getenv("AWS_REGION");
        String serviceEndpoint = String.format("dynamodb.%s.amazonaws.com", region);
        return AmazonDynamoDBClientBuilder
                .standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                        serviceEndpoint,
                        region))
                .build();
    }

    @Provides
    public AmazonS3 s3Client() {
        String region = System.getenv("AWS_REGION");
        String serviceEndpoint = String.format("s3.%s.amazonaws.com", region);
        return AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                        serviceEndpoint,
                        region))
                .build();
    }

    @Provides
    public AmazonKinesis kinesisClient() {

        String region = System.getenv("AWS_REGION");
        String serviceEndpoint = String.format("kinesis.%s.amazonaws.com", region);
        return AmazonKinesisClientBuilder
                .standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                        serviceEndpoint,
                        region))
                .build();
    }

    @Provides
    public static AmazonSQS sqsClient() {
        String region = System.getenv("AWS_REGION");
        String serviceEndpoint = String.format("sqs.%s.amazonaws.com", region);
        return AmazonSQSClientBuilder
                .standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                        serviceEndpoint,
                        region))
                .build();
    }

    @Provides
    public static AWSLambda awsLambda() {
        String region = System.getenv("AWS_REGION");
        String serviceEndpoint = String.format("lambda.%s.amazonaws.com", region);
        return AWSLambdaClientBuilder
                .standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(serviceEndpoint, region))
                .build();
    }

    @Provides
    public static LambdaFunctions lambdaFunctions(AWSLambda awsLambda) {
        return  LambdaInvokerFactory
                .builder()
                .lambdaFunctionNameResolver(new EnvLambdaNameFunctionResolver())
                .lambdaClient(awsLambda)
                .build(LambdaFunctions.class);
    }

    @Provides
    public FacebookClient facebookClient(ObjectMapper objectMapper) {
       return Feign.builder()
               .decoder(new JacksonDecoder(objectMapper))
               .retryer(new Retryer.Default(200, SECONDS.toMillis(1), 2))
               .target(FacebookClient.class, "https://graph.facebook.com/v4.0");
    }

}