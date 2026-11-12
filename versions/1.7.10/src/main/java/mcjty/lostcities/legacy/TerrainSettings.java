package mcjty.lostcities.legacy;

/** Vanilla 1.12 terrain-noise parameters; the actual 1.7.10 pipeline uses its own native generators. */
public final class TerrainSettings {
    public float coordinateScale=684.412F,heightScale=684.412F,lowerLimitScale=512,upperLimitScale=512,
        depthNoiseScaleX=200,depthNoiseScaleZ=200,depthNoiseScaleExponent=.5F,
        mainNoiseScaleX=80,mainNoiseScaleY=160,mainNoiseScaleZ=80,baseSize=8.5F,stretchY=12,
        biomeDepthOffSet=0,biomeDepthWeight=1,biomeScaleOffset=0,biomeScaleWeight=1;
}
