package io.kindx.lambda;

import com.google.inject.Binder;
import io.kindx.BaseTests;
import io.kindx.backoffice.service.EventService;
import io.kindx.backoffice.service.QueueService;
import io.kindx.dto.function.ReadabilityRequest;
import io.kindx.dto.function.ReadabilityResponse;
import io.kindx.util.IDUtil;
import org.bigtesting.routd.NamedParameterElement;
import org.bigtesting.routd.Route;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class LambdaFunctionsTests extends BaseTests {
    @Before
    public void setup() {
        super.setup();
    }

    @Test
    @Ignore
    public void testReadabilityLambdaFunction() {
        ReadabilityResponse response  = injector.getInstance(LambdaFunctions.class)
                .execReadabilityProcessor(ReadabilityRequest
                        .builder()
                        .key(IDUtil.generateGenericId())
                        .sanitize(true)
                        .url("http://www.gianni.ee/restoran/")
                        .build());
        Assert.assertTrue(response.isSuccess());

    }


    @Test
    public void testRoute() {
        Route route = new Route("/kitchens/:kitchenId");
        String path = "/admin/kitchens/KN_11111";

        Map<String, String> params =  new HashMap<>();
        for (NamedParameterElement p : route.getNamedParameterElements()) {
            if (p != null) {
                params.put(p.name(), route.getNamedParameter(p.name(), path));
            }
        }
        assertEquals("KN_11111", params.get("kitchenId"));
    }

    @Override
    protected void bindMocks(Binder binder) {
        binder.bind(EventService.class).toInstance(mock(EventService.class));
        binder.bind(QueueService.class).toInstance(mock(QueueService.class));
    }
}
