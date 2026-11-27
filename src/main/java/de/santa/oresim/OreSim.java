package de.santa.oresim;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.ToIntFunction;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.OrePlacedFeatures;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.util.PlacedFeatureIndexer;

/**
 * Bildet die Vanilla-Erzgenerierung von 1.21.11 exakt nach (ChunkGenerator#generateFeatures,
 * Count/Rarity/Square/HeightRange-Placement, OreFeature, ScatteredOreFeature).
 */
public final class OreSim {
    private OreSim() {}

    public enum Kind { DIAMOND, DEBRIS }

    public enum Dim { OVERWORLD, NETHER }

    /** Liefert den Client-Blockzustand oder null, wenn der Chunk nicht geladen ist. */
    public interface StateLookup {
        BlockState get(int x, int y, int z);
    }

    public record Ore(BlockPos pos, Kind kind, boolean airSensitive) {}

    /** Werte 1:1 aus OrePlacedFeatures / OreConfiguredFeatures (1.21.11). */
    static final class Def {
        final Dim dim; final Kind kind; final RegistryKey<PlacedFeature> key; final GenerationStep.Feature step;
        final int count; final int rarity; final boolean trapezoid; final int minY; final int maxY;
        final int size; final float discard; final boolean scattered;
        int index = -1;

        Def(Dim dim, Kind kind, RegistryKey<PlacedFeature> key, GenerationStep.Feature step, int count, int rarity,
            boolean trapezoid, int minY, int maxY, int size, float discard, boolean scattered) {
            this.dim = dim; this.kind = kind; this.key = key; this.step = step; this.count = count; this.rarity = rarity;
            this.trapezoid = trapezoid; this.minY = minY; this.maxY = maxY; this.size = size; this.discard = discard;
            this.scattered = scattered;
        }
    }

    // Overworld: bottom -64 -> aboveBottom(-80) = -144, aboveBottom(80) = 16
    // Nether: Generatorhoehe 128 -> belowTop(8) = 119
    static final Def[] DEFS = {
        new Def(Dim.OVERWORLD, Kind.DIAMOND, OrePlacedFeatures.ORE_DIAMOND, GenerationStep.Feature.UNDERGROUND_ORES, 7, 0, true, -144, 16, 4, 0.5F, false),
        new Def(Dim.OVERWORLD, Kind.DIAMOND, OrePlacedFeatures.ORE_DIAMOND_MEDIUM, GenerationStep.Feature.UNDERGROUND_ORES, 2, 0, false, -64, -4, 8, 0.5F, false),
        new Def(Dim.OVERWORLD, Kind.DIAMOND, OrePlacedFeatures.ORE_DIAMOND_LARGE, GenerationStep.Feature.UNDERGROUND_ORES, 1, 9, true, -144, 16, 12, 0.7F, false),
        new Def(Dim.OVERWORLD, Kind.DIAMOND, OrePlacedFeatures.ORE_DIAMOND_BURIED, GenerationStep.Feature.UNDERGROUND_ORES, 4, 0, true, -144, 16, 8, 1.0F, false),
        new Def(Dim.NETHER, Kind.DEBRIS, OrePlacedFeatures.ORE_ANCIENT_DEBRIS_LARGE, GenerationStep.Feature.UNDERGROUND_DECORATION, 1, 0, true, 8, 24, 3, 1.0F, true),
        new Def(Dim.NETHER, Kind.DEBRIS, OrePlacedFeatures.ORE_DEBRIS_SMALL, GenerationStep.Feature.UNDERGROUND_DECORATION, 1, 0, false, 8, 119, 2, 1.0F, true),
    };

    private static volatile boolean ready;
    private static volatile String error;

    public static boolean isReady() { return ready; }
    public static String error() { return error; }

    /** Berechnet die Feature-Indizes genau wie der Server (PlacedFeatureIndexer ueber alle Biome der Dimension). */
    public static synchronized void init() {
        if (ready) return;
        try {
            RegistryWrapper.WrapperLookup lookup = BuiltinRegistries.createWrapperLookup();
            RegistryWrapper.Impl<Biome> biomes = lookup.getOrThrow(RegistryKeys.BIOME);
            RegistryWrapper.Impl<PlacedFeature> placed = lookup.getOrThrow(RegistryKeys.PLACED_FEATURE);
            for (Dim dim : Dim.values()) {
                MultiNoiseBiomeSourceParameterList.Preset preset = dim == Dim.OVERWORLD
                        ? MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD
                        : MultiNoiseBiomeSourceParameterList.Preset.NETHER;
                MultiNoiseBiomeSourceParameterList params = new MultiNoiseBiomeSourceParameterList(preset, biomes);
                MultiNoiseBiomeSource source = MultiNoiseBiomeSource.create(params.getEntries());
                List<RegistryEntry<Biome>> list = List.copyOf(source.getBiomes());
                List<PlacedFeatureIndexer.IndexedFeatures> indexed = PlacedFeatureIndexer.collectIndexedFeatures(
                        list, b -> b.value().getGenerationSettings().getFeatures(), true);
                for (Def def : DEFS) {
                    if (def.dim != dim) continue;
                    PlacedFeature pf = placed.getOrThrow(def.key).value();
                    ToIntFunction<PlacedFeature> mapping = indexed.get(def.step.ordinal()).indexMapping();
                    def.index = mapping.applyAsInt(pf);
                }
            }
            ready = true;
            error = null;
        } catch (Throwable t) {
            error = t.toString();
            SantaOreSim.LOGGER.error("OreSim init fehlgeschlagen", t);
        }
    }

