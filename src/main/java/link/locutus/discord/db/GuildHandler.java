package link.locutus.discord.db;

import link.locutus.discord.Locutus;
import link.locutus.discord.commands.WarCategory;
import link.locutus.discord.commands.manager.v2.command.CommandBehavior;
import link.locutus.discord.commands.manager.v2.command.CommandRef;
import link.locutus.discord.commands.manager.v2.command.IMessageBuilder;
import link.locutus.discord.commands.manager.v2.impl.discord.DiscordChannelIO;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.commands.manager.v2.impl.pw.NationFilter;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.entities.*;
import link.locutus.discord.db.guild.GuildKey;
import link.locutus.discord.event.game.HourChangeTask;
import link.locutus.discord.event.nation.*;
import link.locutus.discord.event.guild.NewApplicantOnDiscordEvent;
import link.locutus.discord.event.war.WarStatusChangeEvent;
import link.locutus.discord.pnw.AllianceList;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.AlertUtil;
import link.locutus.discord.util.AutoAuditType;
import link.locutus.discord.util.MarkupUtil;
import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.RateLimitUtil;
import link.locutus.discord.util.StringMan;
import link.locutus.discord.util.TimeUtil;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.task.war.WarCard;
import com.google.common.eventbus.Subscribe;
import link.locutus.discord.api.types.Rank;
import link.locutus.discord.api.generated.ResourceType;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GuildHandler {
    private final Guild guild;
    private final GuildDB db;
    private final boolean trackInvites;
    private Map<String, Integer> inviteUses = new ConcurrentHashMap<>();
    private Map<String, Boolean> ignoreInvites = new ConcurrentHashMap<>();
    private final Set<Long> ignorePermanentInvitesFrom = new HashSet<>();
    private final Set<Integer> ignoreIncentivesForNations = new HashSet<>();

    public GuildHandler(Guild guild, GuildDB db) {
        this(guild, db, false);
    }

    public GuildHandler(Guild guild, GuildDB db, boolean trackInvites) {
        this.guild = guild;
        this.db = db;
        Locutus.imp().getExecutor().submit(() -> {
            if (db.isWhitelisted()) setupApplicants();
        });
        this.trackInvites = trackInvites;
        if (trackInvites) {
            db.getGuild().retrieveInvites().queue(new Consumer<List<Invite>>() {
                @Override
                public void accept(List<Invite> invites) {
                    for (Invite invite : invites) {
                        addInvite(invite);
                    }

                }
            });
        }
    }

    public void onGuildInviteCreate(GuildInviteCreateEvent event) {
        addInvite(event.getInvite());
    }

    private void addInvite(Invite invite) {
        if (ignorePermanentInvitesFrom.contains(invite.getInviter().getIdLong())) {
            if (invite.getMaxUses() <= 0) ignoreInvites.put(invite.getCode(), true);
        }
        inviteUses.put(invite.getCode(), invite.getUses());
    }

    public void ignoreInviteFromUser(long user) {
        ignorePermanentInvitesFrom.add(user);
    }

    public void ignoreIncentiveForNation(int nationId) {
        ignoreIncentivesForNations.add(nationId);
    }

    public void setupApplicants() {
        MessageChannel alertChannel = getDb().getOrNull(GuildKey.INTERVIEW_PENDING_ALERTS);
        if (alertChannel == null) return;

        Role appRole = Roles.APPLICANT.toRole(getGuild());
        if (appRole == null) return;

        List<Member> members = getGuild().getMembersWithRoles(appRole);

        for (Member member : members) {
            ByteBuffer meta = getDb().getMeta(member.getIdLong(), NationMeta.DISCORD_APPLICANT);
            if (meta == null) {
                newApplicantOnDiscord(member.getUser());
            }
        }
    }

    public Guild getGuild() {
        return guild;
    }

    public GuildDB getDb() {
        return db;
    }
    private Map<Long, Long> usersWithAppRole = new ConcurrentHashMap<>();

    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        Guild guild = event.getGuild();
        GuildDB db = Locutus.imp().getGuildDB(guild);

        MessageChannel alertChannel = db.getOrNull(GuildKey.INTERVIEW_PENDING_ALERTS);
        if (alertChannel == null) return;

        List<Role> roles = event.getRoles();
        Role appRole = Roles.APPLICANT.toRole(guild);
        if (!roles.contains(appRole)) return;
        User user = event.getUser();
        Long last = usersWithAppRole.get(user.getIdLong());
        long now = System.currentTimeMillis();
        if (last != null && now - last < TimeUnit.MINUTES.toMillis(5)) return;
        usersWithAppRole.put(user.getIdLong(), now);

        newApplicantOnDiscord(user);
    }

    public void newApplicantOnDiscord(User author) {
        newApplicantOnDiscord(author, true);
    }

    public boolean newApplicantOnDiscord(User author, boolean sendDM) {
        new NewApplicantOnDiscordEvent(getGuild(), author).post();

        Guild guild = getGuild();
        GuildDB db = getDb();
        Set<Integer> aaIds = db.getAllianceIds();
        DBNation nation = DiscordUtil.getNation(author);
        if (nation != null && nation.active_m() > 1440) {
            db.setMeta(author.getIdLong(), NationMeta.DISCORD_APPLICANT, new byte[]{1});
            return false;
        }

        MessageChannel alertChannel = db.getOrNull(GuildKey.INTERVIEW_PENDING_ALERTS);
        if (alertChannel == null) return false;

        db.setMeta(author.getIdLong(), NationMeta.DISCORD_APPLICANT, new byte[]{1});

        Role interviewerRole = Roles.INTERVIEWER.toRole(guild);
        if (interviewerRole == null) interviewerRole = Roles.MENTOR.toRole(guild);
        if (interviewerRole == null) interviewerRole = Roles.INTERNAL_AFFAIRS_STAFF.toRole(guild);
        if (interviewerRole == null) interviewerRole = Roles.INTERNAL_AFFAIRS.toRole(guild);

        if (interviewerRole == null) return false;
        boolean mentionInterviewer = true;

        String title = "New applicant";
        String emoji = "Claim";

        StringBuilder body = new StringBuilder();
        body.append("User: " + author.getAsMention() + "\n");
        if (nation != null) {
            if (nation.active_m() > 7200) return false;

            body.append("nation: " + MarkupUtil.markdownUrl(nation.getNation(), nation.getUrl()) + "\n");
            if (nation.getPosition() > 1 && aaIds.contains(nation.getAlliance_id())) {
                body.append("\n\n**ALREADY MEMBER OF " + nation.getAllianceName() + "**\n\n");
                mentionInterviewer = false;
            }
            if (nation.getAlliance_id() != 0 && !aaIds.contains(nation.getAlliance_id())) {
                Locutus.imp().runEventsAsync(events -> Locutus.imp().getNationDB().updateAllNations(events));
                if (nation.getAlliance_id() != 0 && !aaIds.contains(nation.getAlliance_id())) {
                    body.append("\n\n**Already member of AA: " + nation.getAllianceName() + "**\n\n");
                    mentionInterviewer = false;
                    RateLimitUtil.queue(RateLimitUtil.complete(author.openPrivateChannel()).sendMessage("As you're already a member of another alliance, message or ping @" + interviewerRole.getName() + " to (proceed"));
                } else {
                    RateLimitUtil.queue(RateLimitUtil.complete(author.openPrivateChannel()).sendMessage("Thank you for applying. People may be busy with irl things, so please be patient. An IA representative will proceed with your application as soon as they are able."));
                }
            }
        }

        body.append("The first on the trigger, react with the " + emoji + " emoji");

        List<CommandRef> cmds = List.of(
                CM.embed.update.cmd.desc("{description}\nAssigned to {usermention} in {timediff}"),
                CM.interview.create.cmd.user(author.getAsMention())
                );
        String cmdStr = cmds.stream().map(f -> f.toCommandArgs()).collect(Collectors.joining("\n"));

        IMessageBuilder msg = new DiscordChannelIO(alertChannel).create().embed(title, body.toString()).commandButton(CommandBehavior.DELETE_BUTTONS, cmdStr, emoji);
        if (mentionInterviewer) {
            msg.append("^ " + interviewerRole.getAsMention());
        }
        msg.send();
        return true;
    }

    public boolean onMessageReceived(MessageReceivedEvent event) {
        handleIAMessageLogging(event);
        return true;
    }

    private void handleIAMessageLogging(MessageReceivedEvent event) {
        if (event.isWebhookMessage() || db.getExistingIACategory() == null) return;
        // not bot or system or fake user
        // channel starts with id
        // channel parent starts with `interview`
        // submit task to add to database
        User author = event.getAuthor();
        if (author.isSystem() || author.isBot()) return;

        GuildMessageChannel channel = event.getGuildChannel();
        if (!(channel instanceof ICategorizableChannel)) return;
        Category category = ((ICategorizableChannel) channel).getParentCategory();
        if (category == null || !category.getName().toLowerCase().startsWith("interview")) return;
        if (!db.isWhitelisted()) return;

        long date = event.getMessage().getTimeCreated().toInstant().toEpochMilli();
        db.addInterviewMessage(event.getMessage(), false);
    }

    @Subscribe
    public void onNationChangeRankEvent(NationChangePositionEvent event) {
        DBNation current = event.getCurrent();
        DBNation previous = event.getPrevious();

        if (previous.getAlliance_id() == current.getAlliance_id() &&
                previous.getPositionEnum().id > Rank.APPLICANT.id &&
                current.getPositionEnum() == Rank.APPLICANT) {

            MessageChannel channel = db.getOrNull(GuildKey.MEMBER_LEAVE_ALERT_CHANNEL);
            if (channel != null) {
                String type;
                String title;
                if (current.isVacation() == false && (current.active_m() < 2880 && !current.hasProtection())) {
                    type = "ACTIVE NATION SET TO APPLICANT";
                    title = type + ": " + current.getNation() + " | " + "" + Settings.INSTANCE.DNS_URL() + "/nation/" + current.getNation_id() + " | " + current.getAllianceName();
                } else {
                    if (current.isVacation()) {
                        type = "INACTIVE VACATION MODE NATION SET TO APPLICANT";
                    } else if (current.hasProtection()) {
                        type = "INACTIVE PROTECTION MODE NATION SET TO APPLICANT";
                    } else {
                        type = "INACTIVE NATION SET TO APPLICANT";
                    }
                    title = type + ": " + current.getNation() + " | " + "" + Settings.INSTANCE.DNS_URL() + "/nation/" + current.getNation_id() + " | " + current.getAllianceName();
                }
                String message = "**" + title + "**\n" + current.toString() + "\n";
                RateLimitUtil.queueMessage(channel, message, true, 60);
            }
        }
    }

