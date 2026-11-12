package mcjty.lostcities.materials;

/** Only tables introduced after the target version are translated; custom namespaces remain authoritative. */
public final class LootTableAliases {
    public static String for112(String table){
        if(table.startsWith("minecraft:chests/village/"))return "minecraft:chests/village_blacksmith";
        switch(table){
            case "minecraft:chests/pillager_outpost":return "minecraft:chests/village_blacksmith";
            case "minecraft:chests/shipwreck_supply":return "minecraft:chests/spawn_bonus_chest";
            case "minecraft:chests/shipwreck_treasure":
            case "minecraft:chests/buried_treasure":return "minecraft:chests/stronghold_corridor";
            case "minecraft:chests/underwater_ruin_big":
            case "minecraft:chests/underwater_ruin_small":return "minecraft:chests/simple_dungeon";
            default:return table;
        }
    }
    private LootTableAliases(){}
}