    public static String indexInfo() {
        StringBuilder sb = new StringBuilder();
        for (Def d : DEFS) sb.append(d.key.getValue().getPath()).append('=').append(d.index).append(' ');
        return sb.toString().trim();
    }

    /** Simuliert alle gewaehlten Erze, deren Feature-Ursprung im Chunk (cx, cz) liegt. */
    public static List<Ore> simulateChunk(long seed, Dim dim, int cx, int cz, boolean diamonds, boolean debris, StateLookup lookup) {
        List<Ore> out = new ArrayList<>();
        if (!ready) return out;
        int bx = cx << 4, bz = cz << 4;
        ChunkRandom random = new ChunkRandom(new Xoroshiro128PlusPlusRandom(RandomSeed.getSeed()));
        long populationSeed = random.setPopulationSeed(seed, bx, bz);
        int worldMinY = dim == Dim.OVERWORLD ? -64 : 0;
        int worldTopY = dim == Dim.OVERWORLD ? 320 : 256; // exklusiv

        for (Def def : DEFS) {
            if (def.dim != dim || def.index < 0) continue;
            if (def.kind == Kind.DIAMOND && !diamonds) continue;
            if (def.kind == Kind.DEBRIS && !debris) continue;
            random.setDecoratorSeed(populationSeed, def.index, def.step.ordinal());

            if (def.rarity > 0 && !(random.nextFloat() < 1.0F / def.rarity)) continue;
            for (int c = 0; c < def.count; c++) {
                int x = random.nextInt(16) + bx;
                int z = random.nextInt(16) + bz;
                int y;
                if (def.trapezoid) {
                    int range = def.maxY - def.minY;
                    int l = range / 2;          // plateau 0
                    int m = range - l;
                    y = def.minY + MathHelper.nextBetween(random, 0, m) + MathHelper.nextBetween(random, 0, l);
                } else {
                    y = MathHelper.nextBetween(random, def.minY, def.maxY);
                }
                if (def.scattered) {
                    scattered(random, def, x, y, z, worldMinY, worldTopY, lookup, out);
                } else {
                    vein(random, def, x, y, z, worldMinY, worldTopY, lookup, out);
                }
            }
        }
        return out;
    }

