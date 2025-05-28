package pink.zak.minecraft.blockcommands.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pink.zak.minecraft.blockcommands.BlockCommandsPlugin;
import pink.zak.minecraft.blockcommands.model.BlockCommand;
import pink.zak.minecraft.blockcommands.model.BlockSettings;
import pink.zak.minecraft.blockcommands.model.CustomBlock;
import pink.zak.minecraft.blockcommands.repository.Repository;
import pink.zak.minecraft.blockcommands.utils.Chat;
import pink.zak.minecraft.blockcommands.utils.EnumUtils;
import pink.zak.minecraft.blockcommands.utils.Pair;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

// I hate how this is named, but I have a model called BlockCommand :\
@Command("blockcommand")
@CommandPermission("blockcommands.command")
public class BlockCommandCommand {
    private static final String INVALID_BLOCK = Chat.fmt("&cYou cannot add commands to a liquid. Please look at a solid block.");
    private static final String ALREADY_SIMILAR_COMMAND = Chat.fmt("&cThis block already has a similar command: &n%s");
    private static final String COMMANDS_EMPTY = Chat.fmt("&cThis block has no commands.");
    private static final String INDEX_TOO_SMALL = Chat.fmt("&cIndex input must be 1 or greater.");
    private static final String NO_BOUND_COMMANDS = Chat.fmt("&cYou must add a command before setting a block's properties");
    private static final Function<Integer, String> INDEX_TOO_LARGE = input -> Chat.fmt("&cIndex must be smaller than " + input);
    private static final Function<String, String> COMMAND_NOT_MATCHED = input -> Chat.fmt("&cCouldn't match the command '%s'".formatted(input));
    private static final Function<String, String> NO_BLOCK = command -> Chat.fmt("&cYou must be looking at a block to use &n" + command);

    private final BlockCommandsPlugin plugin;
    private final Repository repository;
    private final Logger logger;

    private final NamespacedKey commandDataKey;
    private final NamespacedKey cancelInteractDataKey;

    public BlockCommandCommand(@NotNull BlockCommandsPlugin plugin, @NotNull Repository repository) {
        this.plugin = plugin;
        this.repository = repository;
        this.logger = plugin.getLogger();
        this.commandDataKey = plugin.getCommandDataKey();
        this.cancelInteractDataKey = plugin.getCancelInteractDataKey();
    }

    @Command("blockcommand")
    @CommandPermission("blockcommands.command")
    public void root(@NotNull Player sender) {
        this.help(sender);
    }

    @Subcommand({"help"})
    @Usage("blockcommand")
    @CommandPermission("blockcommands.command")
    public void help(@NotNull Player sender) {
        sender.sendMessage(Chat.fmt("&a/blockcommand info &7- List the commands for the block you're looking at"));
        sender.sendMessage(Chat.fmt("&a/blockcommand add <console/player> <left/right_click> <command> &7- Add a command to the block you're looking at"));
        sender.sendMessage(Chat.fmt("&a/blockcommand remove <index> &7- Remove a command from the block you're looking at - get the index from /blockcommand info"));
        sender.sendMessage(Chat.fmt("&a/blockcommand set cancelinteract <true/false> &7- Set whether the block should cancel interactions (e.g. sign edit)"));
    }

