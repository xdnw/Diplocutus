package link.locutus.discord.event.loanrequest;

import link.locutus.discord.api.types.tx.DBLoanRequest;

public class LoanRequestDeleteEvent extends LoanRequestChangeEvent {
    public LoanRequestDeleteEvent(DBLoanRequest previous) {
        super(previous, null);
    }
}
