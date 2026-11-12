package mcjty.lostcities.client;

import com.google.gson.*;
import cpw.mods.fml.relauncher.*;
import mcjty.lostcities.config.*;
import net.minecraft.client.gui.*;
import java.util.*;

/** Native world-creation entry point; profile files remain the source of advanced generation settings. */
@SideOnly(Side.CLIENT)
public final class CityProfileScreen extends GuiScreen {
    private final GuiCreateWorld parent;
    private final List<LostCityProfile> profiles=new ArrayList<>();
    private final JsonObject options;
    private int selected;
    private GuiButton profileButton;
    public CityProfileScreen(GuiCreateWorld parent){
        this.parent=parent;JsonObject parsed;
        try{parsed=parent.field_146334_a.trim().isEmpty()?new JsonObject():new JsonParser().parse(parent.field_146334_a).getAsJsonObject();}catch(RuntimeException invalid){parsed=new JsonObject();}
        options=parsed;String name=options.has("profile")?options.get("profile").getAsString():LostCityConfiguration.DEFAULT_PROFILE;
        for(LostCityProfile profile:LostCityConfiguration.profiles.values())if(profile.isPublic())profiles.add(profile);
        profiles.sort(Comparator.comparing(LostCityProfile::getName));
        if(profiles.isEmpty())throw new IllegalStateException("At least one public Lost Cities profile is required");
        for(int i=0;i<profiles.size();i++)if(profiles.get(i).getName().equals(name))selected=i;
    }
    @Override public void initGui(){
        buttonList.clear();int y=height/2-35;
        buttonList.add(new GuiButton(2,width/2-125,y,20,20,"<"));
        buttonList.add(profileButton=new GuiButton(1,width/2-100,y,200,20,""));
        buttonList.add(new GuiButton(3,width/2+105,y,20,20,">"));
        buttonList.add(new GuiButton(0,width/2-100,height-30,200,20,"Done"));updateLabel();
    }
    private void updateLabel(){profileButton.displayString="Profile: "+profiles.get(selected).getName();}
    @Override protected void actionPerformed(GuiButton button){
        if(button.id==0){options.addProperty("profile",profiles.get(selected).getName());parent.field_146334_a=options.toString();mc.displayGuiScreen(parent);}
        else {selected=Math.floorMod(selected+(button.id==2?-1:1),profiles.size());updateLabel();}
    }
    @Override protected void keyTyped(char character,int key){
        if(key==1){mc.displayGuiScreen(parent);return;}
        if(key==203||key==205){selected=Math.floorMod(selected+(key==203?-1:1),profiles.size());updateLabel();}
    }
    @Override public void drawScreen(int mouseX,int mouseY,float partial){
        drawDefaultBackground();drawCenteredString(fontRendererObj,"Lost Cities",width/2,20,0xffffff);
        LostCityProfile profile=profiles.get(selected);int y=height/2;
        fontRendererObj.drawSplitString(profile.getDescription(),width/2-145,y,290,0xdddddd);
        fontRendererObj.drawSplitString(profile.getExtraDescription(),width/2-145,y+30,290,0xaaaaaa);
        super.drawScreen(mouseX,mouseY,partial);
    }
}
