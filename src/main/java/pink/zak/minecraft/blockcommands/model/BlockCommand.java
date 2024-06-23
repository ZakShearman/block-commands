package pink.zak.minecraft.blockcommands.model;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record BlockCommand(@NotNull ExecType execType, @NotNull BlockClickType clickType, @NotNull String command) implements ConfigurationSerializable {

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        return Map.of(
                "command", this.command,
                "execType", this.execType.ordinal(),
                "clickType", this.clickType.ordinal()
        );
    }

    public static BlockCommand deserialize(@NotNull Map<String, Object> map) {
        return new BlockCommand(
                ExecType.values()[(int) map.get("execType")],
                BlockClickType.values()[(int) map.get("clickType")],
                (String) map.get("command")
        );
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
