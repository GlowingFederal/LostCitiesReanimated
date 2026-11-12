package mcjty.lostcities.legacy;

import com.google.gson.*;
import mcjty.lostcities.LostCities;
import mcjty.lostcities.compat.NativeLootAssets;
import net.minecraft.entity.EntityList;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.*;
import net.minecraft.init.Items;
import net.minecraft.enchantment.*;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.World;
import net.minecraftforge.common.ChestGenHooks;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Native inventories, item functions, enchantment APIs and weighted Forge chest hooks. */
public final class LegacyLoot {
    private static final Map<String,JsonObject> TABLES=new HashMap<>();
    private static final Set<String> MISSING_TABLES=new HashSet<>();
    public static String entity(String name){
        if(EntityList.stringToClassMapping.containsKey(name))return name;
        String path=name.substring(name.indexOf(':')+1);String[] ids={"Zombie","Skeleton","Spider","CaveSpider","Blaze","Witch","Creeper","Enderman","Silverfish","PigZombie"};
        for(String id:ids)if(id.replace("_","").equalsIgnoreCase(path.replace("_","")))return id;
        throw new IllegalArgumentException("Unknown 1.7.10 spawner entity "+name);
    }

    public static void fill(IInventory inventory,String table,Random random){fill(inventory,table,random,inventory instanceof TileEntity?((TileEntity)inventory).getWorldObj():null);}
    public static void fill(IInventory inventory,String table,Random random,World world){
        table=qualified(table);if(table.equals("minecraft:empty"))return;
        if(table.startsWith("minecraft:")&&definition(world,table)==null){vanilla(inventory,table,random);return;}
        List<Integer> slots=new ArrayList<>();for(int i=0;i<inventory.getSizeInventory();i++)if(inventory.getStackInSlot(i)==null)slots.add(i);Collections.shuffle(slots,random);
        fill(inventory,qualified(table),random,world,slots,new HashSet<String>());inventory.markDirty();
    }
    private static String qualified(String id){return id.indexOf(':')<0?"minecraft:"+id:id;}
    private static JsonObject definition(World world,String table){
        JsonObject custom=NativeLootAssets.definition(world,table);if(custom!=null)return custom;
        JsonObject definition=TABLES.get(table);if(definition!=null)return definition;
        if(MISSING_TABLES.contains(table))return null;
        String[] id=table.split(":",2);
        try(InputStream stream=LostCities.class.getResourceAsStream("/assets/"+id[0]+"/loot_tables/"+id[1]+".json")){
            if(stream==null){MISSING_TABLES.add(table);return null;}definition=new JsonParser().parse(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();validate(table,definition);TABLES.put(table,definition);return definition;
        }catch(IOException error){throw new UncheckedIOException(error);}
    }
    private static void fill(IInventory inventory,String table,Random random,World world,List<Integer> slots,Set<String> active){
        if(table.equals("minecraft:empty"))return;
        if(!active.add(table))throw new IllegalArgumentException("Cyclic legacy loot table "+table);
        try{
            JsonObject definition=definition(world,table);
            if(definition==null){if(table.startsWith("minecraft:")){vanilla(inventory,table,random);slots.removeIf(slot->inventory.getStackInSlot(slot)!=null);return;}throw new IllegalArgumentException("Missing legacy loot table "+table);}
            for(JsonElement poolElement:definition.getAsJsonArray("pools")){
                JsonObject pool=poolElement.getAsJsonObject();if(!conditions(pool,random))continue;int rolls=Math.max(0,integer(pool.get("rolls"),random));
                for(int roll=0;roll<rolls&&!slots.isEmpty();roll++){
                    List<JsonObject> entries=new ArrayList<>();int total=0;
                    for(JsonElement value:pool.getAsJsonArray("entries")){JsonObject entry=value.getAsJsonObject();if(conditions(entry,random)){entries.add(entry);total=Math.addExact(total,entry.has("weight")?entry.get("weight").getAsInt():1);}}
                    if(total==0)continue;int choice=random.nextInt(total);JsonObject selected=null;
                    for(JsonObject entry:entries){choice-=entry.has("weight")?entry.get("weight").getAsInt():1;if(choice<0){selected=entry;break;}}
                    String type=selected.get("type").getAsString();if(type.equals("empty"))continue;
                    if(type.equals("loot_table")){fill(inventory,qualified(selected.get("name").getAsString()),random,world,slots,active);continue;}
                    ItemStack stack=new ItemStack(item(selected.get("name").getAsString()),1,0);
                    if(selected.has("functions"))for(JsonElement function:selected.getAsJsonArray("functions"))if(conditions(function.getAsJsonObject(),random))stack=function(stack,function.getAsJsonObject(),random);
                    int remaining=Math.max(0,stack.stackSize);while(remaining>0&&!slots.isEmpty()){ItemStack part=stack.copy();part.stackSize=Math.min(remaining,part.getMaxStackSize());remaining-=part.stackSize;inventory.setInventorySlotContents(slots.remove(slots.size()-1),part);}
                }
            }
        }finally{active.remove(table);}
    }
    private static void vanilla(IInventory inventory,String table,Random random){
            String resolved=mcjty.lostcities.materials.LootTableAliases.for112(table),hook;
            switch(resolved){
                case "minecraft:chests/abandoned_mineshaft":hook=ChestGenHooks.MINESHAFT_CORRIDOR;break;
                case "minecraft:chests/village_blacksmith":hook=ChestGenHooks.VILLAGE_BLACKSMITH;break;
                case "minecraft:chests/desert_pyramid":hook=ChestGenHooks.PYRAMID_DESERT_CHEST;break;
                case "minecraft:chests/jungle_temple":hook=ChestGenHooks.PYRAMID_JUNGLE_CHEST;break;
                case "minecraft:chests/jungle_temple_dispenser":hook=ChestGenHooks.PYRAMID_JUNGLE_DISPENSER;break;
                case "minecraft:chests/stronghold_corridor":case "minecraft:chests/nether_bridge":hook=ChestGenHooks.STRONGHOLD_CORRIDOR;break;
                case "minecraft:chests/stronghold_library":hook=ChestGenHooks.STRONGHOLD_LIBRARY;break;
                case "minecraft:chests/stronghold_crossing":case "minecraft:chests/end_city_treasure":hook=ChestGenHooks.STRONGHOLD_CROSSING;break;
                case "minecraft:chests/spawn_bonus_chest":hook=ChestGenHooks.BONUS_CHEST;break;
                default:hook=ChestGenHooks.DUNGEON_CHEST;
            }
            WeightedRandomChestContent.generateChestContents(random,ChestGenHooks.getItems(hook,random),inventory,ChestGenHooks.getCount(hook,random));inventory.markDirty();return;
    }
    public static void validate(String id,JsonObject definition){
        try{
            validateConditions(definition);
            for(JsonElement value:definition.getAsJsonArray("pools")){
                JsonObject pool=value.getAsJsonObject();validateConditions(pool);range(pool.get("rolls"));if(pool.has("bonus_rolls"))range(pool.get("bonus_rolls"));long total=0;
                for(JsonElement entryValue:pool.getAsJsonArray("entries")){
                    JsonObject entry=entryValue.getAsJsonObject();validateConditions(entry);String type=entry.get("type").getAsString();
                    if(!type.equals("item")&&!type.equals("empty")&&!type.equals("loot_table"))throw new IllegalArgumentException("Unsupported entry "+type);
                    int weight=entry.has("weight")?entry.get("weight").getAsInt():1;if(weight<0)throw new IllegalArgumentException("Negative loot weight");total+=weight;
                    if(type.equals("item"))item(entry.get("name").getAsString());
                    if(entry.has("functions"))for(JsonElement operation:entry.getAsJsonArray("functions"))validateFunction(operation.getAsJsonObject());
                }
                if(total>Integer.MAX_VALUE)throw new IllegalArgumentException("Loot weights exceed the native integer range");
            }
        }catch(RuntimeException error){throw new IllegalArgumentException("Invalid 1.7.10 loot table "+id+": "+error.getMessage(),error);}
    }
    private static Item item(String id){Object value=Item.itemRegistry.getObject(id);if(!(value instanceof Item))throw new IllegalArgumentException("Unavailable loot item "+id);return (Item)value;}
    private static double[] range(JsonElement value){
        double min,max;if(value.isJsonPrimitive())min=max=value.getAsDouble();else{JsonObject object=value.getAsJsonObject();min=object.get("min").getAsDouble();max=object.get("max").getAsDouble();}
        if(!Double.isFinite(min)||!Double.isFinite(max)||max<min||min<Integer.MIN_VALUE||max>Integer.MAX_VALUE)throw new IllegalArgumentException("Invalid loot number range");return new double[]{min,max};
    }
    private static int integer(JsonElement value,Random random){double[] range=range(value);int min=(int)Math.floor(range[0]),max=(int)Math.floor(range[1]);return min==max?min:min+random.nextInt(Math.addExact(Math.subtractExact(max,min),1));}
    private static float decimal(JsonElement value,Random random){double[] range=range(value);return (float)(range[0]+(range[1]-range[0])*random.nextFloat());}
    private static void validateConditions(JsonObject object){if(object.has("conditions"))for(JsonElement value:object.getAsJsonArray("conditions"))validateCondition(value.getAsJsonObject());}
    private static void validateCondition(JsonObject condition){
        String type=condition.get("condition").getAsString();
        if(type.equals("random_chance")||type.equals("random_chance_with_looting")){double chance=condition.get("chance").getAsDouble();if(!Double.isFinite(chance)||chance<0||chance>1)throw new IllegalArgumentException("Invalid random chance");}
        else if(type.equals("inverted"))validateCondition(condition.getAsJsonObject("term"));
        else if(type.equals("alternative")){for(JsonElement term:condition.getAsJsonArray("terms"))validateCondition(term.getAsJsonObject());}
        else if(!type.equals("killed_by_player"))throw new IllegalArgumentException("Unsupported condition "+type);
    }
    private static boolean conditions(JsonObject object,Random random){if(object.has("conditions"))for(JsonElement value:object.getAsJsonArray("conditions"))if(!condition(value.getAsJsonObject(),random))return false;return true;}
    private static boolean condition(JsonObject condition,Random random){
        String type=condition.get("condition").getAsString();if(type.equals("random_chance")||type.equals("random_chance_with_looting"))return random.nextFloat()<condition.get("chance").getAsFloat();
        if(type.equals("inverted"))return !condition(condition.getAsJsonObject("term"),random);
        if(type.equals("alternative")){for(JsonElement term:condition.getAsJsonArray("terms"))if(condition(term.getAsJsonObject(),random))return true;return false;}
        return false; // A generated chest has no killing player or looting enchantment.
    }
    private static void validateFunction(JsonObject function){
        validateConditions(function);String type=function.get("function").getAsString();
        switch(type){
            case "set_count":range(function.get("count"));break;case "set_data":range(function.get("data"));break;case "set_damage":range(function.get("damage"));break;
            case "set_nbt":tag(function.get("tag").getAsString());break;
            case "enchant_with_levels":range(function.get("levels"));if(function.has("treasure")&&function.get("treasure").getAsBoolean())throw new IllegalArgumentException("Treasure enchantments require a newer enchanting engine");break;
            case "enchant_randomly":if(function.has("enchantments"))for(JsonElement enchantment:function.getAsJsonArray("enchantments"))enchantment(enchantment.getAsString());break;
            default:throw new IllegalArgumentException("Unsupported function "+type);
        }
    }
    private static NBTTagCompound tag(String value){try{NBTBase tag=JsonToNBT.func_150315_a(value);if(!(tag instanceof NBTTagCompound))throw new IllegalArgumentException("Loot NBT must be a compound");return (NBTTagCompound)tag;}catch(NBTException error){throw new IllegalArgumentException("Invalid loot NBT",error);}}
    private static ItemStack function(ItemStack stack,JsonObject function,Random random){
        switch(function.get("function").getAsString()){
            case "set_count":stack.stackSize=Math.max(0,integer(function.get("count"),random));break;
            case "set_data":stack.setItemDamage(integer(function.get("data"),random));break;
            case "set_damage":if(stack.isItemStackDamageable())stack.setItemDamage((int)((1-Math.max(0,Math.min(1,decimal(function.get("damage"),random))))*stack.getMaxDamage()));break;
            case "set_nbt":{NBTTagCompound merged=stack.hasTagCompound()?stack.getTagCompound():new NBTTagCompound();merge(merged,tag(function.get("tag").getAsString()));stack.setTagCompound(merged);break;}
            case "enchant_with_levels":stack=EnchantmentHelper.addRandomEnchantment(random,stack,integer(function.get("levels"),random));break;
            case "enchant_randomly":{List<Enchantment> choices=new ArrayList<>();boolean explicit=function.has("enchantments")&&function.getAsJsonArray("enchantments").size()>0;if(explicit){for(JsonElement name:function.getAsJsonArray("enchantments"))choices.add(enchantment(name.getAsString()));}else for(Enchantment enchantment:Enchantment.enchantmentsList)if(enchantment!=null&&(stack.getItem()==Items.book||enchantment.canApply(stack)))choices.add(enchantment);if(!choices.isEmpty()){Enchantment selected=choices.get(random.nextInt(choices.size()));int level=selected.getMinLevel()+random.nextInt(selected.getMaxLevel()-selected.getMinLevel()+1);if(stack.getItem()==Items.book){stack=new ItemStack(Items.enchanted_book);Items.enchanted_book.addEnchantment(stack,new EnchantmentData(selected,level));}else stack.addEnchantment(selected,level);}break;}
        }return stack;
    }
    private static void merge(NBTTagCompound target,NBTTagCompound extra){for(Object value:extra.func_150296_c()){String key=(String)value;NBTBase incoming=extra.getTag(key);if(incoming instanceof NBTTagCompound&&target.hasKey(key,10))merge(target.getCompoundTag(key),(NBTTagCompound)incoming);else target.setTag(key,incoming.copy());}}
    private static Enchantment enchantment(String id){
        String path=id.substring(id.indexOf(':')+1),nativeName=path;
        String[][] names={{"protection","protect.all"},{"fire_protection","protect.fire"},{"feather_falling","protect.fall"},{"blast_protection","protect.explosion"},{"projectile_protection","protect.projectile"},{"respiration","oxygen"},{"aqua_affinity","waterWorker"},{"sharpness","damage.all"},{"smite","damage.undead"},{"bane_of_arthropods","damage.arthropods"},{"fire_aspect","fire"},{"looting","lootBonus"},{"efficiency","digging"},{"silk_touch","untouching"},{"unbreaking","durability"},{"fortune","lootBonusDigger"},{"power","arrowDamage"},{"punch","arrowKnockback"},{"flame","arrowFire"},{"infinity","arrowInfinite"},{"luck_of_the_sea","lootBonusFishing"},{"lure","fishingSpeed"}};
        if(id.indexOf(':')<0||id.startsWith("minecraft:"))for(String[] pair:names)if(pair[0].equals(path)){nativeName=pair[1];break;}
        for(Enchantment enchantment:Enchantment.enchantmentsList)if(enchantment!=null&&(enchantment.getName().equals("enchantment."+nativeName)||enchantment.getName().equals(id)))return enchantment;
        throw new IllegalArgumentException("Unavailable 1.7.10 loot enchantment "+id);
    }
    private LegacyLoot(){}
}
