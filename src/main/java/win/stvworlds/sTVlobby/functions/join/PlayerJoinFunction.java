package win.stvworlds.sTVlobby.functions.join;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import win.stvworlds.sTVlobby.functions.PluginFunction;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerJoinFunction implements PluginFunction, Listener {

    private static final String LOBBY_WORLD = "STVlobby";
    private static final TextColor ACCENT = TextColor.fromHexString("#6F27F5");
    private static final TextColor SOFT = TextColor.fromHexString("#EBEBEB");

    private final JavaPlugin plugin;

    public PlayerJoinFunction(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean firstJoin = !player.hasPlayedBefore();

        event.joinMessage(null);

        Location spawn = resolveLobbySpawn(player);
        player.teleportAsync(spawn).thenAccept(success -> {
            if (!success || !player.isOnline()) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> playWelcomeSequence(player, spawn, firstJoin));
        });
    }

    private Location resolveLobbySpawn(Player player) {
        World lobby = Bukkit.getWorld(LOBBY_WORLD);
        if (lobby != null) {
            Location spawn = lobby.getSpawnLocation().clone();
            // Центр блоку + збережений yaw/pitch спавну світу
            spawn.setX(spawn.getBlockX() + 0.5);
            spawn.setZ(spawn.getBlockZ() + 0.5);
            return spawn;
        }

        World fallback = player.getWorld();
        World defaultWorld = Bukkit.getWorlds().isEmpty() ? fallback : Bukkit.getWorlds().getFirst();
        return new Location(defaultWorld, -0.5, 101, -15.5, 0f, 0f);
    }

    private void playWelcomeSequence(Player player, Location spawn, boolean firstJoin) {
        preparePlayer(player);
        playJoinSounds(player, spawn, firstJoin);
        spawnBurstParticles(spawn);
        runOrbitAnimation(player, spawn, firstJoin);
    }

    private void preparePlayer(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.setFallDistance(0f);
        player.setAllowFlight(false);
        player.setFlying(false);

        // Короткий «м’який» вхід без сильного ванільного ефекту
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.NIGHT_VISION,
                40,
                0,
                false,
                false,
                false
        ));
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOW_FALLING,
                30,
                0,
                false,
                false,
                false
        ));
    }


    private void playJoinSounds(Player player, Location at, boolean firstJoin) {
        World world = at.getWorld();
        if (world == null) {
            return;
        }

        // Базовий «портальний» акорд
        world.playSound(at, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 0.9f, 1.15f);
        world.playSound(at, Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.7f, 1.4f);
        world.playSound(at, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.35f, 1.6f);

        if (firstJoin) {
            world.playSound(at, Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.45f, 1.2f);
            world.playSound(at, Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 0.5f, 1.35f);
        } else {
            world.playSound(at, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.55f, 1.5f);
        }

        // Ледь чутний «whoosh» лише гравцю
        player.playSound(at, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.25f, 1.8f);
    }

    private void spawnBurstParticles(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        Location chest = center.clone().add(0, 1.0, 0);

        world.spawnParticle(Particle.END_ROD, chest, 40, 0.35, 0.55, 0.35, 0.02);
        world.spawnParticle(Particle.FIREWORK, chest, 18, 0.25, 0.4, 0.25, 0.08);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, chest, 22, 0.4, 0.5, 0.4, 0.01);
        world.spawnParticle(Particle.ENCHANT, chest, 50, 0.6, 0.7, 0.6, 0.4);
        world.spawnParticle(Particle.CHERRY_LEAVES, chest, 16, 0.5, 0.35, 0.5, 0.01);
        world.spawnParticle(Particle.GLOW, chest, 12, 0.3, 0.45, 0.3, 0.0);

        // М’який кольоровий пил у фірмовому фіолетовому
        Particle.DustOptions dust = new Particle.DustOptions(
                Color.fromRGB(111, 39, 245),
                1.35f
        );
        world.spawnParticle(Particle.DUST, chest, 28, 0.45, 0.55, 0.45, 0.0, dust);
    }

    private void runOrbitAnimation(Player player, Location center, boolean firstJoin) {
        final int ticks = firstJoin ? 36 : 24;
        final double radius = firstJoin ? 1.35 : 1.05;
        final Location base = center.clone().add(0, 1.05, 0);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline() || tick >= ticks) {
                    if (player.isOnline()) {
                        finishLanding(player, center, firstJoin);
                    }
                    cancel();
                    return;
                }

                World world = base.getWorld();
                if (world == null) {
                    cancel();
                    return;
                }

                double progress = tick / (double) ticks;
                double angle = progress * Math.PI * 4; // 2 повних оберти
                double y = Math.sin(progress * Math.PI) * 0.85;

                double x1 = Math.cos(angle) * radius;
                double z1 = Math.sin(angle) * radius;
                double x2 = Math.cos(angle + Math.PI) * radius;
                double z2 = Math.sin(angle + Math.PI) * radius;

                Location p1 = base.clone().add(x1, y, z1);
                Location p2 = base.clone().add(x2, y, z2);

                world.spawnParticle(Particle.END_ROD, p1, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.END_ROD, p2, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.WITCH, p1, 2, 0.02, 0.02, 0.02, 0);
                world.spawnParticle(Particle.WITCH, p2, 2, 0.02, 0.02, 0.02, 0);

                if (tick % 4 == 0) {
                    world.spawnParticle(Particle.REVERSE_PORTAL, base, 6, 0.2, 0.25, 0.2, 0.01);
                }

                if (tick % 8 == 0) {
                    float pitch = 0.9f + (float) (progress * 0.7);
                    world.playSound(base, Sound.BLOCK_AMETHYST_BLOCK_STEP, SoundCategory.PLAYERS, 0.2f, pitch);
                }

                tick++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void finishLanding(Player player, Location center, boolean firstJoin) {
        World world = center.getWorld();
        if (world == null || !player.isOnline()) {
            return;
        }

        Location feet = center.clone().add(0, 0.15, 0);
        world.spawnParticle(Particle.CLOUD, feet, 14, 0.35, 0.05, 0.35, 0.01);
        world.spawnParticle(Particle.HAPPY_VILLAGER, feet.clone().add(0, 0.4, 0), 8, 0.4, 0.2, 0.4, 0);
        world.playSound(center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.4f, 1.3f);

        if (firstJoin && ThreadLocalRandom.current().nextBoolean()) {
            // Легкий «святковий» фінал лише для частини перших входів
            world.spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0, 1.2, 0), 20, 0.3, 0.5, 0.3, 0.15);
            world.playSound(center, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.2f, 1.6f);
        }
    }
}
