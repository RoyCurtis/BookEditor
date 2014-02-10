package roycurtis;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C12PacketUpdateSign;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/**
 * Gui with additional sign edit functionality.
 * 
 * Based on complete copy-and-paste of GuiEditSign. Needed to avoid the hassle of
 * reflecting away the private fields.
 */
@SideOnly(Side.CLIENT)
public class GuiEditSignExtra extends GuiScreen
{
    protected String screenTitle = "Edit sign message:";
    
    private TileEntitySign tileSign;
    
    private int updateCounter;
    private int editLine;
    
    private GuiButton doneBtn;

    public GuiEditSignExtra(TileEntitySign tileSign)
    {
        this.tileSign = tileSign;
    }

    @Override
    public void initGui()
    {
        buttonList.clear();
        Keyboard.enableRepeatEvents(true);
        buttonList.add(doneBtn = new GuiButton(0, width / 2 - 100, height / 4 + 120, "Done"));
        tileSign.setEditable(true);
    }

    @Override
    public void onGuiClosed()
    {
        Keyboard.enableRepeatEvents(false);
        NetHandlerPlayClient localNetHandlerPlayClient = mc.getNetHandler();
        if (localNetHandlerPlayClient != null)
            localNetHandlerPlayClient.addToSendQueue( new C12PacketUpdateSign(tileSign.xCoord, tileSign.yCoord, tileSign.zCoord, tileSign.signText) );
        
        tileSign.setEditable(true);
    }

    @Override
    public void updateScreen()
    {
        updateCounter += 1;
    }

    @Override
    protected void actionPerformed(GuiButton clicked)
    {
        if (!clicked.enabled)
            return;
        
        if (clicked.id == 0)
        {
            tileSign.markDirty();
            mc.displayGuiScreen(null);
        }
    }

    @Override
    protected void keyTyped(char typed, int lwjglKey)
    {
        switch (typed)
        {
            // Pasting
            case '\026':
                typeIntoSign( GuiScreen.getClipboardString(), editLine );
                return;
        }
        
        // Up
        if (lwjglKey == 200)
            editLine = (editLine - 1 & 0x3);
        
        // Down or enter
        if (lwjglKey == 208 || lwjglKey == 28 || lwjglKey == 156)
            editLine = (editLine + 1 & 0x3);
        
        // Backspace
        if (lwjglKey == 14 && tileSign.signText[editLine].length() > 0)
            tileSign.signText[editLine] = tileSign.signText[editLine].substring(0, tileSign.signText[editLine].length() - 1);
        
        if (ChatAllowedCharacters.isAllowedCharacter(typed) && tileSign.signText[editLine].length() < 15)
            typeIntoSign( Character.toString(typed), editLine );
        
        // ESC
        if (lwjglKey == 1)
            actionPerformed(doneBtn);
    }
    
    protected void typeIntoSign(String append, int line)
    {
        String currentText = tileSign.signText[line];
        String newText     = currentText + append;
        
        if (newText.length() >= 15)
            newText = newText.substring(0, 14);
        
        tileSign.signText[line] = newText;
    }

    public void drawScreen(int paramInt1, int paramInt2, float paramFloat)
    {
        drawDefaultBackground();

        drawCenteredString(this.fontRendererObj, this.screenTitle, this.width / 2, 40, 16777215);

        GL11.glPushMatrix();
        GL11.glTranslatef(this.width / 2, 0.0F, 50.0F);
        float f1 = 93.75F;
        GL11.glScalef(-f1, -f1, -f1);
        GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);

        Block localBlock = this.tileSign.getBlockType();
        if (localBlock == Blocks.standing_sign) {
            float f2 = this.tileSign.getBlockMetadata() * 360 / 16.0F;
            GL11.glRotatef(f2, 0.0F, 1.0F, 0.0F);

            GL11.glTranslatef(0.0F, -1.0625F, 0.0F);
        }
        else {
            int i = this.tileSign.getBlockMetadata();
            float f3 = 0.0F;
            if (i == 2) {
                f3 = 180.0F;
            }
            if (i == 4) {
                f3 = 90.0F;
            }
            if (i == 5) {
                f3 = -90.0F;
            }
            GL11.glRotatef(f3, 0.0F, 1.0F, 0.0F);
            GL11.glTranslatef(0.0F, -1.0625F, 0.0F);
        }
        if (this.updateCounter / 6 % 2 == 0) {
            this.tileSign.lineBeingEdited = this.editLine;
        }
        TileEntityRendererDispatcher.instance.renderTileEntityAt(this.tileSign, -0.5D, -0.75D, -0.5D, 0.0F);
        this.tileSign.lineBeingEdited = -1;

        GL11.glPopMatrix();

        super.drawScreen(paramInt1, paramInt2, paramFloat);
    }
}
