package io.kindx.util;

import io.kindx.constants.MenuConfigurationType;

import java.util.UUID;

import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;

public class IDUtil {

    private static final String FACEBOOK_ID_PREFIX =  "FB";
    private static final String WEBPAGE_ID_PREFIX =  "WP";
    private static final String PLAINTEXT_ID_PREFIX =  "PT";
    private static final String PDF_ID_PREFIX =  "PD";
    private static final String MENU_ID_PREFIX =  "PD";
    private static final String KITCHEN_ID_PREFIX =  "KN";
    private static final String USAGE_ID_PREFIX =  "UG";
    private static final String PREF_ID_PREFIX =  "PF";
    private static final String REPROCESS_ID_PREFIX =  "PR";
    private static final String MESSAGE_PREFIX =  "MG";
    private static final String LOG_ID_PREFIX =  "LG";
    private static final String COR_ID_PREFIX =  "RQ";
    private static final String LOCATION_ID_PREFIX =  "LN";
    private static final String GENERIC_ID_PREFIX =  "00";

    public static boolean isFacebookId(String id) {
        return id != null && id.startsWith(FACEBOOK_ID_PREFIX);
    }

    public static String getExternalIdFromFacebookId(String id) {
        if (!isFacebookId(id)) {
            throw new IllegalArgumentException(String.format("Invalid Facebook ID: '%s'", id));
        }
        return id.substring(3);
    }

    public static String generateFacebookMenuId(String kitchenId, String facebookExternalPostId) {
        return String.format("%s_%s_%s", MENU_ID_PREFIX, FACEBOOK_ID_PREFIX,
                hash(kitchenId, facebookExternalPostId).substring(0, 32));
    }

    public static String generateWebPageMenuId(String kitchenId, String menuConfigId) {
        return String.format("%s_%s_%s", MENU_ID_PREFIX, WEBPAGE_ID_PREFIX,
                hash(kitchenId, menuConfigId).substring(0, 32));
    }

    public static String generatePdfMenuId(String kitchenId, String menuConfigId) {
        return String.format("%s_%s_%s", MENU_ID_PREFIX, PDF_ID_PREFIX,
                hash(kitchenId,  menuConfigId).substring(0, 32));
    }

    public static String generatePlainTextMenuId(String kitchenId, String menuConfigId) {
        return String.format("%s_%s_%s", MENU_ID_PREFIX, PLAINTEXT_ID_PREFIX,
                hash(kitchenId,  menuConfigId).substring(0, 32));
    }



    public static String generateMenuConfigId(MenuConfigurationType type, String configValue) {
        return String.format("%s_%s",type.name(), sha256Hex(configValue).substring(0, 8));
    }

    public static String generateKitchenId() {
        return generateId(KITCHEN_ID_PREFIX);
    }

    public static String generateGenericId() {
        return generateId(GENERIC_ID_PREFIX);
    }

    public static String generateUsageId() {
        return generateId(USAGE_ID_PREFIX);
    }

    public static String generateLogId() {
        return generateId(LOG_ID_PREFIX).substring(0, 9);
    }

    public static String generatePreferencesId() {
        return generateId(PREF_ID_PREFIX);
    }

    public static String generateReprocessId() {
        return generateId(REPROCESS_ID_PREFIX);
    }

    public static String generateMessageId() {
        return generateId(MESSAGE_PREFIX);
    }

    public static String generateCorrelationId() {
        return generateId(COR_ID_PREFIX);
    }

    public static String generateLocationId() {
        return generateId(LOCATION_ID_PREFIX);
    }



    private static String generateId(String prefix) {
        return String.format("%s_%s",prefix,
                UUID.randomUUID().toString().replaceAll("-", ""));
    }


    private static String hash(String... values) {
        return sha256Hex(String.join(",", values));
    }
}
