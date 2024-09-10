package link.locutus.discord.db.entities;

import link.locutus.discord.api.types.DepositType;
import link.locutus.discord.api.types.tx.Transaction2;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.pnw.NationOrAllianceOrGuild;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.StringMan;
import link.locutus.discord.util.TimeUtil;
import link.locutus.discord.util.sheet.SpreadSheet;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class AddBalanceBuilder {
    private final GuildDB db;
    Map<DBNation, Map<String, double[]>> fundsToSendNations = new LinkedHashMap<>();
    Map<DBAlliance, Map<String, double[]>> fundsToSendAAs = new LinkedHashMap<>();
    Map<GuildDB, Map<String, double[]>> fundsToSendGuilds = new LinkedHashMap<>();

    public AddBalanceBuilder(GuildDB db) {
        this.db = db;
    }

    public AddBalanceBuilder add(NationOrAllianceOrGuild account, double[] amount, String note) {
        return add(account, ResourceType.resourcesToMap(amount), note);
    }

    public Map<DBAlliance, Map<String, double[]>> getFundsToSendAAs() {
        return fundsToSendAAs;
    }

    public Map<DBNation, Map<String, double[]>> getFundsToSendNations() {
        return fundsToSendNations;
    }

    public Map<GuildDB, Map<String, double[]>> getFundsToSendGuilds() {
        return fundsToSendGuilds;
    }

    private <T> Map<T, Map<ResourceType, Double>> getTotal(Map<T, Map<String, double[]>> map) {
        Map<T, Map<ResourceType, Double>> total = new LinkedHashMap<>();
        for (Map.Entry<T, Map<String, double[]>> entry : map.entrySet()) {
            T account = entry.getKey();
            for (Map.Entry<String, double[]> entry2 : entry.getValue().entrySet()) {
                String note = entry2.getKey();
                double[] amt = entry2.getValue();

                Map<ResourceType, Double> current = total.computeIfAbsent(account, f -> new HashMap<>());
                current = ResourceType.add(current, ResourceType.resourcesToMap(amt));
                total.put(account, current);
            }
        }
        return total;

    }

    public Map<DBNation, Map<ResourceType, Double>> getTotalForNations() {
        return getTotal(fundsToSendNations);
    }

    public Map<DBAlliance, Map<ResourceType, Double>> getTotalForAAs() {
        return getTotal(fundsToSendAAs);
    }


    public Map<GuildDB, Map<ResourceType, Double>> getTotalForGuilds() {
        return getTotal(fundsToSendGuilds);
    }

    public AddBalanceBuilder add(NationOrAllianceOrGuild account, Map<ResourceType, Double> amount, String note) {
        if (note == null) note = "#deposit";
        Map<String, double[]> existing;
        if (account.isNation()) {
            existing = fundsToSendNations.computeIfAbsent(account.asNation(), k -> new HashMap<>());
        } else if (account.isAlliance()) {
            existing = fundsToSendAAs.computeIfAbsent(account.asAlliance(), k -> new HashMap<>());
        } else if (account.isGuild()) {
            existing = fundsToSendGuilds.computeIfAbsent(account.asGuild(), k -> new HashMap<>());
        } else {
            throw new IllegalArgumentException("Unknown type " + account);
        }
        double[] rss = existing.computeIfAbsent(note, k -> ResourceType.getBuffer());

        for (Map.Entry<ResourceType, Double> entry : amount.entrySet()) {
            rss[entry.getKey().ordinal()] += entry.getValue();
        }
        return this;
    }

    public AddBalanceBuilder addSheet(SpreadSheet sheet, boolean negative, Consumer<String> invalidRows, boolean throwErrorOnInvalid, String defaultNote) {
        List<String> invalid = new ArrayList<>();
        Map<String, Boolean> response = sheet.parseTransfers(this, negative, defaultNote);
        for (Map.Entry<String, Boolean> entry : response.entrySet()) {
            if (!entry.getValue()) {
                invalid.add(entry.getKey());
                invalidRows.accept(entry.getKey());
            }
        }
        if (throwErrorOnInvalid && !invalid.isEmpty()) throw new IllegalArgumentException("Invalid nations/alliance:\n- " + StringMan.join(invalid, "\n- "));
        return this;
    }

    public AddBalanceBuilder resetWithTracked(DBNation nation, @Nullable Set<Long> tracked) throws IOException {
        if (tracked != null) {
            tracked = DNS.expandCoalition(tracked);
        }

        double[] total = nation.getNetDeposits(db, tracked, true, 0L, 0L);
        Map<ResourceType, Double> transfer = ResourceType.subResourcesToA(new HashMap<>(), ResourceType.resourcesToMap(total));
        return add(nation, transfer, "#deposit");
    }

    public AddBalanceBuilder reset(DBNation nation, DepositType... types) {
        return reset(nation, new HashSet<>(Arrays.asList(types)));
    }

    public AddBalanceBuilder reset(DBNation nation, Set<DepositType> types) {
        if (types.isEmpty()) throw new IllegalArgumentException("No types specified");
        Map<DepositType, double[]> depoByType = nation.getDeposits(db, null, true, 0, 0);
        double[] deposits = depoByType.get(DepositType.DEPOSIT);

        if (deposits != null && types.contains(DepositType.DEPOSIT)) {
            add(nation, ResourceType.negative(deposits), "#" + DepositType.DEPOSIT.name().toLowerCase());
        }

        double[] tax = depoByType.get(DepositType.TAX);
        if (tax != null && types.contains(DepositType.TAX)) {
            add(nation, ResourceType.negative(tax), "#" + DepositType.TAX.name().toLowerCase());
        }

        double[] loan = depoByType.get(DepositType.LOAN);
        if (loan != null && types.contains(DepositType.LOAN)) {
            add(nation, ResourceType.negative(loan), "#" + DepositType.LOAN.name().toLowerCase());
        }
        long now = System.currentTimeMillis();
        if (depoByType.containsKey(DepositType.GRANT) && types.contains(DepositType.GRANT)) {
            List<Map.Entry<Integer, Transaction2>> transactions = nation.getTransactions(db, null, true, -1, 0);
            for (Map.Entry<Integer, Transaction2> entry : transactions) {
                Transaction2 tx = entry.getValue();
                if (tx.note == null || (!tx.note.contains("#expire") && !tx.note.contains("#decay")) || (tx.receiver_id != nation.getNation_id() && tx.sender_id != nation.getNation_id())) continue;
                if (tx.sender_id == tx.receiver_id) continue;
                Map<String, String> notes = DNS.parseTransferHashNotes(tx.note);
                String decay2 = notes.get("#decay");
                String expire2 = notes.get("#expire");
                long expireEpoch = 0;
                long decayEpoch = 0;
                if (expire2 != null) {
                    expireEpoch = tx.tx_datetime + TimeUtil.timeToSec(expire2, tx.tx_datetime, true) * 1000L;
                }
                if (decay2 != null) {
                    decayEpoch = tx.tx_datetime + TimeUtil.timeToSec(decay2, tx.tx_datetime, true) * 1000L;
                }
                expireEpoch = Math.min(expireEpoch, decayEpoch);
                if (expireEpoch > now) {
                    String noteCopy = tx.note
                            .replaceAll("#expire=[a-zA-Z0-9:]+", "")
                            .replaceAll("#decay=[a-zA-Z0-9:]+", "");
                    if (expire2 != null) {
                        noteCopy += " #expire=" + "timestamp:" + expireEpoch;
                    }
                    if (decay2 != null) {
                        noteCopy += " #decay=" + "timestamp:" + decayEpoch;
                    }
                    noteCopy = noteCopy.trim();

                    tx.tx_datetime = System.currentTimeMillis();
                    int sign = entry.getKey();
                    if (sign == 1) {
                        db.subBalance(now, nation, nation.getNation_id(), noteCopy, tx.resources);
                    } else if (sign == -1) {
                        add(nation, tx.resources, noteCopy);
                    }
                }
            }
        }
        return this;
    }
    public void buildWithConfirmation(IMessageIO io, JSONObject command) {
        command = command.put("force", "true");
        buildWithConfirmation(io, command.toString());
    }

    @Deprecated
    public void buildWithConfirmation(IMessageIO io, String command) {
        double[] total = getTotal();

        StringBuilder title = new StringBuilder("Addbalance to");
        if (!getFundsToSendNations().isEmpty()) title.append(" ").append(getFundsToSendNations().size()).append(" nations");
        if (!getFundsToSendAAs().isEmpty()) title.append(" ").append(getFundsToSendAAs().size()).append(" alliances");
        if (!getFundsToSendGuilds().isEmpty()) title.append(" ").append(getFundsToSendGuilds().size()).append(" guilds");
        String emoji = "Confirm";

        StringBuilder body = new StringBuilder();

        if (!getFundsToSendNations().isEmpty()) {
            if (getFundsToSendNations().size() == 1) {
                DBNation nation = getFundsToSendNations().keySet().iterator().next();
                body.append("\n" + nation.getNationUrlMarkup(true) + " | ").append(nation.getAllianceUrlMarkup(true));
            } else {
                int vm = 0;
                int inactive = 0;
                int applicants = 0;
                Set<Integer> aaIds = new HashSet<>();
                for (DBNation nation : getFundsToSendNations().keySet()) {
                    aaIds.add(nation.getAlliance_id());
                    if (nation.isVacation()) vm++;
                    if (nation.active_m() > 10000) inactive++;
                    if (nation.getPosition() <= 1) applicants++;
                }
                if (aaIds.size() > 1) {
                    body.append("\n" + getFundsToSendNations().size() + " nations in " + aaIds.size() + " alliances:");
                } else {
                    String aaName = DNS.getMarkdownUrl(aaIds.iterator().next(), true);
                    body.append("\n" + getFundsToSendNations().size() + " nations in " + aaName + ":");
                }
                if (vm > 0) body.append("\n- vm: " + vm);
                if (inactive > 0) body.append("\n- inactive: " + inactive);
                if (applicants > 0) body.append("\n- applicants: " + applicants);
            }
        }
        if (getFundsToSendGuilds().size() == 1) {
            body.append("\nGuild: " + getFundsToSendGuilds().keySet().iterator().next().getGuild());
        }
        if (getFundsToSendAAs().size() == 1) {
            body.append("\nAlliance: " + getFundsToSendAAs().keySet().iterator().next().getMarkdownUrl());
        }
        body.append("\nNotes: `" + StringMan.getString(getNotes(f -> true)) + "`");

        body.append("\nNet Total: ").append(ResourceType.resourcesToFancyString(total));

        body.append("\n\nPress `" + emoji + "` to confirm");

        io.create().embed(title.toString(), body.toString()).commandButton(command, "Confirm").send();
    }

    public Set<String> getNotes(Predicate<NationOrAllianceOrGuild> consumer) {
        Set<String> notes = new HashSet<>();
        for (Map.Entry<DBNation, Map<String, double[]>> entry : getFundsToSendNations().entrySet()) {
            if (consumer.test(entry.getKey())) {
                notes.addAll(entry.getValue().keySet());
            }
        }
        for (Map.Entry<GuildDB, Map<String, double[]>> entry : getFundsToSendGuilds().entrySet()) {
            if (consumer.test(entry.getKey())) {
                notes.addAll(entry.getValue().keySet());
            }
        }
        for (Map.Entry<DBAlliance, Map<String, double[]>> entry : getFundsToSendAAs().entrySet()) {
            if (consumer.test(entry.getKey())) {
                notes.addAll(entry.getValue().keySet());
            }
        }
        Set<String> trimmedNotes = new HashSet<>();
        for (String note : notes) {
            note = note.split("[ =:]")[0].trim().toLowerCase(Locale.ROOT);
            trimmedNotes.add(note);
        }
        return trimmedNotes;
    }

    public String buildAndSend(DBNation bankerNation, boolean hasEcon) {
        List<String> response = new ArrayList<>();
        long tx_datetime = System.currentTimeMillis();
        long receiver_id = 0;
        int receiver_type = 0;
        int banker = bankerNation.getNation_id();

        double[] totalAdded = ResourceType.getBuffer();


        if (!fundsToSendAAs.isEmpty()) {
            if (hasEcon) {
                for (Map.Entry<DBAlliance, Map<String, double[]>> entry : fundsToSendAAs.entrySet()) {
                    for (Map.Entry<String, double[]> entry2 : entry.getValue().entrySet()) {
                        DBAlliance sender = entry.getKey();
                        String note = entry2.getKey();
                        double[] amount = entry2.getValue();
                        db.addTransfer(tx_datetime, sender, receiver_id, receiver_type, banker, note, amount);
                        totalAdded = ResourceType.add(totalAdded, amount);
                        response.add("Added " + ResourceType.resourcesToString(amount) + " to " + sender);
                    }
                }
            } else {
                response.add("You do not have permision add balance to alliances\n");
            }
        }

        if (!fundsToSendGuilds.isEmpty()) {
            if (hasEcon) {
                for (Map.Entry<GuildDB, Map<String, double[]>> entry : fundsToSendGuilds.entrySet()) {
                    for (Map.Entry<String, double[]> entry2 : entry.getValue().entrySet()) {
                        GuildDB sender = entry.getKey();
                        String note = entry2.getKey();
                        double[] amount = entry2.getValue();
                        db.addTransfer(tx_datetime, sender.getIdLong(), sender.getReceiverType(), receiver_id, receiver_type, banker, note, amount);
                        totalAdded = ResourceType.add(totalAdded, amount);
                        response.add("Added " + ResourceType.resourcesToString(amount) + " to " + sender.getGuild());
                    }
                }
            } else {
                response.add("You do not have permision add balance to guilds\n");
            }
        }

        for (Map.Entry<DBNation, Map<String, double[]>> entry : fundsToSendNations.entrySet()) {
            for (Map.Entry<String, double[]> entry2 : entry.getValue().entrySet()) {
                DBNation sender = entry.getKey();
                String note = entry2.getKey();
                double[] amount = entry2.getValue();

                db.addTransfer(tx_datetime, sender, receiver_id, receiver_type, banker, note, amount);
                totalAdded = ResourceType.add(totalAdded, amount);
                response.add("Added " + ResourceType.resourcesToString(amount) + " to " + sender.getUrl());
            }

        }
        return "Done:\n- " +
                StringMan.join(response, "\n- ") +
                "\nTotal added: `" + ResourceType.resourcesToString(totalAdded) + "` worth: ~$" + MathMan.format(ResourceType.convertedTotal(totalAdded));
    }

    public AddBalanceBuilder add(Set<NationOrAllianceOrGuild> accounts, Map<ResourceType, Double> amount, String note) {
        for (NationOrAllianceOrGuild account : accounts) {
            add(account, amount, note);
        }
        return this;
    }

    public double[] getTotal() {
        double[] total = ResourceType.getBuffer();
        for (Map<String, double[]> map : fundsToSendNations.values()) {
            for (double[] doubles : map.values()) {
                ResourceType.add(total, doubles);
            }
        }
        for (Map<String, double[]> map : fundsToSendAAs.values()) {
            for (double[] doubles : map.values()) {
                ResourceType.add(total, doubles);
            }
        }
        for (Map<String, double[]> map : fundsToSendGuilds.values()) {
            for (double[] doubles : map.values()) {
                ResourceType.add(total, doubles);
            }
        }
        return total;
    }
}
