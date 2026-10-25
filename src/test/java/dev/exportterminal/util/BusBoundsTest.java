package dev.exportterminal.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BusBoundsTest {

    @Test
    public void eachBusFaceHasItsOwnThinOutline() {
        double[][] centers = { { 0.5, 0.09125, 0.5 }, { 0.5, 0.90875, 0.5 }, { 0.5, 0.5, 0.09125 },
            { 0.5, 0.5, 0.90875 }, { 0.09125, 0.5, 0.5 }, { 0.90875, 0.5, 0.5 } };
        for (int side = 0; side < 6; side++) {
            double[] bounds = BusBounds.forSide(side);
            for (int axis = 0; axis < 3; axis++) {
                assertTrue(bounds[axis] < bounds[axis + 3]);
                assertEquals(centers[side][axis], (bounds[axis] + bounds[axis + 3]) / 2, 0.00001);
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsCableCenterAsBusFace() {
        BusBounds.forSide(6);
    }
}
