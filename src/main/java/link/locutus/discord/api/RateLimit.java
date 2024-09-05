package link.locutus.discord.api;

public class RateLimit {
    // Amount of requests allowed per interval
    public volatile int limit;
    // The interval in milliseconds (typically 60_000)
    public volatile int intervalMs;
    // The number of ms until the ratelimit resets (unused)
    public volatile long resetAfterMs_unused;
    // The remaining number of requests this interval
    public volatile int remaining;
    // the unix epoch time in milliseconds when the ratelimit resets
    public volatile long resetMs;

    public void reset(long now) {
        if (now > resetMs) {
            remaining = limit;
            long remainder = (now - resetMs) % intervalMs;
            resetAfterMs_unused = intervalMs - remainder;
            resetMs = now + resetAfterMs_unused;
        }
    }
}