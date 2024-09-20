package link.locutus.discord.api.generated;

import link.locutus.discord.commands.manager.v2.binding.annotation.Command;

public enum TreatyType {
    NONE(0),
    MDPAP(12, "MDP", "#ced518"),
    MDOAP(11, "MDoAP", "#c10007"),
    MNDAP(10, "MnDAP", "#ff0000"),
    MNDOAP(9, "MnDoAP", "#ff0000"),
    MDP(8, "MDP", "#ced518"),
    MNDP(7, "MnDP", "#ff0000"),
    PROTECTORATE(6, "Protectorate", "#65c765"),
    ODOAP(5, "ODoAP", "#0d2572"),
    ODP(4, "ODP", "#00aeef"),
    PIAT(3, "PIAT", "#ffb6c1"),
    TOA(2, "TOA", "#ffb6c1"),
    NAP(1, "NAP", "#ffb6c1"),

        ;

    public static final TreatyType[] values = values();

    private final int strength;
    private final String id;
    private final String color;

    TreatyType(int strength) {
        this.strength = strength;
        this.id = name();
        this.color = null;
    }

    TreatyType(int strength, String id, String color) {
        this.strength = strength;
        this.id = id;
        this.color = color;
    }

    @Command(desc = "Get the name of the treaty.")
    public String getName() {
        return id;
    }

    @Command(desc = "Hex Color of treaty")
    public String getColor() {
        return color;
    }

    public String getId() {
        return id;
    }

    @Command(desc = "Get the numeric strength of the treaty")
    public int getStrength() {
        return strength;
    }

    @Command(desc = "If this is a defensive treaty")
    public boolean isDefensive() {
        return switch (this) {
            case MDPAP, MDOAP, MNDAP, MNDOAP, MDP, MNDP, PROTECTORATE, ODOAP, ODP -> true;
            default -> false;
        };
    }

    @Command(desc = "If this is a defensive treaty")
    public boolean isMandatoryDefensive() {
        return switch (this) {
            case MDPAP, MDOAP, MNDAP, MNDOAP, MDP, MNDP, PROTECTORATE -> true;
            default -> false;
        };
    }

    @Command(desc = "If this is an offensive treaty")
    public boolean isOffensive() {
        return switch (this) {
            case MDPAP, MDOAP, MNDAP, MNDOAP, ODOAP -> true;
            default -> false;
        };
    }

    public static TreatyType parse(String arg) {
        return TreatyType.valueOf(arg.toUpperCase());
    }

    public boolean isOptional() {
        return switch (this) {
            case ODOAP, ODP, PIAT, TOA, NAP -> true;
            default -> false;
        };
    }
}
