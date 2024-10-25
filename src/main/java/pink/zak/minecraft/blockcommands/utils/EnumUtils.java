package pink.zak.minecraft.blockcommands.utils;

import org.jetbrains.annotations.NotNull;

import java.util.StringJoiner;

public class EnumUtils {
    
    public static String friendlyName(@NotNull Enum<?> e) {
        String name = e.name();
        StringJoiner friendlyName = new StringJoiner(" ");
        for (String word : name.split("_")) {
            String casedName = word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
            friendlyName.add(casedName);
        }

        return friendlyName.toString();
    }
}
