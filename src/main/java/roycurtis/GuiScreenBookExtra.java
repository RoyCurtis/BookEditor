package roycurtis;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Scanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import static roycurtis.BookEditor.BaseDir;

/**
 * Gui with book import/export functionality.
 * 
 * Based on complete copy-and-paste of GuiScreenBook. Needed to avoid the hassle of
 * reflecting away the private fields.
 */
@SideOnly(Side.CLIENT)
public class GuiScreenBookExtra extends GuiScreen
{
    private static final int ACTION_DONE_READING = 0;
    private static final int ACTION_NEXT         = 1;
    private static final int ACTION_PREV         = 2;
    private static final int ACTION_SIGN         = 3;
    private static final int ACTION_CANCEL_SIGN  = 4;
    private static final int ACTION_FINALIZE     = 5;
    private static final int ACTION_IMPORT       = 6;
    private static final int ACTION_EXPORT       = 7;
    private static final int ACTION_CHANGE_DIR   = 8;
    
    private static final ResourceLocation bookGuiTextures = new ResourceLocation("textures/gui/book.png");
    
    private final EntityPlayer editingPlayer;
    private final ItemStack    bookObj;
    private final boolean      bookIsUnsigned;
    
    private boolean bookModified;
    private boolean bookGettingSigned;
    
    private int updateCount;
    private int bookImageWidth  = 192;
    private int bookImageHeight = 192;
    private int bookTotalPages  = 1;
    private int currPage;
    
    private String     infoLine  = EnumChatFormatting.GRAY + "Nothing to report.";
    private String     bookTitle = "";
    private NBTTagList bookPages;
    
    private NextPageButton buttonNextPage;
    private NextPageButton buttonPreviousPage;
    
    private GuiButton buttonDoneReading;
    private GuiButton buttonSign;
    private GuiButton buttonFinalize;
    private GuiButton buttonCancelSign;
    private GuiButton buttonImport;
    private GuiButton buttonExport;
    private GuiButton buttonChangeDir;

    public GuiScreenBookExtra(EntityPlayer editingPlayer, ItemStack bookObj, boolean bookIsUnsigned)
    {
        this.editingPlayer  = editingPlayer;
        this.bookObj        = bookObj;
        this.bookIsUnsigned = bookIsUnsigned;
        
        if (bookObj.hasTagCompound())
        {
            NBTTagCompound bookTag = bookObj.getTagCompound();
            
            bookPages = bookTag.getTagList("pages", 8);
            
            if (bookPages != null)
            {
                bookPages      = (NBTTagList) bookPages.copy();
                bookTotalPages = bookPages.tagCount();
                
                if (bookTotalPages < 1)
                    bookTotalPages = 1;
            }
        }
        
        if (bookPages == null && bookIsUnsigned)
        {
            bookPages = new NBTTagList();
            bookPages.appendTag( new NBTTagString("") );

            bookTotalPages = 1;
        }
    }

    @Override
    public void updateScreen()
    {
        super.updateScreen();
        updateCount += 1;
        
        if (updateCount / 6 % 2 == 0)
            updateButtons();
    }

