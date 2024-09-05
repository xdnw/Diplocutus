package link.locutus.discord.event.equipment;

import link.locutus.discord.Locutus;
import link.locutus.discord.api.types.tx.EquipmentTransfer;
import link.locutus.discord.event.guild.GuildScopeEvent;

public class EquipmentChangeEvent extends GuildScopeEvent {
    private final EquipmentTransfer previous;
    private final EquipmentTransfer current;

    public EquipmentChangeEvent(EquipmentTransfer previous, EquipmentTransfer current) {
        this.previous = previous;
        this.current = current;
    }

    public EquipmentTransfer getCurrent() {
        return current;
    }

    public EquipmentTransfer getPrevious() {
        return previous;
    }

    @Override
    protected void postToGuilds() {
        if (current != null && current.SenderType == 1) {
            post(Locutus.imp().getGuildDBByAA(current.SenderId));
        }
        if (current != null && current.ReceiverType == 1) {
            post(Locutus.imp().getGuildDBByAA(current.ReceiverId));
        }
    }
}
