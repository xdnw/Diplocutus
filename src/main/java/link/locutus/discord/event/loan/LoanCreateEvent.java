package link.locutus.discord.event.loan;

import link.locutus.discord.Locutus;
import link.locutus.discord.api.types.tx.LoanTransfer;
import link.locutus.discord.event.guild.GuildScopeEvent;

public class LoanCreateEvent extends LoanChangeEvent {
    public LoanCreateEvent(LoanTransfer current) {
        super(null, current);
    }
}
