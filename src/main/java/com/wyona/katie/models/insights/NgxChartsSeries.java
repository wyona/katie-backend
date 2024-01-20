package com.wyona.katie.models.insights;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ngx charts (https://swimlane.gitbook.io/ngx-charts/v/docs-test/examples/line-area-charts/line-chart)
 */
public class NgxChartsSeries {

    private String name;
    private List<NgxChartsDataPoint> dataPoints;

    /**
     *
     */
    public NgxChartsSeries(String name) {
        this.name = name;
        dataPoints = new ArrayList<NgxChartsDataPoint>();
    }

    /**
     *
     */
    public void addDataPoint(NgxChartsDataPoint dataPoint) {
        dataPoints.add(dataPoint);
    }

    /**
     *
     */
    public String getName() {
        return name;
    }

    /**
     *
     */
    public NgxChartsDataPoint[] getSeries() {
        return dataPoints.toArray(new NgxChartsDataPoint[0]);
    }
}
