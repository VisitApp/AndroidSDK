package com.getvisitapp.google_fit.util;


import androidx.annotation.Keep;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Keep
public class DateHelper {


    public static List<Long> convertSessioninDays(long sessionStart, long sessionEnd) {

        List<Long> time = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(sessionStart);


        while (compareTime(cal.getTimeInMillis(), sessionEnd)) {
            time.add(cal.getTimeInMillis());
            cal.add(Calendar.DATE, 1);
        }

        return time;
    }


    public static boolean compareTime(long sessionStart, long sessionEnd) {

        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();

        cal1.setTimeInMillis(sessionStart);
        cal2.setTimeInMillis(sessionEnd);
        int i = cal2.compareTo(cal1);
        if (i > 0) return true;
        else if (i < 0) return false;
        else return false;

    }


    public static int getDifferenceBetweenTwoDays(long sessionStart, long sessionEnd) {


        long difference = sessionEnd - sessionStart;
        int days = (int) (difference / (1000 * 60 * 60 * 24));
        return days;
    }


}
