package pink.zak.minecraft.blockcommands.listener;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import pink.zak.minecraft.blockcommands.BlockCommandsPlugin;
import pink.zak.minecraft.blockcommands.model.BlockCommand;
import pink.zak.minecraft.blockcommands.model.CustomBlock;
import pink.zak.minecraft.blockcommands.repository.Repository;
import pink.zak.minecraft.blockcommands.utils.Chat;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BlockClickListener implements Listener {
    private final Repository repository;

    public BlockClickListener(@NotNull Repository repository) {
        this.repository = repository;
    }

    @EventHandler
    public void onBlockClick(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) return;
        if (event.getHand() == EquipmentSlot.OFF_HAND) return; // event will trigger for both

        CustomBlock customBlock;
        try {
            customBlock = this.repository.GetCustomBlock(clickedBlock)
                    .get(50, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }

        Player player = event.getPlayer();

        if (customBlock.settings().cancelInteract()) {
            event.setCancelled(true);

            if (action == Action.LEFT_CLICK_BLOCK && player.hasPermission("blockcommands.notifybreakinteract")) {
                player.sendMessage(Chat.fmt("&cYou cannot break this block. Use /blockcommand set cancelinteract false to disable this."));
            }
        }

        for (BlockCommand command : customBlock.commands()) {
            if (command.clickType() == BlockCommand.BlockClickType.LEFT && action != Action.LEFT_CLICK_BLOCK) continue;
            if (command.clickType() == BlockCommand.BlockClickType.RIGHT && action != Action.RIGHT_CLICK_BLOCK)
                continue;

            CommandSender sender = command.execType() == BlockCommand.ExecType.CONSOLE ? Bukkit.getConsoleSender() : player;

            String commandStr = command.command();
            if (BlockCommandsPlugin.papiEnabled()) commandStr = PlaceholderAPI.setPlaceholders(player, commandStr);

            Bukkit.dispatchCommand(sender, commandStr);
        }
    }
}
