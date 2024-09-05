package link.locutus.discord.db;

import link.locutus.discord.Locutus;
import link.locutus.discord.api.endpoints.DnsApi;
import link.locutus.discord.api.endpoints.DnsQuery;
import link.locutus.discord.api.generated.BankHistory;
import link.locutus.discord.api.types.tx.*;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.DBEntity;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;


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