    @Override
    public void initGui()
    {
        buttonList.clear();
        Keyboard.enableRepeatEvents(true);
        
        if (bookIsUnsigned)
        {
            buttonList.add(buttonDoneReading = new GuiButton(ACTION_DONE_READING, width / 2 + 2, 4 + bookImageHeight, 98, 20, I18n.format("gui.done")));
            
            buttonList.add(buttonSign       = new GuiButton(ACTION_SIGN, width / 2 - 100, 4 + bookImageHeight, 98, 20, I18n.format("book.signButton")));
            buttonList.add(buttonCancelSign = new GuiButton(ACTION_CANCEL_SIGN, width / 2 + 2, 4 + bookImageHeight, 98, 20, I18n.format("gui.cancel")));
            buttonList.add(buttonFinalize   = new GuiButton(ACTION_FINALIZE, width / 2 - 100, 4 + bookImageHeight, 98, 20, I18n.format("book.finalizeButton")));

            // TODO: fix locale
            buttonList.add(buttonImport    = new GuiButton(ACTION_IMPORT, width / 2 - 100, 32 + bookImageHeight, 98, 20, "Import..."));
            buttonList.add(buttonExport    = new GuiButton(ACTION_EXPORT, width / 2 + 2, 32 + bookImageHeight, 98, 20, "Export..."));
            buttonList.add(buttonChangeDir = new GuiButton(ACTION_CHANGE_DIR, width / 2 - 100, 64 + bookImageHeight, 200, 20, "Set Subdirectory..."));
        }
        else
        {
            buttonList.add(buttonDoneReading = new GuiButton(0, width / 2 - 100, 4 + bookImageHeight, 200, 20, I18n.format("gui.done")));
        }
        
        int i = width / 2;
        int j = 2;

        buttonList.add(buttonNextPage = new NextPageButton(ACTION_NEXT, i - 38 - 38, j + 154, true));
        buttonList.add(buttonPreviousPage = new NextPageButton(ACTION_PREV, i - 120 - 38, j + 154, false));

        updateButtons();
    }
    

    @Override
    public void onGuiClosed()
    {
        Keyboard.enableRepeatEvents(false);
    }

    private void updateButtons()
    {
        buttonNextPage.visible     = !bookGettingSigned && ((currPage < bookTotalPages - 1) || bookIsUnsigned);
        buttonPreviousPage.visible = !bookGettingSigned && currPage > 0;

        buttonDoneReading.visible = !bookIsUnsigned || !bookGettingSigned;
        
        if (bookIsUnsigned)
        {
            buttonImport.visible     = !bookGettingSigned;
            buttonExport.visible     = !bookGettingSigned;
            buttonChangeDir.visible  = !bookGettingSigned;
            buttonSign.visible       = !bookGettingSigned;
            buttonCancelSign.visible = bookGettingSigned;
            
            buttonFinalize.visible = bookGettingSigned;
            buttonFinalize.enabled = bookGettingSigned && (updateCount > 6) && (bookTitle.trim().length() > 0);
        }
    }

    private void sendBookToServer(boolean finalizing)
    {
        if (!bookIsUnsigned || !bookModified)
            return;
        
        if (bookPages == null)
            return;
        
        // Purges 0-length pages
        while (bookPages.tagCount() > 1)
        {
            String contents = bookPages.getStringTagAt(bookPages.tagCount() - 1);
            
            if (contents.length() != 0)
                break;
            
            bookPages.removeTag(bookPages.tagCount() - 1);
        }
        
        // ???
        if (bookObj.hasTagCompound())
            bookObj.getTagCompound().setTag("pages", bookPages);
        else
            bookObj.setTagInfo("pages", bookPages);
        
        
        String payload = "MC|BEdit";
        
        if (finalizing)
        {
            payload = "MC|BSign";
            bookObj.setTagInfo("author", new NBTTagString( editingPlayer.getCommandSenderName() ));
            bookObj.setTagInfo("title",  new NBTTagString( bookTitle.trim() ));

            bookObj.func_150996_a(Items.written_book);
        }
        
        ByteBuf buffer = Unpooled.buffer();
        try
        {
            new PacketBuffer(buffer).writeItemStackToBuffer(bookObj);
            mc.getNetHandler().addToSendQueue( new C17PacketCustomPayload(payload, buffer) );
        }
        catch (Exception localException)
        {
            BookEditor.Logger.error("Couldn't send book info", localException);
        }
        finally
        {
            buffer.release();
        }
        
    }

