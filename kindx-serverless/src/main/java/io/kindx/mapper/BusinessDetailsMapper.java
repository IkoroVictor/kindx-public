package io.kindx.mapper;

import com.google.inject.Inject;
import com.google.maps.model.AddressComponent;
import com.google.maps.model.AddressComponentType;
import com.google.maps.model.OpeningHours;
import com.google.maps.model.PlaceDetails;
import io.kindx.client.PlacesApiClient;
import io.kindx.dto.facebook.FacebookLocationDataDto;
import io.kindx.dto.facebook.FacebookPageDto;
import io.kindx.entity.Menu;
import lombok.SneakyThrows;

import java.time.format.DateTimeFormatter;
import java.util.*;

public class BusinessDetailsMapper {

    private PlacesApiClient placesApiClient;

    @Inject
    public BusinessDetailsMapper(PlacesApiClient placesApiClient) {
        this.placesApiClient = placesApiClient;
    }

    @SneakyThrows
    public Menu.BusinessProfile mapBusinessLocationFromPlacesApi(String placeId) {
        PlaceDetails details = placesApiClient.getPlaceDetails(placeId);
        return mapPlaceDetailsToBusinessProfile(details);
    }

    public Menu.BusinessProfile mapFBPageToBusinessProfile(FacebookPageDto pageDto) {

        Menu.BusinessProfile businessProfile = new Menu.BusinessProfile();
        Menu.Location businessLocation = mapFBLocationToMenuLocation(pageDto.getLocation());
        businessLocation.setAddress(pageDto.getSingleLineAddress());


        businessProfile.setLocation(businessLocation);
        businessProfile.setBusinessName(pageDto.getName());
        businessProfile.setEmails(pageDto.getEmails());
        businessProfile.setPhoneNumbers(Collections.singleton(pageDto.getPhone()));
        businessProfile.setWebsite(pageDto.getWebsite());
        if (pageDto.getHours() != null) {
            businessProfile.setOpeningHours(mapFbOpenHours(pageDto.getHours()));
        }
        businessProfile.setFacebookPageUrl("http://facebook.com/" + pageDto.getId());
        return businessProfile;
    }

    public Menu.Location mapFBLocationToMenuLocation(FacebookLocationDataDto locationDataDto) {
        Menu.Location location = new Menu.Location();
        location.setStreet(locationDataDto.getStreet());
        location.setCity(locationDataDto.getCity());
        location.setCountry(locationDataDto.getCountry());

        Menu.GeoPoint geoPoint = new Menu.GeoPoint();
        geoPoint.setLat(locationDataDto.getLatitude());
        geoPoint.setLon(locationDataDto.getLongitude());
        location.setGeoPoint(geoPoint);
        location.setZip(locationDataDto.getZip());
        return location;
    }

    public Menu.BusinessProfile mapPlaceDetailsToBusinessProfile(PlaceDetails placeDetails) {
        Menu.BusinessProfile profile = new Menu.BusinessProfile();
        profile.setBusinessName(placeDetails.name);

        if (placeDetails.website != null) {
            profile.setWebsite(placeDetails.website.toString());
        }
        if (placeDetails.internationalPhoneNumber != null) {
            profile.setPhoneNumbers(Collections.singleton(placeDetails.internationalPhoneNumber));
        }

        if (placeDetails.openingHours != null) {
            profile.setOpeningHours(mapOpenHours(placeDetails.openingHours));
        }

        Menu.Location location = new Menu.Location();
        location.setName(placeDetails.name);
        location.setAddress(placeDetails.formattedAddress);

        Menu.GeoPoint geoPoint = new Menu.GeoPoint();
        geoPoint.setLat(placeDetails.geometry.location.lat);
        geoPoint.setLon(placeDetails.geometry.location.lng);
        location.setGeoPoint(geoPoint);

        Map<AddressComponentType, List<AddressComponent>> map = buildAddressComponentMap(placeDetails.addressComponents);
        if (map.containsKey(AddressComponentType.COUNTRY))
            location.setCountry(map.get(AddressComponentType.COUNTRY).get(0).longName);
        if (map.containsKey(AddressComponentType.LOCALITY))
            location.setCity(map.get(AddressComponentType.LOCALITY).get(0).longName);
        if (map.containsKey(AddressComponentType.STREET_ADDRESS))
            location.setStreet(map.get(AddressComponentType.STREET_ADDRESS).get(0).longName);
        if (map.containsKey(AddressComponentType.POSTAL_CODE))
            location.setZip(map.get(AddressComponentType.POSTAL_CODE).get(0).longName);

        profile.setLocation(location);
        return profile;
    }

