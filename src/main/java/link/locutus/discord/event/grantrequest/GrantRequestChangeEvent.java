package link.locutus.discord.event.grantrequest;

import link.locutus.discord.Locutus;
import link.locutus.discord.api.types.tx.DBGrantRequest;
import link.locutus.discord.event.guild.GuildScopeEvent;

public class GrantRequestChangeEvent extends GuildScopeEvent {
    private final DBGrantRequest previous;
    private final DBGrantRequest current;

    public GrantRequestChangeEvent(DBGrantRequest previous, DBGrantRequest current) {
        this.previous = previous;
        this.current = current;
    }

    public DBGrantRequest getCurrent() {
        return current;
    }

    public DBGrantRequest getPrevious() {
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
