package pink.zak.minecraft.blockcommands.commands;

import com.jeff_media.customblockdata.CustomBlockData;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pink.zak.minecraft.blockcommands.BlockCommandsPlugin;
import pink.zak.minecraft.blockcommands.model.BlockCommand;
import pink.zak.minecraft.blockcommands.utils.Chat;
import pink.zak.minecraft.blockcommands.utils.CustomDataTypes;
import pink.zak.minecraft.blockcommands.utils.Pair;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.DefaultFor;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.Arrays;
import java.util.function.Function;

// I hate how this is named, but I have a model called BlockCommand :\
@Command("blockcommand")
@CommandPermission("blockcommands.command")
public class BlockCommandCommand {
    private static final String NO_BLOCK = Chat.fmt("&cYou must be looking at a block to use &n%s");
    private static final String ALREADY_SIMILAR_COMMAND = Chat.fmt("&cThis block already has a similar command: &n%s");
    private static final String COMMANDS_EMPTY = Chat.fmt("&cThis block has no commands.");
    private static final String INDEX_TOO_SMALL = Chat.fmt("&cIndex input must be 1 or greater.");
    private static final Function<Integer, String> INDEX_TOO_LARGE = input -> Chat.fmt("&cIndex must be smaller than " + input);
    private static final Function<String, String> COMMAND_NOT_MATCHED = input -> Chat.fmt("&cCouldn't match the command '%s'".formatted(input));

    private final BlockCommandsPlugin plugin;
    private final NamespacedKey dataKey;

    public BlockCommandCommand(@NotNull BlockCommandsPlugin plugin) {
        this.plugin = plugin;
        this.dataKey = plugin.getDataKey();
    }

    @Subcommand({"help"})
    @DefaultFor("blockcommand")
    @CommandPermission("blockcommands.command")
    public void help(@NotNull Player sender) {
        sender.sendMessage(Chat.fmt("&a/blockcommand add <command> &7- Add a command to the block you're looking at"));
        sender.sendMessage(Chat.fmt("&a/blockcommand remove <index> &7- Remove a command from the block you're looking at - get the index from /blockcommand list"));
        sender.sendMessage(Chat.fmt("&a/blockcommand list &7- List all commands for the block you're looking at"));
    }

    @Subcommand("add")
    @CommandPermission("blockcommands.command.add")
    public void add(@NotNull Player sender, @NotNull BlockCommand.ExecType execType,
                    @NotNull BlockCommand.BlockClickType clickType, @NotNull String inputCommand) {

        Block targetBlock = sender.getTargetBlock(null, 5);
        if (targetBlock.isEmpty()) {
            sender.sendMessage(NO_BLOCK.formatted("/blockcommand add"));
            return;
        }

        // Remove the slash from the start if it's there
        String commandWithoutSlash = inputCommand.startsWith("/") ? inputCommand.substring(1) : inputCommand;

        PersistentDataContainer container = new CustomBlockData(targetBlock, this.plugin);
        BlockCommand[] currentCommands = container.getOrDefault(this.dataKey, CustomDataTypes.BLOCK_COMMAND, new BlockCommand[0]);

        for (BlockCommand currentCommand : currentCommands) {
            if (currentCommand.command().equalsIgnoreCase(commandWithoutSlash)) {
                sender.sendMessage(ALREADY_SIMILAR_COMMAND.formatted(inputCommand));
                return;
            }
        }

        BlockCommand[] newCommands = Arrays.copyOf(currentCommands, currentCommands.length + 1);
        newCommands[newCommands.length - 1] = new BlockCommand(execType, clickType, commandWithoutSlash);

        container.set(this.dataKey, CustomDataTypes.BLOCK_COMMAND, newCommands);
        sender.sendMessage(Chat.fmt("&aSuccessfully added command &n%s&r &ato block. It now has %s commands", inputCommand, newCommands.length));
    }

    // input can be either an index (1+) or an exact match of the command
    @Subcommand("remove")
    @CommandPermission("blockcommands.command.remove")
    public void remove(@NotNull Player sender, String input) {
        Block targetBlock = sender.getTargetBlock(null, 5);
        if (targetBlock.isEmpty()) {
            sender.sendMessage(NO_BLOCK.formatted("/blockcommand remove"));
            return;
        }

        PersistentDataContainer container = new CustomBlockData(targetBlock, this.plugin);
        BlockCommand[] currentCommands = container.getOrDefault(this.dataKey, CustomDataTypes.BLOCK_COMMAND, new BlockCommand[0]);
        int currentCommandsLen = currentCommands.length;

        if (currentCommandsLen == 0) {
            sender.sendMessage(COMMANDS_EMPTY);
            return;
        }

        int index;
        try {
            index = Integer.parseInt(input) - 1;
            if (index < 0) {
                sender.sendMessage(INDEX_TOO_SMALL);
                return;
            } else if (index > currentCommandsLen) {
                sender.sendMessage(INDEX_TOO_LARGE.apply(currentCommandsLen));
            }
        } catch (NumberFormatException ex) {
            // find an exact match of the command if possible
            Pair<Integer, BlockCommand> matchedCommand = this.findByCommand(currentCommands, input);
            if (matchedCommand == null) {
                sender.sendMessage(COMMAND_NOT_MATCHED.apply(input));
                return;
            }

            index = matchedCommand.getLeft();
        }

        this.removeCommandAtIndex(sender, currentCommands, container, index);
    }

