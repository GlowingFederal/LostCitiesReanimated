package mcjty.lostcities.compat;

import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Optional EndlessIDs boundary. The active biome module is detected on the chunk itself. */
public final class BiomeStorage {
    private static final Class<?> HOOK;
    private static final Method SHORT_ARRAY;
    static {
        Class<?> hook=null;Method accessor=null;
        try {
            hook=Class.forName("com.falsepattern.endlessids.mixin.helpers.ChunkBiomeHook",false,BiomeStorage.class.getClassLoader());
            accessor=hook.getMethod("getBiomeShortArray");
        } catch(ClassNotFoundException absent) {
            // Ordinary Forge installations retain their native byte array.
        } catch(NoSuchMethodException incompatible) {
            throw new ExceptionInInitializerError(incompatible);
        }
        HOOK=hook;SHORT_ARRAY=accessor;
    }
    public static void write(Chunk chunk,BiomeGenBase[] biomes) {
        if(HOOK!=null&&HOOK.isInstance(chunk)) {
            try {
                short[] ids=(short[])SHORT_ARRAY.invoke(chunk);
                for(int i=0;i<256;i++)ids[i]=(short)biomes[i].biomeID;
            } catch(IllegalAccessException|InvocationTargetException failure) {
                throw new IllegalStateException("Cannot write EndlessIDs chunk biomes",failure);
            }
        } else {
            byte[] ids=chunk.getBiomeArray();
            for(int i=0;i<256;i++)ids[i]=(byte)biomes[i].biomeID;
        }
    }
    private BiomeStorage(){}
}
