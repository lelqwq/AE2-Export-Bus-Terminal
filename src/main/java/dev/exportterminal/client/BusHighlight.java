package dev.exportterminal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import dev.exportterminal.BusEntry;
import dev.exportterminal.util.BusBounds;

public class BusHighlight {

    private static BusEntry selected;
    private static long expiresAt;

    public static void select(BusEntry entry) {
        selected = entry;
        expiresAt = System.currentTimeMillis() + 300000L;
    }

    public static void clear() {
        selected = null;
    }

    private static BusEntry target() {
        if (System.currentTimeMillis() >= expiresAt) selected = null;
        return selected;
    }

    @SubscribeEvent
    public void renderWorld(RenderWorldLastEvent event) {
        BusEntry entry = target();
        Minecraft mc = Minecraft.getMinecraft();
        if (entry == null || mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.dimension != entry.dimension)
            return;
        EntityPlayer player = mc.thePlayer;
        double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks;
        double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks;
        double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks;
        double[] b = BusBounds.forSide(entry.side);
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
            entry.x + b[0] - px,
            entry.y + b[1] - py,
            entry.z + b[2] - pz,
            entry.x + b[3] - px,
            entry.y + b[4] - py,
            entry.z + b[5] - pz);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(0.3F, 1.0F, 0.75F, 0.95F);
            GL11.glLineWidth(3.0F);
            RenderGlobal.drawOutlinedBoundingBox(box, -1);
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    @SubscribeEvent
    public void renderHud(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        BusEntry entry = target();
        Minecraft mc = Minecraft.getMinecraft();
        if (entry == null || mc.thePlayer == null || mc.gameSettings.hideGUI) return;
        int center = event.resolution.getScaledWidth() / 2;
        String location = "D" + entry.dimension
            + "  "
            + entry.x
            + ", "
            + entry.y
            + ", "
            + entry.z
            + "  ["
            + GuiExportBusTerminal.sideName(entry.side)
            + "]";
        mc.fontRenderer
            .drawStringWithShadow(location, center - mc.fontRenderer.getStringWidth(location) / 2, 41, 0x70E5C5);
        if (mc.thePlayer.dimension != entry.dimension) {
            String text = StatCollector.translateToLocal("ae2exportterminal.gui.otherdimension");
            mc.fontRenderer.drawStringWithShadow(text, center - mc.fontRenderer.getStringWidth(text) / 2, 54, 0xFFD080);
            return;
        }
        int meters = (int) Math
            .ceil(Math.sqrt(entry.distanceSquared(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)));
        String distance = meters + " m";
        mc.fontRenderer.drawStringWithShadow(distance, center + 15, 66, 0xFFFFFF);
        double dx = entry.x + 0.5 - mc.thePlayer.posX;
        double dz = entry.z + 0.5 - mc.thePlayer.posZ;
        float angle = MathHelper
            .wrapAngleTo180_float((float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F - mc.thePlayer.rotationYaw);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glTranslatef(center, 70, 0);
            GL11.glRotatef(angle, 0, 0, 1);
            GL11.glColor4f(0.44F, 0.9F, 0.77F, 1.0F);
            GL11.glBegin(GL11.GL_TRIANGLES);
            GL11.glVertex2f(0, -10);
            GL11.glVertex2f(-6, 7);
            GL11.glVertex2f(6, 7);
            GL11.glEnd();
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }
}