    @Override
    protected void actionPerformed(GuiButton clicked)
    {
        if (!clicked.enabled)
            return;
        
        switch (clicked.id)
        {
            case ACTION_DONE_READING:
                mc.displayGuiScreen(null);
                sendBookToServer(false);
                break;
                
            case ACTION_NEXT:
                if (currPage < bookTotalPages - 1)
                    currPage += 1;
                else if (bookIsUnsigned)
                {
                    addNewPage();
                    if (currPage < bookTotalPages - 1)
                        currPage += 1;
                }
                break;
                
            case ACTION_PREV:
                if (currPage > 0)
                    currPage -= 1;
                break;
                
            case ACTION_SIGN:
                if (bookIsUnsigned)
                {
                    // Workaround for finalize bug
                    updateCount = 0;
                    bookGettingSigned = true;
                }
                break;
                
            case ACTION_CANCEL_SIGN:
                if (bookGettingSigned)
                    bookGettingSigned = false;
                break;
                
            case ACTION_FINALIZE:
                if (bookGettingSigned)
                {
                    sendBookToServer(true);
                    mc.displayGuiScreen(null);
                }
                break;
                
            case ACTION_IMPORT:
                if (bookIsUnsigned)
                    importBook();
                break;
                
            case ACTION_EXPORT:
                if (bookIsUnsigned)
                    exportBook();
                break;
            
            case ACTION_CHANGE_DIR:
                if (bookIsUnsigned)
                    changeDir();
                break;                
        }
        
        updateButtons();
    }

    private void addNewPage()
    {
        if (bookPages == null || bookPages.tagCount() >= 50)
            return;
        
        bookPages.appendTag( new NBTTagString("") );
        bookTotalPages += 1;

        bookModified = true;
    }

    @Override
    protected void keyTyped(char typed, int lwjglKey)
    {
        super.keyTyped(typed, lwjglKey);
        
        if (!bookIsUnsigned)
            return;
        
        if (bookGettingSigned)
            keyTypedInTitle(typed, lwjglKey);
        else
            keyTypedInBook(typed, lwjglKey);
    }

    private void keyTypedInBook(char typed, int lwjglKey) {
        switch (typed)
        {
            // Pasting
            case '\026':
                pageInsertIntoCurrent( GuiScreen.getClipboardString() );
                return;
        }
        
        switch (lwjglKey)
        {
            // Backspace
            case 14:
                String text = pageGetCurrent();
                
                if (text.length() > 0)
                    pageSetCurrent( text.substring(0, text.length() - 1) );
                
                return;
                
            // Enter
            case 28:
            case 156:
                pageInsertIntoCurrent("\n");
                return;
        }
        
        if ( ChatAllowedCharacters.isAllowedCharacter(typed) )
            pageInsertIntoCurrent( Character.toString(typed) );
    }

    private void keyTypedInTitle(char typed, int lwjglKey)
    {
        switch (typed)
        {
            // Pasting
            case '\026':
                bookTitle += GuiScreen.getClipboardString();
                
                if (bookTitle.length() >= 17)
                    bookTitle  = bookTitle.substring(0, 16);
                
                updateButtons();
                bookModified = true;
                return;
        }
        
        switch (lwjglKey)
        {
            // Backspace
            case 14:
                if ( !bookTitle.isEmpty() )
                {
                    bookTitle = bookTitle.substring(0, bookTitle.length() - 1);
                    updateButtons();
                }
                return;
                
            // Enter
            case 28:
            case 156:
                if ( !bookTitle.isEmpty() )
                {
                    sendBookToServer(true);
                    mc.displayGuiScreen(null);
                }
                return;
        }
        
        if ( bookTitle.length() < 16 && ChatAllowedCharacters.isAllowedCharacter(typed) )
        {
            bookTitle += Character.toString(typed);
            updateButtons();
            bookModified = true;
        }
    }

    private String pageGetCurrent()
    {
        if ( bookPages != null && currPage >= 0 && currPage < bookPages.tagCount() )
            return bookPages.getStringTagAt(currPage);
        
        return "";
    }

    private void pageSetCurrent(String text)
    {
        if ( bookPages != null && currPage >= 0 && currPage < bookPages.tagCount() )
        {
            bookPages.func_150304_a( currPage, new NBTTagString(text) );

            bookModified = true;
        }
    }

