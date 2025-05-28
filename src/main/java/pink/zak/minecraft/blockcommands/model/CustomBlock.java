package pink.zak.minecraft.blockcommands.model;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public record CustomBlock(int x, int y, int z, @NotNull UUID worldId, @NotNull BlockSettings settings,
                          @NotNull List<BlockCommand> commands) {

}
