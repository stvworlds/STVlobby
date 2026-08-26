package win.stvworlds.sTVlobby.functions.portal;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import win.stvworlds.sTVlobby.functions.PluginFunction;

public class ServerTeleport implements PluginFunction, Listener {
    private static final String CHANNEL = "BungeeCord";
    private static final Particle.DustOptions EDGE_DUST = new Particle.DustOptions(
            Color.fromRGB(111, 39, 245),
            1.1f
    );
    /** Крок між частинками на ребрі (блоки). */
    private static final double EDGE_STEP = 0.45;
    /** Період оновлення outline (тіки). */
    private static final long PARTICLE_PERIOD_TICKS = 8L;

    private final JavaPlugin plugin;
    private final String worldName;
    private final String targetServer;
    private final double minX;
    private final double minY;
    private final double minZ;
    private final double maxX;
    private final double maxY;
    private final double maxZ;
    private final long cooldownMillis;
    private final boolean particlesEnabled;

    private BukkitTask particleTask;

    public ServerTeleport(JavaPlugin plugin) {
        this.plugin = plugin;

        worldName = plugin.getConfig().getString("portal.world", "world");
        targetServer = plugin.getConfig().getString("portal.target-server", "beta");
        particlesEnabled = plugin.getConfig().getBoolean("portal.particles", true);

        double x1 = plugin.getConfig().getDouble("portal.pos1.x");
        double y1 = plugin.getConfig().getDouble("portal.pos1.y");
        double z1 = plugin.getConfig().getDouble("portal.pos1.z");
        double x2 = plugin.getConfig().getDouble("portal.pos2.x");
        double y2 = plugin.getConfig().getDouble("portal.pos2.y");
        double z2 = plugin.getConfig().getDouble("portal.pos2.z");

        minX = Math.min(x1, x2);
        minY = Math.min(y1, y2);
        minZ = Math.min(z1, z2);
        maxX = Math.max(x1, x2);
        maxY = Math.max(y1, y2);
        maxZ = Math.max(z1, z2);
        cooldownMillis = Math.max(0L, plugin.getConfig().getLong("portal.cooldown-seconds", 3L)) * 1000L;
    }

    @Override
    public void register() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startParticleOutline();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (!isInPortal(to) || isInPortal(from)) {
            return;
        }

        long now = System.currentTimeMillis();
        Long lastTeleport = event.getPlayer().getMetadata("stv-lobby-portal-cooldown").stream()
                .map(metadata -> metadata.value() instanceof Number number ? number.longValue() : 0L)
                .findFirst()
                .orElse(0L);
        if (now - lastTeleport < cooldownMillis) {
            return;
        }

        event.getPlayer().setMetadata(
                "stv-lobby-portal-cooldown",
                new FixedMetadataValue(plugin, now)
        );
        plugin.getLogger().info("Teleporting player to " + targetServer);
        connect(event.getPlayer(), targetServer);
    }

    private void startParticleOutline() {
        if (!particlesEnabled) {
            return;
        }
        if (particleTask != null) {
            particleTask.cancel();
        }
        particleTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::drawPortalCuboid,
                20L,
                PARTICLE_PERIOD_TICKS
        );
    }

    private void drawPortalCuboid() {
        World world = plugin.getServer().getWorld(worldName);
        if (world == null || world.getPlayers().isEmpty()) {
            return;
        }

        double x0 = minX;
        double y0 = minY;
        double z0 = minZ;
        double x1 = maxX;
        double y1 = maxY;
        double z1 = maxZ;

        // Якщо зона вироджена в точку — маленький «маяк», щоб було видно конфіг 0,0,0
        if (x0 == x1 && y0 == y1 && z0 == z1) {
            Location c = new Location(world, x0 + 0.5, y0 + 0.5, z0 + 0.5);
            world.spawnParticle(Particle.END_ROD, c, 6, 0.15, 0.25, 0.15, 0.01);
            world.spawnParticle(Particle.DUST, c, 8, 0.2, 0.3, 0.2, 0.0, EDGE_DUST);
            world.spawnParticle(Particle.REVERSE_PORTAL, c, 4, 0.1, 0.15, 0.1, 0.0);
            return;
        }

        // 12 ребер AABB
        drawEdge(world, x0, y0, z0, x1, y0, z0);
        drawEdge(world, x0, y0, z1, x1, y0, z1);
        drawEdge(world, x0, y1, z0, x1, y1, z0);
        drawEdge(world, x0, y1, z1, x1, y1, z1);

        drawEdge(world, x0, y0, z0, x0, y1, z0);
        drawEdge(world, x1, y0, z0, x1, y1, z0);
        drawEdge(world, x0, y0, z1, x0, y1, z1);
        drawEdge(world, x1, y0, z1, x1, y1, z1);

        drawEdge(world, x0, y0, z0, x0, y0, z1);
        drawEdge(world, x1, y0, z0, x1, y0, z1);
        drawEdge(world, x0, y1, z0, x0, y1, z1);
        drawEdge(world, x1, y1, z0, x1, y1, z1);

        // Легке «заповнення» центру — видно об’єм зони
        double cx = (x0 + x1) * 0.5;
        double cy = (y0 + y1) * 0.5;
        double cz = (z0 + z1) * 0.5;
        double halfW = Math.max(0.15, (x1 - x0) * 0.25);
        double halfH = Math.max(0.15, (y1 - y0) * 0.25);
        double halfD = Math.max(0.15, (z1 - z0) * 0.25);
        Location center = new Location(world, cx, cy, cz);
        world.spawnParticle(Particle.REVERSE_PORTAL, center, 10, halfW, halfH, halfD, 0.0);
        world.spawnParticle(Particle.ENCHANT, center, 8, halfW, halfH, halfD, 0.15);
    }

    private void drawEdge(World world, double ax, double ay, double az, double bx, double by, double bz) {
        double dx = bx - ax;
        double dy = by - ay;
        double dz = bz - az;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-4) {
            Location p = new Location(world, ax, ay, az);
            world.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, EDGE_DUST);
            return;
        }

        int steps = Math.max(1, (int) Math.ceil(length / EDGE_STEP));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Location p = new Location(world, ax + dx * t, ay + dy * t, az + dz * t);
            world.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, EDGE_DUST);
            if (i % 3 == 0) {
                world.spawnParticle(Particle.END_ROD, p, 1, 0, 0, 0, 0);
            }
        }
    }

    private boolean isInPortal(Location location) {
        World world = location.getWorld();
        return world != null
                && world.getName().equals(worldName)
                && location.getX() >= minX && location.getX() <= maxX
                && location.getY() >= minY && location.getY() <= maxY
                && location.getZ() >= minZ && location.getZ() <= maxZ;
    }

    private void connect(org.bukkit.entity.Player player, String server) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF("Connect");
        output.writeUTF(server);
        player.sendPluginMessage(plugin, CHANNEL, output.toByteArray());
    }
}
