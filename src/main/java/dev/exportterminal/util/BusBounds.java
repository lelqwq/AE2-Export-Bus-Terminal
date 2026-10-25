package dev.exportterminal.util;

public final class BusBounds {

    private BusBounds() {}

    public static double[] forSide(int side) {
        if (side < 0 || side > 5) throw new IllegalArgumentException("Not a bus face");
        double[] bounds = { 0.25, 0.25, 0.25, 0.75, 0.75, 0.75 };
        int axis = side < 2 ? 1 : side < 4 ? 2 : 0;
        boolean positive = (side & 1) == 1;
        bounds[axis] = positive ? 0.8125 : -0.005;
        bounds[axis + 3] = positive ? 1.005 : 0.1875;
        return bounds;
    }
}
