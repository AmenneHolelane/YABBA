package com.latmod.yabba.client.gui;

import com.latmod.yabba.Yabba;
import com.latmod.yabba.YabbaRegistry;
import com.latmod.yabba.api.IBarrelSkin;
import com.latmod.yabba.net.MessageSelectSkin;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author LatvianModder
 */
public class GuiSelectSkin extends GuiYabba
{
    public static final GuiSelectSkin INSTANCE = new GuiSelectSkin();

    private static class Button
    {
        int posX, posY, width, height;

        private Button(int x, int y, int w, int h)
        {
            posX = x;
            posY = y;
            width = w;
            height = h;
        }

        boolean isMouseOver(int rmouseX, int rmouseY)
        {
            return rmouseX >= posX && rmouseY >= posY && rmouseX < posX + width && rmouseY < posY + height;
        }
    }

    private static final Button SEARCH_BAR = new Button(3, 3, 187, 12);
    private static final Button SCROLL_BAR = new Button(174, 20, 16, 111);
    private static final Button SKINS_PANEL = new Button(1, 18, 171, 115);

    private class Skin extends Button
    {
        private int index;
        private final IBarrelSkin skin;
        private final String spriteName;
        private String mouseOverText = "";

        private Skin(int i, IBarrelSkin s)
        {
            super(0, 0, 18, 18);
            index = i;
            skin = s;
            mouseOverText = s.getDisplayName();
            spriteName = skin.getTextures().getTexture(EnumFacing.NORTH).toString();
        }

        void updatePos()
        {
            posX = 2 + (index % 9) * 19;
            posY = 19 + (index / 9) * 19 - (int) (scroll * skinsHeight);
        }

        boolean isVisible()
        {
            return posY + 19 >= SKINS_PANEL.posY && posY - 19 < SKINS_PANEL.posY + SKINS_PANEL.height;
        }
    }

    private final List<Skin> ALL_SKINS = new ArrayList<>();
    private List<Skin> visibleSkins = new ArrayList<>();
    private String searchBar = "";
    private boolean searchSelected = false, updateVisibleSkins = true;
    private float scroll = 0F;
    private float skinsHeight;
    private boolean scrollGrabbed = false;

    private GuiSelectSkin()
    {
        super(new ResourceLocation(Yabba.MOD_ID, "textures/gui/skin.png"), 193, 155);
    }

    public void initSkins()
    {
        ALL_SKINS.clear();

        for(int i = 0; i < YabbaRegistry.ALL_SKINS.size(); i++)
        {
            ALL_SKINS.add(new Skin(0, YabbaRegistry.ALL_SKINS.get(i)));
        }
    }

