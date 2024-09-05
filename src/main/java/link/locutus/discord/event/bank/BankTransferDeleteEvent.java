package link.locutus.discord.event.bank;

import link.locutus.discord.api.types.tx.BankTransfer;

public class BankTransferDeleteEvent extends BankTransferChangeEvent {
    public BankTransferDeleteEvent(BankTransfer previous) {
        super(previous, null);
    }
}
