package pink.zak.minecraft.blockcommands.repository;

import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import pink.zak.minecraft.blockcommands.model.BlockCommand;
import pink.zak.minecraft.blockcommands.model.CustomBlock;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Repository {
    void shutdown();

    CompletableFuture<Void> CreateDefaultCustomBlockIfNotExists(@NotNull CustomBlock block);

    CompletableFuture<List<CustomBlock>> ListAllCustomBlocks(int page, int pageSize);

    default CompletableFuture<Void> DeleteCustomBlock(@NotNull Block block) {
        return this.DeleteCustomBlock(block.getX(), block.getY(), block.getZ(), block.getWorld().getUID());
    }

    CompletableFuture<Void> DeleteCustomBlock(int x, int y, int z, UUID worldId);

    default CompletableFuture<CustomBlock> GetCustomBlock(Block block) {
        return this.GetCustomBlock(block.getX(), block.getY(), block.getZ(), block.getWorld().getUID());
    }

    CompletableFuture<CustomBlock> GetCustomBlock(int x, int y, int z, UUID worldId);

    CompletableFuture<Void> CreateBlockCommand(@NotNull BlockCommand blockCommand);

    default CompletableFuture<List<BlockCommand>> ListBlockCommands(Block block) {
        return this.ListBlockCommands(block.getX(), block.getY(), block.getZ(), block.getWorld().getUID());
    }

    CompletableFuture<List<BlockCommand>> ListBlockCommands(int x, int y, int z, UUID worldId);

    CompletableFuture<Void> DeleteBlockCommand(UUID id);

    default CompletableFuture<Void> DeleteBlockCommands(Block block) {
        return this.DeleteBlockCommands(block.getX(), block.getY(), block.getZ(), block.getWorld().getUID().toString());
    }

    CompletableFuture<Void> DeleteBlockCommands(int x, int y, int z, String worldId);

    // Settings

    default CompletableFuture<Void> UpdateBlockSettingCancelInteract(Block block, boolean cancelInteract) {
        return this.UpdateBlockSettingCancelInteract(block.getX(), block.getY(), block.getZ(), block.getWorld().getUID().toString(), cancelInteract);
    }

    CompletableFuture<Void> UpdateBlockSettingCancelInteract(int x, int y, int z, String worldId, boolean cancelInteract);
}