    private void updateVisibleSkins()
    {
        visibleSkins.clear();
        scroll = 0F;

        List<Skin> matchingSkins = searchBar.isEmpty() ? ALL_SKINS : new ArrayList<>();

        if(!searchBar.isEmpty())
        {
            String searchBar1 = searchBar.toLowerCase().replace(" ", "");

            for(Skin skin : ALL_SKINS)
            {
                if(skin.mouseOverText.toLowerCase().replace(" ", "").contains(searchBar1))
                {
                    matchingSkins.add(skin);
                }
            }
        }

        skinsHeight = MathHelper.ceil(matchingSkins.size() / 9F) * 19F - SKINS_PANEL.height + 1;

        if(!matchingSkins.isEmpty())
        {
            for(int i = 0; i < matchingSkins.size(); i++)
            {
                Skin skin = matchingSkins.get(i);
                skin.index = i;
                skin.updatePos();
                visibleSkins.add(skin);
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float ticks)
    {
        if(updateVisibleSkins)
        {
            updateVisibleSkins();
            updateVisibleSkins = false;
        }

        if(skinsHeight > SKINS_PANEL.height)
        {
            float scroll0 = scroll;
            int mouseWheel = Mouse.getDWheel();

            if(mouseWheel != 0)
            {
                scroll += 19F / (mouseWheel > 0 ? -skinsHeight : skinsHeight);
            }

            if(scrollGrabbed)
            {
                scroll = (mouseY - guiY - SCROLL_BAR.posY - 6F) / (SCROLL_BAR.height - 12F);
            }

            if(scroll < 0F)
            {
                scroll = 0F;
            }
            else if(scroll > 1F)
            {
                scroll = 1F;
            }

            if(scroll0 != scroll)
            {
                for(Skin skin : visibleSkins)
                {
                    skin.updatePos();
                }
            }
        }

        super.drawScreen(mouseX, mouseY, ticks);

        int rmouseX = mouseX - guiX;
        int rmouseY = mouseY - guiY;
        boolean mouseInSkinsPanel = SKINS_PANEL.isMouseOver(rmouseX, rmouseY);

        drawTexturedModalRect(guiX + SCROLL_BAR.posX, guiY + SCROLL_BAR.posY + (int) (scroll * (SCROLL_BAR.height - 12F)), 213, 0, SCROLL_BAR.width, 12);

        ScaledResolution screen = new ScaledResolution(mc);
        int sf = screen.getScaleFactor();
        int scissorX = guiX + SKINS_PANEL.posX;
        int scissorY = guiY + SKINS_PANEL.posY;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX * sf, (screen.getScaledHeight() * sf) - (scissorY * sf + SKINS_PANEL.height * sf), SKINS_PANEL.width * sf, SKINS_PANEL.height * sf);

        for(Skin skin : visibleSkins)
        {
            if(!skin.isVisible())
            {
                continue;
            }

            drawTexturedModalRect(guiX + skin.posX, guiY + skin.posY, 194, (mouseInSkinsPanel && skin.isMouseOver(rmouseX, rmouseY)) ? 19 : 0, skin.width, skin.height);
        }

        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        for(Skin skin : visibleSkins)
        {
            if(!skin.isVisible())
            {
                continue;
            }

            TextureAtlasSprite sprite = mc.getTextureMapBlocks().getAtlasSprite(skin.spriteName);
            Tessellator tessellator = Tessellator.getInstance();
            VertexBuffer buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            double x = guiX + skin.posX + 1D;
            double y = guiY + skin.posY + 1D;
            buffer.pos(x, y + 16D, 0D).tex(sprite.getMinU(), sprite.getMaxV()).endVertex();
            buffer.pos(x + 16D, y + 16D, 0D).tex(sprite.getMaxU(), sprite.getMaxV()).endVertex();
            buffer.pos(x + 16D, y, 0D).tex(sprite.getMaxU(), sprite.getMinV()).endVertex();
            buffer.pos(x, y, 0D).tex(sprite.getMinU(), sprite.getMinV()).endVertex();
            tessellator.draw();
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        String searchBarText = searchBar;

        if(searchSelected && (System.currentTimeMillis() % 800L >= 400L))
        {
            searchBarText += '_';
        }

        GlStateManager.color(1F, 1F, 1F, 1F);
        fontRendererObj.drawString(searchBarText, guiX + 6, guiY + 5, 0xFFFFFFFF);

        if(mouseInSkinsPanel)
        {
            for(Skin skin : visibleSkins)
            {
                if(!skin.mouseOverText.isEmpty() && skin.isMouseOver(rmouseX, rmouseY))
                {
                    drawHoveringText(Collections.singletonList(skin.mouseOverText), mouseX, mouseY);
                }
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        searchSelected = false;
        int rmouseX = mouseX - guiX;
        int rmouseY = mouseY - guiY;

        if(SEARCH_BAR.isMouseOver(rmouseX, rmouseY))
        {
            searchSelected = true;

            if(mouseButton != 0)
            {
                searchBar = "";
                updateVisibleSkins = true;
            }
        }
        else if(SCROLL_BAR.isMouseOver(rmouseX, rmouseY))
        {
            scrollGrabbed = true;
        }
        else if(SKINS_PANEL.isMouseOver(rmouseX, rmouseY))
        {
            for(Skin skin : visibleSkins)
            {
                if(skin.isMouseOver(rmouseX, rmouseY))
                {
                    new MessageSelectSkin(skin.skin.getName()).sendToServer();
                    INSTANCE.mc.player.closeScreen();
                    return;
                }
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        super.mouseReleased(mouseX, mouseY, state);
        scrollGrabbed = false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if(searchSelected)
        {
            switch(keyCode)
            {
                case Keyboard.KEY_ESCAPE:
                case Keyboard.KEY_RETURN:
                case Keyboard.KEY_TAB:
                    searchSelected = false;
                    break;
                case Keyboard.KEY_BACK:
                    if(!searchBar.isEmpty())
                    {
                        if(isShiftKeyDown())
                        {
                            searchBar = "";
                        }
                        else
                        {
                            searchBar = searchBar.substring(0, searchBar.length() - 1);
                        }
                        updateVisibleSkins = true;
                        break;
                    }
                    break;
                default:
                    if(typedChar >= 'a' && typedChar <= 'z' || typedChar >= 'A' && typedChar <= 'Z' || typedChar >= '0' && typedChar <= '1' || " .,/-_".indexOf(typedChar) != -1)
                    {
                        searchBar += typedChar;
                        updateVisibleSkins = true;
                    }
                    break;
            }
        }
        else
        {
            super.keyTyped(typedChar, keyCode);
        }
    }
}