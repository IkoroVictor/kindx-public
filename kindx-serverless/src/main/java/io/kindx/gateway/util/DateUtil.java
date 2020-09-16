package io.kindx.gateway.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class DateUtil {

    public static LocalDateTime zonedStartOfDay(ZoneOffset zoneOffset) {
        return LocalDate.now(Clock.system(zoneOffset)).atStartOfDay();
    }
}
