package link.locutus.discord.api.types.tx;

import link.locutus.discord.api.generated.BankHistory;
import link.locutus.discord.api.generated.BankType;
import link.locutus.discord.api.generated.LoanHistory;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.db.entities.DBEntity;
import link.locutus.discord.event.Event;
import link.locutus.discord.event.bank.BankTransferUpdateEvent;
import link.locutus.discord.event.grant.GrantUpdateEvent;
import link.locutus.discord.util.TimeUtil;

import java.util.Map;
import java.util.function.Consumer;

public class BankTransfer extends Transaction2 implements DBEntity<BankHistory, BankTransfer> {
    public BankType TypeTxt;

    @Override
    public String getTableName() {
        return "bank_history";
    }

    public long generateId() {
        return ((this.tx_datetime - TimeUtil.getOrigin()) << 26) ^ (sender_id << 13) ^ receiver_id;
    }

    @Override
    public boolean update(BankHistory entity, Consumer<Event> eventConsumer) {
        int senderId = entity.NationId == null ? entity.AllianceId : entity.NationId;
        int senderType = entity.NationId == null ? 1 : 0;
        int receiverId = entity.NationId2 == null ? entity.AllianceId2 : entity.NationId2;
        int receiverType = entity.NationId2 == null ? 1 : 0;
        double[] newResources = ResourceType.getBuffer();
        newResources[ResourceType.CASH.ordinal()] = entity.CashAmount;
        newResources[ResourceType.TECHNOLOGY.ordinal()] = entity.TechAmount;
        newResources[ResourceType.MINERALS.ordinal()] = entity.MineralAmount;
        newResources[ResourceType.URANIUM.ordinal()] = entity.UraniumAmount;
        newResources[ResourceType.RARE_METALS.ordinal()] = entity.RareMetalAmount;
        newResources[ResourceType.FUEL.ordinal()] = entity.FuelAmount;
        BankTransfer copy = super.update(
                tx_id,
                entity.TimeStampTxt.getTime(),
                sender_id,
                senderType,
                receiverId,
                receiverType,
                0,
                entity.MessageTxt,
                newResources,
                this::copy
        );
        this.tx_id = generateId();
        if (this.TypeTxt != entity.TypeTxt) {
            if (copy == null) copy = this.copy();
            this.TypeTxt = entity.TypeTxt;
        }
        if (copy != null && eventConsumer != null) {
            eventConsumer.accept(new BankTransferUpdateEvent(copy, this));
        }
        return copy != null;

    }

    @Override
    public Object[] write() {
        Object[] arr = new Object[11];
        super.write(arr);
        arr[6] = TypeTxt.ordinal();
        return arr;
    }

    @Override
    public void load(Object[] raw) {
        super.load(raw);
        TypeTxt = BankType.values[(int) raw[6]];

    }

    @Override
    public Map<String, Class<?>> getTypes() {
        Map<String, Class<?>> types = super.getTypes();
        types.put("TypeTxt", int.class);
        return types;
    }

    @Override
    public BankTransfer emptyInstance() {
        return new BankTransfer();
    }
}