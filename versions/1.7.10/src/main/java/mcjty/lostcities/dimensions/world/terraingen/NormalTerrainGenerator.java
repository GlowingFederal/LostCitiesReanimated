package mcjty.lostcities.dimensions.world.terraingen;

import mcjty.lostcities.dimensions.world.LostCityChunkGenerator;
import mcjty.lostcities.dimensions.world.lost.BiomeInfo;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.legacy.LegacyMath;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import mcjty.lostcities.legacy.ChunkPrimer;
import mcjty.lostcities.legacy.TerrainSettings;
import net.minecraft.world.gen.NoiseGeneratorOctaves;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.ChunkProviderEvent;
import cpw.mods.fml.common.eventhandler.Event;

public class NormalTerrainGenerator {
    private World world;
    protected LostCityChunkGenerator provider;

    public final double[] heightMap;
    private double[] mainNoiseRegion;
    private double[] minLimitRegion;
    private double[] maxLimitRegion;
    private double[] depthRegion;

    private NoiseGeneratorOctaves minLimitPerlinNoise;
    private NoiseGeneratorOctaves maxLimitPerlinNoise;
    private NoiseGeneratorOctaves mainPerlinNoise;
    private NoiseGeneratorPerlin surfaceNoise;

    // A NoiseGeneratorOctaves used in generating terrain
    private NoiseGeneratorOctaves depthNoise;

    private final float[] biomeWeights;
    private double[] depthBuffer = new double[256];


    public NormalTerrainGenerator(LostCityChunkGenerator provider) {
        this.provider = provider;
        this.heightMap = new double[825];

        this.biomeWeights = new float[25];
        for (int j = -2; j <= 2; ++j) {
            for (int k = -2; k <= 2; ++k) {
                float f = (float) (10.0F / Math.sqrt((j * j + k * k) + 0.2F));
                this.biomeWeights[j + 2 + (k + 2) * 5] = f;
            }
        }
    }


    public void setup(World world) {
        this.world = world;

        this.minLimitPerlinNoise = new NoiseGeneratorOctaves(provider.rand, 16);
        this.maxLimitPerlinNoise = new NoiseGeneratorOctaves(provider.rand, 16);
        this.mainPerlinNoise = new NoiseGeneratorOctaves(provider.rand, 8);
        this.surfaceNoise = new NoiseGeneratorPerlin(provider.rand, 4);
        NoiseGeneratorOctaves noiseGen5 = new NoiseGeneratorOctaves(provider.rand, 10);
        this.depthNoise = new NoiseGeneratorOctaves(provider.rand, 16);
        NoiseGeneratorOctaves mobSpawnerNoise = new NoiseGeneratorOctaves(provider.rand, 8);

        net.minecraft.world.gen.NoiseGenerator[] generators = net.minecraftforge.event.terraingen.TerrainGen.getModdedNoiseGenerators(world, provider.rand,
                new net.minecraft.world.gen.NoiseGenerator[]{minLimitPerlinNoise, maxLimitPerlinNoise, mainPerlinNoise, surfaceNoise, noiseGen5, depthNoise, mobSpawnerNoise});
        minLimitPerlinNoise=(NoiseGeneratorOctaves)generators[0]; maxLimitPerlinNoise=(NoiseGeneratorOctaves)generators[1]; mainPerlinNoise=(NoiseGeneratorOctaves)generators[2];
        surfaceNoise=(NoiseGeneratorPerlin)generators[3]; depthNoise=(NoiseGeneratorOctaves)generators[5];
    }