//    @Subscribe
//    public void onMemberEnterVM(NationChangeVacationEvent event) {
//        DBNation previous = event.getPrevious();
//        DBNation current = event.getCurrent();
//
//        if (previous.isVacation() == false && current.isVacation()) {
//            MessageChannel channel = db.getOrNull(GuildKey.MEMBER_LEAVE_ALERT_CHANNEL);
//            if (channel != null) {
//                Rank rank = Rank.byId(previous.getPosition());
//                String title = previous.getNation() + " (" + rank.name() + ") VM";
//                StringBuilder body = new StringBuilder();
//                body.append(MarkupUtil.markdownUrl(current.getNation(), current.getUrl()));
//                body.append("\nActive: " + TimeUtil.secToTime(TimeUnit.MINUTES, current.active_m()));
//                User user = current.getUser();
//                if (user != null) {
//                    body.append("\nUser: " + user.getAsMention());
//                }
//                DiscordUtil.createEmbedCommand(channel, title, body.toString());
//            }
//        }
//    }

//    @Subscribe
//    public void onMemberLeaveVM(NationLeaveVacationEvent event) {
//        DBNation previous = event.getPrevious();
//        DBNation current = event.getCurrent();
//        if (current != null && current.active_m() > 10000) return;
//
//        MessageChannel channel = db.getOrNull(GuildKey.MEMBER_LEAVE_ALERT_CHANNEL);
//        if (channel != null) {
//            Rank rank = Rank.byId(previous.getPosition());
//            String title = previous.getNation() + " (" + rank.name() + ") left VM";
//            StringBuilder body = new StringBuilder();
//            body.append(MarkupUtil.markdownUrl(current.getNation(), current.getUrl()));
//            body.append("\nActive: " + TimeUtil.secToTime(TimeUnit.MINUTES, current.active_m()));
//            User user = current.getUser();
//            if (user != null) {
//                body.append("\nUser: " + user.getAsMention());
//            }
//            DiscordUtil.createEmbedCommand(channel, title, body.toString());
//        }
//    }

    private void onNewApplicant(DBNation current) {
        Set<Integer> aaIds = db.getAllianceIds();
        if (!aaIds.isEmpty()) {

            // New applicant
            if (current.getPositionEnum() == Rank.APPLICANT && aaIds.contains(current.getAlliance_id()) && !aaIds.contains(current.getAlliance_id()) && current.active_m() < 2880) {
                MessageChannel channel = db.getOrNull(GuildKey.MEMBER_LEAVE_ALERT_CHANNEL);
                if (channel != null) {
                    String type = "New Applicant Ingame";
                    User user = current.getUser();
                    if (user != null) {
                        type += " | " + user.getAsMention();
                    }
                    String title = type + ": " + current.getNation() + " | " + "" + Settings.INSTANCE.DNS_URL() + "/nation/" + current.getNation_id() + " | " + current.getAllianceName();

                    String message = "**" + title + "**" + "\n" + current.toString() + "\n";
                    RateLimitUtil.queueMessage(channel, message, true, 60);
                }
            }
        }
    }

    @Subscribe
    public void onNationChangeAlliance(NationChangeAllianceEvent event) {
        DBNation current = event.getCurrent();
        DBNation previous = event.getPrevious();

        onNewApplicant(current);
        autoRoleMemberApp(current);
        Set<Integer> aaIds = db.getAllianceIds();
        if (!aaIds.isEmpty()) {
            if (aaIds.contains(previous.getAlliance_id()) && current.getAlliance_id() != previous.getAlliance_id()) {
                MessageChannel channel = db.getOrNull(GuildKey.MEMBER_LEAVE_ALERT_CHANNEL);
                if (channel != null) {
                    addLeaveMessage(channel, previous, current);
                }
            }
        }
    }

    private void autoRoleMemberApp(DBNation current) {
        if (current == null) return;
        User user = current.getUser();
        if (user == null) return;
        Member member = db.getGuild().getMember(user);
        if (member == null) return;
        try {
            db.getAutoRoleTask().autoRoleMember(member, current);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    private void addLeaveMessage(MessageChannel channel, DBNation previous, DBNation current) {
        // Ignore if inactive and no user in discord
        User user = current.getUser();
        Member member = user == null ? null : db.getGuild().getMember(user);
        if (member == null && current.active_m() > 7200) return;

        Rank rank = previous.getPositionEnum();
        String title = previous.getNation() + " (" + rank.name() + ") left";
        StringBuilder body = new StringBuilder();
        body.append(MarkupUtil.markdownUrl(current.getNation(), current.getUrl()));

        body.append("\nActive: " + TimeUtil.secToTime(TimeUnit.MINUTES, current.active_m()));
        if (user != null) {
            body.append("\nUser: " + user.getAsMention());
        }
        body.append("\nSee: " + CM.channel.memberChannels.cmd.toSlashMention());

        DiscordChannelIO io = new DiscordChannelIO(channel);

        if (user != null && current.active_m() < 2880) {
            try {
                double[] depoTotal = current.getNetDeposits(db, false);
                body.append("\n\nPlease check the following:\n" +
                        "- Discord roles\n" +
                        "- Deposits: `" + ResourceType.resourcesToString(depoTotal) + "` worth: ~$" + MathMan.format(ResourceType.convertedTotal(depoTotal)));
            } catch (IOException e) {
                e.printStackTrace();
            }
            String emoji = "Claim";
            CM.embed.update cmd = CM.embed.update.cmd.desc("{description}\nAssigned to {usermention} in {timediff}");


            io.create().embed(title, body.toString())
                    .commandButton(CommandBehavior.DELETE_BUTTONS, cmd, emoji)
                    .send();
        } else {
            RateLimitUtil.queueMessage(io, new Function<IMessageBuilder, Boolean>() {
                @Override
                public Boolean apply(IMessageBuilder msg) {
                    msg.embed(title, body.toString());
                    return true;
                }
            }, true, 60);
        }
    }

    @Subscribe
    public void onNationDelete(NationDeleteEvent event) {
        autoRoleMemberApp(event.getPrevious().getUser(), null);

        DBNation previous = event.getPrevious();
        MessageChannel channel = db.getOrNull(GuildKey.MEMBER_LEAVE_ALERT_CHANNEL);
        if (channel != null) {
            Rank rank = previous.getPositionEnum();
            String title = previous.getNation() + " (" + rank.name() + ") deleted";
            StringBuilder body = new StringBuilder();
            body.append(MarkupUtil.markdownUrl(previous.getNation(), previous.getUrl()));
            body.append("\nActive: " + TimeUtil.secToTime(TimeUnit.MINUTES, previous.active_m()));
            User user = previous.getUser();
            if (user != null) {
                body.append("\nUser: " + user.getAsMention());
            }
            DiscordUtil.createEmbedCommand(channel, title, body.toString());
        }
    }

    private void autoRoleMemberApp(User user, DBNation nation) {
        if (user == null) return;
        Member member = getGuild().getMember(user);
        if (member == null) return;
        try {
            db.getAutoRoleTask().autoRoleMember(member, nation);
        } catch (RuntimeException ignore) {
            ignore.printStackTrace();
        }
    }

    @Subscribe
    public void onNationActive(NationChangeActiveEvent event) {
        DBNation previous = event.getPrevious();
        DBNation current = event.getCurrent();

        if (previous.active_m() > 7200 && previous.getPositionEnum() == Rank.APPLICANT && current.isVacation() == false && current.active_m() < 15) {
            MessageChannel channel = db.getOrNull(GuildKey.MEMBER_LEAVE_ALERT_CHANNEL);
            if (channel != null) {
                String title = "Inactive Applicant " + current.getNation() + " logged in (just now)";
                StringBuilder body = new StringBuilder();
                body.append(MarkupUtil.markdownUrl(current.getNation(), current.getUrl()));
                body.append("\n").append("Land: " + current.getLand());
                body.append("\n").append("Infra: " + current.getInfra());
                body.append("\n").append("WarIndex: " + current.getWarIndex());
                body.append("\n").append("Defensive Wars: " + current.getDef());
                body.append("\n").append("VM: " + current.isVacation());
                DiscordUtil.createEmbedCommand(channel, title, body.toString());
            }
        }
    }

    @Subscribe
    public void onWarStatusChangeEvent(WarStatusChangeEvent event) {
        if (event.getPrevious().isActive() && !event.getCurrent().isActive()) {
            DBWar war = event.getCurrent();
            WarStatus status = war.getStatus();
            if (status == WarStatus.PEACE || status == WarStatus.EXPIRED) return;
            Set<Integer> aaIds = db.getAllianceIds();
            if (aaIds.isEmpty()) return;

            DBNation winner = status == WarStatus.ATTACKER_VICTORY ? war.getNation(true) : war.getNation(false);
            DBNation loser = status == WarStatus.ATTACKER_VICTORY ? war.getNation(false) : war.getNation(true);
            if (getDb().isAllianceId(winner.getAlliance_id())) {
                handleWonWars(loser, aaIds, war, winner);
            } else if (getDb().isAllianceId(loser.getAlliance_id())) {
                handleLostWars(winner, aaIds, war, loser);
            }

        }
    }

    private void handleWonWars(DBNation enemy, Set<Integer> aaIds, DBWar war, DBNation memberNation) {
        if (enemy == null || aaIds.contains(enemy.getAlliance_id()) || enemy.getNation_id() == memberNation.getNation_id()) return;
        MessageChannel channel = db.getOrNull(GuildKey.WON_WAR_CHANNEL);
        if (enemy.active_m() > 1440 || enemy.isVacation()) return;
        if (channel == null) return;
        String title = "War Won";
        createWarInfoEmbed(title, war, enemy, memberNation, channel);
    }

    private void handleLostWars(DBNation enemy, Set<Integer> aaIds, DBWar war, DBNation memberNation) {
        if (enemy == null || aaIds.contains(enemy.getAlliance_id()) || enemy.getNation_id() == memberNation.getNation_id()) return;
        MessageChannel channel = db.getOrNull(GuildKey.LOST_WAR_CHANNEL);

        if (channel == null) return;
        String title = "War Lost";
        createWarInfoEmbed(title, war, enemy, memberNation, channel);
    }

    private void createWarInfoEmbed(String title, DBWar war, DBNation enemy, DBNation memberNation, MessageChannel channel) {
        boolean isAttacker = war.isAttacker(memberNation);

        WarCard card = new WarCard(war, false);

        String subInfo = card.condensedSubInfo(false);
//        WarAttackParser parser = war.toParser(isAttacker);  TODO FIXME :||remove
//        AttackTypeBreakdown breakdown = parser.toBreakdown();
//
//        String infoEmoji = "War Info";
//        CommandRef infoCommand = CM.war.card.cmd.warId(war.getWarId() + "");
//
//        String costEmoji = "War Cost";
//        CommandRef costCommand = CM.stats_war.warCost.cmd.war(war.getWarId() + "");
//
//        String assignEmoji = "Claim";
//        CommandRef assignCmd = CM.embed.update.cmd.desc("{description}\nAssigned to {usermention} in {timediff}");

        DiscordChannelIO io = new DiscordChannelIO(channel);
        IMessageBuilder msg = io.create();

        StringBuilder builder = new StringBuilder();
        builder.append(war.toUrl() + "\n");

        builder.append(enemy.getNationUrlMarkup(true))
                .append(" | ").append(enemy.getAllianceUrlMarkup(true)).append(":");
//        builder.append(enemy.toCityMilMarkedown()); TODO FIXME :||remove

        String typeStr = isAttacker ? "\uD83D\uDD2A" : "\uD83D\uDEE1";
        builder.append(typeStr);
        builder.append(memberNation.getNationUrlMarkup(true) + " (member):");
//        builder.append("\n").append(memberNation.toCityMilMarkedown()); TODO FIXME :||remove

        String attStr = card.condensedSubInfo(isAttacker);
        String defStr = card.condensedSubInfo(!isAttacker);
        builder.append("```\n" + attStr + "|" + defStr + "```\n");

//        msg.writeTable(title, breakdown.toTableList(), false, builder.toString()); TODO FIXME :||remove
//        msg.commandButton(CommandBehavior.DELETE_PRESSED_BUTTON, infoCommand, infoEmoji);
//        msg.commandButton(CommandBehavior.DELETE_PRESSED_BUTTON, costCommand, costEmoji);
//        msg.commandButton(CommandBehavior.DELETE_PRESSED_BUTTON, assignCmd, assignEmoji);
        msg.cancelButton();
        msg.send();
    }

    public void onDefensiveWarAlert(List<Map.Entry<DBWar, DBWar>> wars, boolean rateLimit) {
        MessageChannel channel = getDb().getOrNull(GuildKey.DEFENSE_WAR_CHANNEL);
        if (channel == null) return;
        onWarAlert(channel, wars, rateLimit, false);
    }

    public void onOffensiveWarAlert(List<Map.Entry<DBWar, DBWar>> wars, boolean rateLimit) {
        MessageChannel channel = getDb().getOrNull(GuildKey.OFFENSIVE_WAR_CHANNEL);
        if (channel == null) return;
        onWarAlert(channel, wars, rateLimit, true);
    }

    public void onWarAlert(MessageChannel channel, List<Map.Entry<DBWar, DBWar>> wars, boolean rateLimit, boolean offensive) {
        if (wars.isEmpty()) return;

        Set<Integer> enemies = db.getCoalition(Coalition.ENEMIES);
        Set<Integer> dnrAAs = db.getCoalition(Coalition.DNR);
        Set<Integer> counterAAs = db.getCoalition(Coalition.COUNTER);
        Set<Integer> ignoreFA = db.getCoalition(Coalition.IGNORE_FA);

        Map<DBWar, Set<String>> pingUserOrRoles = new HashMap<>();
        Set<DBWar> dnrViolations = new HashSet<>();

        Function<DBNation, Boolean> dnr = getDb().getCanRaid();

        Guild alertGuild = (channel instanceof GuildMessageChannel) ? ((GuildMessageChannel) channel).getGuild() : guild;

        Role milcomRole = Roles.MILCOM.toRole(alertGuild);
        Role faRole = Roles.FOREIGN_AFFAIRS.toRole(alertGuild);

        Set<Integer> aaIds = db.getAllianceIds();
        WarCategory warCat = db.getWarChannel();

        for (Map.Entry<DBWar, DBWar> entry : wars) {
            DBWar war = entry.getValue();
            DBNation attacker = war.getNation(true);
            DBNation defender = war.getNation(false);

            if (offensive) {
                if (defender != null && !dnr.apply(defender)) {
                    boolean violation = true;
                    {
                        if (defender != null && warCat != null) {
                            WarCategory.WarRoom warRoom = warCat.get(defender, false, false, false);
                            if (warRoom != null && warRoom.channel != null && warRoom.isParticipant(attacker, false)) {
                                violation = false;
                            }
                        }
                        if (violation) {
                            CounterStat counterStat = war.getCounterStat();
                            if (counterStat != null && counterStat.type == CounterType.IS_COUNTER) violation = false;
                            if (violation) {
                                dnrViolations.add(war);
                                if (faRole != null) {
                                    pingUserOrRoles.computeIfAbsent(war, f -> new HashSet<>()).add(faRole.getAsMention());
                                }
                                User user = attacker.getUser();
                                if (user != null) {
                                    Member member = guild.getMember(user);
                                    if (member != null) {
                                        pingUserOrRoles.computeIfAbsent(war, f -> new HashSet<>()).add(member.getAsMention());
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (defender != null) {
                    boolean pingMilcom = defender.getPosition() >= Rank.MEMBER.id || defender.active_m() < 2440 || (aaIds.contains(defender.getAlliance_id()) && defender.active_m() < 7200);
                    if (pingMilcom && milcomRole != null) {
                        if (pingMilcom && attacker != null && enemies.contains(attacker.getAlliance_id()) && attacker.getDef() >= 3) {
                            pingMilcom = false;
                        }
                        if (pingMilcom) {
                            NationFilter allowedMentions = db.getOrNull(GuildKey.MENTION_MILCOM_FILTER);
                            if (allowedMentions != null) {
                                if (!allowedMentions.test(attacker) && !allowedMentions.test(defender)) {
                                    pingMilcom = false;
                                }
                            }
                            if (pingMilcom && GuildKey.MENTION_MILCOM_COUNTERS.getOrNull(db) != Boolean.TRUE) {
                                CounterStat counterStat = war.getCounterStat();
                                if (counterStat != null && counterStat.type == CounterType.IS_COUNTER && !enemies.contains(attacker.getAlliance_id())) {
                                    pingMilcom = false;
                                    pingUserOrRoles.computeIfAbsent(war, f -> new HashSet<>()).add("No `" + Roles.MILCOM.name() + "` ping as `" + GuildKey.MENTION_MILCOM_COUNTERS.name() + "` is NOT `true`." );
                                }
                            }
                        }
                        if (pingMilcom) {
                            if (getDb().getCoalition(Coalition.FA_FIRST).contains(attacker.getAlliance_id()) && faRole != null) {
                                String response = "```" + DNS.getName(attacker.getAlliance_id(), true) + " is marked as FA_FIRST:\n" +
                                        "- Solicit peace in that alliance embassy before taking military action\n" +
                                        "- Set a reminder for 24h, to remind milcom if peace has not been obtained\n" +
                                        "- React to this message```\n" + faRole.getAsMention();
                                pingUserOrRoles.computeIfAbsent(war, f -> new HashSet<>()).add(response);
                            } else {
                                pingUserOrRoles.computeIfAbsent(war, f -> new HashSet<>()).add(milcomRole.getAsMention());
                            }
                        }
                    }
                    User user = defender.getUser();
                    if (user != null) {
                        Member member = guild.getMember(user);
                        if (member != null) {
                            pingUserOrRoles.computeIfAbsent(war, f -> new HashSet<>()).add(member.getAsMention());
                        }
                    }
                    if (attacker != null && defender.getPosition() == Rank.APPLICANT.id && !enemies.contains(attacker.getAlliance_id())) {
                        if (counterAAs.contains(attacker.getAlliance_id())) {
                            String msg = "```" + DNS.getName(attacker.getAlliance_id(), true) + " is added to the COUNTER coalition which forbids raiding inactive applicants (typically due to them countering for their own applicants)```";
                            pingUserOrRoles.computeIfAbsent(war, f -> new HashSet<>()).add(msg);
                            if (milcomRole != null) {
                                pingUserOrRoles.computeIfAbsent(war, f -> new HashSet<>()).add(milcomRole.getAsMention());
                            }
                            if (faRole != null && !counterAAs.contains(attacker.getAlliance_id())) {
                                pingUserOrRoles.computeIfAbsent(war, f -> new HashSet<>()).add(faRole.getAsMention());
                            }
                        } else if (dnrAAs.contains(attacker.getAlliance_id())) {
                            String msg = "```" + DNS.getName(attacker.getAlliance_id(), true) + " is added to DNR coalition which forbids raiding inactive applicants```";
                            pingUserOrRoles.computeIfAbsent(war, f -> new HashSet<>()).add(msg);
                            if (counterAAs.contains(attacker.getAlliance_id()) && milcomRole != null) {
                                pingUserOrRoles.computeIfAbsent(war, f -> new HashSet<>()).add(milcomRole.getAsMention());
                            }
                            if (faRole != null && !counterAAs.contains(attacker.getAlliance_id()) && !ignoreFA.contains(attacker.getAlliance_id())) {
                                pingUserOrRoles.computeIfAbsent(war, f -> new HashSet<>()).add(faRole.getAsMention() + " (add enemy to IGNORE_FA coalition to not ping FA- or add to COUNTER to ping milcom)");
                            }
                        }
                    }
                }
            }
        }

        Locutus.imp().getExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (rateLimit && wars.size() > 1) {
                        String title = wars.size() + " new " + (offensive ? "Offensive" : "Defensive") + " wars";
                        StringBuilder body = new StringBuilder();
                        StringBuilder footer = new StringBuilder();
                        StringBuilder bodyRaw = new StringBuilder();

                        for (Map.Entry<DBWar, DBWar> pair : wars) {
                            DBWar war = pair.getValue();
                            DBNation attacker = war.getNation(true);
                            DBNation defender = war.getNation(false);

                            CounterStat counterStat = war.getCounterStat();
                            String counterStatStr = counterStat == null ? "" : counterStat.type.getDescription();

                            String attUrl = attacker != null ? attacker.getUrl() : DNS.getNationUrl(war.getAttacker_id());
                            String defUrl = defender != null ? defender.getUrl() : DNS.getNationUrl(war.getDefender_id());

                            String attAAUrl = attacker != null ? attacker.getAllianceUrl() : DNS.getNationUrl(war.getAttacker_aa());
                            String defAAUrl = defender != null ? defender.getAllianceUrl() : DNS.getNationUrl(war.getDefender_aa());
                            {
                                if (!offensive && !counterStatStr.isEmpty())
                                    body.append("**" + counterStatStr + "**\n");
                                if (offensive && dnrViolations.contains(war)) body.append("**DNR VIOLATION**\n");
                                body.append(MarkupUtil.markdownUrl("War Link", war.toUrl()) + "\n");
                                if (attacker != null) {
                                    body.append(attacker.getNationUrlMarkup(true));
                                    if (attacker.getAlliance_id() != 0) {
                                        body.append(" | " + attacker.getAllianceUrlMarkup(true));
                                        if (attacker.getPosition() < Rank.MEMBER.id) {
                                            body.append("- " + attacker.getPositionEnum());
                                        }
                                    }
                                    // TODO FIXME :||remove war info
//                                    body.append(" ```\nc" + attacker.getCities() + " | " + attacker.getAvg_infra() + "\uD83C\uDFD7 | " + MathMan.format(attacker.getScore()) + "ns``` ");
//                                    body.append("``` " + attacker.getSoldiers()).append(" \uD83D\uDC82").append(" | ");
//                                    body.append(attacker.getTanks()).append(" \u2699").append(" | ");
//                                    body.append(attacker.getAircraft()).append(" \u2708").append(" | ");
//                                    body.append(attacker.getShips()).append(" \u26F5").append("```");
                                }
                                if (defender != null) {
                                    body.append(defender.getNationUrlMarkup(true));
                                    if (defender.getAlliance_id() != 0) {
                                        body.append(" | " + defender.getAllianceUrlMarkup(true));
                                        if (defender.active_m() > 1440) {
                                            body.append(" | " + TimeUtil.secToTime(TimeUnit.MINUTES, defender.active_m()));
                                        }
                                        if (defender.getPosition() < Rank.MEMBER.id) {
                                            body.append("- " + defender.getPositionEnum());
                                        }
                                    }
                                    if (!offensive && defender.getPosition() >= Rank.MEMBER.id) {
                                        User user = defender.getUser();
                                        if (user != null) {
                                            body.append(" | " + user.getAsMention());
                                        }
                                    }
                                    // TODO FIXME :||remove war info
//                                    body.append(" ```\nc" + defender.getCities() + " | " + defender.getAvg_infra() + "\uD83C\uDFD7 | " + MathMan.format(defender.getScore()) + "ns``` ");
//                                    body.append("```" + defender.getSoldiers()).append(" \uD83D\uDC82").append(" | ");
//                                    body.append(defender.getTanks()).append(" \u2699").append(" | ");
//                                    body.append(defender.getAircraft()).append(" \u2708").append(" | ");
//                                    body.append(defender.getShips()).append(" \u26F5").append("```");
                                    body.append("\n");
                                }
                                body.append("\n");
                            }
                            {
                                if (!offensive && !counterStatStr.isEmpty())
                                    bodyRaw.append("**" + counterStatStr + "**\n");
                                if (offensive && dnrViolations.contains(war)) body.append("**DNR VIOLATION**\n");
                                bodyRaw.append("**War**: <" + war.toUrl() + ">\n");
                                bodyRaw.append("Attacker: <" + attUrl + ">");
                                if (attacker != null && attacker.getPosition() > 0) {
                                    bodyRaw.append("- " + attacker.getPositionEnum());
                                }
                                bodyRaw.append("\n");
                                if (!offensive && war.getAttacker_aa() != 0) {
                                    bodyRaw.append("AA: <" + attAAUrl + "> (" + DNS.getName(war.getAttacker_aa(), true) + ")\n");
                                }
                                if (attacker != null) {
                                    bodyRaw.append(attacker.toMarkdown(false, true, true));
                                }

                                bodyRaw.append("Defender: <" + defUrl + ">");
                                if (defender != null && defender.getPosition() > 0) {
                                    bodyRaw.append("- " + defender.getPositionEnum());
                                }
                                bodyRaw.append("\n");
                                if (offensive && war.getAttacker_aa() != 0) {
                                    bodyRaw.append("AA: <" + defAAUrl + "> (" + DNS.getName(war.getDefender_aa(), true) + ")\n");
                                }
                                if (defender != null) {
                                    bodyRaw.append(defender.toMarkdown(false, true, true));
                                }
                                if (dnrViolations.contains(war)) {
                                    bodyRaw.append("^ violates the `Do Not Raid` list. If you were not asked to attack (e.g. as a counter), please offer peace (Note: This is an automated message)\n");
                                }
                                Set<String> mentions = pingUserOrRoles.get(war);
                                if (mentions != null && !mentions.isEmpty()) {
                                    bodyRaw.append("^ " + StringMan.join(mentions, "\n")).append("\n");
                                }
                                bodyRaw.append("\n");
                            }
                        }

                        if (!dnrViolations.isEmpty()) {
                            footer.append("To modify the `Do Not Raid` see: " + CM.coalition.list.cmd.toSlashMention() + " / " + CM.settings.info.cmd.toSlashMention() + " with key `" + GuildKey.DO_NOT_RAID_TOP_X.name() + "`\n");
                        }

                        RateLimitUtil.queueMessage(new DiscordChannelIO(channel), new Function<IMessageBuilder, Boolean>() {
                            @Override
                            public Boolean apply(IMessageBuilder msg) {
                                if (title.length() + 10 + body.length() < 2000) {
                                    msg.embed(title, body.toString());

                                    Set<String> allMentions = new HashSet<>();
                                    for (Set<String> pings : pingUserOrRoles.values()) allMentions.addAll(pings);
                                    if (!allMentions.isEmpty()) {
                                        footer.append(StringMan.join(allMentions, " "));
                                    }

                                    if (footer.length() > 0) {
                                        msg.append(footer.toString());
                                    }
                                } else {
                                    String full = "__**" + title + "**__\n" + bodyRaw.toString();
                                    msg.append(full);
                                }
                                return true;
                            }
                        }, true, null);
                    } else {
                        for (Map.Entry<DBWar, DBWar> entry : wars) {
                            DBWar war = entry.getValue();
                            WarCard card = new WarCard(war.getWarId());
                            CounterStat stat = card.getCounterStat();
                            IMessageBuilder msg = card.embed(new DiscordChannelIO(channel).create(), false, true, false);


                            StringBuilder footer = new StringBuilder();
                            if (dnrViolations.contains(war)) {
                                footer.append("^ violates the `Do Not Raid` (DNR) list. If you were not asked to attack (e.g. as a counter), please offer peace (Note: This is an automated message)\n");
                                footer.append("(To modify the DNR: " + CM.coalition.list.cmd.toSlashMention() + " / " + CM.settings.info.cmd.toSlashMention() + " with key `" + GuildKey.DO_NOT_RAID_TOP_X.name() + "`\n");
                            }
                            List<String> tips = new ArrayList<>();

                            Set<String> mentions = pingUserOrRoles.get(war);
                            if (mentions != null && !mentions.isEmpty()) {
                                footer.append(StringMan.join(mentions, " ")).append("\n");
                            }


                            DBNation attacker = war.getNation(true);
                            DBNation defender = war.getNation(false);
                            if (!offensive && attacker != null && defender != null && footer.length() > 0 && defender.getPosition() >= Rank.MEMBER.id && db.isWhitelisted() && defender.active_m() < 15000 && !mentions.isEmpty()) {
                                Set<Integer> enemies = db.getCoalition(Coalition.ENEMIES);

                                if (stat != null && stat.type == CounterType.IS_COUNTER && !enemies.contains(attacker.getAlliance_id())) {
                                    tips.add("This is a counter for one of your wars. We can provide solely military advice and diplomatic assistance");
                                }

                                if (!enemies.contains(attacker.getAlliance_id())) {
                                    if (faRole != null) {
                                        String enemyStr = "(Or mark as enemy "
                                                /* + CM.coalition.create.cmd.alliances(attacker.getAlliance_id() + "").coalitionName(Coalition.ENEMIES.name()) */ + ")";
                                        tips.add("Please ping @\u200B" + faRole.getName() + " to get help negotiating peace. " + (attacker.getAlliance_id() == 0 ? "" : enemyStr));
                                    }
                                    if (milcomRole != null) {
                                        tips.add("Please ping @\u200B" + milcomRole.getName() + " to get military advice");
                                    }
                                }
                            }

                            if (footer.length() > 0 || !tips.isEmpty()) {
                                String footerMsg = footer.toString().trim();
                                if (!tips.isEmpty()) {
                                    footerMsg += " ```- " + StringMan.join(tips, "\n- ") + "```";
                                }
                                msg.append(footerMsg);
                            }
                            msg.send();
                        }
                    }
                } catch (InsufficientPermissionException e) {
                    if (offensive) {
                        db.deleteInfo(GuildKey.OFFENSIVE_WAR_CHANNEL);
                    } else {
                        db.deleteInfo(GuildKey.DEFENSE_WAR_CHANNEL);
                    }
                }
            }
        });
    }

    public BiFunction<DBWar, DBWar, Boolean> shouldAlertWar() {
        return new BiFunction<>() {
            private Set<Integer> trackedOff;
            private Set<Integer> trackedDef;

            @Override
            public Boolean apply(DBWar previous, DBWar current) {
                if (previous != null) return false;
                DBNation attacker = current.getNation(true);
                DBNation defender = current.getNation(false);
                if (attacker == null || defender == null) {
                    return false;
                }
                if (defender.active_m() > 10000 && defender.getAlliance_id() == 0) {
                    return false;
                }
                Boolean hideApps = db.getOrNull(GuildKey.HIDE_APPLICANT_WARS);
                if (hideApps == null && !db.hasAlliance()) {
                    hideApps = true;
                }

                if (trackedDef == null) {
                    trackedDef = getTrackedWarAlliances(false);
                }

                if (trackedDef.contains(current.getDefender_aa())) {
                    // defensive
                    if (hideApps == Boolean.TRUE && defender.getPosition() <= 1) {
                        return false;
                    }
                } else {
                    if (trackedOff == null) {
                        trackedOff = getTrackedWarAlliances(true);
                    }
                    if (trackedOff.contains(current.getAttacker_aa())) {
                        // offensive
                    }
                }
                return true;
            }
        };
    }

    public Set<Integer> getTrackedWarAlliances(boolean offensive) {
        Set<Integer> tracked = new HashSet<>();
        Set<Integer> aaIds = db.getAllianceIds();
        if (!aaIds.isEmpty()) {
            for (int id : aaIds) {
                if (id != 0) tracked.add(id);
            }
        }

        if (offensive && (aaIds.isEmpty() || db.getOrNull(GuildKey.SHOW_ALLY_OFFENSIVE_WARS) == Boolean.TRUE)) {
            tracked.addAll(db.getCoalition(Coalition.ALLIES));
        }
        if (!offensive && (aaIds.isEmpty() || db.getOrNull(GuildKey.SHOW_ALLY_DEFENSIVE_WARS) == Boolean.TRUE)) {
            tracked.addAll(db.getCoalition(Coalition.ALLIES));
        }

        Set<Integer> untracked = db.getCoalition(Coalition.UNTRACKED);
        tracked.removeAll(untracked);
        tracked.remove(0);
        return tracked;
    }

    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (trackInvites) {
            User user = event.getUser();
            getGuild().retrieveInvites().queue(new Consumer<List<Invite>>() {
                @Override
                public void accept(List<Invite> invites) {
                    Map.Entry<Integer, Long> referPair = getNationTimestampReferrer(user.getIdLong());
                    Invite inviteUsed = null;
                    boolean foundInvite = false;

                    for (Invite invite : invites) {
                        Integer previousUses = inviteUses.get(invite.getCode());
                        if (previousUses != null && previousUses == invite.getUses() - 1) {
                            if (foundInvite) {
                                inviteUsed = null;
                            } else {
                                foundInvite = true;
                                inviteUsed = invite;
                            }
                        }
                        addInvite(invite);
                    }
                    if (inviteUsed != null && referPair == null) {
                        if (!ignoreInvites.getOrDefault(inviteUsed.getCode(), false)) {
                            User inviter = inviteUsed.getInviter();
                            DBNation inviterNation = DiscordUtil.getNation(inviter);
                            if (inviterNation != null) {
                                setReferrer(user, inviterNation);
                            }
                        }
                    }
                }
            });
        }
    }

    public Map.Entry<Integer, Long> getNationTimestampReferrer(long userId) {
        ByteBuffer meta = db.getMeta(userId, NationMeta.REFERRER);
        if (meta == null) return null;

        return new AbstractMap.SimpleEntry<>(meta.getInt(), meta.getLong());
    }

    public void setReferrer(User user, DBNation referrer) {
        ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES + Long.BYTES);
        buf.putInt(referrer.getNation_id());
        buf.putLong(System.currentTimeMillis());
        db.setMeta(user.getIdLong(), NationMeta.REFERRER, buf.array());
    }

    @Subscribe
    public void onTurnChange(HourChangeTask event) {
        handleInactiveAudit();
    }

    public void handleInactiveAudit() {
        if (!GuildKey.MEMBER_AUDIT_ALERTS.has(db, false)) return;
        Set<AutoAuditType> disabledAudits = db.getOrNull(GuildKey.DISABLED_MEMBER_AUDITS);
        if (disabledAudits != null && disabledAudits.contains(AutoAuditType.INACTIVE)) return;

        AllianceList alliance = db.getAllianceList();
        if (alliance == null || alliance.isEmpty()) return;
        long turnStart = TimeUtil.getHour() - 24 * 3;
        long timeCheckStart = TimeUtil.getTimeFromHour(turnStart - 1);
        long timeCheckEnd = TimeUtil.getTimeFromHour(turnStart);

        alliance.getNations(f -> f.getPositionEnum().id > Rank.APPLICANT.id && f.isVacation() == false && f.lastActiveMs() > timeCheckStart && f.lastActiveMs() < timeCheckEnd).forEach(nation -> {
            AlertUtil.auditAlert(nation, AutoAuditType.INACTIVE, f ->
                    AutoAuditType.INACTIVE.message
            );
        });
    }

    @Subscribe
    public void onNationCreate(NationCreateEvent event) {
        onNewApplicant(event.getCurrent());
    }

    @Subscribe
    public void onNationChangePosition(NationChangePositionEvent event) {
        DBNation nation = event.getCurrent();
        autoRoleMemberApp(event.getCurrent());
    }

//    @Subscribe
//    public void onPeaceChange(WarPeaceStatusEvent event) {
//        MessageChannel channel = db.getOrNull(GuildKey.WAR_PEACE_ALERTS);
//        if (channel == null) return;
//
//        DBWar previous = event.getPrevious();
//        DBWar current = event.getCurrent();
//
//        DBNation attacker = current.getNation(true);
//        DBNation defender = current.getNation(false);
//        if (attacker != null && defender != null) {
//            boolean attInactiveNone = attacker.getAlliance_id() == 0 && attacker.active_m() > 2880;
//            boolean defInactiveNone = defender.getAlliance_id() == 0 && defender.active_m() > 2880;
//            if (attInactiveNone || defInactiveNone) {
//                return;
//            }
//            Boolean hideApps = db.getOrNull(GuildKey.HIDE_APPLICANT_WARS);
//            if (hideApps == Boolean.TRUE && (attacker.getPosition() <= Rank.APPLICANT.id || defender.getPosition() <= Rank.APPLICANT.id)) {
//               return;
//            }
//        }
//
//        String title = "War " + previous.getStatus() + " -> " + current.getStatus();
//        StringBuilder body = new StringBuilder();
//        body.append("War: " + MarkupUtil.markdownUrl("Click Here", current.toUrl())).append("\n");
//        body.append("ATT: " + DNS.getMarkdownUrl(current.getAttacker_id(), false) +
//                " | " + DNS.getMarkdownUrl(current.getAttacker_aa(), true)).append("\n");
//        body.append("DEF: " + DNS.getMarkdownUrl(current.getDefender_id(), false) +
//                " | " + DNS.getMarkdownUrl(current.getDefender_aa(), true)).append("\n");
//        DiscordUtil.createEmbedCommand(channel, title, body.toString());
//    }
}
