package win.stvworlds.sTVlobby.functions.msg;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import win.stvworlds.sTVlobby.functions.PluginFunction;
import win.stvworlds.sTVlobby.utils.Messages;
import win.stvworlds.sTVlobby.utils.suggestions.AllPlayersSuggestions;

import java.util.List;

public class MsgCommand implements PluginFunction {

    private final JavaPlugin plugin;

    public MsgCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private static Component playerInfo(CommandSender sender, TextColor color) {
        if (!(sender instanceof Player player)) {
            return Component.text("Консоль").color(color);
        }

        Component text = Component.empty()
                .append(Component.text("✎ ")
                        .color(color))
                .append(Component.text("Натисни, ")
                        .color(TextColor.fromHexString("#AAAAAA")))
                .append(Component.text("щоб надіслати повідомлення.")
                        .color(color)
                )
                .append(Component.newline())
                .append(Component.newline())
                .append(Component
                        .text("☠ ⏸ " + PlaceholderAPI.setPlaceholders(player, "%statistic_DEATHS%"))
                        .color(TextColor.fromHexString("#AAAAAA"))
                )
                .append(Component.newline())
                .append(Component
                        .text("⌚ ⏸ " + PlaceholderAPI.setPlaceholders(player, "%statistic_hours_played%") + "г.")
                        .color(TextColor.fromHexString("#AAAAAA"))
                );

        return Component.text(sender.getName()).color(color)
                .clickEvent(ClickEvent.suggestCommand(sender.getName()))
                .hoverEvent(HoverEvent.showText(text));
    }

    private static Component copyMsg(TextColor color) {
        return Component.empty()
                .append(Component
                        .text("✎ ")
                        .color(color)
                )
                .append(Component
                        .text("Натисни, ")
                        .color(TextColor.fromHexString("#AAAAAA"))
                )
                .append(Component
                        .text("щоб скопіювати.")
                        .color(color)
                );
    }

    private static Component parseMsg(String message) {
        return MiniMessage.miniMessage().deserialize(message);
    }

    @Override
    public void register() {

        MiniMessage mm = MiniMessage.miniMessage();
        String usageMSG = "<gray>/</gray><i>msg</i> <gray>[</gray><aqua>player</aqua><gray>]</gray> <gray>[</gray><yellow><</yellow><aqua>message</aqua><yellow>></yellow><gray>]</gray>";
        Component usageText = Component.text("Використання ").append(mm.deserialize(usageMSG));



        TextColor nameColor = TextColor.fromHexString("#5DADE2");
        TextColor selfColor = TextColor.fromHexString("#58D68D");
        TextColor arrowColor = TextColor.fromHexString("#888888");
        TextColor msgColor = TextColor.fromHexString("#F0F0F0");


        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final var registrar = event.registrar();
            registrar
                    .register(Commands
                                    .literal("msg")
                                    .executes(ctx -> {
                                        Messages.sendDM(usageText, ctx.getSource().getSender(), "msg", "warn");
                                        return 1;
                                    })
                                    .then(Commands.argument("player", StringArgumentType.word()).suggests(AllPlayersSuggestions.suggestion())
                                            .executes(ctx -> {
                                                        Messages.sendDM(usageText, ctx.getSource().getSender(), "msg", "warn");
                                                        return 1;
                                                    }
                                            )
                                            .then(Commands.argument("message", StringArgumentType.greedyString())
                                                    .executes(ctx -> {
                                                        CommandSender sender = ctx.getSource().getSender();
                                                        String receiverName = StringArgumentType.getString(ctx, "player");
                                                        String message = StringArgumentType.getString(ctx, "message");
                                                        OfflinePlayer offlineReceiver = Bukkit.getOfflinePlayerIfCached(receiverName);
                                                        Player receiver = offlineReceiver != null ? offlineReceiver.getPlayer() : null;

                                                        if (receiver == null || !receiver.isOnline()) {
                                                            Messages.sendDM(
                                                                    Component.text("Гравець ").color(arrowColor)
                                                                            .append(Component.text(receiverName).color(nameColor))
                                                                            .append(Component.text(" наразі офлайн.").color(arrowColor)),
                                                                    sender, "msg", "warn"
                                                            );
                                                            return 1;
                                                        }

                                                        Component result = Component.text(message);
                                                        if (sender.hasPermission("stv.sponsor")) {
                                                            result = parseMsg(message);
                                                        }

                                                        Messages.sendDM(Component.empty()
                                                                        .append(playerInfo(sender, nameColor))
                                                                        .append(Component.text(" ⏵ ").color(arrowColor))
                                                                        .append(Component.text("Ви").color(selfColor))
                                                                        .append(Component.text(": ").color(arrowColor))
                                                                        .append(result
                                                                                .color(msgColor)
                                                                                .clickEvent(ClickEvent.suggestCommand(message))
                                                                                .hoverEvent(copyMsg(msgColor))
                                                                        ),
                                                                receiver, "msg", "info"
                                                        );
                                                        Component senderLabel = (sender instanceof Player)
                                                                ? Component.text("Ви").color(selfColor)
                                                                : Component.text("Консоль").color(nameColor);
                                                        Messages.sendDM(Component.empty()
                                                                        .append(senderLabel)
                                                                        .append(Component.text(" ⏵ ").color(arrowColor))
                                                                        .append(playerInfo(receiver, nameColor))
                                                                        .append(Component.text(": ").color(arrowColor))
                                                                        .append(result
                                                                                .color(msgColor)
                                                                                .clickEvent(ClickEvent.suggestCommand(message))
                                                                                .hoverEvent(copyMsg(msgColor))
                                                                        ),
                                                                sender, "msg", "info"
                                                        );
                                                        return 1;
                                                    })
                                            )
                                    )
                                    .build(),
                            "Приватні повідомлення.",
                            List.of("tell", "w", "pm", "t", "message")
                    );
        });
    }
}
