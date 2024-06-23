package pink.zak.minecraft.blockcommands.utils;

import com.jeff_media.morepersistentdatatypes.datatypes.serializable.ConfigurationSerializableArrayDataType;
import org.bukkit.persistence.PersistentDataType;
import pink.zak.minecraft.blockcommands.model.BlockCommand;

public class CustomDataTypes {
    public static final PersistentDataType<byte[], BlockCommand[]> BLOCK_COMMAND =
            new ConfigurationSerializableArrayDataType<>(BlockCommand[].class);
}
