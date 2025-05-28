package pink.zak.minecraft.blockcommands.listener;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;
import pink.zak.minecraft.blockcommands.BlockCommandsPlugin;
import pink.zak.minecraft.blockcommands.repository.Repository;

public class BlockBreakListener implements Listener {
    private final @NotNull BlockCommandsPlugin plugin;
    private final @NotNull Repository repository;

    public BlockBreakListener(@NotNull BlockCommandsPlugin plugin, @NotNull Repository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        // DeleteCustomBlock will result in a cascade delete of all the block_command associated with this block
        this.repository.DeleteCustomBlock(block)
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    return null;
                });

        this.repository.DeleteBlockCommands(block)
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    return null;
                });
    }
}
