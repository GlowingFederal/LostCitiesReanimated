package mcjty.lostcities.dimensions.world;

import mcjty.lostcities.config.BiomeSelectionStrategy;
import mcjty.lostcities.config.LostCityConfiguration;
import mcjty.lostcities.config.LostCityProfile;
import mcjty.lostcities.gui.GuiLostCityConfiguration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class LostWorldType extends WorldType {

    public LostWorldType() {
        super("lostcities");
    }

    public LostWorldType(String name) {
        super(name);

    }

    @Override
    public double getHorizon(World world) {
        LostCityProfile profile = WorldTypeTools.getProfile(world);
        if (profile.HORIZON < 0) {
            return super.getHorizon(world);
        } else {
            return profile.HORIZON;
        }
    }

    @Override
    public int getSpawnFuzz(WorldServer world, MinecraftServer server) {
        LostCityProfile profile = WorldTypeTools.getProfile(world);
        switch (profile.LANDSCAPE_TYPE) {
            case DEFAULT:
            case FLOATING:
                return super.getSpawnFuzz(world, server);
            case SPACE:
            case CAVERN:
                return 0;
        }
        return super.getSpawnFuzz(world, server);
    }

    @Override
    public IChunkGenerator getChunkGenerator(World world, String generatorOptions) {
        return new LostCityChunkGenerator(world, world.getSeed());
    }

    protected BiomeProvider getInternalBiomeProvider(World world) {
        return super.getBiomeProvider(world);
    }

    @Override
    public BiomeProvider getBiomeProvider(World world) {
        return LostWorldFilteredBiomeProvider.wrap(world,getInternalBiomeProvider(world));
    }

    @Override
    public boolean isCustomizable() {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onCustomizeButton(Minecraft mc, GuiCreateWorld guiCreateWorld) {
        mc.displayGuiScreen(new GuiLostCityConfiguration(guiCreateWorld));
    }



    @Override
    public int getMinimumSpawnHeight(World world) {
        LostCityProfile profile = WorldTypeTools.getProfile(world);
        switch (profile.LANDSCAPE_TYPE) {
            case DEFAULT:
            case FLOATING:
                return super.getMinimumSpawnHeight(world);
            case SPACE:
            case CAVERN:
                return profile.GROUNDLEVEL;
        }
        return super.getMinimumSpawnHeight(world);
    }
}
