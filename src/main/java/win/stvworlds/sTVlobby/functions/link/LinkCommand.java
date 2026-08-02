package win.stvworlds.sTVlobby.functions.link;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import win.stvworlds.sTVlobby.functions.PluginFunction;
import win.stvworlds.sTVlobby.utils.Messages;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LinkCommand implements PluginFunction {

    private final JavaPlugin plugin;

    public LinkCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    MiniMessage mm = MiniMessage.miniMessage();
    String usageMSG = "<gray>/</gray><i>link</i> <gray>[</gray><aqua>code</aqua><gray>]</gray>";
    Component usageText = Component.text("Використання ").append(mm.deserialize(usageMSG));

    @Override
    public void register() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final var registrar = event.registrar();

            registrar.register(
                    Commands.literal("link")
                            .executes(ctx -> {
                                Messages.sendDM(usageText, ctx.getSource().getSender(), "msg", "warn");
                                return 1;
                            })
                            .then(Commands.argument("code", StringArgumentType.greedyString())
                                    .executes(this::LinkHandler)
                            )
                            .build()
            );

        });
    }

    private int LinkHandler(CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();
        final String code = StringArgumentType.getString(ctx, "code");

        if (code == null || code.isEmpty()) {
            return 0;
        }

        if (!(sender instanceof Player)) {
            Messages.sendDM(Component.text("Нашо ти то в консоль пишеш"), sender, "msg", "warn");
            return 1;
        }

        Player player = ((Player) sender).getPlayer();
        if (player == null) {
            return 0;
        }


        HttpClient client = HttpClient.newHttpClient();

        String json = String.format("""
                {
                    "code": "%s",
                    "nicknameMC": "%s",
                    "uuidMC": "%s"
                }
                """, code, sender.getName(), player.getUniqueId());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.stvworlds.win/user/check"))
                .header("Content-Type", "application/json")
                .header("X-API-KEY", "6767SixSeven_")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    switch (response.statusCode()) {
                        case 200 -> {
                            Messages.sendDM(
                                    Component.text("Акаунт успішно підключено"),
                                    sender,
                                    "msg",
                                    "success"
                            );
                        }

                        case 400 -> {
                            Messages.sendDM(
                                    Component.text("Некоректні дані"),
                                    sender,
                                    "msg",
                                    "error"
                            );
                        }

                        case 403 -> {
                            Messages.sendDM(
                                    Component.text("Доступ заборонено"),
                                    sender,
                                    "msg",
                                    "error"
                            );
                        }

                        case 404 -> {
                            Messages.sendDM(
                                    Component.text("Код не знайдено"),
                                    sender,
                                    "msg",
                                    "error"
                            );
                        }

                        default -> {
                            System.err.println(response.statusCode());
                            System.err.println(response.body());

                            Messages.sendDM(
                                    Component.text("Сталася невідома помилка"),
                                    sender,
                                    "msg",
                                    "error"
                            );
                        }
                    }
                })
                .exceptionally(e -> {
                    e.printStackTrace();

                    Messages.sendDM(
                            Component.text("Не вдалося з'єднатися із сервером"),
                            sender,
                            "msg",
                            "error"
                    );

                    return null;
                });


        return 1;
    }

}
