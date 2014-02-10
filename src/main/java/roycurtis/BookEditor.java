package roycurtis;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.io.File;
import java.lang.reflect.Field;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.Logger;

@Mod(modid = BookEditor.MODID, version = BookEditor.VERSION)
public class BookEditor
{
    public static final String  MODID   = "bookeditor";
    public static final String  VERSION = "0.1";
    public static final Boolean DEV     = Boolean.parseBoolean( System.getProperty("development", "false") );
    
    public static Logger Logger;
    public static File   BaseDir;
    public static File   SubDir;
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        Logger  = event.getModLog();
        BaseDir = new File(event.getModConfigurationDirectory(), MODID);
        
        if ( !BaseDir.exists() )
            BaseDir.mkdir();
    }
        
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
        Logger.debug("Registered events");
        
        Logger.info("Loaded version %s", VERSION);
    }
    
    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event)
    {
        Logger.trace("Open GUI event: %s", event.gui);
        
        if ( event.gui instanceof GuiScreenBook )
            interceptBookGui(event);
        else if (event.gui instanceof GuiEditSign)
            interceptSignGui(event);              
    }

    private void interceptBookGui(GuiOpenEvent event)
    {
        Logger.debug("Intercepting book open GUI");

        GuiScreenBook old = (GuiScreenBook) event.gui;
        
        Class<GuiScreenBook> guiClass = GuiScreenBook.class;
        
        try
        {
            Field fieldPlayer, fieldStack, fieldUnsigned;
            
            if (DEV)
            {
                fieldPlayer   = guiClass.getDeclaredField("editingPlayer");
                fieldStack    = guiClass.getDeclaredField("bookObj");
                fieldUnsigned = guiClass.getDeclaredField("bookIsUnsigned");
            }
            else
            {
                fieldPlayer   = guiClass.getDeclaredField("field_146468_g");
                fieldStack    = guiClass.getDeclaredField("field_146474_h");
                fieldUnsigned = guiClass.getDeclaredField("field_146475_i");
            }
            
            fieldPlayer.setAccessible(true);
            fieldStack.setAccessible(true);
            fieldUnsigned.setAccessible(true);
            Logger.debug("Tampered with GuiScreenBook");

            GuiScreenBookExtra gui = new GuiScreenBookExtra( (EntityPlayer) fieldPlayer.get(old), (ItemStack) fieldStack.get(old), fieldUnsigned.getBoolean(old) );
            event.gui = gui;
        }
        catch (Exception ex)
        {
            Logger.error("Could not get data from GuiScreenBook", ex);
        } 
    }

    private void interceptSignGui(GuiOpenEvent event)
    {
        Logger.debug("Intercepting sign edit GUI");
        
        GuiEditSign old = (GuiEditSign) event.gui;
        
        Class<GuiEditSign> guiClass = GuiEditSign.class;
        
        try
        {
            Field fieldTileEntity;
            if (DEV)
                fieldTileEntity = guiClass.getDeclaredField("tileSign");
            else
                fieldTileEntity = guiClass.getDeclaredField("field_146848_f");
            
            fieldTileEntity.setAccessible(true);
            Logger.debug("Tampered with GuiEditSign");

            GuiEditSignExtra gui = new GuiEditSignExtra( (TileEntitySign) fieldTileEntity.get(old) );
            event.gui = gui;
        }
        catch (Exception ex)
        {
            Logger.error("Could not get data from GuiScreenBook", ex);
        } 
    }
}