    public void generateHeightmap(int chunkX, int chunkZ) {
        int chunkX4 = chunkX * 4;
        int chunkZ4 = chunkZ * 4;
        TerrainSettings settings = provider.getSettings();
        this.depthRegion = this.depthNoise.generateNoiseOctaves(this.depthRegion, chunkX4, chunkZ4, 5, 5, (double)settings.depthNoiseScaleX, (double)settings.depthNoiseScaleZ, (double)settings.depthNoiseScaleExponent);
        float f = settings.coordinateScale;
        float f1 = settings.heightScale;
        this.mainNoiseRegion = this.mainPerlinNoise.generateNoiseOctaves(this.mainNoiseRegion, chunkX4, 0, chunkZ4, 5, 33, 5, (double)(f / settings.mainNoiseScaleX), (double)(f1 / settings.mainNoiseScaleY), (double)(f / settings.mainNoiseScaleZ));
        this.minLimitRegion = this.minLimitPerlinNoise.generateNoiseOctaves(this.minLimitRegion, chunkX4, 0, chunkZ4, 5, 33, 5, (double)f, (double)f1, (double)f);
        this.maxLimitRegion = this.maxLimitPerlinNoise.generateNoiseOctaves(this.maxLimitRegion, chunkX4, 0, chunkZ4, 5, 33, 5, (double)f, (double)f1, (double)f);

        int i = 0;
        int j = 0;

        BiomeGenBase[] biomes = BiomeInfo.getBiomeInfo(provider, new ChunkCoord(provider.dimensionId, chunkX, chunkZ)).getBiomes();

        float biomeDepthOffSet = settings.biomeDepthOffSet;
        float biomeDepthWeight = settings.biomeDepthWeight;
        float biomeScaleOffset = settings.biomeScaleOffset;
        float biomeScaleWeight = settings.biomeScaleWeight;

        for (int k = 0; k < 5; ++k) {
            for (int l = 0; l < 5; ++l) {
                float f2 = 0.0F;
                float f3 = 0.0F;
                float f4 = 0.0F;
                BiomeGenBase biome = biomes[k + 2 + (l + 2) * 10];
                float biomeBaseHeight = biome.rootHeight;

                for (int j1 = -2; j1 <= 2; ++j1) {
                    for (int k1 = - 2; k1 <=  2; ++k1) {
                        BiomeGenBase biome1 = biomes[k + j1 + 2 + (l + k1 + 2) * 10];
                        float biome1BaseHeight = biome1.rootHeight;

                        float baseHeight = biomeDepthOffSet + biome1BaseHeight * biomeDepthWeight;
                        float heightVariation = Math.abs(biomeScaleOffset + biome1.heightVariation * biomeScaleWeight);

                        float f7 = biomeWeights[j1 + 2 + (k1 + 2) * 5] / (baseHeight + 2.0F);

                        if (biome1BaseHeight > biomeBaseHeight) {
                            f7 /= 2.0F;
                        }

                        f2 += heightVariation * f7;
                        f3 += baseHeight * f7;
                        f4 += f7;
                    }
                }

                f2 /= f4;
                f3 /= f4;
                f2 = f2 * 0.9F + 0.1F;
                f3 = (f3 * 4.0F - 1.0F) / 8.0F;
                double d12 = this.depthRegion[j] / 8000.0D;

                if (d12 < 0.0D) {
                    d12 = -d12 * 0.3D;
                }

                d12 = d12 * 3.0D - 2.0D;

                if (d12 < 0.0D) {
                    d12 /= 2.0D;

                    if (d12 < -1.0D) {
                        d12 = -1.0D;
                    }

                    d12 /= 1.4D;
                    d12 /= 2.0D;
                } else {
                    if (d12 > 1.0D) {
                        d12 = 1.0D;
                    }

                    d12 /= 8.0D;
                }

                ++j;
                double d13 = f3;
                double d14 = f2;
                d13 += d12 * 0.2D;
                d13 = d13 * settings.baseSize / 8.0D;
                double d5 = settings.baseSize + d13 * 4.0D;

                for (int j2 = 0; j2 < 33; ++j2) {
                    double d6 = (j2 - d5) * settings.stretchY * 128.0D / 256.0D / d14;

                    if (d6 < 0.0D) {
                        d6 *= 4.0D;
                    }

                    double d7 = this.minLimitRegion[i] / settings.lowerLimitScale;
                    double d8 = this.maxLimitRegion[i] / settings.upperLimitScale;
                    double d9 = (this.mainNoiseRegion[i] / 10.0D + 1.0D) / 2.0D;
                    double d10 = LegacyMath.clampedLerp(d7, d8, d9) - d6;

                    if (j2 > 29) {
                        double d11 = ((j2 - 29) / 3.0F);
                        d10 = d10 * (1.0D - d11) + -10.0D * d11;
                    }

                    this.heightMap[i] = d10;
                    ++i;
                }
            }
        }
    }

    public void replaceBlocksForBiome(int chunkX,int chunkZ,ChunkPrimer primer,BiomeGenBase[] biomes) {
        net.minecraft.block.Block[] blocks=primer.blocks(); byte[] metadata=primer.metadata();
        ChunkProviderEvent.ReplaceBiomeBlocks event=new ChunkProviderEvent.ReplaceBiomeBlocks(provider,chunkX,chunkZ,blocks,metadata,biomes,world);
        MinecraftForge.EVENT_BUS.post(event);
        if(event.getResult()!=Event.Result.DENY) {
            depthBuffer=surfaceNoise.func_151599_a(depthBuffer,chunkX*16,chunkZ*16,16,16,.0625D,.0625D,1.0D);
            for(int x=0;x<16;x++)for(int z=0;z<16;z++)biomes[z+x*16].genTerrainBlocks(world,provider.rand,blocks,metadata,chunkX*16+x,chunkZ*16+z,depthBuffer[z+x*16]);
        }
        primer.load(blocks,metadata);
    }
}
