package pink.zak.minecraft.blockcommands.model;

import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record BlockCommand(@NotNull UUID id, int x, int y, int z, @NotNull UUID worldId, @NotNull ExecType execType,
                           @NotNull BlockClickType clickType, @NotNull String command) {

    public BlockCommand(@NotNull Block block, @NotNull ExecType execType, @NotNull BlockClickType clickType, @NotNull String command) {
        this(UUID.randomUUID(), block.getX(), block.getY(), block.getZ(), block.getWorld().getUID(), execType, clickType, command);
    }

    public enum ExecType {
        PLAYER,
        CONSOLE
    }

    public enum BlockClickType {
        LEFT("left_click", "Left Click"),
        RIGHT("right_click", "Right Click"),
        BOTH("left_and_right_click", "Left and Right Click");

        private final @NotNull String identifier;
        private final @NotNull String friendlyName;

        BlockClickType(@NotNull String identifier, @NotNull String friendlyName) {
            this.identifier = identifier;
            this.friendlyName = friendlyName;
        }

        public @NotNull String identifier() {
            return this.identifier;
        }

        public @NotNull String friendlyName() {
            return this.friendlyName;
        }

        public static @Nullable BlockClickType fromIdentifier(@NotNull String identifier) {
            for (BlockClickType clickType : BlockClickType.values()) {
                if (clickType.identifier().equals(identifier)) return clickType;
            }

            return null;
        }
    }
}