    private Map<AddressComponentType, List<AddressComponent>> buildAddressComponentMap(AddressComponent[] components) {
        Map<AddressComponentType, List<AddressComponent>> map = new HashMap<>();
        for (AddressComponent c : components) {
            for (AddressComponentType t: c.types) {
                map.putIfAbsent(t, new ArrayList<>());
                map.get(t).add(c);
            }
        }
        return map;
    }

    private List<Menu.BusinessProfile.OpeningHour> mapOpenHours(OpeningHours hours) {
        List<Menu.BusinessProfile.OpeningHour> openingHours = new ArrayList<>();
        for (OpeningHours.Period period : hours.periods) {
            Menu.BusinessProfile.OpeningHour hour =  new Menu.BusinessProfile.OpeningHour();
            hour.setOpenDayOfWeek(getDayOfWeek(period.open.day));
            hour.setOpenTime(period.open.time.format(DateTimeFormatter.ISO_LOCAL_TIME));
            hour.setCloseDayOfWeek(getDayOfWeek(period.close.day));
            hour.setCloseTime(period.close.time.format(DateTimeFormatter.ISO_LOCAL_TIME));
            openingHours.add(hour);
        }
        return openingHours;
    }

    private  List<Menu.BusinessProfile.OpeningHour> mapFbOpenHours(Map<String, String> openHours) {
        Map<String, Menu.BusinessProfile.OpeningHour> openingHoursMap = new HashMap<>();
        for (Map.Entry<String, String> entry : openHours.entrySet()) {
            String[] day = entry.getKey().split("_");
            Menu.BusinessProfile.OpeningHour openingHour = new Menu.BusinessProfile.OpeningHour();
            openingHoursMap.put(day[0] + day[1], openingHour);
            switch (day[0].toLowerCase()) {
                case "sun" : setFBOpenHours(openingHour, 0, day, entry.getValue()); break;
                case "mon" : setFBOpenHours(openingHour, 1, day, entry.getValue()); break;
                case "tue" : setFBOpenHours(openingHour, 2, day, entry.getValue()); break;
                case "wed" : setFBOpenHours(openingHour, 3, day, entry.getValue()); break;
                case "thu" : setFBOpenHours(openingHour, 4, day, entry.getValue()); break;
                case "fri" : setFBOpenHours(openingHour, 5, day, entry.getValue()); break;
                case "sat" : setFBOpenHours(openingHour, 6, day, entry.getValue()); break;
            }
        }
        return new ArrayList<>(openingHoursMap.values());
    }

    private void setFBOpenHours(Menu.BusinessProfile.OpeningHour hour, int ordinal, String[] day, String time) {
        time  = time + ":00"; //ISO_LOCAL_TIME compatibility
        if ("open".equals(day[day.length - 1])) {
            hour.setOpenDayOfWeek(ordinal) ;
            hour.setOpenTime(time);
        } else  {
            hour.setCloseDayOfWeek(ordinal);
            hour.setCloseTime(time);
        }
    }

    private int getDayOfWeek(OpeningHours.Period.OpenClose.DayOfWeek day) {
        int value = -1;
        switch (day) {
            case SUNDAY: value = 0;break;
            case MONDAY: value = 1;break;
            case TUESDAY: value = 2;break;
            case WEDNESDAY: value = 3;break;
            case THURSDAY: value = 4;break;
            case FRIDAY: value = 5;break;
            case SATURDAY: value = 6;break;
        }
        return value;
    }
}
