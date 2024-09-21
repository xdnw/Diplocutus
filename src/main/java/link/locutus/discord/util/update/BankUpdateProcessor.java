package link.locutus.discord.util.update;

import com.google.common.eventbus.Subscribe;
import link.locutus.discord.Locutus;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.api.types.tx.DBLoanRequest;
import link.locutus.discord.commands.manager.v2.command.IMessageBuilder;
import link.locutus.discord.commands.manager.v2.impl.discord.DiscordChannelIO;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.BankDB;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.db.entities.NationMeta;
import link.locutus.discord.api.types.tx.Transaction2;
import link.locutus.discord.db.guild.GuildKey;
import link.locutus.discord.db.guild.GuildSetting;
import link.locutus.discord.event.grantrequest.GrantRequestCreateEvent;
import link.locutus.discord.event.grantrequest.GrantRequestDeleteEvent;
import link.locutus.discord.event.grantrequest.GrantRequestUpdateEvent;
import link.locutus.discord.event.loanrequest.LoanRequestCreateEvent;
import link.locutus.discord.event.loanrequest.LoanRequestDeleteEvent;
import link.locutus.discord.event.loanrequest.LoanRequestUpdateEvent;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.AlertUtil;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.TimeUtil;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.DNS;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static link.locutus.discord.db.guild.GuildKey.DEPOSIT_ALERT_CHANNEL;
import static link.locutus.discord.db.guild.GuildKey.WITHDRAW_ALERT_CHANNEL;

public class BankUpdateProcessor {
    private GuildDB getGuild(int id) {
        return Locutus.imp().getGuildDBByAA(id);
    }

    private GuildMessageChannel getChannel(Transaction2 transfer, boolean isReceiver, GuildSetting<MessageChannel> key) {
        int id = (int) (isReceiver ? transfer.receiver_id : transfer.sender_id);
        GuildDB db = getGuild(id);
        MessageChannel channel = db == null ? null : key.getOrNull(db);
        return channel instanceof GuildMessageChannel ? (GuildMessageChannel) channel : null;
    }

    private String toRequestTitle(String title, Transaction2 transfer) {
        return title + "#" + transfer.tx_id + " " + DNS.getName(transfer.receiver_id, transfer.receiver_type == 1);
    }

    private String loanBody(DBLoanRequest loanRequest) {
        StringBuilder body = new StringBuilder("\n");
        body.append("Upfront: $" + MathMan.format(loanRequest.upfront) + "\n");
        body.append("Interest Type: " + loanRequest.interestType.name() + "\n");
        body.append("Interest Rate: " + MathMan.format(loanRequest.interestRate) + "\n");
        body.append("Forced Interest: " + MathMan.format(loanRequest.forcedInterest) + "\n");
        body.append("Minimum Payment: " + MathMan.format(loanRequest.minimumPayment) + "\n");
        body.append("Duration: " + loanRequest.duration + "x" + loanRequest.durationType.name() + "\n");
        return body.toString();
    }

    private String toRequestBody(Transaction2 transfer) {
        StringBuilder body = new StringBuilder();
        body.append("Date: " + DiscordUtil.timestamp(transfer.tx_datetime, null) + "\n");
        body.append("From: " + DNS.getMarkdownUrl((int) transfer.sender_id, transfer.sender_type == 1) + "\n");
        body.append("To: " + DNS.getMarkdownUrl((int) transfer.receiver_id, transfer.receiver_type == 1) + "\n");
        if (transfer.note != null) {
            body.append("Note: `" + transfer.note + "`\n");
        }
        body.append("Resources: " + ResourceType.resourcesToString(transfer.resources) + "\n");
        return body.toString();
    }

    @Subscribe
    public void onLoanRequestCreate(LoanRequestCreateEvent event) {
        GuildMessageChannel channel = getChannel(event.getCurrent(), false, GuildKey.LOAN_REQUEST_ALERTS);
        if (channel == null) return;
        String title =  toRequestTitle("Loan Request: ", event.getCurrent());
        String body = toRequestBody(event.getCurrent()) + loanBody(event.getCurrent());
        IMessageBuilder msg = new DiscordChannelIO(channel).create().embed(title, body);
        Role econRole = Roles.ECON.toRole(channel.getGuild());
        if (econRole != null) msg.append(econRole.getAsMention());
        msg.sendWhenFree();
    }

    @Subscribe
    public void onLoanRequestUpdate(LoanRequestUpdateEvent event) {
        GuildMessageChannel channel = getChannel(event.getCurrent(), false, GuildKey.LOAN_REQUEST_ALERTS);
        if (channel == null) return;
        String title =  toRequestTitle("Loan Request Update: ", event.getCurrent());
        String body = toRequestBody(event.getCurrent()) + loanBody(event.getCurrent());
        IMessageBuilder msg = new DiscordChannelIO(channel).create().embed(title, body);
        Role econRole = Roles.ECON.toRole(channel.getGuild());
        if (econRole != null) msg.append(econRole.getAsMention());
        msg.sendWhenFree();
    }

