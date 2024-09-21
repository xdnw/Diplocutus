package link.locutus.discord.event.loanrequest;

import link.locutus.discord.Locutus;
import link.locutus.discord.api.types.tx.DBLoanRequest;
import link.locutus.discord.event.guild.GuildScopeEvent;

public class LoanRequestChangeEvent extends GuildScopeEvent {
    private final DBLoanRequest previous;
    private final DBLoanRequest current;

    public LoanRequestChangeEvent(DBLoanRequest previous, DBLoanRequest current) {
        this.previous = previous;
        this.current = current;
    }

    public DBLoanRequest getCurrent() {
        return current;
    }

    public DBLoanRequest getPrevious() {
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
