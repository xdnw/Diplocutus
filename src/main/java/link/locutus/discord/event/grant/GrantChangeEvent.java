package link.locutus.discord.event.grant;

import link.locutus.discord.Locutus;
import link.locutus.discord.api.types.tx.GrantTransfer;
import link.locutus.discord.event.Event;
import link.locutus.discord.event.guild.GuildScopeEvent;

public class GrantChangeEvent extends GuildScopeEvent {
    private final GrantTransfer previous;
    private final GrantTransfer current;

    public GrantChangeEvent(GrantTransfer previous, GrantTransfer current) {
        this.previous = previous;
        this.current = current;
    }

    public GrantTransfer getCurrent() {
        return current;
    }

    public GrantTransfer getPrevious() {
        return previous;
    }

    @Override
    protected void postToGuilds() {
        int type = current != null ? current.sender_type : previous.sender_type;
        if (type == 1) {
            int aaId = (int) (current != null ? current.sender_id : previous.sender_id);
            if (aaId != 0) {
                post(Locutus.imp().getGuildDBByAA(aaId));
            }
        }
    }
}