    @Subcommand("list")
    @CommandPermission("blockcommands.command.list")
    public void list(@NotNull Player sender) {
        Block targetBlock = sender.getTargetBlock(null, 5);
        Location targetBlockLoc = targetBlock.getLocation();
        if (targetBlock.isEmpty()) {
            sender.sendMessage(NO_BLOCK.formatted("/blockcommand list"));
            return;
        }

        PersistentDataContainer container = new CustomBlockData(targetBlock, this.plugin);
        BlockCommand[] currentCommands = container.getOrDefault(this.dataKey, CustomDataTypes.BLOCK_COMMAND, new BlockCommand[0]);

        if (currentCommands.length == 0) {
            sender.sendMessage(COMMANDS_EMPTY);
            return;
        }

        Location blockLoc = targetBlock.getLocation();
        ComponentBuilder componentBuilder = new ComponentBuilder()
                .append("Commands for block %s (x: %s, y: %s, z: %s)".formatted(targetBlock.getBlockData().getMaterial().name(),
                        blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ())).color(ChatColor.GREEN);

        for (int i = 0; i < currentCommands.length; i++) {
            BlockCommand command = currentCommands[i];

            TextComponent clickHereComponent = new TextComponent("remove");
            String clickCommand = "/blockcommand listremove %s %s %s %s"
                    .formatted(targetBlockLoc.getBlockX(), targetBlockLoc.getBlockY(), targetBlockLoc.getBlockZ(), "\"%s\"".formatted(command.command()));

            componentBuilder.append("\n")
                    .append("%s ".formatted(i))
                    .append("[")

                    .append(clickHereComponent).color(ChatColor.RED).underlined(true).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, clickCommand))
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(clickCommand)))

                    .append("]:").reset().color(ChatColor.GREEN);

            componentBuilder.append("\n  Command: /" + command.command())
                    .append("\n  Click Type: " + command.clickType().friendlyName())
                    .append("\n  Exec As: " + command.execType());
        }

        sender.spigot().sendMessage(componentBuilder.build());
    }

    @Subcommand("listremove")
    @CommandPermission("blockcommands.command.remove")
    public void listRemove(@NotNull Player sender, int x, int y, int z, @NotNull String command) {
        Block block = sender.getWorld().getBlockAt(x, y, z);
        if (block.isEmpty()) {
            sender.sendMessage(Chat.fmt("&cCouldn't find block at x: %s, y: %s, z: %s".formatted(x, y, z)));
            return;
        }

        PersistentDataContainer container = new CustomBlockData(block, this.plugin);
        BlockCommand[] currentCommands = container.getOrDefault(this.dataKey, CustomDataTypes.BLOCK_COMMAND, new BlockCommand[0]);

        if (currentCommands.length == 0) {
            sender.sendMessage(COMMANDS_EMPTY);
            return;
        }

        Pair<Integer, BlockCommand> foundCommand = this.findByCommand(currentCommands, command);
        if (foundCommand == null) {
            sender.sendMessage(COMMAND_NOT_MATCHED.apply(command));
            return;
        }

        this.removeCommandAtIndex(sender, currentCommands, container, foundCommand.getLeft());
    }

    private void removeCommandAtIndex(@NotNull Player sender, @NotNull BlockCommand[] currentCommands,
                                      @NotNull PersistentDataContainer container, int index) {

        BlockCommand removedCommand = currentCommands[index];
        currentCommands[index] = currentCommands[currentCommands.length - 1];
        BlockCommand[] newCommands = Arrays.copyOf(currentCommands, currentCommands.length - 1);

        if (newCommands.length == 0) {
            container.remove(this.dataKey);
        } else {
            container.set(this.dataKey, CustomDataTypes.BLOCK_COMMAND, newCommands);
        }

        sender.sendMessage(Chat.fmt("&aSuccessfully removed %s command &n/%s&r &afrom block. It now has %s commands",
                removedCommand.clickType().friendlyName(), removedCommand.command(), newCommands.length));

    }

    private @Nullable Pair<Integer, BlockCommand> findByCommand(@NotNull BlockCommand[] commands, @NotNull String commandInput) {
        for (int i = 0; i < commands.length; i++) {
            BlockCommand command = commands[i];
            if (command.command().equalsIgnoreCase(commandInput)) return new Pair<>(i, command);
        }

        return null;
    }
}
