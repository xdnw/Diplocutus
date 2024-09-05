package link.locutus.discord.api.types;

public enum Rank {
    NONE(0),
    APPLICANT(1),
    MEMBER(2),
    LEADER(3),
    ;
    public static Rank[] values = values();
    public final int id;

    Rank(int id) {
        this.id = id;
    }
}
