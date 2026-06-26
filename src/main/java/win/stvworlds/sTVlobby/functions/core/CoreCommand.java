package win.stvworlds.sTVlobby.functions.core;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;
import win.stvworlds.sTVlobby.functions.PluginFunction;
import win.stvworlds.sTVlobby.utils.Messages;

public class CoreCommand implements PluginFunction {

    private final JavaPlugin plugin;

    public CoreCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final var registrar = event.registrar();

            registrar.register(
                    Commands.literal("stvlobby")
                            .executes(ctx -> {

                                Component text = Component.text(plugin.getConfig().getString("messages.system.info", "Private Stevious plugins system by Maksim6opka"));

                                Messages.sendDM(text, ctx.getSource().getSender(), "core", "info");

                                return 1;
                            })
                            .then(Commands.literal("reload")
                                    .requires(req -> req.getSender().hasPermission("stv.admin"))
                                    .executes(ctx -> {

                                        String status;
                                        Component text;

                                        try {
                                            plugin.reloadConfig();
                                            text = Component.text(plugin.getConfig().getString("messages.system.reload", "STVlobby reloaded"));
                                            status = "info";
                                        } catch (Exception e) {
                                            text = Component.text(e.getLocalizedMessage());
                                            status = "error";

                                        }

                                        Messages.sendDM(text, ctx.getSource().getSender(), "core", status);
                                        return 1;
                                    })
                            )
                            .build()
            );
        });
    }
}
