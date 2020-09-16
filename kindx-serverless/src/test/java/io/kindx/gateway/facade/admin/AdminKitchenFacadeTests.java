package io.kindx.gateway.facade.admin;

import com.google.inject.Binder;
import com.google.maps.model.PlaceDetails;
import io.kindx.BaseTests;
import io.kindx.backoffice.service.QueueService;
import io.kindx.client.FacebookClient;
import io.kindx.constants.Defaults;
import io.kindx.constants.Language;
import io.kindx.constants.LocationSource;
import io.kindx.constants.MenuConfigurationType;
import io.kindx.dao.KitchenConfigurationDao;
import io.kindx.dao.KitchenDao;
import io.kindx.dto.facebook.FacebookLocationDataDto;
import io.kindx.dto.facebook.FacebookPageDto;
import io.kindx.entity.Kitchen;
import io.kindx.entity.KitchenConfiguration;
import io.kindx.gateway.dto.KitchenCreateDto;
import io.kindx.gateway.dto.KitchenDto;
import io.kindx.gateway.dto.KitchenUpdateDto;
import io.kindx.gateway.dto.MenuConfigurationDto;
import io.kindx.gateway.exception.ConflictException;
import io.kindx.gateway.exception.InvalidRequestException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static io.kindx.util.DbTestUtil.createAllTables;
import static io.kindx.util.DbTestUtil.dropAllTables;
import static io.kindx.util.TestUtil.mockWebDriver;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class AdminKitchenFacadeTests extends BaseTests {

    private final String FACEBOOK_PAGE_USERNAME = "tallinn.kitchen.demo";
    private final String FACEBOOK_PAGE_USERNAME_2 = "Helsinki-Kitchen-Demo-115449079899568";
    private final String FACEBOOK_PAGE_ID = "11213481679800";
    private final String FACEBOOK_PAGE_ID_2 = "115449079899568";
    private final String GOOGLE_PLACES_ID = "ChIJN1t_tDeuEmsRUsoyG83frY4"; //Google HQ
    private final String GOOGLE_PLACES_ID_2 = "ChIJrTLr-GyuEmsRBfy61i59si0"; //Google HQ
    private final String WEBPAGE_CONFIG_VALUE = "http://www.gianni.ee/restoran/";

    private QueueService queueService =  mock(QueueService.class);
    private  AdminKitchenFacade kitchenFacade;

    @Before
    public void setup() {
        super.setup();
        createAllTables();
        kitchenFacade = injector.getInstance(AdminKitchenFacade.class);
    }

    @After
    public void tearDown() {
        dropAllTables();
    }

    @Test
    public void testKitchenCreateWithFacebookAndPlaces() {
        KitchenCreateDto createDto = createMockKitchenDto();
        KitchenDto dto = kitchenFacade.createKitchen(createDto, "test");

        assertEquals("test", dto.getCreatedBy());
        assertEquals(FACEBOOK_PAGE_ID, dto.getFbPageId());
        assertEquals(GOOGLE_PLACES_ID, dto.getGooglePlacesId());
        assertEquals(false, dto.getIsDisabled());
        assertEquals(Defaults.LINE_DELIMITER_REGEX, dto.getLineDelimiter());
        assertEquals(Defaults.WORD_DELIMITER_REGEX, dto.getWordDelimiter());
        assertEquals(LocationSource.FACEBOOK_PAGE, dto.getPrimaryLocationSource());
        assertEquals("head isu", dto.getMenuSignatureText());
        assertEquals(2, dto.getMenuConfigurations().size());
        Assert.assertNotNull(dto.getCreatedTimestamp());
        Assert.assertNotNull(dto.getPageUrl());
        Assert.assertNotNull(dto.getId());
        assertEquals(3, dto.getLanguages().size());
        Assert.assertTrue(dto.getLanguages().contains(Language.ENGLISH));
        Assert.assertTrue(dto.getLanguages().contains(Language.FINNISH));
        Assert.assertTrue(dto.getLanguages().contains(Language.ESTONIAN));


        Map<MenuConfigurationType, MenuConfigurationDto> map  = new HashMap<>();
        dto.getMenuConfigurations().forEach(m -> map.put(m.getType(), m));

        Assert.assertNotNull(map.get(MenuConfigurationType.FACEBOOK_PAGE));
        Assert.assertNotNull(map.get(MenuConfigurationType.PAGE));

        assertEquals(WEBPAGE_CONFIG_VALUE, map.get(MenuConfigurationType.PAGE).getValue());
        assertEquals(FACEBOOK_PAGE_ID, map.get(MenuConfigurationType.FACEBOOK_PAGE).getValue());
    }


    @Test
    public void testKitchenCreateOnlyFacebook() {
        KitchenCreateDto createDto = createMockKitchenDto();
        createDto.setPlacesId(null);
        createDto.setPrimaryLocationSource(null);

        KitchenDto dto = kitchenFacade.createKitchen(createDto, "test");

        assertEquals("test", dto.getCreatedBy());
        assertEquals(FACEBOOK_PAGE_ID, dto.getFbPageId());
        Assert.assertNull(dto.getGooglePlacesId());
        assertEquals(LocationSource.FACEBOOK_PAGE, dto.getPrimaryLocationSource());
        assertEquals("https://facebook.com/" + FACEBOOK_PAGE_ID, dto.getPageUrl());
        Assert.assertNotNull(dto.getId());
        Assert.assertNotNull(dto.getDefaultDisplayAddress());
        Assert.assertNotNull(dto.getDefaultDisplayName());


        assertEquals(2, dto.getMenuConfigurations().size());

        Map<MenuConfigurationType, MenuConfigurationDto> map  = new HashMap<>();
        dto.getMenuConfigurations().forEach(m -> map.put(m.getType(), m));

        Assert.assertNotNull(map.get(MenuConfigurationType.FACEBOOK_PAGE));
        Assert.assertNotNull(map.get(MenuConfigurationType.PAGE));

        assertEquals(WEBPAGE_CONFIG_VALUE, map.get(MenuConfigurationType.PAGE).getValue());
        assertEquals(FACEBOOK_PAGE_ID, map.get(MenuConfigurationType.FACEBOOK_PAGE).getValue());
    }


    @Test
    public void testKitchenCreateGooglePlacesOnly() {
        KitchenCreateDto createDto = createMockKitchenDto();
        createDto.setFbPageId(null);
        createDto.setPrimaryLocationSource(null);

        createDto.setMenuConfigurations(createDto.getMenuConfigurations()
                .stream()
                .filter(c -> c.getType() != MenuConfigurationType.FACEBOOK_PAGE)
                .collect(Collectors.toList()));

        KitchenDto dto = kitchenFacade.createKitchen(createDto, "test");

        Assert.assertNotNull(dto.getId());
        assertEquals("test", dto.getCreatedBy());
        assertEquals(GOOGLE_PLACES_ID, dto.getGooglePlacesId());
        assertEquals(LocationSource.GOOGLE_PLACES, dto.getPrimaryLocationSource());
        Assert.assertNull(dto.getFbPageId());
        Assert.assertNull(dto.getPageUrl());
        Assert.assertNotNull(dto.getDefaultDisplayAddress());
        Assert.assertNotNull(dto.getDefaultDisplayName());


        assertEquals(1, dto.getMenuConfigurations().size());

        Map<MenuConfigurationType, MenuConfigurationDto> map  = new HashMap<>();
        dto.getMenuConfigurations().forEach(m -> map.put(m.getType(), m));
        Assert.assertNotNull(map.get(MenuConfigurationType.PAGE));

        assertEquals(WEBPAGE_CONFIG_VALUE, map.get(MenuConfigurationType.PAGE).getValue());
    }


    @Test
    public void testKitchenCreateWithPlaceDetails() throws Exception {
        KitchenCreateDto createDto = createMockKitchenDto();
        createDto.setPlacesId(null);
        createDto.setFbPageId(null);
        createDto.setPrimaryLocationSource(null);

        String placeId = "Ch00000";
        PlaceDetails details = new PlaceDetails();
        details.name = "Test Kitchen";
        details.website = new URL("http://test.kitchen");
        details.placeId = placeId;
        details.formattedPhoneNumber = "567890";
        details.internationalPhoneNumber = "+234567890";
        details.formattedAddress = "Test Address";

        createDto.setMenuConfigurations(createDto.getMenuConfigurations()
                .stream()
                .filter(c -> c.getType() != MenuConfigurationType.FACEBOOK_PAGE)
                .collect(Collectors.toList()));

        KitchenDto dto = kitchenFacade.createKitchenFromPlaceDetails(
                createDto,
                details,
                "test");

        Assert.assertNotNull(dto.getId());
        assertEquals("test", dto.getCreatedBy());
        assertEquals(placeId,  dto.getGooglePlacesId());
        assertEquals(LocationSource.GOOGLE_PLACES, dto.getPrimaryLocationSource());
        Assert.assertNull(dto.getFbPageId());
        Assert.assertNull(dto.getPageUrl());
        assertEquals("Test Address", dto.getDefaultDisplayAddress());
        Assert.assertNotNull("Test Kitchen", dto.getDefaultDisplayName());


        assertEquals(1, dto.getMenuConfigurations().size());

        Map<MenuConfigurationType, MenuConfigurationDto> map  = new HashMap<>();
        dto.getMenuConfigurations().forEach(m -> map.put(m.getType(), m));
        Assert.assertNotNull(map.get(MenuConfigurationType.PAGE));

        assertEquals(WEBPAGE_CONFIG_VALUE, map.get(MenuConfigurationType.PAGE).getValue());
    }


    @Test
    public void testKitchenCreateNoPrimaryLocationUsePlacesAsDefault() {
        KitchenCreateDto createDto = createMockKitchenDto();
        createDto.setPrimaryLocationSource(null);

        createDto.setMenuConfigurations(createDto.getMenuConfigurations()
                .stream()
                .filter(c -> c.getType() != MenuConfigurationType.FACEBOOK_PAGE)
                .collect(Collectors.toList()));

        KitchenDto dto = kitchenFacade.createKitchen(createDto, "test");

        Assert.assertNotNull(dto.getId());
        assertEquals("test", dto.getCreatedBy());
        assertEquals(GOOGLE_PLACES_ID, dto.getGooglePlacesId());
        assertEquals(LocationSource.GOOGLE_PLACES, dto.getPrimaryLocationSource());

        assertEquals(FACEBOOK_PAGE_ID, dto.getFbPageId());
        assertEquals("https://facebook.com/" + FACEBOOK_PAGE_ID, dto.getPageUrl());
    }

    @Test
    public void testKitchenFetch() throws Exception {
        testKitchenCreateWithPlaceDetails();
        KitchenDao dao = injector.getInstance(KitchenDao.class);
        List<Kitchen> kitchens = dao.getActiveKitchens();
        assertEquals(1, kitchens.size());

        for (Kitchen kitchen : kitchens) {
            KitchenDto dto = kitchenFacade.fetchKitchen(kitchen.getKitchenId());
            assertEquals(kitchen.getKitchenId(), dto.getId());
        }
    }


    @Test
    public void testKitchenFetchByPlaceId() throws Exception {
        testKitchenCreateWithPlaceDetails();
        KitchenDao dao = injector.getInstance(KitchenDao.class);
        List<Kitchen> kitchens = dao.getActiveKitchens();
        assertEquals(1, kitchens.size());

        for (Kitchen kitchen : kitchens) {
            KitchenDto dto = kitchenFacade.fetchKitchenByPlacesId(kitchen.getGooglePlacesId());
            assertEquals(kitchen.getGooglePlacesId(), dto.getGooglePlacesId());

        }
    }

    @Test
    public void testKitchenFetchByFacebookId() throws Exception {
        testKitchenCreateOnlyFacebook();
        KitchenDao dao = injector.getInstance(KitchenDao.class);
        List<Kitchen> kitchens = dao.getActiveKitchens();
        assertEquals(1, kitchens.size());

        for (Kitchen kitchen : kitchens) {
            KitchenDto dto = kitchenFacade.fetchKitchenByFacebookPageId(kitchen.getFacebookId());
            assertEquals(kitchen.getFacebookId(), dto.getFbPageId());
        }
    }


    @Test
    public void testKitchenDelete() throws Exception {
        testKitchenCreateWithPlaceDetails();
        KitchenDao dao = injector.getInstance(KitchenDao.class);
        KitchenConfigurationDao configDao = injector.getInstance(KitchenConfigurationDao.class);

        List<Kitchen> kitchens = dao.getActiveKitchens();
        List<KitchenConfiguration> configs = configDao.getActiveKitchenConfigurations();

        assertEquals(1, kitchens.size());
        assertEquals(1, configs.size());

        Kitchen kitchen =  kitchens.get(0);
        kitchenFacade.deleteKitchen(kitchen.getKitchenId());

        assertEquals(0, dao.getActiveKitchens().size());
        assertEquals(0, configDao.getActiveKitchenConfigurations().size());

        verify(queueService, times(1)).enqueueJanitorEventMessages(any());
    }

    @Test(expected = InvalidRequestException.class)
    public void testKitchenCreateFailureNoPlacesAndFacebook() {
        KitchenCreateDto createDto = createMockKitchenDto();
        createDto.setFbPageId(null);
        createDto.setPlacesId(null);
        kitchenFacade.createKitchen(createDto, "test");
    }

    @Test(expected = InvalidRequestException.class)
    public void testKitchenCreateFailureNoPlacesAndPrimaryLocationSourceIsPlaces() {
        KitchenCreateDto createDto = createMockKitchenDto();
        createDto.setPlacesId(null);
        createDto.setPrimaryLocationSource(LocationSource.GOOGLE_PLACES);
        kitchenFacade.createKitchen(createDto, "test");
    }

    @Test(expected = InvalidRequestException.class)
    public void testKitchenCreateFailureNoFacebookAndPrimaryLocationSourceIsFacebook() {
        KitchenCreateDto createDto = createMockKitchenDto();
        createDto.setFbPageId(null);
        createDto.setPrimaryLocationSource(LocationSource.FACEBOOK_PAGE);
        kitchenFacade.createKitchen(createDto, "test");
    }

    @Test(expected = ConflictException.class)
    public void testKitchenCreateFailureExistingFacebook() {

        KitchenCreateDto createDto = createMockKitchenDto();
        kitchenFacade.createKitchen(createDto, "test");

        createDto.setPlacesId(null);
        createDto.setPrimaryLocationSource(LocationSource.FACEBOOK_PAGE);
        kitchenFacade.createKitchen(createDto, "test");
    }

    @Test(expected = ConflictException.class)
    public void testKitchenCreateFailureExistingPlaces() {

        KitchenCreateDto createDto = createMockKitchenDto();
        kitchenFacade.createKitchen(createDto, "test");

        createDto.setFbPageId(null);
        createDto.setPrimaryLocationSource(LocationSource.GOOGLE_PLACES);
        kitchenFacade.createKitchen(createDto, "test");
    }


    @Test
    public void testKitchenUpdate() {

        KitchenCreateDto createDto = createMockKitchenDto();
        KitchenDto newKitchenDto = kitchenFacade.createKitchen(createDto, "test");

        KitchenUpdateDto updateDto = updateMockKitchenDto();
        KitchenDto dto = kitchenFacade.updateKitchen(updateDto, newKitchenDto.getId(), "test_updater");

        Assert.assertNull(dto.getFbPageId());
        assertEquals("test", dto.getCreatedBy());
        assertEquals("test_updater", dto.getLastUpdatedBy());
        assertEquals(GOOGLE_PLACES_ID_2, dto.getGooglePlacesId());
        assertEquals(false, dto.getIsDisabled());
        assertEquals("\n\n\n", dto.getLineDelimiter());
        assertEquals(";;;", dto.getWordDelimiter());
        assertEquals(LocationSource.GOOGLE_PLACES, dto.getPrimaryLocationSource());
        assertEquals("tail isu", dto.getMenuSignatureText());
        assertEquals(1 , dto.getMenuConfigurations().size());
        Assert.assertNotNull(dto.getCreatedTimestamp());
        Assert.assertNotNull(dto.getDefaultDisplayAddress());
        Assert.assertNotNull(dto.getDefaultDisplayName());
        Assert.assertNotNull(dto.getUpdatedTimestamp());
        Assert.assertNotNull(dto.getId());
        Assert.assertNull(dto.getPageUrl());

        assertEquals(1, dto.getLanguages().size());
        Assert.assertTrue(dto.getLanguages().contains(Language.ENGLISH));

        Map<MenuConfigurationType, MenuConfigurationDto> map  = new HashMap<>();
        dto.getMenuConfigurations().forEach(m -> map.put(m.getType(), m));

        Assert.assertNotNull(map.get(MenuConfigurationType.PAGE));

        //Janitor invocation
        verify(queueService, times(1)).enqueueJanitorEventMessages(any());
    }

    @Test
    public void testKitchenUpdateWithOnlyFacebook() {
        KitchenCreateDto createDto = createMockKitchenDto();
        KitchenDto newKitchenDto = kitchenFacade.createKitchen(createDto, "test");

        KitchenUpdateDto updateDto = updateMockKitchenDto();
        updateDto.setPlacesId(null);
        updateDto.setPrimaryLocationSource(null);

        updateDto.setFbPageId(FACEBOOK_PAGE_USERNAME);
        List<MenuConfigurationDto> configs = Arrays.asList(
                updateDto.getMenuConfigurations().get(0),
                MenuConfigurationDto
                        .builder()
                        .type(MenuConfigurationType.FACEBOOK_PAGE)
                        .value(FACEBOOK_PAGE_USERNAME)
                        .build()
        );
        updateDto.setMenuConfigurations(configs);

        KitchenDto dto = kitchenFacade.updateKitchen(updateDto, newKitchenDto.getId(), "test_updater");

        Assert.assertNull(dto.getGooglePlacesId());
        assertEquals(FACEBOOK_PAGE_ID, dto.getFbPageId());
        assertEquals(LocationSource.FACEBOOK_PAGE, dto.getPrimaryLocationSource());
        assertEquals(2 , dto.getMenuConfigurations().size());

        Map<MenuConfigurationType, MenuConfigurationDto> map  = new HashMap<>();
        dto.getMenuConfigurations().forEach(m -> map.put(m.getType(), m));

        Assert.assertNotNull(map.get(MenuConfigurationType.FACEBOOK_PAGE));
        Assert.assertNotNull(map.get(MenuConfigurationType.PAGE));
        assertEquals("Test Address", dto.getDefaultDisplayAddress());
        assertEquals("Test Kitchen", dto.getDefaultDisplayName());

        assertEquals(WEBPAGE_CONFIG_VALUE, map.get(MenuConfigurationType.PAGE).getValue());
        assertEquals(FACEBOOK_PAGE_ID, map.get(MenuConfigurationType.FACEBOOK_PAGE).getValue());


        //No Janitor invocation, menu config was only added
        verify(queueService, times(0)).enqueueJanitorEventMessages(any());
    }

    @Test(expected = InvalidRequestException.class)
    public void testKitchenUpdateFailureNoPlacesAndFacebook() {
        KitchenCreateDto createDto = createMockKitchenDto();
        KitchenDto newKitchenDto = kitchenFacade.createKitchen(createDto, "test");

        KitchenUpdateDto updateDto = updateMockKitchenDto();
        updateDto.setPlacesId(null);
        updateDto.setFbPageId(null);

        kitchenFacade.updateKitchen(updateDto, newKitchenDto.getId(), "test_updater");
    }

    @Test(expected = InvalidRequestException.class)
    public void testKitchenUpdateFailureNoPlacesAndPrimaryLocationSourceIsPlaces() {
        KitchenCreateDto createDto = createMockKitchenDto();
        KitchenDto newKitchenDto = kitchenFacade.createKitchen(createDto, "test");

        KitchenUpdateDto updateDto = updateMockKitchenDto();
        updateDto.setFbPageId(FACEBOOK_PAGE_USERNAME);
        updateDto.setPlacesId(null);
        updateDto.setPrimaryLocationSource(LocationSource.GOOGLE_PLACES);

        kitchenFacade.updateKitchen(updateDto, newKitchenDto.getId(), "test_updater");
    }

    @Test(expected = InvalidRequestException.class)
    public void testKitchenUpdateFailureNoFacebookAndPrimaryLocationSourceIsFacebook() {
        KitchenCreateDto createDto = createMockKitchenDto();
        KitchenDto newKitchenDto = kitchenFacade.createKitchen(createDto, "test");

        KitchenUpdateDto updateDto = updateMockKitchenDto();
        updateDto.setFbPageId(null);
        updateDto.setPlacesId(null);
        updateDto.setPrimaryLocationSource(LocationSource.FACEBOOK_PAGE);

        kitchenFacade.updateKitchen(updateDto, newKitchenDto.getId(), "test_updater");
    }

    @Test(expected = ConflictException.class)
    public void testKitchenUpdateFailureExistingFacebook() {

        KitchenCreateDto createDto = createMockKitchenDto();
        KitchenDto newKitchenDto = kitchenFacade.createKitchen(createDto, "test");

        //Second kitchen with different facebook id
        createDto.setFbPageId(FACEBOOK_PAGE_USERNAME_2);
        createDto.setPlacesId(null);
        createDto.setPrimaryLocationSource(null);
        createDto.setMenuConfigurations(createDto.getMenuConfigurations()
                .stream()
                .filter(c -> c.getType() != MenuConfigurationType.FACEBOOK_PAGE)
                .collect(Collectors.toList()));
        kitchenFacade.createKitchen(createDto, "test");

        KitchenUpdateDto updateDto = updateMockKitchenDto();
        updateDto.setFbPageId(FACEBOOK_PAGE_USERNAME_2);
        kitchenFacade.updateKitchen(updateDto, newKitchenDto.getId(), "test_updater");
    }

    @Test(expected = ConflictException.class)
    public void testKitchenUpdateFailureExistingPlaces() {
        KitchenCreateDto createDto = createMockKitchenDto();
        KitchenDto newKitchenDto =  kitchenFacade.createKitchen(createDto, "test");

        //Second kitchen with different places id
        createDto.setPlacesId(GOOGLE_PLACES_ID_2);
        createDto.setFbPageId(null);
        createDto.setPrimaryLocationSource(null);
        createDto.setMenuConfigurations(createDto.getMenuConfigurations()
                .stream()
                .filter(c -> c.getType() != MenuConfigurationType.FACEBOOK_PAGE)
                .collect(Collectors.toList()));

        kitchenFacade.createKitchen(createDto, "test");

        KitchenUpdateDto updateDto = updateMockKitchenDto();
        updateDto.setPlacesId(GOOGLE_PLACES_ID_2);
        updateDto.setPrimaryLocationSource(LocationSource.GOOGLE_PLACES);
        kitchenFacade.updateKitchen(updateDto, newKitchenDto.getId(), "test_updater");
    }


    @Override
    protected void bindMocks(Binder binder) {
        binder.bind(FacebookClient.class).toInstance(mockFbClient());
        binder.bind(WebDriver.class).toInstance(mockWebDriver());
        binder.bind(QueueService.class).toInstance(queueService);
    }

    private FacebookClient mockFbClient() {
        FacebookPageDto pageDto = FacebookPageDto.builder()
                .id(FACEBOOK_PAGE_ID)
                .username(FACEBOOK_PAGE_USERNAME)
                .name("Test Kitchen")
                .singleLineAddress("Test Address")
                .location(FacebookLocationDataDto.builder()
                        .latitude(9.0)
                        .longitude(6.0)
                        .zip("10101")
                        .street("Gonsiori 13")
                        .city("Tallinn").country("Nigeria")
                        .build())
                .build();

        FacebookPageDto pageDto2 = FacebookPageDto.builder()
                .id(FACEBOOK_PAGE_ID_2)
                .username(FACEBOOK_PAGE_USERNAME_2)
                .name("Test Kitchen 2")
                .location(FacebookLocationDataDto.builder()
                        .latitude(9.0)
                        .longitude(6.0)
                        .zip("10102")
                        .street("Gonsiori 14")
                        .city("Riga").country("Congi")
                        .build())
                .build();
        FacebookClient client = mock(FacebookClient.class);
        when(client.getFacebookPageWithPosts(anyString(), eq(FACEBOOK_PAGE_USERNAME), anyString()))
                .thenReturn(pageDto);
        when(client.getFacebookPageWithPosts(anyString(), eq(FACEBOOK_PAGE_ID), anyString()))
                .thenReturn(pageDto);
        when(client.getFacebookPageWithPosts(anyString(), eq(FACEBOOK_PAGE_USERNAME_2), anyString()))
                .thenReturn(pageDto2);
        when(client.getFacebookPageWithPosts(anyString(), eq(FACEBOOK_PAGE_ID_2), anyString()))
                .thenReturn(pageDto2);
        return client;
    }


    private KitchenCreateDto createMockKitchenDto() {
        return KitchenCreateDto.builder()
                .fbPageId(FACEBOOK_PAGE_USERNAME)
                .placesId(GOOGLE_PLACES_ID)
                .languages(new HashSet<>(Arrays.asList(Language.ESTONIAN, Language.ENGLISH, Language.FINNISH)))
                .primaryLocationSource(LocationSource.FACEBOOK_PAGE)
                .menuSignatureText("Head Isu")
                .menuConfigurations(
                        Arrays.asList(
                                MenuConfigurationDto
                                        .builder()
                                        .type(MenuConfigurationType.PAGE)
                                        .value(WEBPAGE_CONFIG_VALUE)
                                        .build(),
                                MenuConfigurationDto
                                        .builder()
                                        .type(MenuConfigurationType.FACEBOOK_PAGE)
                                        .value(FACEBOOK_PAGE_USERNAME)
                                        .build()
                        )
                )
                .build();
    }

    private KitchenUpdateDto updateMockKitchenDto() {
        return KitchenUpdateDto.builder()
                .fbPageId(null)
                .placesId(GOOGLE_PLACES_ID_2)
                .languages(new HashSet<>(Collections.singletonList(Language.ENGLISH)))
                .primaryLocationSource(LocationSource.GOOGLE_PLACES)
                .lineDelimiter("\n\n\n")
                .wordDelimiter(";;;")
                .menuSignatureText("Tail Isu")
                .menuConfigurations(
                        Collections.singletonList(
                                MenuConfigurationDto
                                        .builder()
                                        .type(MenuConfigurationType.PAGE)
                                        .value(WEBPAGE_CONFIG_VALUE)
                                        .build()
                        )
                )
                .build();
    }
}
