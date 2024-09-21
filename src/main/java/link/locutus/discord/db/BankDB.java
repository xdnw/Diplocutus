package link.locutus.discord.db;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import link.locutus.discord.Locutus;
import link.locutus.discord.api.endpoints.DnsApi;
import link.locutus.discord.api.endpoints.DnsQuery;
import link.locutus.discord.api.generated.AllianceGrantRequest;
import link.locutus.discord.api.generated.AllianceLoanRequest;
import link.locutus.discord.api.generated.BankHistory;
import link.locutus.discord.api.types.tx.*;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.DBEntity;
import link.locutus.discord.event.Event;
import link.locutus.discord.event.grantrequest.GrantRequestCreateEvent;
import link.locutus.discord.event.loanrequest.LoanRequestCreateEvent;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;


public class BankDB extends DBMainV2 {

    public BankDB() throws SQLException, ClassNotFoundException {
        super(Settings.INSTANCE.DATABASE, "bank");
    }

    @Override
    public void createTables() {
        executeStmt(SQLUtil.createTable(new GrantTransfer()));
        executeStmt(SQLUtil.createTable(new LoanTransfer()));
        executeStmt(SQLUtil.createTable(new BankTransfer()));
        executeStmt(SQLUtil.createTable(new EquipmentTransfer()));

        executeStmt(SQLUtil.createTable(new DBGrantRequest()));
        executeStmt(SQLUtil.createTable(new DBLoanRequest()));
    }

    public void updateAllGrantLoanRequests(Consumer<Event> eventConsumer) {
        for (DBAlliance alliance : Locutus.imp().getNationDB().getAlliances()) {
            DnsApi api = alliance.getApi(false);
            if (api == null) continue;
            updateGrantLoanRequests(alliance, eventConsumer);
        }
    }

    public void updateGrantLoanRequests(DBAlliance alliance, Consumer<Event> eventConsumer) {
        syncGrantRequests(alliance, eventConsumer);
        syncLoanRequests(alliance, eventConsumer);
    }

    public boolean syncGrantRequests(DBAlliance alliance, Consumer<Event> eventConsumer) {
        DnsApi api = alliance.getApi(false);
        if (api == null) return false;
        long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);

        Map<Integer, DBGrantRequest> existing = new Int2ObjectOpenHashMap<>();
        select(new DBGrantRequest(), "sender_id = ?", ps -> {
            ps.setInt(1, alliance.getId());
        }).forEach(gr -> existing.put((int) gr.tx_id, gr));

        List<DBGrantRequest> toSave = new ArrayList<>();
        List<AllianceGrantRequest> list = api.allianceGrantRequest().call();
        for (AllianceGrantRequest req : list) {
            DBGrantRequest existingReq = existing.get(req.grantId);
            if (existingReq == null) {
                DBGrantRequest newReq = new DBGrantRequest();
                newReq.update(req, alliance.getId(), null);
                toSave.add(newReq);
                if (newReq.tx_datetime > cutoff) eventConsumer.accept(new GrantRequestCreateEvent(newReq));
            } else {
                existingReq.update(req, alliance.getId(), eventConsumer);
                toSave.add(existingReq);
            }
        }
        save(toSave);
        return true;
    }

    public boolean syncLoanRequests(DBAlliance alliance, Consumer<Event> eventConsumer) {
        DnsApi api = alliance.getApi(false);
        if (api == null) return false;
        long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);

        Map<Integer, DBLoanRequest> existing = new Int2ObjectOpenHashMap<>();
        select(new DBLoanRequest(), "sender_id = ?", ps -> {
            ps.setInt(1, alliance.getId());
        }).forEach(gr -> existing.put((int) gr.tx_id, gr));

        List<DBLoanRequest> toSave = new ArrayList<>();
        List<AllianceLoanRequest> list = api.allianceLoanRequest().call();
        for (AllianceLoanRequest req : list) {
            DBLoanRequest existingReq = existing.get(req.LoanId);
            if (existingReq == null) {
                DBLoanRequest newReq = new DBLoanRequest();
                newReq.update(req, null);
                toSave.add(newReq);
                if (newReq.tx_datetime > cutoff) eventConsumer.accept(new LoanRequestCreateEvent(newReq));
            } else {
                existingReq.update(req, eventConsumer);
                toSave.add(existingReq);
            }
        }
        save(toSave);
        return true;
    }

    public void updateBankTransfers(DBAlliance alliance, int nationId) {
        updateRecords(new BankTransfer(), alliance, DnsApi::nationBankHistory, nationId);
    }

    public void updateBankTransfers(DBAlliance alliance) {
        updateRecords(new BankTransfer(), alliance, DnsApi::allianceBankHistory);
    }

    public void updateLoanTransfers(DBAlliance alliance) {
        updateRecords(new LoanTransfer(), alliance, DnsApi::allianceLoanHistory);
    }

    public void updateGrantTransfers(DBAlliance alliance) {
        updateRecords(new GrantTransfer(), alliance, DnsApi::allianceGrantHistory);
    }

    public void updateEquipmentTransfers(DBAlliance alliance, int nationId) {
        updateRecords(new EquipmentTransfer(), alliance, DnsApi::nationEquipmentTransactionHistory, nationId);
    }

    public void updateEquipmentTransfers(DBAlliance alliance) {
        updateRecords(new EquipmentTransfer(), alliance, DnsApi::allianceEquipmentTransactionHistory);
    }

    public List<BankTransfer> getTransactionsByNation(int nation) {
        String whereClause = "(sender_id = ? AND sender_type = 0) OR (receiver_id = ? AND receiver_type = 0)";
        return select(new BankTransfer(), whereClause, ps -> {
            ps.setInt(1, nation);
            ps.setInt(2, nation);
        });
    }
}