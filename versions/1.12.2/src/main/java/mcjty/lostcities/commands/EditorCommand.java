package mcjty.lostcities.commands;

import com.google.gson.*;
import mcjty.lostcities.assets.AssetExporter;
import mcjty.lostcities.dimensions.world.*;
import mcjty.lostcities.dimensions.world.lost.BuildingInfo;
import mcjty.lostcities.dimensions.world.lost.cityassets.*;
import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Native operator editor. All asset reads and placements use the actual sender world's generation scope. */
public class EditorCommand extends CommandBase {
    private final String operation;
    public EditorCommand(String operation){this.operation=operation;}
    
    @Override public String getName(){return operation;}
    @Override public String getUsage(ICommandSender sender){return operation+(operation.endsWith("buildpart")?" <part>":operation.endsWith("exportpart")?" <file> <slices>":operation.endsWith("exportbuilding")?" <file> <floors>":operation.equals("lostcities")?" debug":"");}
    @Override public int getRequiredPermissionLevel(){return 2;}
    @Override public void execute(net.minecraft.server.MinecraftServer server,ICommandSender sender,String[] args) throws CommandException {
        LostCityChunkGenerator generator=WorldTypeTools.getLostCityChunkGenerator(sender.getEntityWorld());
        if(generator==null)throw new CommandException("This dimension has no Lost Cities generator");
        try{generator.contextual(()->{perform(generator,sender,args);return null;});}
        catch(RuntimeException error){mcjty.lostcities.LostCities.setup.getLogger().error("Lost Cities editor command failed",error);throw new CommandException(error.getMessage()==null?"Lost Cities editor command failed":error.getMessage());}
    }
    private void perform(LostCityChunkGenerator generator,ICommandSender sender,String[] args){
        net.minecraft.util.math.BlockPos position=sender.getPosition();int px=position.getX(),py=position.getY(),pz=position.getZ();
        int cx=px>>4,cz=pz>>4;
        if(operation.equals("lc_debug")||operation.equals("lostcities")){
            if(args.length!=(operation.equals("lostcities")?1:0)||operation.equals("lostcities")&&!args[0].equals("debug"))throw new IllegalArgumentException(getUsage(sender));
            BuildingInfo info=BuildingInfo.getBuildingInfo(cx,cz,generator);message(sender,"Profile "+generator.getProfile().getName()+", chunk "+cx+","+cz+", city="+info.isCity()+", building="+info.getBuildingType()+", level="+info.getCityLevel()+", highway="+info.getHighwayXLevel()+"/"+info.getHighwayZLevel());return;
        }
        if(!(sender instanceof EntityPlayer))throw new IllegalArgumentException("This editor command requires a player position");
        if(operation.equals("lc_buildpart")){
            if(args.length!=1)throw new IllegalArgumentException(getUsage(sender));
            BuildingPart part=AssetRegistries.PARTS.get(args[0]);if(part==null)throw new IllegalArgumentException("Unknown building part "+args[0]);
            generator.terrainGenerator.placeEditorPart(part,cx,cz,py);message(sender,"Placed "+part.getName()+" in chunk "+cx+","+cz);return;
        }
        if(args.length!=2)throw new IllegalArgumentException(getUsage(sender));
        int count;try{count=Integer.parseInt(args[1]);}catch(NumberFormatException error){throw new IllegalArgumentException("Export size must be an integer");}
        boolean building=operation.equals("lc_exportbuilding");int height;
        try{height=Math.multiplyExact(count,building?6:1);}catch(ArithmeticException error){throw new IllegalArgumentException("Export exceeds the native height limit");}
        int y0=py-1;if(count<1||height>256||y0<0||y0+height>256)throw new IllegalArgumentException("Export must fit within Y 0–255");
        File file=new File(args[0]);String name=file.getName().replaceFirst("\\.json$","").toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_.-]","_");if(name.isEmpty())name="exported";
        World world=sender.getEntityWorld();JsonArray assets=AssetExporter.export(name,height,building,(x,y,z)->{
            int wx=cx*16+x,wy=y0+y,wz=cz*16+z;JsonObject entry=new JsonObject();
            net.minecraft.util.math.BlockPos pos=new net.minecraft.util.math.BlockPos(wx,wy,wz);net.minecraft.block.state.IBlockState state=world.getBlockState(pos);net.minecraft.block.Block block=state.getBlock();int metadata=block.getMetaFromState(state);String id=String.valueOf(block.getRegistryName());
            entry.addProperty("block",id.equals("minecraft:air")?"minecraft:structure_void":id+"@"+metadata);
            TileEntity tile=world.getTileEntity(pos);
            if(tile!=null){NBTTagCompound tag=new NBTTagCompound();tile.writeToNBT(tag);tag.removeTag("x");tag.removeTag("y");tag.removeTag("z");JsonObject nbt=new JsonObject();nbt.addProperty("$snbt",tag.toString());entry.add("tag",nbt);}return entry;
        });
        try(Writer writer=Files.newBufferedWriter(file.toPath(),StandardCharsets.UTF_8)){new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(assets,writer);}
        catch(IOException error){throw new java.io.UncheckedIOException("Cannot write export "+file,error);}
        message(sender,"Exported "+height+" layers from dimension "+generator.dimensionId+" to "+file);
    }
    private void message(ICommandSender sender,String text){sender.sendMessage(new net.minecraft.util.text.TextComponentString(text));}
}
