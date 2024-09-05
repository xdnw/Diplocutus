package link.locutus.discord.event.loan;

import link.locutus.discord.api.types.tx.LoanTransfer;

public class LoanUpdateEvent extends LoanChangeEvent {
    public LoanUpdateEvent(LoanTransfer previous, LoanTransfer current) {
        super(previous, current);
    }
}
