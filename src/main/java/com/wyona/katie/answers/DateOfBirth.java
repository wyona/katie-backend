package com.wyona.katie.answers;

import lombok.extern.slf4j.Slf4j;

import com.wyona.katie.models.Entity;

/**
 *
 */
@Slf4j
public class DateOfBirth {

    /**
     *
     */
    public DateOfBirth() {
    }

    /**
     * @param name Name of person, e.g. "michael wechner"
     * @return age, e.g. '54'
     */
    public String getAgeByPersonName(String name) throws Exception {
        log.info("Name of person: " + name);
        String nameLC = name.toLowerCase();
        if ((nameLC.indexOf("michael") >= 0 && nameLC.indexOf("wechner") >= 0) || nameLC.equals("wechner")) {
            return getAgeByDateOfBirth("1969.02.16").toString();
        } else if ((nameLC.indexOf("michael") >= 0 && nameLC.indexOf("jackson") >= 0) || nameLC.equals("jackson")) {
            //return getAgeByDateOfBirth("1958.08.29");
            String msg = "Michael Jackson was 50 years old when he died 2009.";
            log.warn(msg);
            throw new Exception(msg);
        } else if (nameLC.equals("michael")) {
            String msg = "There are more than one person with the name '" + name + "'!";
            log.warn(msg);
            throw new Exception(msg);
        } else if (name.equals(Entity.AK_PERSON)) {
            String msg = "No person name recognized, therefore no age available!";
            log.warn(msg);
            throw new Exception(msg);
        } else {
            String msg = "No age available for person '" + name + "'!";
            log.warn(msg);
            throw new Exception(msg);
        }
    }

    /**
     * @param dateOfBirth Date of birth, e.g. '1969.02.16'
     * @return age, e.g. '54'
     */
    private Integer getAgeByDateOfBirth(String dateOfBirth) {
        log.info("Date of birth: " + dateOfBirth);
        java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyy.MM.dd");
        try {
            java.util.Date dOfBirth = dateFormat.parse(dateOfBirth);
            java.util.Date currentDate = new java.util.Date();
            if ((currentDate.getMonth() < dOfBirth.getMonth()) || (currentDate.getMonth() == dOfBirth.getMonth() && currentDate.getDate() < dOfBirth.getDate())) {
                return Integer.valueOf(currentDate.getYear() - dOfBirth.getYear()) - 1;
            } else {
                return Integer.valueOf(currentDate.getYear() - dOfBirth.getYear());
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return -1;
        }
    }
}
