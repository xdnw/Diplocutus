package link.locutus.discord.event.loanrequest;

import link.locutus.discord.api.types.tx.DBLoanRequest;

public class LoanRequestUpdateEvent extends LoanRequestChangeEvent {
    public LoanRequestUpdateEvent(DBLoanRequest previous, DBLoanRequest current) {
        super(previous, current);
    }
}
