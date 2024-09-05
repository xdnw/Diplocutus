package link.locutus.discord.event.equipment;

import link.locutus.discord.api.types.tx.EquipmentTransfer;

public class EquipmentUpdateEvent extends EquipmentChangeEvent {
    public EquipmentUpdateEvent(EquipmentTransfer previous, EquipmentTransfer current) {
        super(previous, current);
    }
}
