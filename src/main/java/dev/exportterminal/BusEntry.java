package dev.exportterminal;

import net.minecraft.item.ItemStack;

public class BusEntry {

    public int dimension;
    public int x;
    public int y;
    public int z;
    public int side;
    public boolean online;
    public int enabledSlots;
    public boolean craftingCard;
    public boolean redstoneCard;
    public String redstoneMode = "IGNORE";
    public String fuzzyMode = "";
    public String oreFilter = "";
    public final ItemStack[] filters = new ItemStack[9];

    public double distanceSquared(double px, double py, double pz) {
        double dx = x + 0.5 - px;
        double dy = y + 0.5 - py;
        double dz = z + 0.5 - pz;
        return dx * dx + dy * dy + dz * dz;
    }
}
