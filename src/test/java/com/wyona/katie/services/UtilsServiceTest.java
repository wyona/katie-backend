package com.wyona.katie.services;

import com.wyona.katie.models.FloatVector;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class UtilsServiceTest {

    /**
     * TODO
     */
    @Test
    void getCentroid() {
        FloatVector a = new FloatVector(2);
        a.set(0, 0);
        a.set(1, 3);

        FloatVector b = new FloatVector(2);
        b.set(0, 2);
        b.set(1, 5);

        FloatVector c = new FloatVector(2);
        c.set(0, 4);
        c.set(1, 1);

        List<FloatVector> vectors = new ArrayList<>();
        vectors.add(a);
        vectors.add(b);
        vectors.add(c);

        FloatVector centroid = UtilsService.getCentroid(vectors.toArray(new FloatVector[0]));
        log.info("Centroid: " + centroid);

        assertTrue(centroid.getValues()[0] == 2);
        assertTrue(centroid.getValues()[1] == 3);

        FloatVector d = a.clone();
        d.add(b);
        log.info("a + b: " + d);
        assertTrue(d.getValues()[0] == 2);
        assertTrue(d.getValues()[1] == 8);
    }
}
