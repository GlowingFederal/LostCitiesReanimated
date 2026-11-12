package mcjty.lostcities.dimensions.world.lost.cityassets;

import mcjty.lostcities.dimensions.world.terraingen.LostCitiesTerrainGenerator;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * More efficient representation of a palette useful for a single chunk
 */
public class CompiledPalette {

    private final Map<Character, Object> palette = new HashMap<>();
    private final Map<Character, Character> damagedToBlock = new HashMap<>();
    private final Map<Character, Info> information = new HashMap<>();
    private final Map<Character,Info> explicitInformation=new HashMap<>();
    private final Map<Character,Character> aliases=new HashMap<>();

    public static class Info {
        private com.google.gson.JsonObject tag;
        public com.google.gson.JsonObject getTag(){return tag;}
        private boolean singleContainer;
        public boolean isSingleContainer(){return singleContainer;}
        private String mobId;
        private String loot;
        private Map<String, Integer> torchOrientations;

        public Info(String mobId, String loot, Map<String, Integer> torchOrientations) {
            this.mobId = mobId;
            this.loot = loot;
            this.torchOrientations = torchOrientations;
        }

        public String getMobId() {
            return mobId;
        }

        public String getLoot() {
            return loot;
        }

        public Map<String, Integer> getTorchOrientations() {
            return torchOrientations;
        }
    }


    public CompiledPalette(CompiledPalette other, Palette... palettes) {
        this.palette.putAll(other.palette);
        this.damagedToBlock.putAll(other.damagedToBlock);
        this.information.putAll(other.information);
        this.explicitInformation.putAll(other.explicitInformation);
        this.aliases.putAll(other.aliases);
        addPalettes(palettes);
    }

    public CompiledPalette(Palette... palettes) {
        addPalettes(palettes);
    }

    private int addEntries(char[] randomBlocks, int idx, char c, int cnt) {
        for (int i = 0 ; i < cnt ; i++) {
            if (idx >= randomBlocks.length) {
                return idx;
            }
            randomBlocks[idx++] = c;
        }
        return idx;
    }

