package mcjty.lostcities.materials;

import java.util.*;

/** Explicit material families and functional/shape fallbacks; no arbitrary substring substitution. */
public final class SemanticMaterials {
    private static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
    private static final String[] WOODS = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped", "pale_oak"};
    private static final Map<String, String> ALIASES = new HashMap<>();
    private static final Map<String, String> FALLBACKS = new HashMap<>();
    static {
        alias("grass_block", "grass"); alias("bricks", "brick_block"); alias("stone_bricks", "stonebrick");
        alias("cracked_stone_bricks", "stonebrick@2"); alias("mossy_stone_bricks", "stonebrick@1");
        alias("chiseled_stone_bricks", "stonebrick@3"); alias("infested_stone_bricks", "monster_egg@2");
        alias("cobweb", "web"); alias("spawner", "mob_spawner"); alias("oak_door", "wooden_door");
        alias("oak_trapdoor", "trapdoor"); alias("wall_torch", "torch");
        alias("redstone_wall_torch", "redstone_torch"); alias("powered_rail", "golden_rail");
        alias("oak_fence", "fence"); alias("oak_fence_gate", "fence_gate");
        alias("oak_pressure_plate", "wooden_pressure_plate"); alias("oak_button", "wooden_button");
        alias("enchanting_table", "enchanting_table"); alias("stone_stairs", "stone_stairs");
        alias("smooth_stone_slab", "stone_slab"); fallback("smooth_stone", "stone");
        fallback("smooth_quartz", "quartz_block"); alias("cut_sandstone", "sandstone@2");
        alias("poppy", "red_flower"); alias("dandelion", "yellow_flower");
        alias("potted_cactus", "flower_pot@9"); alias("structure_void", "command_block");
        alias("nether_bricks", "nether_brick"); alias("stone_brick_slab", "stone_slab@5");
        alias("brick_slab", "stone_slab@4"); alias("sandstone_slab", "stone_slab@1");
        alias("cobblestone_slab", "stone_slab@3"); alias("quartz_slab", "stone_slab@7");
        alias("nether_brick_slab", "stone_slab@6"); alias("red_sandstone_slab", "stone_slab2");
        alias("granite", "stone@1"); alias("polished_granite", "stone@2");
        alias("diorite", "stone@3"); alias("polished_diorite", "stone@4");
        alias("andesite", "stone@5"); alias("polished_andesite", "stone@6");
        for (int i = 0; i < COLORS.length; i++) {
            alias(COLORS[i] + "_terracotta", "stained_hardened_clay@" + i);
            alias(COLORS[i] + "_stained_glass", "stained_glass@" + i);
            alias(COLORS[i] + "_stained_glass_pane", "stained_glass_pane@" + i);
            alias(COLORS[i] + "_concrete", "concrete@" + i);
            alias(COLORS[i] + "_concrete_powder", "concrete_powder@" + i);
            fallback(COLORS[i] + "_concrete", "stained_hardened_clay@" + i);
            fallback(COLORS[i] + "_concrete_powder", "stained_hardened_clay@" + i);
        }
        for (int i = 0; i < WOODS.length; i++) {
            String wood = WOODS[i];
            if (i < 6) { alias(wood + "_planks", "planks@" + i); alias(wood + "_sapling", "sapling@" + i); alias(wood + "_slab", "wooden_slab@" + i); }
            fallback(wood + "_door", "wooden_door"); fallback(wood + "_trapdoor", "trapdoor");
            fallback(wood + "_fence", "fence"); fallback(wood + "_fence_gate", "fence_gate");
            fallback(wood + "_pressure_plate", "wooden_pressure_plate"); fallback(wood + "_button", "wooden_button");
            fallback(wood + "_planks", "planks"); fallback(wood + "_stairs", "oak_stairs");
            fallback(wood + "_slab", "wooden_slab");
        }
        for (String family : new String[]{"deepslate", "deepslate_bricks", "deepslate_tiles", "polished_blackstone_bricks", "blackstone", "polished_blackstone", "tuff_bricks", "mud_bricks", "copper_block", "cut_copper", "exposed_copper", "weathered_copper", "oxidized_copper"}) {
            fallback(family, "stonebrick");
        }
        for (String family : new String[]{"deepslate_brick", "deepslate_tile", "cobbled_deepslate", "polished_deepslate", "polished_blackstone_brick", "polished_blackstone", "blackstone", "polished_diorite", "polished_andesite", "polished_granite", "mossy_stone_brick", "stone", "cut_copper", "mud_brick", "tuff_brick"}) {
            fallback(family + "_stairs", "stone_brick_stairs"); fallback(family + "_slab", "stone_slab@5"); fallback(family + "_wall", "cobblestone_wall");
        }
        fallback("cobbled_deepslate", "cobblestone"); fallback("polished_deepslate", "stonebrick");
        fallback("cracked_deepslate_bricks", "stonebrick@2"); fallback("cracked_deepslate_tiles", "stonebrick@2");
        fallback("cracked_polished_blackstone_bricks", "stonebrick@2"); fallback("red_nether_bricks", "nether_brick");
        fallback("red_sandstone", "sandstone"); fallback("red_sandstone_slab", "stone_slab@1");
        fallback("iron_trapdoor", "trapdoor"); fallback("tinted_glass", "stained_glass@15");
        fallback("lantern", "glowstone"); fallback("soul_lantern", "glowstone"); fallback("sea_lantern", "glowstone");
        fallback("barrel", "chest"); fallback("chain", "iron_bars"); fallback("diorite_wall", "cobblestone_wall");
        fallback("granite", "stone"); fallback("diorite", "stone"); fallback("andesite", "stone");
        fallback("polished_granite", "stone"); fallback("polished_diorite", "stone"); fallback("polished_andesite", "stone");
        fallback("concrete", "stained_hardened_clay"); fallback("concrete_powder", "stained_hardened_clay");
        fallback("magma", "netherrack");
    }
    private static void alias(String from, String to) { ALIASES.put("minecraft:" + from, "minecraft:" + to); }
    private static void fallback(String from, String to) { FALLBACKS.put("minecraft:" + from, "minecraft:" + to); }
    private static BlockSpec map(BlockSpec input, String target) {
        BlockSpec spec = BlockSpec.parse(target);
        return new BlockSpec(spec.id, spec.metadata, input.properties,spec.explicitMetadata);
    }
    public static BlockSpec nativeAlias(BlockSpec input) {
        String target = ALIASES.get(input.id);
        return target == null ? input : map(input, target);
    }
    public static List<BlockSpec> uabAliases(BlockSpec input) {
        List<BlockSpec> result = new ArrayList<>();
        BlockSpec alias = nativeAlias(input);
        result.add(alias.withId("etfuturum:" + alias.id.substring(10)));
        if (input.id.equals("minecraft:deepslate_tiles")) result.add(map(input, "etfuturum:deepslate_bricks@2"));
        if (input.id.equals("minecraft:cracked_deepslate_bricks")) result.add(map(input, "etfuturum:deepslate_bricks@1"));
        if (input.id.equals("minecraft:cracked_deepslate_tiles")) result.add(map(input, "etfuturum:deepslate_bricks@3"));
        if (input.id.equals("minecraft:polished_diorite")) result.add(map(input, "etfuturum:stone@4"));
        if (input.id.equals("minecraft:polished_andesite")) result.add(map(input, "etfuturum:stone@6"));
        String[] copper={"copper_block","exposed_copper","weathered_copper","oxidized_copper","cut_copper","exposed_cut_copper","weathered_cut_copper","oxidized_cut_copper"};
        for(int i=0;i<copper.length;i++){
            if(input.id.equals("minecraft:"+copper[i]))result.add(map(input,"etfuturum:copper_block@"+i));
            if(input.id.equals("minecraft:waxed_"+copper[i]))result.add(map(input,"etfuturum:copper_block@"+(i+8)));
        }
        return result;
    }
    public static List<BlockSpec> substitutes(BlockSpec input) {
        List<BlockSpec> result = new ArrayList<>();
        String target = FALLBACKS.get(input.id);
        if (target != null) result.add(input.id.equals("minecraft:red_sandstone")?new BlockSpec("minecraft:sandstone",Math.min(2,input.metadata),input.properties):map(input, target));
        // Functional and shaped blocks never degrade to an opaque cube.
        String name = input.id.substring(input.id.indexOf(':') + 1);
        if (name.endsWith("_stairs")) result.add(map(input, "minecraft:stone_brick_stairs"));
        else if (name.endsWith("_slab")) result.add(map(input, "minecraft:stone_slab@5"));
        else if (name.endsWith("_wall")) result.add(map(input, "minecraft:cobblestone_wall"));
        else if (name.endsWith("_trapdoor")) result.add(map(input, "minecraft:trapdoor"));
        else if (name.endsWith("_door")) result.add(map(input, "minecraft:wooden_door"));
        else if (name.endsWith("_fence_gate")) result.add(map(input, "minecraft:fence_gate"));
        else if (name.endsWith("_fence")) result.add(map(input, "minecraft:fence"));
        else if (name.endsWith("_stained_glass_pane")) result.add(map(input, "minecraft:stained_glass_pane"));
        else if (name.endsWith("_glass")) result.add(map(input, "minecraft:glass"));
        else if (target == null) result.add(map(input, "minecraft:stonebrick"));
        return result;
    }
    private SemanticMaterials() {}
}
