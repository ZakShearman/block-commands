package pink.zak.minecraft.blockcommands;

import com.jeff_media.customblockdata.CustomBlockData;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import pink.zak.minecraft.blockcommands.commands.BlockCommandCommand;
import pink.zak.minecraft.blockcommands.listener.BlockClickListener;
import pink.zak.minecraft.blockcommands.model.BlockCommand;
import revxrsal.commands.bukkit.BukkitCommandHandler;
import revxrsal.commands.exception.CommandErrorException;

import java.util.Arrays;

public final class BlockCommandsPlugin extends JavaPlugin {
    private static boolean PAPI_ENABLED;

    private final NamespacedKey commandDataKey = new NamespacedKey(this, "blockCommandData");
    private final NamespacedKey interactPropertyDataKey = new NamespacedKey(this, "interactPropertyCommandData");

    @Override
    public void onEnable() {
        PAPI_ENABLED = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");

        this.registerCommands();
        this.registerListeners();
        this.registerSerializers();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void registerCommands() {
        BukkitCommandHandler handler = BukkitCommandHandler.create(this);

        handler.getAutoCompleter().registerParameterSuggestions(BlockCommand.BlockClickType.class, (list, commandActor, executableCommand) -> {
            return Arrays.stream(BlockCommand.BlockClickType.values()).map(BlockCommand.BlockClickType::identifier).toList();
        });

        handler.registerValueResolver(BlockCommand.BlockClickType.class, context -> {
            String input = context.pop();

            BlockCommand.BlockClickType clickType = BlockCommand.BlockClickType.fromIdentifier(input);
            if (clickType == null) throw new CommandErrorException("'%s' is not a valid click type.".formatted(input));
            return clickType;
        });

        handler.register(
                new BlockCommandCommand(this)
        );

        handler.registerBrigadier();
    }

    private void registerListeners() {
        PluginManager pluginManager = this.getServer().getPluginManager();
        pluginManager.registerEvents(new BlockClickListener(this), this);

        CustomBlockData.registerListener(this);
    }

    private void registerSerializers() {
        ConfigurationSerialization.registerClass(BlockCommand.class);
    }

    public static boolean papiEnabled() {
        return PAPI_ENABLED;
    }

    public @NotNull NamespacedKey getCommandDataKey() {
        return this.commandDataKey;
    }

    public NamespacedKey getInteractPropertyDataKey() {
        return this.interactPropertyDataKey;
    }
}