    @Subcommand("add")
    @CommandPermission("blockcommands.command.add")
    public void add(@NotNull Player sender, @NotNull BlockCommand.ExecType execType,
                    @NotNull BlockCommand.BlockClickType clickType, @NotNull String inputCommand) {

        Block targetBlock = sender.getTargetBlock(null, 5);
        if (targetBlock.isEmpty()) {
            sender.sendMessage(NO_BLOCK.apply("/blockcommand add"));
            return;
        }

        if (targetBlock.isLiquid()) {
            sender.sendMessage(INVALID_BLOCK);
            return;
        }

        // Remove the slash from the start if it's there
        String commandWithoutSlash = inputCommand.startsWith("/") ? inputCommand.substring(1) : inputCommand;

        this.repository.ListBlockCommands(targetBlock)
                .thenAcceptAsync(blockCommands -> {
                    for (BlockCommand blockCommand : blockCommands) {
                        if (blockCommand.command().equalsIgnoreCase(commandWithoutSlash)) {
                            sender.sendMessage(ALREADY_SIMILAR_COMMAND.formatted(inputCommand));
                            return;
                        }
                    }

                    BlockCommand newCommand = new BlockCommand(targetBlock, execType, clickType, commandWithoutSlash);

                    this.repository.CreateDefaultCustomBlockIfNotExists(new CustomBlock(
                            targetBlock.getX(), targetBlock.getY(), targetBlock.getZ(), targetBlock.getWorld().getUID(),
                            new BlockSettings(false), List.of()
                    )).exceptionally(throwable -> {
                        sender.sendMessage(Chat.fmt("&cFailed to create custom block for %s. Check the console for more details".formatted(EnumUtils.friendlyName(targetBlock.getType()))));
                        throwable.printStackTrace();
                        return null;
                    });
                    this.repository.CreateBlockCommand(newCommand).thenAccept(unused -> {
                        sender.sendMessage(Chat.fmt("&aSuccessfully added command &n%s&r &ato %s. It now has %s commands",
                                inputCommand, EnumUtils.friendlyName(targetBlock.getType()), blockCommands.size() + 1));
                    }).exceptionally(throwable -> {
                        sender.sendMessage(Chat.fmt("&cFailed to add command '%s'. Check the console for more details".formatted(throwable.getMessage())));
                        throwable.printStackTrace();
                        return null;
                    });
                });
    }

    // input can be either an index (1+) or an exact match of the command
    @Subcommand("remove")
    @CommandPermission("blockcommands.command.remove")
    public void remove(@NotNull Player sender, String input) {
        Block targetBlock = sender.getTargetBlock(null, 5);
        if (targetBlock.isEmpty()) {
            sender.sendMessage(NO_BLOCK.apply("/blockcommand remove"));
            return;
        }

        this.repository.ListBlockCommands(targetBlock).thenAcceptAsync(blockCommands -> {
            int currentCommandsLen = blockCommands.size();
            if (blockCommands.isEmpty()) {
                sender.sendMessage(COMMANDS_EMPTY);
                return;
            }

            BlockCommand matchedCommand;
            try {
                int index = Integer.parseInt(input) - 1;
                if (index < 0) {
                    sender.sendMessage(INDEX_TOO_SMALL);
                    return;
                } else if (index > currentCommandsLen) {
                    sender.sendMessage(INDEX_TOO_LARGE.apply(currentCommandsLen));
                }
                matchedCommand = blockCommands.get(index);
            } catch (NumberFormatException ex) {
                // find an exact match of the command if possible
                Pair<Integer, BlockCommand> matchedPair = this.findByCommand(blockCommands, input);
                if (matchedPair == null) {
                    sender.sendMessage(COMMAND_NOT_MATCHED.apply(input));
                    return;
                }

                matchedCommand = matchedPair.getRight();
            }

            this.repository.DeleteBlockCommand(matchedCommand.id());
        });
    }

    @Subcommand("info")
    @CommandPermission("blockcommands.command.info")
    public void info(@NotNull Player sender) {
        Block targetBlock = sender.getTargetBlock(null, 5);
        Location targetBlockLock = targetBlock.getLocation();
        if (targetBlock.isEmpty()) {
            sender.sendMessage(NO_BLOCK.apply("/blockcommand info"));
            return;
        }

        this.repository.GetCustomBlock(targetBlock).thenAcceptAsync(customBlock -> {
            if (customBlock.commands().isEmpty()) {
                sender.sendMessage(COMMANDS_EMPTY);
                return;
            }

            Location blockLoc = targetBlock.getLocation();
            ComponentBuilder componentBuilder = new ComponentBuilder()
                    .append("Info for block %s (x: %s, y: %s, z: %s)".formatted(EnumUtils.friendlyName(targetBlock.getType()),
                            blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ())).color(ChatColor.GREEN)
                    .append("\nCancel Interact: " + customBlock.settings().cancelInteract())
                    .append("\nCommands:").color(ChatColor.GREEN);

            int i = 0;
            for (BlockCommand command : customBlock.commands()) {

                TextComponent clickHereComponent = new TextComponent("remove");
                String clickCommand = "/blockcommand removebyid %s".formatted(command.id());

                componentBuilder.append("\n")
                        .append("%s ".formatted(i + 1))
                        .append("[")

                        .append(clickHereComponent).color(ChatColor.RED).underlined(true).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, clickCommand))
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(clickCommand)))

