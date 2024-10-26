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
import revxrsal.commands.Lamp;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.node.ExecutionContext;
import revxrsal.commands.parameter.ParameterType;
import revxrsal.commands.stream.MutableStringStream;

import java.util.Arrays;

public final class BlockCommandsPlugin extends JavaPlugin {
    private static boolean PAPI_ENABLED;

    private final NamespacedKey commandDataKey = new NamespacedKey(this, "blockCommandData");
    private final NamespacedKey cancelInteractDataKey = new NamespacedKey(this, "cancelInteractData");

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
        Lamp<BukkitCommandActor> lamp = BukkitLamp.builder(this)
                .parameterTypes(builder -> {
                    builder.addParameterType(BlockCommand.BlockClickType.class, new ParameterType<BukkitCommandActor, BlockCommand.BlockClickType>() {
                        @Override
                        public BlockCommand.BlockClickType parse(@NotNull MutableStringStream inputStream, @NotNull ExecutionContext<@NotNull BukkitCommandActor> context) {
                            String input = inputStream.readString();
                            return BlockCommand.BlockClickType.fromIdentifier(input);
                        }
                    });
                })
                .suggestionProviders(providers -> {
                    providers.addProvider(BlockCommand.BlockClickType.class, context -> {
                        return Arrays.stream(BlockCommand.BlockClickType.values()).map(BlockCommand.BlockClickType::identifier).toList();
                    });
                })
                .build();

        lamp.register(
                new BlockCommandCommand(this)
        );
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

    public NamespacedKey getCancelInteractDataKey() {
        return this.cancelInteractDataKey;
    }
}
