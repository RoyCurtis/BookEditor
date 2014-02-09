package roycurtis;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.lang.reflect.Field;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = BookEditor.MODID, version = BookEditor.VERSION)
public class BookEditor
{
    public static final String MODID = "BookEditor";
    public static final String VERSION = "0.1";
    public static Logger LOGGER = LogManager.getFormatterLogger(MODID);
    
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
        LOGGER.info("Registered events");
        
        LOGGER.info("Loaded version %s", VERSION);
    }
    
    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event)
    {
        LOGGER.info("Open GUI event: %s", event.gui);
        
        if ( !(event.gui instanceof GuiScreenBook) )
            return;

        LOGGER.info("Intercepting book open GUI");

        GuiScreenBook old = (GuiScreenBook) event.gui;
        
        Class<GuiScreenBook> guiClass = GuiScreenBook.class;
        
        try
        {
            Field fieldPlayer   = guiClass.getDeclaredField("field_146468_g");
            Field fieldStack    = guiClass.getDeclaredField("field_146474_h");
            Field fieldUnsigned = guiClass.getDeclaredField("field_146475_i");
            
            fieldPlayer.setAccessible(true);
            fieldStack.setAccessible(true);
            fieldUnsigned.setAccessible(true);
            LOGGER.info("Tampered with GuiScreenBook");

            GuiScreenBookExtra gui = new GuiScreenBookExtra(
                (EntityPlayer) fieldPlayer.get(old),
                (ItemStack)    fieldStack.get(old),
                               fieldUnsigned.getBoolean(old)
            );
            event.gui = gui;
        }
        catch (Exception ex)
        {
            LOGGER.error("Could not get data from GuiScreenBook", ex);
        }        
    }
}
