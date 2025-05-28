package pink.zak.minecraft.blockcommands.model;

import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public record CustomBlock(int x, int y, int z, @NotNull UUID worldId, @NotNull BlockSettings settings,
                          @NotNull List<BlockCommand> commands) {

    public CustomBlock(@NotNull  Block block, @NotNull BlockSettings settings, @NotNull List<BlockCommand> commands) {
        this(block.getX(), block.getY(), block.getZ(), block.getWorld().getUID(), settings, commands);
    }

}
