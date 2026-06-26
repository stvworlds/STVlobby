package win.stvworlds.sTVlobby.utils.suggestions;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class AllPlayersSuggestions {

    public static SuggestionProvider<CommandSourceStack> suggestion() {
        return (ctx, builder) -> {
            String input = builder.getRemaining().toLowerCase();
            for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                String name = op.getName();
                if (name != null && name.toLowerCase().startsWith(input)) {
                    builder.suggest(name);
                }
            }
            return builder.buildFuture();
        };
    }

}
