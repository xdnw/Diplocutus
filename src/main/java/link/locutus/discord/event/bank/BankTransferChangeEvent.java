package link.locutus.discord.event.bank;

import link.locutus.discord.Locutus;
import link.locutus.discord.api.types.tx.BankTransfer;
import link.locutus.discord.event.guild.GuildScopeEvent;

public class BankTransferChangeEvent extends GuildScopeEvent {
    private final BankTransfer previous;
    private final BankTransfer current;

    public BankTransferChangeEvent(BankTransfer previous, BankTransfer current) {
        this.previous = previous;
        this.current = current;
    }

    public BankTransfer getCurrent() {
        return current;
    }

    public BankTransfer getPrevious() {
        return previous;
    }

    @Override
    protected void postToGuilds() {
        if (current != null && current.sender_type == 1) {
            post(Locutus.imp().getGuildDBByAA((int) current.sender_id));
        }
        if (current != null && current.receiver_type == 1) {
            post(Locutus.imp().getGuildDBByAA((int) current.receiver_id));
        }
    }
}
