package mcjty.lostcities.compat;

import com.google.gson.*;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import java.util.*;

/** Applies template data to the actual surviving native tile entity during population. */
public final class BlockEntityTags {
    public static void apply(TileEntity tile,JsonObject template){
        NBTTagCompound data=new NBTTagCompound();tile.writeToNBT(data);
        if(template.has("$snbt")){try{NBTBase parsed=JsonToNBT.func_150315_a(template.get("$snbt").getAsString());if(!(parsed instanceof NBTTagCompound))throw new IllegalArgumentException("Block entity NBT must be a compound");NBTTagCompound compound=(NBTTagCompound)parsed;for(String key:keys(compound))if(!key.equals("id")&&!key.equals("x")&&!key.equals("y")&&!key.equals("z"))data.setTag(key,compound.getTag(key).copy());tile.readFromNBT(data);tile.markDirty();return;}catch(NBTException error){throw new IllegalArgumentException("Invalid exported block entity NBT",error);}}
        for(Map.Entry<String,JsonElement> entry:template.entrySet()){
            String key=entry.getKey();if(key.equals("id")||key.equals("x")||key.equals("y")||key.equals("z"))continue;
            data.setTag(key,convert(entry.getValue(),key,false));
        }
        tile.readFromNBT(data);tile.markDirty();
    }
    private static NBTBase convert(JsonElement value,String key,boolean inventoryItem){
        if(value.isJsonObject()){
            NBTTagCompound tag=new NBTTagCompound();for(Map.Entry<String,JsonElement> entry:value.getAsJsonObject().entrySet())tag.setTag(entry.getKey(),convert(entry.getValue(),entry.getKey(),inventoryItem&&entry.getKey().equals("id")));return tag;
        }
        if(value.isJsonArray()){
            NBTTagList list=new NBTTagList();for(JsonElement entry:value.getAsJsonArray())list.appendTag(convert(entry,key,key.equals("Items")));return list;
        }
        JsonPrimitive primitive=value.getAsJsonPrimitive();
        if(primitive.isBoolean())return new NBTTagByte((byte)(primitive.getAsBoolean()?1:0));
        if(primitive.isNumber()){
            if(key.equals("Count")||key.equals("Slot"))return new NBTTagByte(primitive.getAsByte());
            if(key.equals("Damage"))return new NBTTagShort(primitive.getAsShort());
            String number=primitive.getAsString();if(number.contains(".")||number.contains("e")||number.contains("E"))return new NBTTagDouble(primitive.getAsDouble());
            long numberValue=primitive.getAsLong();return numberValue>Integer.MAX_VALUE||numberValue<Integer.MIN_VALUE?new NBTTagLong(numberValue):new NBTTagInt((int)numberValue);
        }
        if(inventoryItem&&key.equals("id")&&primitive.getAsString().contains(":")){
            net.minecraft.item.Item item=(net.minecraft.item.Item)net.minecraft.item.Item.itemRegistry.getObject(primitive.getAsString());
            if(item==null)throw new IllegalArgumentException("Missing template inventory item "+primitive.getAsString());
            return new NBTTagShort((short)net.minecraft.item.Item.getIdFromItem(item));
        }
        return new NBTTagString(primitive.getAsString());
    }
    private static java.util.Set<String> keys(NBTTagCompound tag){java.util.Set<String> keys=new java.util.HashSet<>();for(Object key:tag.func_150296_c())keys.add((String)key);return keys;}
    private BlockEntityTags(){}
}
