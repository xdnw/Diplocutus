package link.locutus.discord.api.generated;

public enum WarType {
    ANNIHILATION_WAR,
    CONQUEST_WAR,
    NORMAL_WAR,
    RAID_WAR,
    ;

    public static WarType[] values = values();

    public static WarType parse(String warType) {
        try {
            return valueOf(warType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
