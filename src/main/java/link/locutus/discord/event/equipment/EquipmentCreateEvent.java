package link.locutus.discord.event.equipment;

import link.locutus.discord.api.types.tx.EquipmentTransfer;

public class EquipmentCreateEvent extends EquipmentChangeEvent {
    public EquipmentCreateEvent(EquipmentTransfer current) {
        super(null, current);
    }
}
