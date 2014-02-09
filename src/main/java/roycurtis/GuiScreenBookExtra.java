package roycurtis;

import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class GuiScreenBookExtra extends GuiScreenBook
{

    public GuiScreenBookExtra(EntityPlayer player, ItemStack book, boolean writable)
    {
        super(player, book, writable);
        
        BookEditor.LOGGER.trace("Opened BookEditor GUI");
    }
       
}
