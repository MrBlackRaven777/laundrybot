package il.blackraven.klita.orm;

import java.util.HashMap;
import java.util.Map;

public enum MachineTypes {
    wash,
    dry,
    WASH,
    DRY,
    Wash,
    Dry,
    unknown;

    private static final HashMap<MachineTypes, String> typeEmoji = new HashMap<>(Map.of(
            wash, "\uD83D\uDCA7",
            WASH, "\uD83D\uDCA7",
            Wash, "\uD83D\uDCA7",
            dry, "\uD83D\uDCA8",
            DRY, "\uD83D\uDCA8",
            Dry, "\uD83D\uDCA8"
    ));

    public static String getTypeEmoji(MachineTypes type) {
        return typeEmoji.getOrDefault(type, "");
    }
}
