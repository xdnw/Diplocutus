package link.locutus.discord.event.bank;

import link.locutus.discord.api.types.tx.BankTransfer;

public class BankTransferUpdateEvent extends BankTransferChangeEvent {
    public BankTransferUpdateEvent(BankTransfer previous, BankTransfer current) {
        super(previous, current);
    }
}
