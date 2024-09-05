package link.locutus.discord.event.bank;

import link.locutus.discord.api.types.tx.BankTransfer;

public class BankTransferCreateEvent extends BankTransferChangeEvent {
    public BankTransferCreateEvent(BankTransfer current) {
        super(null, current);
    }
}