                        .append("]:").reset().color(ChatColor.GREEN);

                componentBuilder.append("\n  Command: /" + command.command())
                        .append("\n  Click Type: " + command.clickType().friendlyName())
                        .append("\n  Exec As: " + EnumUtils.friendlyName(command.execType()));

                i++;
            }

            sender.spigot().sendMessage(componentBuilder.create());
        });
    }

    @Subcommand("removebyid")
    @SecretCommand
    @CommandPermission("blockcommands.command.remove")
    public void removeById(@NotNull Player sender, @NotNull UUID id) {
        this.repository.DeleteBlockCommand(id).thenAccept(unused -> {
            sender.sendMessage(Chat.fmt("&aSuccessfully removed command with ID &n%s&r &afrom the block.".formatted(id)));
        }).exceptionally(throwable -> {
            sender.sendMessage(Chat.fmt("&cFailed to remove command with ID '%s'. Check the console for more details".formatted(id)));
            throwable.printStackTrace();
            return null;
        });
    }

    @Subcommand("set cancelinteract")
    @CommandPermission("blockcommands.command.set.cancelinteract")
    public void cancelInteract(Player sender, boolean value) {
        Block block = sender.getTargetBlock(null, 5);

        if (block.isEmpty()) {
            sender.sendMessage(NO_BLOCK.apply("/blockcommand set cancelinteract"));
            return;
        }

        if (block.isLiquid()) {
            sender.sendMessage(INVALID_BLOCK);
            return;
        }

        // Note this does not set the cancel interact setting, the next db request does.
        this.repository.CreateDefaultCustomBlockIfNotExists(new CustomBlock(block, new BlockSettings(value), List.of()))
                .exceptionally(throwable -> {
                    sender.sendMessage(Chat.fmt("&cFailed to create custom block for %s. Check the console for more details".formatted(EnumUtils.friendlyName(block.getType()))));
                    throwable.printStackTrace();
                    return null;
                });

        this.repository.UpdateBlockSettingCancelInteract(block, value).thenAccept(unused -> {
            sender.sendMessage(Chat.fmt("&aSuccessfully set cancel interact for %s to %s.".formatted(EnumUtils.friendlyName(block.getType()), value)));
        }).exceptionally(throwable -> {
            sender.sendMessage(Chat.fmt("&cFailed to set cancel interact for %s. Check the console for more details".formatted(EnumUtils.friendlyName(block.getType()))));
            throwable.printStackTrace();
            return null;
        });
    }

    @Subcommand("list")
    @CommandPermission("blockcommands.command.list")
    public void list(@NotNull Player sender, @Default("1") int page) {
        if (page < 1) {
            sender.sendMessage(Chat.fmt("&cPage number must be 1 or greater."));
            return;
        }

        this.repository.ListAllCustomBlocks(page, 10).thenAcceptAsync(result -> {
            if (result.isEmpty()) {
                sender.sendMessage(Chat.fmt("&cNo block commands found."));
                return;
            }

            sender.sendMessage(Chat.fmt("&aBlock Commands (Page %s):", page));
            for (CustomBlock customBlock : result) {
                String commands = customBlock.commands().stream()
                        .map(command -> "\n  - " + command.command() + " (" + command.clickType().friendlyName() + ")")
                        .collect(Collectors.joining());

                World world = Bukkit.getWorld(customBlock.worldId());
                sender.sendMessage(Chat.fmt("&eBlock at [%s, %s, %s] in %s: %s", customBlock.x(), customBlock.y(), customBlock.z(),
                        world == null ? "null" : world.getName(), commands));
            }
        }).exceptionally(throwable -> {
            sender.sendMessage(Chat.fmt("&cFailed to list block commands. Check the console for more details"));
            throwable.printStackTrace();
            return null;
        });
    }

    private @Nullable Pair<Integer, BlockCommand> findByCommand(@NotNull List<BlockCommand> commands, @NotNull String commandInput) {
        for (int i = 0; i < commands.size(); i++) {
            BlockCommand command = commands.get(i);
            if (command.command().equalsIgnoreCase(commandInput)) return new Pair<>(i, command);
        }

        return null;
    }
}
