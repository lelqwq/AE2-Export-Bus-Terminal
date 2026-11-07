package dev.exportterminal.client;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import dev.exportterminal.BusEntry;
import dev.exportterminal.FluidFilters;
import dev.exportterminal.TerminalNetwork;
import dev.exportterminal.util.BusNameOrder;
import dev.exportterminal.util.ItemSearch;

public class GuiExportBusTerminal extends GuiScreen {

    private static final int ROW_HEIGHT = 46;
    private static boolean sortByName = true;
    private static final String[] BUS_TYPES = { "all", "itembus", "fluidbus", "universalbus", "essentiabus",
        "manabus" };
    private static int selectedType;
    private final List<BusEntry> entries = new ArrayList<>();
    private final List<String> searchTexts = new ArrayList<>();
    private final List<BusEntry> visible = new ArrayList<>();
    private GuiTextField search;
    private int session;
    private int expected;
    private int top;
    private int left;
    private int panelWidth;
    private int panelHeight;
    private int rowCount;
    private int scroll;
    private int refreshTicks;
    private String query = "";
    private BusNameOrder nameOrder;
    private boolean sortDirty;
    private List<BusEntry> pending;
    private BitSet received;
    private int changesRemaining;

    public void begin(int session, int total, int changes, boolean delta) {
        this.session = session;
        expected = total;
        pending = new ArrayList<>(delta ? entries : Collections.<BusEntry>emptyList());
        while (pending.size() > total) pending.remove(pending.size() - 1);
        while (pending.size() < total) pending.add(null);
        received = new BitSet(total);
        changesRemaining = changes;
        if (changes == 0) finishSnapshot();
    }

    public void accept(int session, int index, BusEntry entry) {
        if (this.session != session || pending == null || index < 0 || index >= expected || received.get(index)) return;
        pending.set(index, entry);
        received.set(index);
        if (--changesRemaining == 0) finishSnapshot();
    }

    private void finishSnapshot() {
        if (pending.contains(null)) {
            pending = null;
            session = 0;
            return;
        }
        List<BusEntry> complete = pending;
        pending = null;
        entries.clear();
        searchTexts.clear();
        visible.clear();
        nameOrder = new BusNameOrder(
            mc.thePlayer.dimension,
            Locale.forLanguageTag(mc.gameSettings.language.replace('_', '-')));
        scroll = 0;
        for (BusEntry entry : complete) addEntry(entry);
        sortVisible();
    }

    private void addEntry(BusEntry entry) {
        entries.add(entry);
        List<String> configuredNames = new ArrayList<>();
        StringBuilder text = new StringBuilder(entry.oreFilter);
        text.append(' ')
            .append(tr(entry.kind));
        text.append(' ')
            .append(entry.dimension)
            .append(' ')
            .append(entry.x)
            .append(' ')
            .append(entry.y)
            .append(' ')
            .append(entry.z);
        for (int slot = 0; slot < entry.filters.length; slot++) {
            ItemStack stack = entry.filters[slot];
            String filterName = entry.filterNames[slot];
            if (filterName != null && !filterName.isEmpty()) {
                configuredNames.add(filterName);
                text.append(' ')
                    .append(filterName);
            }
            if (stack != null) {
                FluidStack configuredFluid = entry.fluidFilters[slot] ? FluidFilters.fromPacket(stack) : null;
                configuredNames
                    .add(configuredFluid == null ? stack.getDisplayName() : configuredFluid.getLocalizedName());
                text.append(' ')
                    .append(stack.getDisplayName())
                    .append(' ')
                    .append(Item.itemRegistry.getNameForObject(stack.getItem()));
                if (entry.fluidFilters[slot]) {
                    FluidStack fluid = FluidFilters.fromPacket(stack);
                    if (fluid != null) {
                        text.append(' ')
                            .append(fluid.getLocalizedName())
                            .append(' ')
                            .append(
                                fluid.getFluid()
                                    .getName());
                    }
                }
            }
        }
        String searchable = text.toString();
        searchTexts.add(searchable);
        nameOrder.setNames(entry, configuredNames);
        if (matches(entry, searchable)) visible.add(entry);
        sortDirty = true;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        panelWidth = Math.min(500, width - 16);
        panelHeight = Math.min(364, height - 24);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        rowCount = Math.max(1, (panelHeight - 125) / ROW_HEIGHT);
        search = new GuiTextField(fontRendererObj, left + 9, top + 28, panelWidth - 201, 18);
        search.setMaxStringLength(128);
        search.setText(query);
        search.setFocused(false);
        buttonList.clear();
        buttonList.add(new GuiButton(0, left + panelWidth - 184, top + 27, 68, 20, tr("refresh")));
        buttonList.add(new GuiButton(1, left + panelWidth - 112, top + 27, 103, 20, tr("clear")));
        buttonList.add(
            new GuiButton(
                2,
                left + panelWidth - 164,
                top + 5,
                155,
                20,
                tr(sortByName ? "sort.name" : "sort.distance")));
        buttonList.add(new GuiButton(3, left + panelWidth - 164, top + 50, 155, 20, typeLabel()));
    }

