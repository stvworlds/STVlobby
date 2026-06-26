package win.stvworlds.sTVlobby.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;

public class Messages {
    public static void sendDM(Component text, CommandSender sender, String func, String status) {

        String color = switch (status) {
            case "info" -> "#6F27F5";
            case "warn" -> "#F5E727";
            case "error" -> "#FF4400";
            default -> "#CCCCCC";
        };


        MessageFunction.Info info = MessageFunction.Info.fromName(func);

        String symbol = info != null ? info.getSymbol() : "?";
        String description = info != null ? info.getDescription() : "Невідома команда";

        Component prefix = Component.empty()
                .append(Component
                        .text("[")
                        .color(TextColor.fromHexString("#EBEBEB"))
                )
                .append(Component
                        .text(symbol)
                        .color(TextColor.fromHexString(color))
                )
                .append(Component
                        .text("]")
                        .color(TextColor.fromHexString("#EBEBEB"))
                )
                .hoverEvent(HoverEvent.showText(Component.text(description)));

        Component message = Component.empty()
                .append(prefix)
                .append(Component.text(" | ").color(TextColor.fromHexString(color)))
                .append(text);

        sender.sendMessage(message);
    }

}