    @Subscribe
    public void onLoanRequestDelete(LoanRequestDeleteEvent event) {
        GuildMessageChannel channel = getChannel(event.getPrevious(), false, GuildKey.LOAN_REQUEST_ALERTS);
        if (channel == null) return;
        String title =  toRequestTitle("Loan Request Delete: ", event.getPrevious());
        String body = toRequestBody(event.getPrevious()) + loanBody(event.getPrevious());
        IMessageBuilder msg = new DiscordChannelIO(channel).create().embed(title, body);
        Role econRole = Roles.ECON.toRole(channel.getGuild());
        if (econRole != null) msg.append(econRole.getAsMention());
        msg.sendWhenFree();
    }

    @Subscribe
    public void onGrantRequestCreate(GrantRequestCreateEvent event) {
        GuildMessageChannel channel = getChannel(event.getCurrent(), false, GuildKey.GRANT_REQUEST_ALERTS);
        if (channel == null) return;
        String title =  toRequestTitle("Grant Request: ", event.getCurrent());
        String body = toRequestBody(event.getCurrent());
        IMessageBuilder msg = new DiscordChannelIO(channel).create().embed(title, body);
        Role econRole = Roles.ECON.toRole(channel.getGuild());
        if (econRole != null) msg.append(econRole.getAsMention());
        msg.sendWhenFree();
    }

    @Subscribe
    public void onGrantRequestUpdate(GrantRequestUpdateEvent event) {
        GuildMessageChannel channel = getChannel(event.getCurrent(), false, GuildKey.GRANT_REQUEST_ALERTS);
        if (channel == null) return;
        String title =  toRequestTitle("Grant Request Update: ", event.getCurrent());
        String body = toRequestBody(event.getCurrent());
        IMessageBuilder msg = new DiscordChannelIO(channel).create().embed(title, body);
        Role econRole = Roles.ECON.toRole(channel.getGuild());
        if (econRole != null) msg.append(econRole.getAsMention());
        msg.sendWhenFree();
    }

    @Subscribe
    public void onGrantRequestDelete(GrantRequestDeleteEvent event) {
        GuildMessageChannel channel = getChannel(event.getPrevious(), false, GuildKey.GRANT_REQUEST_ALERTS);
        if (channel == null) return;
        String title =  toRequestTitle("Grant Request Delete: ", event.getPrevious());
        String body = toRequestBody(event.getPrevious());
        IMessageBuilder msg = new DiscordChannelIO(channel).create().embed(title, body);
        Role econRole = Roles.ECON.toRole(channel.getGuild());
        if (econRole != null) msg.append(econRole.getAsMention());
        msg.sendWhenFree();
    }

