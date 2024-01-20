package com.wyona.katie.models.insights;

import java.util.Date;

/**
 * ngx charts (https://swimlane.gitbook.io/ngx-charts/v/docs-test/examples/line-area-charts/line-chart)
 */
public class NgxChartsDataPoint {

    private Date timestamp;
    private long value;

    /**
     *
     */
    public NgxChartsDataPoint(Date timestamp, long value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    /**
     *
     */
    public String getName() {
        java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(timestamp);
    }

    /**
     *
     */
    public long getValue() {
        return value;
    }
}
