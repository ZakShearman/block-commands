package pink.zak.minecraft.blockcommands.listener;

import com.jeff_media.customblockdata.CustomBlockData;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import pink.zak.minecraft.blockcommands.BlockCommandsPlugin;
import pink.zak.minecraft.blockcommands.model.BlockCommand;
import pink.zak.minecraft.blockcommands.utils.CustomDataTypes;

public class BlockClickListener implements Listener {
    private final BlockCommandsPlugin plugin;
    private final NamespacedKey dataKey;

    public BlockClickListener(@NotNull BlockCommandsPlugin plugin) {
        this.plugin = plugin;
        this.dataKey = plugin.getDataKey();
    }

    @EventHandler
    public void onBlockClick(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) return;
        if (event.getHand() == EquipmentSlot.OFF_HAND) return; // event will trigger for both

        PersistentDataContainer container = new CustomBlockData(clickedBlock, this.plugin);
        BlockCommand[] commands = container.get(this.dataKey, CustomDataTypes.BLOCK_COMMAND);
        if (commands == null) return;

        Player player = event.getPlayer();

        for (BlockCommand command : commands) {
            if (command.clickType() == BlockCommand.BlockClickType.LEFT && action != Action.LEFT_CLICK_BLOCK) continue;
            if (command.clickType() == BlockCommand.BlockClickType.RIGHT && action != Action.RIGHT_CLICK_BLOCK) continue;

            CommandSender sender = command.execType() == BlockCommand.ExecType.CONSOLE ? Bukkit.getConsoleSender() : player;

            String commandStr = command.command();
            if (BlockCommandsPlugin.papiEnabled()) commandStr = PlaceholderAPI.setPlaceholders(player, commandStr);

            Bukkit.dispatchCommand(sender, commandStr);
        }
    }
}
