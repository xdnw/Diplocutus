package link.locutus.discord.event.equipment;

import link.locutus.discord.api.types.tx.EquipmentTransfer;

public class EquipmentDeleteEvent extends EquipmentChangeEvent {
    public EquipmentDeleteEvent(EquipmentTransfer previous) {
        super(previous, null);
    }
}
