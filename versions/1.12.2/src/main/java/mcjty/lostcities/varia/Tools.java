package mcjty.lostcities.varia;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Tools {

    public static IBlockState stringToState(String s) {
        return mcjty.lostcities.compat.LegacyMaterials.resolve(s);
    }

    public static String stateToString(IBlockState state) {
        int meta = state.getBlock().getMetaFromState(state);
        if (meta == 0) {
            return state.getBlock().getRegistryName().toString();
        } else {
            return state.getBlock().getRegistryName().toString() + "@" + meta;
        }
    }

    public static String getRandomFromList(Random random, List<Pair<Float, String>> list) {
        if (list.isEmpty()) {
            return null;
        }
        List<Pair<Float, String>> elements = new ArrayList<>();
        float totalweight = 0;
        for (Pair<Float, String> pair : list) {
            elements.add(pair);
            totalweight += pair.getKey();
        }
        float r = random.nextFloat() * totalweight;
        for (Pair<Float, String> pair : elements) {
            r -= pair.getKey();
            if (r <= 0) {
                return pair.getRight();
            }
        }
        return null;
    }
}
