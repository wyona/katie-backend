package com.wyona.katie.services;

import com.wyona.katie.models.ByteVector;
import com.wyona.katie.models.FloatVector;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
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
        log.info("Float vector addition: a + b = " + d);
        FloatVector result = new FloatVector(2);
        result.set(0, 2);
        result.set(1, 8);
        assertTrue(Arrays.equals(d.getValues(), result.getValues()));

        ByteVector bv_a = new ByteVector(2);
        bv_a.set(0, (byte) -5);
        bv_a.set(1, (byte) 4);

        ByteVector bv_b = new ByteVector(2);
        bv_b.set(0, (byte) 3);
        bv_b.set(1, (byte) 7);

        ByteVector bv_result = new ByteVector(2);
        bv_result.set(0, (byte) -2);
        bv_result.set(1, (byte) 11);
        log.info("Expected byte vector addition result: " + bv_result);

        bv_a.add(bv_b);
        log.info("Byte vector addition: a + b = " + bv_a);
        assertTrue(Arrays.equals(bv_a.getValues(), bv_result.getValues()));
    }
}
