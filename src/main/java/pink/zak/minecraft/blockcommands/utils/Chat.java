package pink.zak.minecraft.blockcommands.utils;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

public class Chat {

    public static String fmt(@NotNull String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public static String fmt(@NotNull String msg, Object... args) {
        return fmt(msg).formatted(args);
    }
}
