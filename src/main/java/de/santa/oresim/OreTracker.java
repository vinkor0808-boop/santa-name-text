package de.santa.oresim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

/** Haelt die simulierten Erze im Umkreis aktuell und baut die sichtbare Liste fuers Rendern. */
public final class OreTracker {
    private OreTracker() {}

    public record Visible(BlockPos pos, OreSim.Kind kind, boolean confirmed) {}

    private static final class Entry {
        List<OreSim.Ore> ores;
        boolean complete;
    }

    private static final Long2ObjectOpenHashMap<Entry> CACHE = new Long2ObjectOpenHashMap<>();
    private static volatile List<Visible> visible = List.of();
    private static ClientWorld lastWorld;
    private static long lastSeed;
    private static String lastSettings = "";
    private static int tick;
    private static boolean initStarted;

    public static List<Visible> visible() { return visible; }

    public static int cachedChunks() { return CACHE.size(); }

    public static void reset() {
        CACHE.clear();
        visible = List.of();
    }

    public static void tick(MinecraftClient client) {
        OreConfig cfg = OreConfig.INSTANCE;
        ClientWorld world = client.world;
        if (world == null || client.player == null || !cfg.enabled || !cfg.hasSeed) {
            if (!visible.isEmpty()) visible = List.of();
            return;
        }
        if (!initStarted) {
            initStarted = true;
            Thread t = new Thread(OreSim::init, "Santa-OreSim-Init");
            t.setDaemon(true);
            t.start();
        }
        if (!OreSim.isReady()) return;

        OreSim.Dim dim;
        if (world.getRegistryKey() == World.OVERWORLD) dim = OreSim.Dim.OVERWORLD;
        else if (world.getRegistryKey() == World.NETHER) dim = OreSim.Dim.NETHER;
        else { if (!visible.isEmpty()) visible = List.of(); return; }

        String settings = cfg.diamonds + "|" + cfg.debris;
        if (world != lastWorld || cfg.seed != lastSeed || !settings.equals(lastSettings)) {
            reset();
            lastWorld = world;
            lastSeed = cfg.seed;
            lastSettings = settings;
        }

        BlockPos ppos = client.player.getBlockPos();
        ChunkPos center = new ChunkPos(ppos);
        int cr = (cfg.radius >> 4) + 1;
        OreSim.StateLookup lookup = (x, y, z) -> {
            if (!world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) return null;
            return world.getBlockState(new BlockPos(x, y, z));
        };

        // Chunks von innen nach aussen simulieren (Budget pro Tick)
        int budget = 24;
        for (int ring = 0; ring <= cr && budget > 0; ring++) {
            for (int dx = -ring; dx <= ring && budget > 0; dx++) {
                for (int dz = -ring; dz <= ring && budget > 0; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    int cx = center.x + dx, cz = center.z + dz;
                    long key = ChunkPos.toLong(cx, cz);
                    Entry e = CACHE.get(key);
                    boolean loaded = areaLoaded(world, cx, cz);
                    if (e != null && (e.complete || !loaded)) continue;
                    if (e == null) e = new Entry();
                    e.ores = OreSim.simulateChunk(cfg.seed, dim, cx, cz, cfg.diamonds, cfg.debris, lookup);
                    e.complete = loaded;
                    CACHE.put(key, e);
                    budget--;
                }
            }
        }

        // Weit entfernte Chunks verwerfen
        CACHE.long2ObjectEntrySet().removeIf(en -> {
            ChunkPos cp = new ChunkPos(en.getLongKey());
            return Math.abs(cp.x - center.x) > cr + 2 || Math.abs(cp.z - center.z) > cr + 2;
        });

        if (++tick % 5 != 0) return;

        // Sichtbare Liste neu aufbauen
        double r2 = (double) cfg.radius * cfg.radius;
        List<Visible> list = new ArrayList<>();
        for (Entry e : CACHE.values()) {
            for (OreSim.Ore ore : e.ores) {
                BlockPos p = ore.pos();
                double ddx = p.getX() + 0.5 - client.player.getX();
                double ddz = p.getZ() + 0.5 - client.player.getZ();
                if (ddx * ddx + ddz * ddz > r2) continue;
                BlockState state = lookup.get(p.getX(), p.getY(), p.getZ());
                boolean confirmed = OreSim.isOreBlock(ore.kind(), state);
                if (cfg.filter && state != null && !confirmed) {
                    if (!OreSim.isTarget(ore.kind(), state)) continue;
                    if (ore.airSensitive() && OreSim.exposedToAir(p.getX(), p.getY(), p.getZ(), lookup)) continue;
                }
                list.add(new Visible(p, ore.kind(), confirmed));
            }
        }
        double px = client.player.getX(), py = client.player.getY(), pz = client.player.getZ();
        list.sort(Comparator.comparingDouble(v -> v.pos().getSquaredDistance(px, py, pz)));
        if (list.size() > 2500) list = new ArrayList<>(list.subList(0, 2500));
        visible = List.copyOf(list);
    }

    private static boolean areaLoaded(ClientWorld world, int cx, int cz) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                if (!world.getChunkManager().isChunkLoaded(cx + dx, cz + dz)) return false;
        return true;
    }
}
