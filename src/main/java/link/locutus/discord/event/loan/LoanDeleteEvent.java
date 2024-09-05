package link.locutus.discord.event.loan;

import link.locutus.discord.api.types.tx.LoanTransfer;

public class LoanDeleteEvent extends LoanChangeEvent {
    public LoanDeleteEvent(LoanTransfer previous) {
        super(previous, null);
    }
}
