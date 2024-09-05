package link.locutus.discord.util.io;
public enum PagePriority {
    ACTIVE_PAGE(0, 0),

    ;

    public static final PagePriority[] values = values();
    private final int allowedBufferingMs;
    private final int allowableDelayMs;

    PagePriority(int allowedBufferingMs, int allowableDelayMs) {
        this.allowedBufferingMs = allowedBufferingMs;
        this.allowableDelayMs = allowableDelayMs;
    }

    public int getAllowedBufferingMs() {
        return allowedBufferingMs;
    }

    public int getAllowableDelayMs() {
        return allowableDelayMs;
    }
}
