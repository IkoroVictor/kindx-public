package io.kindx.mapper;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.kindx.client.FacebookClient;
import io.kindx.constants.LocationSource;
import io.kindx.constants.MenuSource;
import io.kindx.dao.FoodItemAggregationDao;
import io.kindx.dao.MenuFoodItemDao;
import io.kindx.dto.GeoPointDto;
import io.kindx.dto.facebook.FacebookPageDto;
import io.kindx.dto.facebook.FacebookPostDto;
import io.kindx.entity.Kitchen;
import io.kindx.entity.KitchenConfiguration;
import io.kindx.entity.Menu;
import io.kindx.entity.MenuFoodItem;
import io.kindx.gateway.dto.MenuDto;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.geo.GeoUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MenuMapper {

    private final String facebookAccessToken;
    private FoodItemAggregationDao foodItemAggregationDao;
    private MenuFoodItemDao menuFoodItemDao;
    private BusinessDetailsMapper businessDetailsMapper;
    private FacebookClient facebookClient;

    private static final DateTimeFormatter FACEBOOK_DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final String FACEBOOK_POST_URL_TEMPLATE = "https://facebook.com/%s";



    @Inject
    public MenuMapper(@Named("facebookToken") String facebookAccessToken,
                      FoodItemAggregationDao foodItemAggregationDao,
                      MenuFoodItemDao menuFoodItemDao,
                      BusinessDetailsMapper businessDetailsMapper,
                      FacebookClient facebookClient) {
        this.foodItemAggregationDao = foodItemAggregationDao;
        this.menuFoodItemDao = menuFoodItemDao;
        this.businessDetailsMapper = businessDetailsMapper;
        this.facebookClient = facebookClient;
        this.facebookAccessToken  = facebookAccessToken;
    }
    public MenuDto toMenuDto(Menu menu, List<String> highlights, GeoPointDto pointOfInterest, String userId) {
        MenuDto.MenuDtoBuilder builder = MenuDto.builder()
                .kitchenName(menu.getBusinessProfile().getBusinessName())
                .kitchenId(menu.getKitchenId())
                .menuId(menu.getMenuId())
                .menu(menu.getMenuText())
                .highlights(highlights)
                .languages(menu.getLanguages())
                .address(menu.getLocation().getAddress())
                .fullAddress(buildFullAddress(menu.getLocation()))
                .lat(menu.getLocation().getGeoPoint().getLat())
                .lon(menu.getLocation().getGeoPoint().getLon())
                .emails(menu.getBusinessProfile().getEmails())
                .phones(menu.getBusinessProfile().getPhoneNumbers())
                .headerUrl(menu.getHeaderImageUrl())
                .thumbnailUrl(menu.getThumbnailImageUrl())
                .source(menu.getSource())
                .sourceUrl(menu.getSourceUrl())
                .pageUrl(menu.getBusinessProfile().getFacebookPageUrl())
                .website(menu.getBusinessProfile().getWebsite())
                .postedTimestamp(menu.getMenuDate().getTime());
        if (pointOfInterest != null) {
            double distanceInMeters = GeoUtils.arcDistance(pointOfInterest.getLat(),
                    pointOfInterest.getLon(),
                    menu.getLocation().getGeoPoint().getLat(),
                    menu.getLocation().getGeoPoint().getLon());
            builder.distanceInMeters(Math.round(distanceInMeters));
        }
        if (menu.getSource() == MenuSource.PDF
                || menu.getSource() == MenuSource.WEBPAGE) {
            builder.menu(menu.getSourceValue()); //Distilled HTML
        }

        Set<String> preferences = new HashSet<>();

        if (StringUtils.isNotBlank(userId)) {
           List<String> preferenceList = menuFoodItemDao.getUserFoodItems(userId, menu.getMenuId())
                    .stream()
                    .map(MenuFoodItem::getSystemName)
                    .collect(Collectors.toList());
           preferences.addAll(preferenceList);
        }

        Set<MenuDto.FoodItem> items = foodItemAggregationDao.findAggregationsForMenu(menu.getMenuId())
                .stream()
                .map(aggregation -> MenuDto.FoodItem.builder()
                        .count(aggregation.getCount())
                        .name(aggregation.getName())
                        .preference(preferences.contains(aggregation.getSystemName()))
                        .build())
                .collect(Collectors.toSet());
        builder.items(items);
        return builder.build();

    }

    public Menu menuFromFacebookPost(String menuId, Kitchen kitchen,
                                      KitchenConfiguration kitchenConfiguration,
                                      FacebookPostDto postDto, FacebookPageDto pageDto) {
        Menu.MenuBuilder menuBuilder = Menu.builder();
        menuBuilder.menuId(menuId);
        menuBuilder.createdTimestamp(new Date().getTime());
        menuBuilder.kitchenId(kitchen.getKitchenId());
        menuBuilder.menuText(postDto.getMessage());
        menuBuilder.languages(kitchenConfiguration.getLanguages());
        menuBuilder.source(MenuSource.FACEBOOK);

        String postUrl = String.format(FACEBOOK_POST_URL_TEMPLATE, postDto.getId());
        menuBuilder.sourceUrl(postUrl);
        menuBuilder.sourceValue(postUrl);

        Instant instant = Instant.from(FACEBOOK_DATE_TIME_PATTERN.parse(postDto.getCreatedTime()));
        Date menuDate = Date.from(instant);
        menuBuilder.menuDate(menuDate);

        if (pageDto.getCover() != null) {
            menuBuilder.headerImageUrl(pageDto.getCover().getSourceUrl());
        }

        if (pageDto.getPicture() != null) {
            menuBuilder.thumbnailImageUrl(pageDto.getPicture().getData().getUrl());
        }
        setFBDefaultBusinessDetails(menuBuilder, pageDto, postDto, kitchen, kitchenConfiguration);

        return menuBuilder.build();
    }

    public Menu menuFromHtmlText(String menuId, Kitchen kitchen,
                                 KitchenConfiguration kitchenConfiguration,
                                 String strippedHtmlText, String sourceHtmlText,
                                 String pageTextUrl, Date menuDate, boolean isPdfSource) {
        Menu.MenuBuilder menuBuilder = Menu.builder();
        menuBuilder.menuId(menuId);
        menuBuilder.createdTimestamp(new Date().getTime());
        menuBuilder.kitchenId(kitchen.getKitchenId());
        menuBuilder.menuText(strippedHtmlText);
        menuBuilder.languages(kitchenConfiguration.getLanguages());
        menuBuilder.sourceValue(sourceHtmlText);
        menuBuilder.sourceUrl(pageTextUrl);
        menuBuilder.menuDate(menuDate);
        setPlacesDefaultBusinessDetails(menuBuilder, kitchen, kitchenConfiguration);
        if (isPdfSource) {
            menuBuilder.source(MenuSource.PDF);
        } else {
            menuBuilder.source(MenuSource.WEBPAGE);
        }
        return menuBuilder.build();

    }

    public Menu menuFromPlainText(String menuId, Kitchen kitchen,
                                 KitchenConfiguration kitchenConfiguration,
                                 String text, Date menuDate) {
        Menu.MenuBuilder menuBuilder = Menu.builder();
        menuBuilder.menuId(menuId);
        menuBuilder.createdTimestamp(new Date().getTime());
        menuBuilder.kitchenId(kitchen.getKitchenId());
        menuBuilder.menuText(text);
        menuBuilder.languages(kitchenConfiguration.getLanguages());
        menuBuilder.source(MenuSource.PLAINTEXT);
        menuBuilder.sourceValue(text);
        menuBuilder.sourceUrl(null);
        menuBuilder.menuDate(menuDate);
        setPlacesDefaultBusinessDetails(menuBuilder, kitchen, kitchenConfiguration);
        return menuBuilder.build();

    }

    private void setPlacesDefaultBusinessDetails(Menu.MenuBuilder menuBuilder, Kitchen kitchen,
                                    KitchenConfiguration kitchenConfiguration) {
        Menu.BusinessProfile businessProfile;
        if (LocationSource.FACEBOOK_PAGE.equals(kitchenConfiguration.getPrimaryLocationSource())) {
            FacebookPageDto page = facebookClient.getFacebookPageWithPosts(facebookAccessToken,
                    kitchen.getFacebookId(), LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
            businessProfile = businessDetailsMapper.mapFBPageToBusinessProfile(page);
            if (page.getCover() != null) {
                menuBuilder.headerImageUrl(page.getCover().getSourceUrl());
            }
            if (page.getPicture() != null) {
                menuBuilder.thumbnailImageUrl(page.getPicture().getData().getUrl());
            }
        } else {
            businessProfile = businessDetailsMapper.mapBusinessLocationFromPlacesApi(kitchen.getGooglePlacesId());
            menuBuilder.thumbnailImageUrl(kitchen.getFallbackThumbnailUrl());
        }
        menuBuilder.businessProfile(businessProfile);
        menuBuilder.location(businessProfile.getLocation());
    }

    private void setFBDefaultBusinessDetails(Menu.MenuBuilder menuBuilder,
                                             FacebookPageDto pageDto,
                                             FacebookPostDto postDto,
                                             Kitchen kitchen,
                                             KitchenConfiguration kitchenConfiguration) {
        Menu.BusinessProfile businessProfile;
        if (LocationSource.GOOGLE_PLACES.equals(kitchenConfiguration.getPrimaryLocationSource())) {
            businessProfile = businessDetailsMapper.mapBusinessLocationFromPlacesApi(kitchen.getGooglePlacesId());
            menuBuilder.location(businessProfile.getLocation());
            menuBuilder.thumbnailImageUrl(kitchen.getFallbackThumbnailUrl());
        } else {
            businessProfile = businessDetailsMapper.mapFBPageToBusinessProfile(pageDto);
            menuBuilder.location(businessProfile.getLocation());
            //Set fb post location if available
            if ((postDto.getPlace() != null) && (postDto.getPlace().getLocation() != null)) {
                Menu.Location location = businessDetailsMapper.mapFBLocationToMenuLocation(postDto.getPlace().getLocation());
                location.setName(postDto.getPlace().getName());
                menuBuilder.location(location);
            }
        }
        menuBuilder.businessProfile(businessProfile);
    }


    private String buildFullAddress(Menu.Location location) {
        return Stream.of(location.getName(),
                location.getStreet(),
                location.getCity(),
                location.getCountry(),
                location.getZip())
                .filter(StringUtils::isNotBlank)
                .reduce((u, v) -> String.join(", ", u, v))
                .orElse("");
    }


}
