package com.myseotoolbox.crawler;


import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;


public class CalendarService {
    public Date now() {
        return new Date();
    }

    public LocalDate today() {
        return LocalDate.now();
    }

    public static LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}


