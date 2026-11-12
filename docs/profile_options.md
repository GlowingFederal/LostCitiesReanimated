# Native legacy profile options

Forge 1.7.10 and 1.12.2 load the same profile identifiers from config/lostcities. Existing legacy keys, including the historical spelling cityThresshold, retain their meanings. Modern-only settings are in each profile's modern_<profile> category. The generated configuration includes individual ranges and descriptions.

| Option | Default | Range and behavior |
| --- | --- | --- |
| cityChance | 0.01 for newly created configuration | -1 uses modern noise; 0–1 is radial city-center probability. Existing configured values are retained. |
| cityStyleThreshold | -1 | -1 disables; 0–1 selects cityStyleAlternative below this city's radial influence, or its raw noise influence in noise mode. Modern mode only. |
| cityStyleAlternative | empty | Registered city-style name. Empty disables the alternative. |
| allowedBiomeFactors | empty | biome=factor list. Empty preserves original biomes; otherwise climate similarity selects from the listed biomes. A larger factor makes a dissimilar biome less likely to be selected. |
| manualBiomeMappings | empty | source_biome=destination_biome entries applied after climate selection; also work without an allowed-biome list. |
| biomeSelectionStrategy | original | original chooses the nearest climate match. randomized and varied allow increasingly similar candidate matches with stable biome-name seeded choices. |
| cityLevel4Height / cityLevel5Height / cityLevel6Height / cityLevel7Height | 107 / 115 / 123 / 131 | 1–255 native terrain thresholds for additional modern city levels. Legacy mode retains its original four-level selection. |
| fastTerrainPlanning | true | Use exact native density-lattice planning samples for normal terrain. False expands the full raw chunk. Custom terrain adapters, floating worlds, caverns and spheres retain full sampling. |
| cityMinHeight / cityMaxHeight | 50 / 150 | 1–254 normal-terrain bounds for modern cities. |
| intercityHighways / hierarchicalStreets / modernMultibuildings / scatteredStructures | true | Independent modern planner switches. |
| cityPerlinScale / cityPerlinOffset / cityPerlinInnerScale | 3 / 0.1 / 0.1 | Noise distribution controls used when cityChance=-1. |
| citySpawnDistance1 / citySpawnDistance2 | 0 / 0 | 0–30000000 blocks from origin. Outer distance 0 disables scaling; otherwise it must exceed the inner distance. |
| citySpawnMultiplier1 / citySpawnMultiplier2 | 1 / 1 | 0–100 city influence multipliers at the two distances, interpolated and clamped outside them. |
| openLotParkChance | 0.8 | 0–1 park probability in modern hierarchical open lots. |
| parkElevation / parkBorder | true / true | Raise connected modern parks by one block and use street material at exposed park edges. Inherited city-style parkblocks can override these settings. |
| parkStreetThreshold | 3 | 0–8 surrounding street or park chunks required for park elevation. |
| primaryBridgeChance / primaryBridgeMaximumLength | 1 / 12 | 0–1 span chance; 1–64 water-gap chunks. Both endpoints must be primary city roads at base city level. |
| corridorMaximumLength | 128 | 1–1024 open chunks between cellar endpoints. Modern spans longer than the bound are rejected along their entire length. Legacy-mode behavior is retained. |
| legacyBridgeMaximumLength | 128 | 1–1024 search bound for original bridge parts in modern worlds. |
| surfaceRailStations | true | Permit surface stations only when spacing allows a safe descent. |
| railwayLevelOffset | 0 | -8–2 six-block levels; final placement remains within world height. |
| railwaySpacingNorthSouth / railwaySpacingEastWest | 10 / 10 | Even values, 4–128 chunks, independently controlling the two grid directions. |
| scatteredChanceMultiplier | 1 | 0–100 multiplier on world-style scattered structure chance. |

Primary, secondary and tertiary road settings and bounded intercity-highway search controls are generated in the same modern category. The built-in defaults mirror the inspected upstream planner settings. World-style assets choose transit parts, bridge/support parts, scattered definitions and multibuilding distributions. Native configuration files remain the advanced-settings interface on 1.7.10; the world-creation Customize screen selects a public profile.

Generation mode and material selections are saved per world/dimension. Changing a profile, asset pack or provider mapping can change newly generated terrain; it does not regenerate completed chunks.

Normal-world integration uses the general `overlayDimensions` list, empty by default. Each entry is `dimensionId:profile`, for example `0:realistic`. This keeps the saved world type and native biome layers, installing Lost Cities before fresh spawn generation or an existing world’s first chunk load. Only future chunks change. Profiles must use the default landscape. Native overworld generators are supported directly; custom dimension generators must implement the target’s `IChunkPrimerFactory`. In this mode the selected profile controls city structures and native feature/population switches. The original overworld generator supplies base terrain and non-city biome surfaces, including its terrain settings. Unsupported generators fail with their class name instead of being replaced silently.

Modern city styles inherit optional `parkblocks` values (`parkchance`, `avoidfoliage`, `parkborder`, `parkelevation`, `parkstreetthreshold`, `grass`), `streetblocks` values (`fountainchance`, `frontchance`), `corridorblocks.corridorchance`, and `profileOverrides.openLotParkChance`. Missing values use the selected profile. Floor and cellar values retain their constraint semantics. Probabilities, thresholds and palette characters are checked when assets load. Street slope parts use their `z1`/`z2` metadata to open the upper border.

The landscapeType setting also accepts spheres and cavernspheres, with built-in profiles of the same names. They retain native terrain and enclose the selected city areas with modern shells. In cityspheres_<profile>, citySphereClearAbove and citySphereClearBelow choose bounded clearing distances (0–256); citySphereClearAboveUntilAir and citySphereClearBelowUntilAir extend that clearing to air/water or the native height boundary. Profile spawnBiome, spawnCity, spawnSphere (<in>, <out> or a predefined sphere) and spawnNotInBuilding work on both native targets. Named predefined spawn searches begin around that asset, including far from the world origin.
