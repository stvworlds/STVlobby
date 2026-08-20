package win.stvworlds.sTVlobby.functions.join;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import win.stvworlds.sTVlobby.functions.PluginFunction;

public class PlayerJoinFunction implements PluginFunction, Listener {

    private final JavaPlugin plugin;

    public PlayerJoinFunction(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();

        World world = Bukkit.getWorld("STVlobby");
        Location loc = (world != null)
                ? world.getSpawnLocation()
                : new Location(Bukkit.getWorld("world"), -0.5, 101, -15.5, 0, 0);

        p.teleport(loc);

        p.getWorld().playSound(
                p.getLocation(),
                Sound.BLOCK_AMETHYST_BLOCK_RESONATE,
                1f,
                1f
        );

        p.getWorld().spawnParticle(
                Particle.FLASH,
                p.getLocation().add(0.5, 1.1, 0.5),
                25,
                0.2, 0.2, 0.2,
                0.05
        );


    }
}