    private void pageInsertIntoCurrent(String append)
    {
        String currentText = pageGetCurrent();
        String newText     = currentText + append;

        int i = fontRendererObj.splitStringWidth(newText + "" + EnumChatFormatting.BLACK + "_", 118);
        
        if (i <= 118 && newText.length() < 256)
            pageSetCurrent(newText);
    }

    @Override
    public void drawScreen(int paramInt1, int paramInt2, float paramFloat)
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        
        mc.getTextureManager().bindTexture(bookGuiTextures);
        
        int i = (width / 2);
        int j = 2;
        
        drawTexturedModalRect(i - bookImageWidth, j, 0, 0, bookImageWidth, bookImageHeight);
        drawTexturedModalRect(i, j, 0, 0, bookImageWidth, bookImageHeight);
        
        if (bookGettingSigned)
        {
            String lineTitle   = bookTitle;
            String lineEdit    = I18n.format("book.editTitle");
            String lineAuthor  = I18n.format("book.byAuthor", editingPlayer.getCommandSenderName());
            String lineWarning = I18n.format("book.finalizeWarning");
            
            // Blinking type cursor
            if (bookIsUnsigned)
            {
                if (updateCount / 6 % 2 == 0)
                    lineTitle = lineTitle + "" + EnumChatFormatting.BLACK + "_";
                else
                    lineTitle = lineTitle + "" + EnumChatFormatting.GRAY + "_";
            }
            
            int k = fontRendererObj.getStringWidth(lineEdit);
            int m = fontRendererObj.getStringWidth(lineTitle);
            int n = fontRendererObj.getStringWidth(lineAuthor);
            
            fontRendererObj.drawString(lineTitle, i - bookImageWidth + 36 + (116 - m) / 2, j + 48, 0);
            fontRendererObj.drawString(lineEdit,  i - bookImageWidth + 36 + (116 - k) / 2, j + 16 + 16, 0);
            fontRendererObj.drawString(EnumChatFormatting.DARK_GRAY + lineAuthor, i - bookImageWidth + 36 + (116 - n) / 2, j + 48 + 10, 0);
            fontRendererObj.drawSplitString(lineWarning, i - bookImageWidth + 36, j + 80, 116, 0);
        }
        else
        {
            String linePage     = I18n.format( "book.pageIndicator", Integer.valueOf(currPage + 1), Integer.valueOf(bookTotalPages) );
            String linePageText = "";
            
            int k = fontRendererObj.getStringWidth(linePage);
            
            if (bookPages != null && currPage >= 0 && currPage < bookPages.tagCount())
                linePageText = bookPages.getStringTagAt(currPage);
            
            if (bookIsUnsigned)
            {
                if ( fontRendererObj.getBidiFlag() )
                    linePageText = linePageText + "_";
                else if (updateCount / 6 % 2 == 0)
                    linePageText = linePageText + "" + EnumChatFormatting.BLACK + "_";
                else
                    linePageText = linePageText + "" + EnumChatFormatting.GRAY + "_";
            }
            
            fontRendererObj.drawString(linePage, i - k - 44, j + 16, 0);
            fontRendererObj.drawSplitString(linePageText, i - bookImageWidth + 36, j + 16 + 16, 116, 0);
        }
        
        String lineEditor = "BookEditor";
        int k = fontRendererObj.getStringWidth(lineEditor);
        int l = fontRendererObj.getStringWidth(infoLine);
        fontRendererObj.drawString(lineEditor, i + bookImageWidth - k - 44, j + 16, 0);
        fontRendererObj.drawSplitString(infoLine, i + 36, j + 16 + 16, 116, 0);
        