    // TODO FIXME :||remove
//    @Subscribe
//    public void process(TransactionEvent event) {
//        Transaction2 transfer = event.getTransaction();
//        if (transfer.tx_datetime < System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)) {
//            return;
//        }
//
//        boolean isLoot = transfer.note != null && transfer.note.contains("of the alliance bank inventory.");
//        if (!transfer.isReceiverNation() && !transfer.isSenderNation()) return;
//        if (!transfer.isReceiverAA() && !transfer.isSenderAA()) return;
//        int nationId = (int) (transfer.isSenderNation() ? transfer.getSender() : transfer.getReceiver());
//        int aaId = (int) (transfer.isSenderAA() ? transfer.getSender() : transfer.getReceiver());
//
//        if (!isLoot) {
//            Set<Integer> trackedAlliances = new HashSet<>();
//            trackedAlliances.add(aaId);
//            if (transfer.note != null) {
//                for (Map.Entry<String, String> entry : DNS.parseTransferHashNotes(transfer.note).entrySet()) {
//                    if (MathMan.isInteger(entry.getValue())) {
//                        try {
//                            int id = Integer.parseInt(entry.getValue());
//                            trackedAlliances.add(id);
//                        } catch (NumberFormatException ignore) {
//                        }
//                    }
//                }
//            }
//            for (int allianceId : trackedAlliances) {
//                GuildDB guildDb = Locutus.imp().getGuildDBByAA(allianceId);
//                if (guildDb != null) {
//                    boolean isDeposit = transfer.isReceiverAA();
//                    GuildSetting<MessageChannel> key = isDeposit ? DEPOSIT_ALERT_CHANNEL : WITHDRAW_ALERT_CHANNEL;
//                    Roles locrole = isDeposit ? Roles.ECON_DEPOSIT_ALERTS : Roles.ECON_WITHDRAW_ALERTS;
//                    MessageChannel channel = guildDb.getOrNull(key);
//
//                    if (channel != null) {
//                        Guild guild = guildDb.getGuild();
//                        if (guild != null) {
//                            Map.Entry<String, String> card = createCard(transfer, nationId);
//                            try {
//                                IMessageBuilder msg = new DiscordChannelIO(channel).create().embed(card.getKey(), card.getValue());
//                                Map.Entry<GuildDB, Integer> offshore = guildDb.getOffshoreDB();
//                                if (isDeposit && offshore != null) {
//                                    msg = msg.commandButton(CM.offshore.send.cmd, "offshore");
//                                }
//                                Role role = locrole.toRole(guild);
//                                if (role != null) {
//                                    msg.append(role.getAsMention());
//                                }
//                                if (isDeposit) {
//                                    msg.send();
//                                } else {
//                                    msg.sendWhenFree();
//                                }
//                            } catch (InsufficientPermissionException ignore) {
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        double value = transfer.convertedTotal();
//        if (value > Settings.INSTANCE.UPDATE_PROCESSOR.THRESHOLD_BANK_SUB_ALERT) {
//            long longValue = (long) value;
//
//            Set<Long> receiver = new HashSet<>();
//            Set<BankDB.Subscription> subs = new HashSet<>();
//
//            subs.addAll(Locutus.imp().getBankDB().getSubscriptions(0, BankDB.BankSubType.ALL, true, longValue));
//            subs.addAll(Locutus.imp().getBankDB().getSubscriptions(0, BankDB.BankSubType.ALL, false, longValue));
//
//            subs.addAll(Locutus.imp().getBankDB().getSubscriptions((int) transfer.getSender(), BankDB.BankSubType.of(transfer.isSenderAA()), false, longValue));
//            subs.addAll(Locutus.imp().getBankDB().getSubscriptions((int) transfer.getReceiver(), BankDB.BankSubType.of(transfer.isReceiverAA()), true, longValue));
//
//            for (BankDB.Subscription sub : subs) {
//                DBNation nation = DiscordUtil.getNation(sub.user);
//                if (nation == null) {
//                    Locutus.imp().getBankDB().unsubscribeAll(sub.user);
//                    continue;
//                }
//                ByteBuffer thresholdBuf = nation.getMeta(NationMeta.BANK_TRANSFER_REQUIRED_AMOUNT);
//                if (thresholdBuf != null) {
//                    long threshold = (long) thresholdBuf.getDouble();
//                    if (threshold > value) {
//                        continue;
//                    }
//                }
//                receiver.add(sub.user);
//            }
//
//            if (!receiver.isEmpty()) {
//                Map.Entry<String, String> card = createCard(transfer, nationId);
//
//                for (GuildDB guildDB : Locutus.imp().getGuildDatabases().values()) {
//                    MessageChannel channel = guildDB.getOrNull(LARGE_TRANSFERS_CHANNEL, false);
//                    if (channel == null) {
//                        continue;
//                    }
//
//                    Guild guild = ((GuildMessageChannel) channel).getGuild();
//                    Set<String> mentions = new HashSet<>();
//                    for (long user : receiver) {
//                        Member member = guild.getMemberById(user);
//                        if (member != null) {
//                            mentions.add(member.getAsMention());
//                        }
//                    }
//                    if (mentions.isEmpty() && value < Settings.INSTANCE.UPDATE_PROCESSOR.THRESHOLD_ALL_BANK_ALERT) continue;
//
//                    DiscordUtil.createEmbedCommand(channel, card.getKey(), card.getValue());
//                    AlertUtil.bufferPing(channel, mentions.toArray(new String[0]));
//                }
//            }
//        }
//    }

    public static Map.Entry<String, String> createCard(Transaction2 transfer, int nationid) {
        String fromName = (transfer.isSenderAA() ? "AA:" : "") + DNS.getName(transfer.getSender(), transfer.isSenderAA());
        String toName = (transfer.isReceiverAA() ? "AA:" : "") + DNS.getName(transfer.getReceiver(), transfer.isReceiverAA());
        String title = "#" + transfer.tx_id + " worth $" + MathMan.format(transfer.convertedTotal()) + " | " + fromName + " > " + toName;
        StringBuilder body = new StringBuilder();
        body.append(DNS.getBBUrl((int) transfer.getSender(), transfer.isSenderAA()) + " > " + DNS.getBBUrl((int) transfer.getReceiver(), transfer.isReceiverAA()));

        String note = transfer.note == null ? "~~NO NOTE~~" : transfer.note;
        String url = DNS.getBBUrl(nationid, false) + "&display=bank";

        if (transfer.note != null) {
            body.append(transfer.note);
        }
        body.append("\n").append("From: " + DNS.getBBUrl((int) transfer.sender_id, transfer.isSenderAA()));
        body.append("\n").append("To: " + DNS.getBBUrl((int) transfer.receiver_id, transfer.isReceiverAA()));
        body.append("\n").append("Banker: " + DNS.getBBUrl(transfer.banker_nation, false));
        body.append("\n").append("Date: " + TimeUtil.YYYY_MM_DD_HH_MM_SS.format(new Date(transfer.tx_datetime)));
        body.append("\n").append(ResourceType.resourcesToString(transfer.resources));

        return new AbstractMap.SimpleEntry<>(title, body.toString());
    }
}