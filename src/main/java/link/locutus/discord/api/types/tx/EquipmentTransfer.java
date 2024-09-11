package link.locutus.discord.api.types.tx;

import link.locutus.discord.api.generated.*;
import link.locutus.discord.api.types.MilitaryUnit;
import link.locutus.discord.db.SQLUtil;
import link.locutus.discord.db.entities.DBEntity;
import link.locutus.discord.event.Event;
import link.locutus.discord.event.equipment.EquipmentUpdateEvent;
import link.locutus.discord.event.loan.LoanUpdateEvent;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class EquipmentTransfer implements DBEntity<EquipmentTransactionHistory, EquipmentTransfer> {
    public int TransactionId;
    public int Quantity;
    public int Quality;
    public @Nullable String MessageTxt;
    public BankType TypeTxt;
    public MilitaryUnit EquipmentType;
    public int SenderId;
    public int ReceiverId;
    public int SenderType;
    public int ReceiverType;
    public long TimeStampTxt;

    @Override
    public String getTableName() {
        return "equipment_transfer";
    }

    @Override
    public boolean update(EquipmentTransactionHistory entity, Consumer<Event> eventConsumer) {
        EquipmentTransfer copy = null;
        if (entity.TransactionId != TransactionId) {
            if (copy == null) copy = copy();
            TransactionId = entity.TransactionId;
        }
        if (entity.Quantity != Quantity) {
            if (copy == null) copy = copy();
            Quantity = entity.Quantity;
        }
        if (entity.Quality != Quality) {
            if (copy == null) copy = copy();
            Quality = entity.Quality;
        }
        if (!Objects.equals(entity.MessageTxt, MessageTxt)) {
            if (copy == null) copy = copy();
            MessageTxt = entity.MessageTxt;
        }
        if (entity.TypeTxt != TypeTxt) {
            if (copy == null) copy = copy();
            TypeTxt = entity.TypeTxt;
        }
        if (entity.EquipmentType != EquipmentType) {
            if (copy == null) copy = copy();
            EquipmentType = entity.EquipmentType;
        }
        int newSenderId = entity.AllianceId != null ? entity.AllianceId : entity.NationId;
        int newSenderType = entity.AllianceId != null ? 1 : 0;
        if (newSenderId != SenderId || newSenderType != SenderType) {
            if (copy == null) copy = copy();
            SenderId = newSenderId;
            SenderType = newSenderType;
        }
        int newReceiverId = entity.AllianceId2 != null ? entity.AllianceId2 : entity.NationId2;
        int newReceiverType = entity.AllianceId2 != null ? 1 : 0;
        if (newReceiverId != ReceiverId || newReceiverType != ReceiverType) {
            if (copy == null) copy = copy();
            ReceiverId = newReceiverId;
            ReceiverType = newReceiverType;
        }
        long newTimeStamp = entity.TimeStampTxt.getTime();
        if (newTimeStamp != TimeStampTxt) {
            if (copy == null) copy = copy();
            TimeStampTxt = newTimeStamp;
        }
        if (copy != null && eventConsumer != null) {
            eventConsumer.accept(new EquipmentUpdateEvent(copy, this));
        }
        return copy != null;
    }

    @Override
    public Object[] write() {
        return new Object[]{
                TransactionId,
                Quantity,
                Quality,
                MessageTxt,
                TypeTxt.ordinal(),
                EquipmentType.ordinal(),
                SenderId,
                ReceiverId,
                SenderType,
                ReceiverType,
                TimeStampTxt,
        };
    }

    @Override
    public void load(Object[] raw) {
        TransactionId = (int) raw[0];
        Quantity = (int) raw[1];
        Quality = (int) raw[2];
        MessageTxt = (String) raw[3];
        TypeTxt = BankType.values[(int) raw[4]];
        EquipmentType = MilitaryUnit.values[(int) raw[5]];
        SenderId = (int) raw[6];
        ReceiverId = (int) raw[7];
        SenderType = (int) raw[8];
        ReceiverType = (int) raw[9];
        TimeStampTxt = SQLUtil.castLong(raw[10]);
    }

    @Override
    public Map<String, Class<?>> getTypes() {
        Map<String, Class<?>> types = new LinkedHashMap<>();
        types.put("TransactionId", Integer.class);
        types.put("Quantity", Integer.class);
        types.put("Quality", Integer.class);
        types.put("MessageTxt", String.class);
        types.put("TypeTxt", Integer.class);
        types.put("EquipmentType", Integer.class);
        types.put("SenderId", Integer.class);
        types.put("ReceiverId", Integer.class);
        types.put("SenderType", Integer.class);
        types.put("ReceiverType", Integer.class);
        types.put("TimeStampTxt", Long.class);
        return types;
    }

    @Override
    public EquipmentTransfer emptyInstance() {
        return new EquipmentTransfer();
    }
}
