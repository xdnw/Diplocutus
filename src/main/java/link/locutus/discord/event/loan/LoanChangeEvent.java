package link.locutus.discord.event.loan;

import link.locutus.discord.Locutus;
import link.locutus.discord.api.types.tx.LoanTransfer;
import link.locutus.discord.event.Event;
import link.locutus.discord.event.guild.GuildScopeEvent;

public class LoanChangeEvent extends GuildScopeEvent {
    private final LoanTransfer previous;
    private final LoanTransfer current;

    public LoanChangeEvent(LoanTransfer previous, LoanTransfer current) {
        this.previous = previous;
        this.current = current;
    }

    public LoanTransfer getCurrent() {
        return current;
    }

    public LoanTransfer getPrevious() {
        return previous;
    }

    @Override
    protected void postToGuilds() {
        int type = current != null ? current.GiverType : previous.GiverType;
        if (type == 1) {
            int aaId = current != null ? current.GiverId : previous.GiverId;
            if (aaId != 0) {
                post(Locutus.imp().getGuildDBByAA(aaId));
            }
        }
    }
}