    private static String typeLabel() {
        return tr("type") + ": " + tr(BUS_TYPES[selectedType]);
    }

    private boolean matches(BusEntry entry, String text) {
        return (selectedType == 0 || BUS_TYPES[selectedType].equals(entry.kind)) && ItemSearch.matches(text, query);
    }

    private void filter() {
        query = search.getText();
        visible.clear();
        for (int i = 0; i < entries.size(); i++) {
            if (matches(entries.get(i), searchTexts.get(i))) visible.add(entries.get(i));
        }
        scroll = 0;
        sortVisible();
    }

    private void sortVisible() {
        if (sortByName) Collections.sort(visible, nameOrder);
        sortDirty = false;
    }

    @Override
    public void updateScreen() {
        if (sortDirty) sortVisible();
        search.updateCursorCounter();
        if (refreshTicks > 0) refreshTicks--;
        ((GuiButton) buttonList.get(0)).enabled = refreshTicks == 0 && pending == null;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            TerminalNetwork.CHANNEL.sendToServer(new TerminalNetwork.Refresh(session));
            refreshTicks = 20;
        } else if (button.id == 1) {
            BusHighlight.clear();
        } else if (button.id == 2) {
            sortByName = !sortByName;
            button.displayString = tr(sortByName ? "sort.name" : "sort.distance");
            filter();
        } else if (button.id == 3) {
            cycleType(1);
        }
    }

    private void cycleType(int direction) {
        selectedType = (selectedType + direction + BUS_TYPES.length) % BUS_TYPES.length;
        ((GuiButton) buttonList.get(3)).displayString = typeLabel();
        filter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawRect(left, top, left + panelWidth, top + panelHeight, 0xF01A222A);
        drawRect(left, top, left + panelWidth, top + 3, 0xFF56D6BF);
        fontRendererObj
            .drawString(fontRendererObj.trimStringToWidth(tr("title"), panelWidth - 182), left + 9, top + 11, 0xE3F6F1);
        search.drawTextBox();
        String status = StatCollector
            .translateToLocalFormatted("ae2exportterminal.gui.count", visible.size(), entries.size(), expected);
        fontRendererObj
            .drawString(fontRendererObj.trimStringToWidth(status, panelWidth - 180), left + 9, top + 56, 0x9DB4C3);
        int listTop = top + 90;
        ItemStack hoveredStack = null;
        BusEntry hoveredEntry = null;
        int hoveredSlot = -1;
        for (int row = 0; row < rowCount && row + scroll < visible.size(); row++) {
            BusEntry entry = visible.get(row + scroll);
            int y = listTop + row * ROW_HEIGHT;
            boolean hovered = mouseX >= left + 7 && mouseX < left + panelWidth - 7
                && mouseY >= y
                && mouseY < y + ROW_HEIGHT - 2;
            drawRect(left + 7, y, left + panelWidth - 7, y + ROW_HEIGHT - 2, hovered ? 0xFF344954 : 0xFF25343F);
            String location = "[" + tr(entry.kind)
                + "] D"
                + entry.dimension
                + "  "
                + entry.x
                + ", "
                + entry.y
                + ", "
                + entry.z
                + "  ["
                + sideName(entry.side)
                + "]";
            fontRendererObj
                .drawString(fontRendererObj.trimStringToWidth(location, panelWidth - 91), left + 12, y + 5, 0xE2E9ED);
            String locate = tr("locate");
            fontRendererObj
                .drawString(locate, left + panelWidth - 12 - fontRendererObj.getStringWidth(locate), y + 5, 0x70E5C5);
            for (int slot = 0; slot < (entry.kind.equals("manabus") ? 0 : 9); slot++) {
                int x = left + 12 + slot * 18;
                GL11.glDisable(GL11.GL_LIGHTING);
                drawRect(x - 1, y + 20, x + 17, y + 38, 0xFF14212A);
                ItemStack stack = entry.filters[slot];
                if (stack != null) {
                    FluidStack fluid = entry.fluidFilters[slot] ? FluidFilters.fromPacket(stack) : null;
                    if (fluid != null) {
                        drawFluid(fluid, x, y + 21);
                    } else {
                        RenderHelper.enableGUIStandardItemLighting();
                        itemRender
                            .renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), stack, x, y + 21);
                        RenderHelper.disableStandardItemLighting();
                    }
                }
                if (stack == null && entry.filterNames[slot] != null && !entry.filterNames[slot].isEmpty()) {
                    fontRendererObj.drawString("?", x + 4, y + 25, 0xFFFFFF);
                }
                if (slot >= entry.enabledSlots) {
                    GL11.glDisable(GL11.GL_LIGHTING);
                    GL11.glDisable(GL11.GL_DEPTH_TEST);
                    drawRect(x - 1, y + 20, x + 17, y + 38, 0xB0000000);
                    GL11.glEnable(GL11.GL_DEPTH_TEST);
                }
                if (hovered && mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y + 20 && mouseY < y + 38) {
                    hoveredStack = stack;
                    hoveredSlot = slot;
                }
            }
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            String state = tr(entry.online ? "online" : "offline");
            if (entry.craftingCard) state += " / " + tr("crafting");
            if (!entry.oreFilter.isEmpty()) state += " / " + tr("ore");
            fontRendererObj.drawString(
                fontRendererObj.trimStringToWidth(state, panelWidth - 190),
                left + 182,
                y + 25,
                entry.online ? 0x8CD6AA : 0xE7A58F);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            if (hovered) hoveredEntry = entry;
        }
        if (visible.isEmpty()) fontRendererObj
            .drawString(tr(entries.size() < expected ? "loading" : "empty"), left + 12, listTop + 14, 0xB2C7D1);
        int maxScroll = Math.max(0, visible.size() - rowCount);
        if (maxScroll > 0) {
            int trackHeight = rowCount * ROW_HEIGHT - 2;
            int thumbHeight = Math.max(12, trackHeight * rowCount / visible.size());
            int thumbY = listTop + (trackHeight - thumbHeight) * scroll / maxScroll;
            drawRect(left + panelWidth - 5, listTop, left + panelWidth - 2, listTop + trackHeight, 0xFF14212A);
            drawRect(left + panelWidth - 5, thumbY, left + panelWidth - 2, thumbY + thumbHeight, 0xFF56D6BF);
        }
        fontRendererObj.drawString(
            fontRendererObj.trimStringToWidth(tr("hint"), panelWidth - 18),
            left + 9,
            top + panelHeight - 24,
            0xA0B4BE);
        fontRendererObj.drawString(
            fontRendererObj.trimStringToWidth(tr("snapshot"), panelWidth - 18),
            left + 9,
            top + panelHeight - 13,
            0x7C939F);
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (hoveredEntry != null) {
            List<String> tooltip = new ArrayList<>();
            if (hoveredSlot >= 0 && hoveredEntry.filterNames[hoveredSlot] != null
                && !hoveredEntry.filterNames[hoveredSlot].isEmpty()) {
                tooltip.add(hoveredEntry.filterNames[hoveredSlot]);
            }
            if (hoveredStack != null) {
                tooltip.addAll(hoveredStack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips));
                FluidStack fluid = hoveredEntry.fluidFilters[hoveredSlot] ? FluidFilters.fromPacket(hoveredStack)
                    : null;
                if (fluid != null) tooltip.add(
                    "\u00a77" + fluid.getFluid()
                        .getName());
            }
            tooltip.add("\u00a77" + tr(hoveredEntry.kind));
            if (hoveredSlot >= hoveredEntry.enabledSlots) tooltip.add("\u00a7c" + tr("disabledslot"));
            tooltip.add(
                "\u00a77" + tr("redstone")
                    + ": "
                    + tr(
                        "rs." + (hoveredEntry.redstoneCard || hoveredEntry.kind.equals("manabus")
                            ? hoveredEntry.redstoneMode
                            : "IGNORE")));
            if (!hoveredEntry.fuzzyMode.isEmpty()) tooltip.add("\u00a77" + tr("fuzzy") + ": " + hoveredEntry.fuzzyMode);
            if (!hoveredEntry.oreFilter.isEmpty()) tooltip.add("\u00a77" + tr("ore") + ": " + hoveredEntry.oreFilter);
            tooltip.add("\u00a7b" + tr("clickhint"));
            drawHoveringText(tooltip, mouseX, mouseY, fontRendererObj);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        search.mouseClicked(mouseX, mouseY, button);
        if (button == 1 && mouseX >= left + panelWidth - 164
            && mouseX < left + panelWidth - 9
            && mouseY >= top + 50
            && mouseY < top + 70) {
            ((GuiButton) buttonList.get(3)).func_146113_a(mc.getSoundHandler());
            cycleType(-1);
            return;
        }
        if (button != 0 || mouseX < left + 7 || mouseX >= left + panelWidth - 7) return;
        int relative = mouseY - top - 90;
        if (relative < 0 || relative >= rowCount * ROW_HEIGHT) return;
        int index = scroll + relative / ROW_HEIGHT;
        if (index < visible.size() && relative % ROW_HEIGHT < ROW_HEIGHT - 2) {
            BusHighlight.select(visible.get(index));
            mc.displayGuiScreen(null);
        }
    }

    private void drawFluid(FluidStack fluid, int x, int y) {
        IIcon icon = fluid.getFluid()
            .getStillIcon();
        if (icon == null) icon = mc.getTextureMapBlocks()
            .getAtlasSprite("missingno");
        int color = fluid.getFluid()
            .getColor(fluid);
        mc.getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);
        GL11.glColor4f((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, 1.0F);
        drawTexturedModelRectFromIcon(x, y, icon, 16, 16);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            scroll = Math.max(0, Math.min(Math.max(0, visible.size() - rowCount), scroll + (wheel > 0 ? -1 : 1)));
        }
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (key == Keyboard.KEY_ESCAPE
            || (key == mc.gameSettings.keyBindInventory.getKeyCode() && !search.isFocused())) {
            mc.displayGuiScreen(null);
            return;
        }
        if (search.textboxKeyTyped(character, key)) filter();
        else super.keyTyped(character, key);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    static String tr(String key) {
        return StatCollector.translateToLocal("ae2exportterminal.gui." + key);
    }

    static String sideName(int side) {
        return tr("side." + side);
    }
}
