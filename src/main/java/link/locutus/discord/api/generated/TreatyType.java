package link.locutus.discord.api.generated;

import link.locutus.discord.commands.manager.v2.binding.annotation.Command;

public enum TreatyType {
    NONE(0),
    //Mutual Defence and Aggression Pact	MDAP	None	Similar to an MDP, but also obliges both parties to support each other in wars of aggression. Typically found between extensions or close allies.
    MDPAP(12, "MDP", "#ced518"),
    //Mutual Defence Optional Aggression Pact	MDoAP	Red	Similar to an MDP, but also provides both parties the option to support each other in wars of aggression.
    MDOAP(11, "MDoAP", "#c10007"),
    //Mutual non-chaining Defence and Aggression Pact	MnDAP	None	Similar to an MDAP, but with an additional clause which prevents treaties with 3rd parties from triggering the MnDAP also.
    MNDAP(10, "MnDAP", "#ff0000"),
    //Mutual non-chaining Defence Optional Aggression Pact	MnDoAP	Light brown	Similar to an MDoAP, but with an additional clause which prevents treaties with 3rd parties from triggering the MnDOAP also. This is the most common type of defence treaty.
    MNDOAP(9, "MnDoAP", "#ff0000"),
    //Mutual Defence Pact	MDP	Yellow	A mutual defence treaty which obliges both parties to defend each other in defensive wars.
    MDP(8, "MDP", "#ced518"),
    //Mutual non-chaining Defence Pact	MnDP	???	Similar to an MDP, but with an additional clause which prevents treaties with 3rd parties from triggering the MnDP also.
    MNDP(7, "MnDP", "#ff0000"),
    //Protectorate	Prot	Green	A treaty where a typically stronger, older alliance offers protection to a typically weaker, newer alliance. Only the protector is obligated to defend their protectee as in an MDP. However, the protectee has the option to support their protector in any wars. New alliances typically annul Protectorate treaties when they are considered self-sufficient.
    PROTECTORATE(6, "Protectorate", "#65c765"),
    //Optional Defence Optional Aggression Pact	ODoAP	Light purple	Similar to an ODP, but also provides an option to support each other in wars of aggression.
    ODOAP(5, "ODoAP", "#0d2572"),
    //Optional Defence Pact	ODP	Dark blue	A defence treaty which provides both parties an option to defend each other in defensive wars.
    ODP(4, "ODP", "#00aeef"),
    //Peace, Intelligence & Aid Treaty	PIAT	Light brown	A treaty of co-operation where both parties agree to share intelligence and aid, as well as maintain peace. Generally perceived as a step up from ToA.
    PIAT(3, "PIAT", "#ffb6c1"),
    //Treaty of Amity	TOA	None	A treaty of friendship which establishes cordial relations between both parties.
    TOA(2, "TOA", "#ffb6c1"),
    //Non-Aggression Pact	NAP	Light red	A treaty in which both parties agree to avoid engaging in offensive action against each other. This typically covers warfare, espionage, etc.
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