    private static Info copyInfo(Info source){
        Info result=new Info(source==null?null:source.mobId,source==null?null:source.loot,source==null?null:source.torchOrientations);
        if(source!=null){result.tag=source.tag;result.singleContainer=source.singleContainer;}return result;
    }
    private static boolean emptyInfo(Info info){return info.mobId==null&&info.loot==null&&info.torchOrientations==null&&info.tag==null&&!info.singleContainer;}
    public void addPalettes(Palette[] layers) {
        for(Palette layer:layers){
            for(Map.Entry<Character,Object> entry:layer.palette.entrySet()){
                Character character=entry.getKey();Object raw=entry.getValue();
                boolean inherited=raw instanceof String&&raw.equals(String.valueOf(character))&&(palette.containsKey(character)||aliases.containsKey(character));
                if(raw instanceof String){
                    String reference=(String)raw;if(reference.length()!=1)throw new IllegalArgumentException("Palette reference must be one character: "+reference);
                    if(!inherited)aliases.put(character,reference.charAt(0));
                }else{
                    aliases.remove(character);Object value=raw instanceof Palette.Material?((Palette.Material)raw).resolve():raw;
                    if(value instanceof IBlockState)palette.put(character,(char)Block.BLOCK_STATE_IDS.get((IBlockState)value));
                    else if(value instanceof Pair[]){
                        char[] choices=new char[128];int count=0;
                        for(Pair<Integer,?> choice:(Pair<Integer,?>[])value){
                            if(choice.getLeft()<0)throw new IllegalArgumentException("Negative palette weight for "+character);
                            Object material=choice.getRight();IBlockState selected=material instanceof Palette.Material?((Palette.Material)material).resolve():(IBlockState)material;
                            count=addEntries(choices,count,(char)Block.BLOCK_STATE_IDS.get(selected),choice.getLeft());if(count==128)break;
                        }
                        if(count==0)throw new IllegalArgumentException("Empty weighted palette for "+character);
                        while(count<128){choices[count]=choices[count-1];count++;}palette.put(character,choices);
                    }else{if(value==null)throw new IllegalArgumentException("Missing palette material for "+character);palette.put(character,value);}
                }
                Info info=inherited?copyInfo(explicitInformation.get(character)):new Info(null,null,null);
                if(layer.getMobIds().containsKey(character))info.mobId=layer.getMobIds().get(character);
                if(layer.getLootTables().containsKey(character))info.loot=layer.getLootTables().get(character);
                if(layer.getTorchOrientations().containsKey(character))info.torchOrientations=layer.getTorchOrientations().get(character);
                if(layer.tags.containsKey(character))info.tag=layer.tags.get(character);
                if(raw instanceof Palette.Material)info.singleContainer=mcjty.lostcities.materials.BlockSpec.parse(((Palette.Material)raw).source).id.equals("minecraft:barrel");
                if(emptyInfo(info))explicitInformation.remove(character);else explicitInformation.put(character,info);
            }
            for(Map.Entry<String,String> entry:layer.rawDamage.entrySet())damagedToBlock.put((char)Block.BLOCK_STATE_IDS.get(mcjty.lostcities.varia.Tools.stringToState(entry.getKey())),(char)Block.BLOCK_STATE_IDS.get(mcjty.lostcities.varia.Tools.stringToState(entry.getValue())));
            for(Map.Entry<IBlockState,IBlockState> entry:layer.getDamaged().entrySet())damagedToBlock.put((char)Block.BLOCK_STATE_IDS.get(entry.getKey()),(char)Block.BLOCK_STATE_IDS.get(entry.getValue()));
        }
        java.util.List<Character> ordered=mcjty.lostcities.assets.PaletteAliases.resolve(aliases,palette);
        information.clear();for(Map.Entry<Character,Info> entry:explicitInformation.entrySet())information.put(entry.getKey(),copyInfo(entry.getValue()));
        for(Character character:ordered){
            Info parent=information.get(aliases.get(character));if(parent==null)continue;Info info=copyInfo(information.get(character));
            if(info.mobId==null)info.mobId=parent.mobId;if(info.loot==null)info.loot=parent.loot;if(info.torchOrientations==null)info.torchOrientations=parent.torchOrientations;if(info.tag==null)info.tag=parent.tag;info.singleContainer|=parent.singleContainer;
            information.put(character,info);
        }
    }

    public Set<Character> getCharacters() {
        return palette.keySet();
    }

    public IBlockState getStraight(char c) {
        try {
            Object o = palette.get(c);
            if (o instanceof IBlockState) {
                return (IBlockState) o;
            } else if (o instanceof Character) {
                return Block.BLOCK_STATE_IDS.getByValue((Character) o);
            } else {
                char[] randomBlocks = (char[]) o;
                return Block.BLOCK_STATE_IDS.getByValue(randomBlocks[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Return true if this is a simple character that can have only one value in the palette
     */
    public boolean isSimple(char c) {
        Object o = palette.get(c);
        return o instanceof Character;
    }

    // Same as get(c) but with a predefined random generator that is predictable
    public Character get(char c, Random rand) {
        try {
            Object o = palette.get(c);
            if (o instanceof Character) {
                return (Character) o;
            } else if (o == null) {
                return null;
            } else {
                char[] randomBlocks = (char[]) o;
                return randomBlocks[rand.nextInt(128)];
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public Character get(char c) {
        try {
            Object o = palette.get(c);
            if (o instanceof Character) {
                return (Character) o;
            } else if (o == null) {
                return null;
            } else {
                char[] randomBlocks = (char[]) o;
                return randomBlocks[LostCitiesTerrainGenerator.fastrand128()];
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Character canBeDamagedToIronBars(Character b) {
        return damagedToBlock.get(b);
    }

    public Info getInfo(Character c) { return information.get(c); }
}