        super.drawScreen(paramInt1, paramInt2, paramFloat);
    }

    private void exportBook()
    {
        try
        {
            for (int i = 0; i < bookTotalPages; i++)
            {
                String fileName = String.format("Books%d.txt", i);
                
                File exportFile;
                
                if (BookEditor.SubDir == null)
                    exportFile = new File(BookEditor.BaseDir, fileName);
                else
                    exportFile = new File(BookEditor.SubDir, fileName);
                
                FileOutputStream   stream   = new FileOutputStream(exportFile);
                OutputStreamWriter output = new OutputStreamWriter(stream, Charset.forName("UTF-8").newEncoder());

                output.write( bookPages.getStringTagAt(i) );
                output.close();
                stream.close();
            }
            
            infoLine = String.format( "Successfully wrote %d files (%s)", bookTotalPages, new Date() );            
        }
        catch (Exception ex)
        {
            infoLine = EnumChatFormatting.RED + "Could not export book:\n" + ex.getMessage();
        }
    }
    
    private void importBook()
    {
        try
        {
            int i;
            
            NBTTagList importedPages = new NBTTagList();
            
            for (i = 0; i < 50; i++)
            {
                String fileName = String.format("Books%d.txt", i);
                File importFile;
                
                if (BookEditor.SubDir == null)
                    importFile = new File(BookEditor.BaseDir, fileName);
                else
                    importFile = new File(BookEditor.SubDir, fileName);
                
                if ( !importFile.exists() )
                    break;
                
                Scanner reader = new Scanner(importFile, "UTF-8");
                String  page   = reader.useDelimiter("\\A").next().replace("\r", "");
                
                if (page.length() >= 256)
                {
                    infoLine = EnumChatFormatting.RED + String.format("Page %d has too many characters (limit 255)", i);
                    reader.close();
                    return;            
                }
                
                importedPages.appendTag( new NBTTagString(page) );

                reader.close();
            }
            
            if (i == 0)
            {
                infoLine = EnumChatFormatting.GOLD + "No pages were found";
                return;
            }
            
            bookPages      = importedPages;
            bookTotalPages = i;
            currPage       = 0;
            
            sendBookToServer(false);
            infoLine = String.format( "Successfully imported %d (out of 50 max) pages (%s)", i, new Date() );
        }
        catch (Exception ex)
        {
            infoLine = EnumChatFormatting.RED + "Could not import book:\n" + ex.getMessage();
        }
    }

    private void changeDir()
    {
        String subdir;
        
        if (BookEditor.SubDir == null)
            subdir = "";
        else
            subdir = BookEditor.SubDir.getName();
        
        GuiScreenPrompt prompt = new GuiScreenPrompt(this, "Define book subdirectory", subdir, new GuiScreenPrompt.GuiPromptCallback()
        {
            @Override
            public void onConfirm(String data)
            {
                if ("".equals(data))
                {
                    BookEditor.SubDir = null;
                    infoLine = String.format( "Cleared subdirectory" );
                    return;
                }
                
                BookEditor.SubDir = new File(BookEditor.BaseDir, data);
                
                if ( !BookEditor.SubDir.exists() )
                    BookEditor.SubDir.mkdir();
                
                infoLine = String.format( "Changed subdir to '%s'", data );
            }

            @Override
            public void onCancel()
            {
            }
        });
        
        mc.displayGuiScreen(prompt);
    }
    
            
    @SideOnly(Side.CLIENT)
    static class NextPageButton extends GuiButton
    {
        private final boolean isNext;

        public NextPageButton(int id, int posX, int posZ, boolean isNext)
        {
            super(id, posX, posZ, 23, 13, "");
            this.isNext = isNext;
        }

        @Override
        public void drawButton(Minecraft mc, int posX, int posZ)
        {
            if (!this.visible)
                return;
            
            int i = (posX >= this.xPosition) && (posZ >= this.yPosition) && (posX < this.xPosition + this.width) && (posZ < this.yPosition + this.height) ? 1 : 0;

            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(GuiScreenBookExtra.bookGuiTextures);

            int j = 0;
            int k = 192;
            
            if (i != 0)
                j += 23;
            
            if (!this.isNext)
                k += 13;
            
            drawTexturedModalRect(this.xPosition, this.yPosition, j, k, 23, 13);
        }
    }
}
