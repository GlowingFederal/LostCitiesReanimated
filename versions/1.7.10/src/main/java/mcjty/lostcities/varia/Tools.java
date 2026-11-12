package mcjty.lostcities.varia;

import net.minecraft.block.Block;
import mcjty.lostcities.legacy.LegacyState;
import mcjty.lostcities.legacy.LegacyStates;
import net.minecraft.util.ResourceLocation;
import mcjty.lostcities.legacy.LegacyRegistries;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Tools {

    public static LegacyState stringToState(String s) {
        return mcjty.lostcities.compat.LegacyMaterials.resolve(s);
    }

    public static String stateToString(LegacyState state) {
        int meta = state.meta;
        if (meta == 0) {
            return mcjty.lostcities.legacy.LegacyRegistries.blockName(state.getBlock()).toString();
        } else {
            return mcjty.lostcities.legacy.LegacyRegistries.blockName(state.getBlock()).toString() + "@" + meta;
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
