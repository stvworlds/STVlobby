package win.stvworlds.sTVlobby;

import org.bukkit.plugin.java.JavaPlugin;
import win.stvworlds.sTVlobby.functions.PluginFunction;
import win.stvworlds.sTVlobby.functions.core.CoreCommand;
import win.stvworlds.sTVlobby.functions.link.LinkCommand;
import win.stvworlds.sTVlobby.functions.msg.MsgCommand;

import java.util.List;

public final class STVlobby extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        List<PluginFunction> functions = List.of(
            new CoreCommand(this),
            new LinkCommand(this),
            new MsgCommand(this)
        );
        functions.forEach(PluginFunction::register);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
