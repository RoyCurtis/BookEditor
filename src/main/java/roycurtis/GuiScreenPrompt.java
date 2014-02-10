package roycurtis;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class GuiScreenPrompt extends GuiScreen
{
    private final GuiPromptCallback callback;
    
    private final String    prompt;
    private final String    defaultText;
    private final GuiScreen parentScreen;
    
    private GuiTextField textField;
    
    public GuiScreenPrompt(GuiScreen parent, String prompt, String defaultText, GuiPromptCallback callback)
    {
        this.parentScreen = parent;
        this.prompt       = prompt;
        this.defaultText  = defaultText;
        this.callback     = callback;
    }

    @Override
    public void updateScreen()
    {
        textField.updateCursorCounter();
    }

    @Override
    public void initGui()
    {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        buttonList.add(new GuiButton(0, width / 2 - 100, height / 4 + 96 + 12, "Save"));
        buttonList.add(new GuiButton(1, width / 2 - 100, height / 4 + 120 + 12, "Cancel"));
        
        textField = new GuiTextField(fontRendererObj, width / 2 - 100, 60, 200, 20);
        textField.setFocused(true);
        textField.setText(defaultText);
    }

    @Override
    public void onGuiClosed()
    {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(GuiButton clicked)
    {
        if (!clicked.enabled)
            return;
        
        mc.displayGuiScreen(parentScreen);
        
        if (clicked.id == 0)
            callback.onConfirm( textField.getText().trim() );
        else
            callback.onCancel();
    }

    @Override
    protected void keyTyped(char paramChar, int paramInt)
    {
        textField.textboxKeyTyped(paramChar, paramInt);
        
        // Enter
        if (paramInt == 28 || paramInt == 156)
            actionPerformed((GuiButton) buttonList.get(0));
    }

    @Override
    protected void mouseClicked(int paramInt1, int paramInt2, int paramInt3)
    {
        super.mouseClicked(paramInt1, paramInt2, paramInt3);
        textField.mouseClicked(paramInt1, paramInt2, paramInt3);
    }

    @Override
    public void drawScreen(int paramInt1, int paramInt2, float paramFloat)
    {
        drawCenteredString(fontRendererObj, prompt, width / 2, 20, 16777215);

        textField.drawTextBox();

        super.drawScreen(paramInt1, paramInt2, paramFloat);
    }

    public interface GuiPromptCallback
    {
        public void onConfirm(String data);
        public void onCancel();
    }
}
