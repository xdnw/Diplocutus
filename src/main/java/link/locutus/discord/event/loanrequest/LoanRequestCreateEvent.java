package link.locutus.discord.event.loanrequest;

import link.locutus.discord.api.types.tx.DBLoanRequest;

public class LoanRequestCreateEvent extends LoanRequestChangeEvent {
    public LoanRequestCreateEvent(DBLoanRequest current) {
        super(null, current);
    }
}
