package dev.exportterminal.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import dev.exportterminal.BusEntry;
import dev.exportterminal.TerminalNetwork;
import dev.exportterminal.util.ItemSearch;

public class GuiExportBusTerminal extends GuiScreen {

    private static final int ROW_HEIGHT = 46;
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

    public void begin(int session, int total) {
        this.session = session;
        expected = total;
        entries.clear();
        searchTexts.clear();
        visible.clear();
        scroll = 0;
    }

    public void accept(int session, BusEntry entry) {
        if (this.session != session || entries.size() >= expected) return;
        entries.add(entry);
        StringBuilder text = new StringBuilder(entry.oreFilter);
        text.append(' ')
            .append(entry.dimension)
            .append(' ')
            .append(entry.x)
            .append(' ')
            .append(entry.y)
            .append(' ')
            .append(entry.z);
        for (ItemStack stack : entry.filters) {
            if (stack != null) {
                text.append(' ')
                    .append(stack.getDisplayName())
                    .append(' ')
                    .append(Item.itemRegistry.getNameForObject(stack.getItem()));
            }
        }
        String searchable = text.toString();
        searchTexts.add(searchable);
        if (ItemSearch.matches(searchable, query)) visible.add(entry);
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        panelWidth = Math.min(500, width - 16);
        panelHeight = Math.min(364, height - 24);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        rowCount = Math.max(1, (panelHeight - 103) / ROW_HEIGHT);
        search = new GuiTextField(fontRendererObj, left + 9, top + 28, panelWidth - 201, 18);
        search.setMaxStringLength(128);
        search.setText(query);
        search.setFocused(true);
        buttonList.clear();
        buttonList.add(new GuiButton(0, left + panelWidth - 184, top + 27, 68, 20, tr("refresh")));
        buttonList.add(new GuiButton(1, left + panelWidth - 112, top + 27, 103, 20, tr("clear")));
    }

    private void filter() {
        query = search.getText();
        visible.clear();
        for (int i = 0; i < entries.size(); i++) {
            if (ItemSearch.matches(searchTexts.get(i), query)) visible.add(entries.get(i));
        }
        scroll = 0;
    }

    @Override
    public void updateScreen() {
        search.updateCursorCounter();
        if (refreshTicks > 0) refreshTicks--;
        ((GuiButton) buttonList.get(0)).enabled = refreshTicks == 0;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            TerminalNetwork.CHANNEL.sendToServer(new TerminalNetwork.Refresh());
            refreshTicks = 20;
        } else if (button.id == 1) {
            BusHighlight.clear();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawRect(left, top, left + panelWidth, top + panelHeight, 0xF01A222A);
        drawRect(left, top, left + panelWidth, top + 3, 0xFF56D6BF);
        fontRendererObj.drawString(tr("title"), left + 9, top + 11, 0xE3F6F1);
        search.drawTextBox();
        String status = StatCollector
            .translateToLocalFormatted("ae2exportterminal.gui.count", visible.size(), entries.size(), expected);
        fontRendererObj.drawString(status, left + 9, top + 54, 0x9DB4C3);
        int listTop = top + 68;
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
            String location = "D" + entry.dimension
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
            for (int slot = 0; slot < 9; slot++) {
                int x = left + 12 + slot * 18;
                GL11.glDisable(GL11.GL_LIGHTING);
                drawRect(x - 1, y + 20, x + 17, y + 38, 0xFF14212A);
                ItemStack stack = entry.filters[slot];
                if (stack != null) {
                    RenderHelper.enableGUIStandardItemLighting();
                    itemRender.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), stack, x, y + 21);
                    RenderHelper.disableStandardItemLighting();
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
            if (hoveredStack != null) {
                tooltip.addAll(hoveredStack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips));
            }
            if (hoveredSlot >= hoveredEntry.enabledSlots) tooltip.add("\u00a7c" + tr("disabledslot"));
            tooltip.add(
                "\u00a77" + tr("redstone")
                    + ": "
                    + tr("rs." + (hoveredEntry.redstoneCard ? hoveredEntry.redstoneMode : "IGNORE")));
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
        if (button != 0 || mouseX < left + 7 || mouseX >= left + panelWidth - 7) return;
        int relative = mouseY - top - 68;
        if (relative < 0 || relative >= rowCount * ROW_HEIGHT) return;
        int index = scroll + relative / ROW_HEIGHT;
        if (index < visible.size() && relative % ROW_HEIGHT < ROW_HEIGHT - 2) {
            BusHighlight.select(visible.get(index));
            mc.displayGuiScreen(null);
        }
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
        if (key == Keyboard.KEY_ESCAPE) {
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