    // ---------- OreFeature ----------
    private static void vein(ChunkRandom random, Def def, int ox, int oy, int oz, int minY, int topY, StateLookup lookup, List<Ore> out) {
        float f = random.nextFloat() * (float) Math.PI;
        float g = def.size / 8.0F;
        int i = MathHelper.ceil((def.size / 16.0F * 2.0F + 1.0F) / 2.0F);
        double startX = ox + Math.sin(f) * g;
        double endX = ox - Math.sin(f) * g;
        double startZ = oz + Math.cos(f) * g;
        double endZ = oz - Math.cos(f) * g;
        double startY = oy + random.nextInt(3) - 2;
        double endY = oy + random.nextInt(3) - 2;
        int x = ox - MathHelper.ceil(g) - i;
        int y = oy - 2 - i;
        int z = oz - MathHelper.ceil(g) - i;
        int hSize = 2 * (MathHelper.ceil(g) + i);
        int vSize = 2 * (2 + i);
        // Heightmap-Pruefung (OCEAN_FLOOR_WG) wird uebersprungen: Diamanten liegen immer unter der Oberflaeche.

        int j = def.size;
        BitSet bitSet = new BitSet(hSize * vSize * hSize);
        double[] ds = new double[j * 4];
        for (int k = 0; k < j; k++) {
            float t = (float) k / j;
            double d = MathHelper.lerp(t, startX, endX);
            double e = MathHelper.lerp(t, startY, endY);
            double gg = MathHelper.lerp(t, startZ, endZ);
            double h = random.nextDouble() * j / 16.0;
            double l = ((MathHelper.sin((float) Math.PI * t) + 1.0F) * h + 1.0) / 2.0;
            ds[k * 4] = d; ds[k * 4 + 1] = e; ds[k * 4 + 2] = gg; ds[k * 4 + 3] = l;
        }
        for (int k = 0; k < j - 1; k++) {
            if (ds[k * 4 + 3] <= 0.0) continue;
            for (int m = k + 1; m < j; m++) {
                if (ds[m * 4 + 3] <= 0.0) continue;
                double d = ds[k * 4] - ds[m * 4];
                double e = ds[k * 4 + 1] - ds[m * 4 + 1];
                double gg = ds[k * 4 + 2] - ds[m * 4 + 2];
                double h = ds[k * 4 + 3] - ds[m * 4 + 3];
                if (h * h > d * d + e * e + gg * gg) {
                    if (h > 0.0) ds[m * 4 + 3] = -1.0; else ds[k * 4 + 3] = -1.0;
                }
            }
        }
        boolean airSensitive = def.discard >= 1.0F;
        for (int m = 0; m < j; m++) {
            double d = ds[m * 4 + 3];
            if (d < 0.0) continue;
            double e = ds[m * 4], gg = ds[m * 4 + 1], h = ds[m * 4 + 2];
            int n = Math.max(MathHelper.floor(e - d), x);
            int o = Math.max(MathHelper.floor(gg - d), y);
            int p = Math.max(MathHelper.floor(h - d), z);
            int q = Math.max(MathHelper.floor(e + d), n);
            int r = Math.max(MathHelper.floor(gg + d), o);
            int s = Math.max(MathHelper.floor(h + d), p);
            for (int t = n; t <= q; t++) {
                double u = (t + 0.5 - e) / d;
                if (!(u * u < 1.0)) continue;
                for (int v = o; v <= r; v++) {
                    double w = (v + 0.5 - gg) / d;
                    if (!(u * u + w * w < 1.0)) continue;
                    for (int aa = p; aa <= s; aa++) {
                        double ab = (aa + 0.5 - h) / d;
                        if (u * u + w * w + ab * ab < 1.0 && v >= minY && v < topY) {
                            int ac = t - x + (v - y) * hSize + (aa - z) * hSize * vSize;
                            if (!bitSet.get(ac)) {
                                bitSet.set(ac);
                                if (shouldPlace(random, def, t, v, aa, lookup)) {
                                    out.add(new Ore(new BlockPos(t, v, aa), def.kind, airSensitive));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ---------- ScatteredOreFeature ----------
    private static void scattered(ChunkRandom random, Def def, int ox, int oy, int oz, int minY, int topY, StateLookup lookup, List<Ore> out) {
        int count = random.nextInt(def.size + 1);
        for (int j = 0; j < count; j++) {
            int spread = Math.min(j, 7);
            int dx = Math.round((random.nextFloat() - random.nextFloat()) * spread);
            int dy = Math.round((random.nextFloat() - random.nextFloat()) * spread);
            int dz = Math.round((random.nextFloat() - random.nextFloat()) * spread);
            int x = ox + dx, y = oy + dy, z = oz + dz;
            if (y < minY || y >= topY) continue; // ausserhalb = Void-Luft = kein Ziel
            if (shouldPlace(random, def, x, y, z, lookup)) {
                out.add(new Ore(new BlockPos(x, y, z), def.kind, true));
            }
        }
    }

    /** OreFeature#shouldPlace inkl. identischem Zufallsverbrauch. */
    private static boolean shouldPlace(ChunkRandom random, Def def, int x, int y, int z, StateLookup lookup) {
        if (!isTarget(def.kind, lookup.get(x, y, z))) return false;
        float chance = def.discard;
        if (chance <= 0.0F) return true;
        if (chance < 1.0F && random.nextFloat() >= chance) return true;
        return !exposedToAir(x, y, z, lookup);
    }

    /** Unbekannt (Chunk nicht geladen) zaehlt als Stein. */
    public static boolean isTarget(Kind kind, BlockState state) {
        if (state == null) return true;
        if (kind == Kind.DIAMOND) {
            return state.isIn(BlockTags.STONE_ORE_REPLACEABLES) || state.isIn(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                    || state.isOf(Blocks.DIAMOND_ORE) || state.isOf(Blocks.DEEPSLATE_DIAMOND_ORE);
        }
        return state.isIn(BlockTags.BASE_STONE_NETHER) || state.isOf(Blocks.ANCIENT_DEBRIS);
    }

    public static boolean isOreBlock(Kind kind, BlockState state) {
        if (state == null) return false;
        if (kind == Kind.DIAMOND) return state.isOf(Blocks.DIAMOND_ORE) || state.isOf(Blocks.DEEPSLATE_DIAMOND_ORE);
        return state.isOf(Blocks.ANCIENT_DEBRIS);
    }

    public static boolean exposedToAir(int x, int y, int z, StateLookup lookup) {
        for (Direction dir : Direction.values()) {
            BlockState s = lookup.get(x + dir.getOffsetX(), y + dir.getOffsetY(), z + dir.getOffsetZ());
            if (s != null && s.isAir()) return true;
        }
        return false;
    }
}
