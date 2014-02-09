package roycurtis;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.FormattedMessage;

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
        LOGGER.info("Loaded version %s", VERSION);
    }
    
    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event)
    {
        LOGGER.debug("Open GUI event: %s", event.gui);
        
        if ( !(event.gui instanceof GuiScreenBook) )
            return;

        LOGGER.debug("Intercepting book open GUI");

        GuiScreenBook old = (GuiScreenBook) event.gui;
        
        GuiScreenBookExtra gui = new GuiScreenBookExtra(old.editingPlayer, old.bookObj, old.bookIsUnsigned);
        event.gui = gui;
    }
}
