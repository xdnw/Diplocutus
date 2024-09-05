package link.locutus.discord.commands.manager.v2.impl.pw.commands;

import link.locutus.discord.Locutus;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.commands.manager.v2.binding.annotation.*;
import link.locutus.discord.commands.manager.v2.command.CommandBehavior;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.impl.discord.DiscordChannelIO;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.RolePermission;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.config.Messages;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.ReportManager;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.db.entities.DiscordBan;
import link.locutus.discord.db.entities.LoginFactor;
import link.locutus.discord.db.entities.LoginFactorResult;
import link.locutus.discord.db.entities.NationMeta;
import link.locutus.discord.db.guild.SheetKey;
import link.locutus.discord.pnw.NationOrAlliance;
import link.locutus.discord.pnw.RegisteredUser;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.*;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.sheet.SpreadSheet;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static link.locutus.discord.api.generated.ResourceType.subtract;
import static link.locutus.discord.db.guild.GuildKey.REPORT_ALERT_CHANNEL;
import static link.locutus.discord.util.MarkupUtil.sheetUrl;
import static link.locutus.discord.api.generated.ResourceType.add;
import static link.locutus.discord.util.DNS.getAllianceUrl;
import static link.locutus.discord.util.DNS.getName;
import static link.locutus.discord.util.DNS.getNationUrl;
import static link.locutus.discord.api.generated.ResourceType.resourcesToArray;
import static link.locutus.discord.util.discord.DiscordUtil.getGuildName;
import static link.locutus.discord.util.discord.DiscordUtil.getGuildUrl;
import static link.locutus.discord.util.discord.DiscordUtil.getUserName;
import static link.locutus.discord.util.discord.DiscordUtil.userUrl;

public class ReportCommands {
    @Command(desc=  "Generate a sheet of all the community reports for players")
    @RolePermission(value = {Roles.INTERNAL_AFFAIRS, Roles.INTERNAL_AFFAIRS_STAFF, Roles.ECON_STAFF}, any = true)
    public String reportSheet(@Me IMessageIO io, @Me GuildDB db, ReportManager manager, @Switch("s") SpreadSheet sheet) throws IOException, GeneralSecurityException, NoSuchFieldException, IllegalAccessException {
        List<ReportManager.Report> reports = manager.loadReports(null);

        if (sheet == null) {
            sheet = SpreadSheet.create(db, SheetKey.REPORTS_SHEET);
        }


        ReportManager.ReportHeader header = new ReportManager.ReportHeader();
        List<String> column = new ArrayList<>(Arrays.asList(header.getHeaderNames()));
        sheet.addRow(column);

        for (ReportManager.Report report : reports) {
            String natUrl = sheetUrl(getName(report.nationId, false), getNationUrl(report.nationId));
            String discordUrl = sheetUrl(getUserName(report.discordId), userUrl(report.discordId, false));

            String reporterNatUrl = sheetUrl(getName(report.reporterNationId, false), getNationUrl(report.reporterNationId));
            String reporterDiscordUrl = sheetUrl(getUserName(report.reporterDiscordId), userUrl(report.reporterDiscordId, false));

            String reporterAlliance = sheetUrl(getName(report.reporterAllianceId, true), getAllianceUrl(report.reporterAllianceId));
            String reporterGuild = sheetUrl(getGuildName(report.reporterGuildId), getGuildUrl(report.reporterGuildId));

            column.set(0, report.reportId + "");
            column.set(1, natUrl + "");
            column.set(2, discordUrl + "");
            column.set(3, report.type.name());
            column.set(4, reporterNatUrl + "");
            column.set(5, reporterDiscordUrl + "");
            column.set(6, reporterAlliance + "");
            column.set(7, reporterGuild + "");
            column.set(8, report.message);
            column.set(9, StringMan.join(report.imageUrls, "\n"));
            column.set(10, report.forumUrl);
            column.set(11, report.newsUrl);
            column.set(12, report.date + "");
            column.set(13, report.approved + "");

            sheet.addRow(column);
        }
        sheet.updateClearCurrentTab();
        sheet.updateWrite();
        sheet.attach(io.create(), "reports").send();
        return null;
    }

    @Command(desc = "Import the legacy sheet of reports from a google sheet\n" +
            "Expects the columns: `Discord ID`, `Nation ID`, `Reason`, `Reporting Entity`")
    @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.INTERNAL_AFFAIRS}, root = true, any = true)
    public String importLegacyBlacklist(ReportManager reportManager, @Me GuildDB db, @Me DBNation me, @Me User author, SpreadSheet sheet) {
        List<List<Object>> rows = sheet.fetchAll(null);
        List<Object> header = rows.get(0);

        int discordIndex = header.indexOf("Discord ID");
        int nationIdIndex = header.indexOf("Nation ID");
        int reasonIndex = header.indexOf("Reason");
        int reportingEntityIndex = header.indexOf("Reporting Entity");
        if (discordIndex == -1) {
            return "`Discord ID` column not found";
        }
        if (nationIdIndex == -1) {
            return "`Nation ID` column not found";
        }
        if (reasonIndex == -1) {
            return "`Reason` column not found";
        }

        List<ReportManager.Report> reports = new ArrayList<>();
        // iterate rows

        for (int i = 1; i < rows.size(); i++) {
            List<Object> row = rows.get(i);
            if (row.isEmpty()) continue;

            String nationIdStr = row.get(nationIdIndex).toString();
            String discordIdsStr = row.get(discordIndex).toString();
            int nationId = nationIdStr.equalsIgnoreCase("unknown") ||
                    nationIdStr.equalsIgnoreCase("deleted") ||
                    nationIdStr.isEmpty() ? 0 : Integer.parseInt(nationIdStr);
            Set<Long> discordIds = new HashSet<>();

            if (!discordIdsStr.equalsIgnoreCase("unknown") &&
                    !discordIdsStr.equalsIgnoreCase("deleted") &&
                    !discordIdsStr.isEmpty()) {
                for (String idStr : discordIdsStr.split(",")) {
                    discordIds.add(Long.parseLong(idStr.trim()));
                }
            }
            if (discordIds.isEmpty()) discordIds.add(0L);

            String reason = row.get(reasonIndex).toString();
            String reasonLower = reason.toLowerCase();

            ReportManager.ReportType type;
            if (reasonLower.contains("attacking") || reason.contains("declaring war")) {
                type = ReportManager.ReportType.THREATS_COERCION;
            } else if (reasonLower.contains("scam") || reasonLower.contains("$")) {
                type = ReportManager.ReportType.FRAUD;
            } else if (reasonLower.contains("default")) {
                type = ReportManager.ReportType.BANK_DEFAULT;
            } else if (reasonLower.contains("multi")) {
                type = ReportManager.ReportType.MULTI;
            } else {
                type = ReportManager.ReportType.FRAUD;
            }

            String reasonFinal = reason + "\nnote: `legacy blacklist`";
            if (reportingEntityIndex != -1 && reportingEntityIndex < row.size()) {
                Object entityObj = row.get(reportingEntityIndex);
                if (entityObj != null && !entityObj.toString().isEmpty()) {
                    reasonFinal += "\nreporting entity: " + entityObj;
                }
            }
            for (long discordId : discordIds) {
                if (nationId == 0 && discordId == 0) continue;

                ReportManager.Report report = new ReportManager.Report(
                        nationId,
                        discordId,
                        type,
                        me.getNation_id(),
                        author.getIdLong(),
                        me.getAlliance_id(),
                        db.getIdLong(),
                        reasonFinal,
                        new ArrayList<>(),
                        "",
                        "",
                        System.currentTimeMillis(),
                        true
                );
                reports.add(report);
            }
        }


        // ignore reports already exists
        List<ReportManager.Report> existing = reportManager.loadReports();
        Map<Integer, Set<String>> existingMap = new HashMap<>();
        for (ReportManager.Report report : existing) {
            int nationId = report.nationId;
            String msg = report.message;
            existingMap.computeIfAbsent(nationId, k -> new HashSet<>()).add(msg);
        }

        reports.removeIf(f -> existingMap.getOrDefault(f.nationId, Collections.emptySet()).contains(f.message));

        if (reports.isEmpty()) {
            return "No new reports to add";
        }

        reportManager.saveReports(reports);

        return "Added " + reports.size() + " reports. See " + CM.report.sheet.generate.cmd.toSlashMention();
    }

    @Command(desc = "Report a nation or user's game behavior to the bot for things such as fraud\n" +
            "Note: Submitting spam or maliciously false reports may result in being locked out of reporting commands")
//    @RolePermission(value = {Roles.INTERNAL_AFFAIRS, Roles.INTERNAL_AFFAIRS_STAFF, Roles.ECON_STAFF}, any = true)
    public String createReport(@Me DBNation me, @Me User author, @Me GuildDB db, @Me IMessageIO io, @Me JSONObject command,
                         ReportManager reportManager,
                         ReportManager.ReportType type,
                         @Arg("Description of report") @TextArea String message,
                         @Default @Arg("Nation to report") DBNation nation,
                         @Default @Arg("Discord user to report") Long discord_user_id,
                         @Arg("Image evidence of report") @Switch("i") String imageEvidenceUrl,
                         @Arg("Link to relevant forum post") @Switch("p") String forum_post,
                         @Arg("Link to relevant news post") @Switch("m") String news_post,
                               @Switch("u") ReportManager.Report updateReport,
                               @Switch("f") boolean force) {
        Map.Entry<String, Long> ban = reportManager.getBan(me);
        if (ban != null) {
            return "You were banned from reporting on " + DiscordUtil.timestamp(ban.getValue(), null) + " for `" + ban.getKey() + "`";
        }

        Integer nationId = nation == null ? null : nation.getId();
        int reporterNationId = me.getId();
        long reporterUserId = author.getIdLong();
        long reporterGuildId = db.getIdLong();
        int reporterAlliance = me.getAlliance_id();

        ReportManager.Report existing = null;
        if (updateReport != null) {
            existing = updateReport;
            if (!existing.hasPermission(me, author, db)) {
                return "You do not have permission to edit this report: `#" + updateReport.reportId +
                        "` (owned by nation:" + DNS.getName(existing.reporterNationId, false) + ")\n" +
                        "To add a comment: " + CM.report.comment.add.cmd.toSlashMention();
            }
            // set the missing fields to the values from this report
            if (nationId == null) {
                nationId = existing.reporterNationId;
            }
            if (discord_user_id == null) {
                discord_user_id = existing.reporterDiscordId;
            }
            if (type == null) {
                type = existing.type;
            }
            if (message == null) {
                message = existing.message;
            }
            if (forum_post == null && existing.forumUrl != null && !existing.forumUrl.isEmpty()) {
                forum_post = existing.forumUrl;
            }
            if (news_post == null && existing.newsUrl != null && !existing.newsUrl.isEmpty()) {
                news_post = existing.newsUrl;
            }
            // Keep reporter info if updating
            reporterNationId = existing.reporterNationId;
            reporterUserId = existing.reporterDiscordId;
            reporterGuildId = existing.reporterGuildId;
            reporterAlliance = existing.reporterAllianceId;
        }

        if (nationId == null && discord_user_id == null) {
            // say must provide one of either
            // post link for how to get discord id <url>
            return """
                    You must provide either a `nation` or a `discord_user_id` to report
                    To get a discord user id, right click on the user and select `Copy ID`
                    See: <https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID->""";
        }
//        if (forum_post == null && news_post == null) {
//            return "No argument provided\n" + Messages.FORUM_NEWS_ERROR;
//        }

        // At least one forum post or news report must be attached
        Set<Long> supportedServers = new HashSet<>(Arrays.asList(
                869424139037990912L, // Ducc News Network
                446601982564892672L, // Royal Orbis News
                821587932384067584L, // Orbis Crowned News
                827905979575959629L, // Very Good Media
                353730979816407052L, // Pirate Island Times
                1022224780751011860L, // Micro Minute
                580481635645128745L, // Thalmoria
                1139041525817409539L, // Orbis Business & Innovation Forum
                756580822739320883L // Black Label Media
        ));


        if (forum_post != null && !forum_post.startsWith("https://forum.politicsandwar.com/index.php?/topic/")) {
            return "Forum post (`" + forum_post + "`) must be on domain `https://forum.politicsandwar.com/index.php?/topic/`\n" + Messages.FORUM_NEWS_ERROR;
        }
        if (news_post != null) {
            // https://discord.com/channels/SERVER_ID/992205932006228041/1073856622545346641
            // remove the https://discord.com/channels/ part
            news_post = news_post.replace("ptb.", "");
            String[] idsStr = news_post.substring("https://discord.com/channels/".length()).split("/");
            if (idsStr.length != 3) {
                return "News post must be discord message link in the format `https://discord.com/channels/SERVER_ID/CHANNEL_ID/MESSAGE_ID`\n" + Messages.FORUM_NEWS_ERROR;
            }
            try {
                long serverId = Long.parseLong(idsStr[0]);
                if (!supportedServers.contains(serverId)) {
                    return "The news server you linked is not supported\n" + Messages.FORUM_NEWS_ERROR;
                }
            } catch (NumberFormatException e) {
                return "News post must be discord message link in the format `https://discord.com/channels/SERVER_ID/CHANNEL_ID/MESSAGE_ID`\n" + Messages.FORUM_NEWS_ERROR;
            }
    }

        if (discord_user_id != null && nationId != null) {
            RegisteredUser nationUser = Locutus.imp().getDiscordDB().getUserFromNationId(nationId);
            DBNation userNation = DiscordUtil.getNation(discord_user_id);
            if (nationUser != null && nationUser.getDiscordId() != discord_user_id) {
                throw new IllegalArgumentException("The nation you provided is already linked to a different discord user `" + DiscordUtil.getUserName(nationUser.getDiscordId()) + "`.\n" +
                        "Please create a separate report for this discord user.");
            }
            if (userNation != null && userNation.getId() != nationId) {
                throw new IllegalArgumentException("The discord user you provided is already linked to a different nation `" + userNation.getName() + "`.\n" +
                        "Please create a separate report for this nation.");
            }
        }

        if (discord_user_id == null) {
            RegisteredUser user = Locutus.imp().getDiscordDB().getUserFromNationId(nationId);
            if (user != null) {
                discord_user_id = user.getDiscordId();
            }
        }
        if (nationId == null) {
            DBNation nation2 = DiscordUtil.getNation(discord_user_id);
            nationId = nation2 != null ? nation2.getId() : null;
        }

        if (!force) {
            String title = (existing != null ? "Update " : "Report") +
                    type + " report by " +
                    DiscordUtil.getUserName(reporterUserId) + " | " + DNS.getName(reporterNationId, false);

            StringBuilder body = new StringBuilder();
            if (existing != null && existing.approved) {
                body.append("`This report will lose its approved status if you update it.`\n");
            }
            if (nationId != null) {
                body.append("Nation: " + DNS.getMarkdownUrl(nationId, false) + "\n");
            }
            if (discord_user_id != null) {
                body.append("Discord user: <@" + discord_user_id + ">\n");
            }
            body.append("Your message:\n```\n" + message + "\n```\n");
            if (imageEvidenceUrl != null) {
                body.append("Image evidence: " + imageEvidenceUrl + "\n");
            }
            if (forum_post != null) {
                body.append("Forum post: " + forum_post + "\n");
            }
            if (news_post != null) {
                body.append("News post: " + news_post + "\n");
            }

            List<ReportManager.Report> reportList = reportManager.loadReportsByNationOrUser(nationId, discord_user_id);
            if (!reportList.isEmpty()) {
                body.append("To add a comment: " + CM.report.comment.add.cmd.toSlashMention() + "\n");
                body.append("**Please look at these existing reports and add a comment instead if you are reporting the same thing**\n");
                for (ReportManager.Report report : reportList) {
                    body.append("#" + report.reportId +": ```\n" + report.message + "\n```\n");
                }
            }

            io.create().embed(title, body.toString()).append("""
                    If there is a game rule violation, create a report on the P&W discord, or forums
                    If there is a violation of discord ToS, report to discord: 
                    <https://discord.com/safety/360044103651-reporting-abusive-behavior-to-discord>""")
                    .confirmation(command).send();
            return null;
        }

        List<String> imageUrls = new ArrayList<>();
        if (imageEvidenceUrl != null) {
            // split by space
            String[] split = imageEvidenceUrl.split(" ");
            for (String imageUrl : split) {
                imageUrls.add(imageUrl);
                // ensure imageEvidenceUrl is discord image url
                String imageOcr = ImageUtil.getText(imageUrl);
                if (imageOcr != null) {
                    message += "\nScreenshot transcript:\n```\n" + imageOcr + "\n```";
                }
            }
        } else if (updateReport != null) {
            imageUrls = existing.imageUrls;
        }

        ReportManager.Report report = new ReportManager.Report(
            nationId == null ? 0 : nationId,
            discord_user_id == null ? 0 : discord_user_id,
            type,
            reporterNationId,
            reporterUserId,
            reporterAlliance,
            reporterGuildId,
            message,
            imageUrls,
            forum_post == null ? "" : forum_post,
            news_post == null ? "" : news_post,
            System.currentTimeMillis(),
            Roles.INTERNAL_AFFAIRS_STAFF.hasOnRoot(author));

        if (existing != null) {
            report.reportId = existing.reportId;
        }

        reportManager.saveReport(report);

        ReportManager.Report finalExisting = existing;
        AlertUtil.forEachChannel(f -> true, REPORT_ALERT_CHANNEL, new BiConsumer<MessageChannel, GuildDB>() {
            @Override
            public void accept(MessageChannel channel, GuildDB db) {
                String title = (finalExisting != null ? "Updating" : "New") + " " + report.getTitle();
                String body = report.toMarkdown(false);
                new DiscordChannelIO(channel).create().embed(title, body).sendWhenFree();
            }
        });

        return "Report " + (existing == null ? "created" : "updated") + " with id `" + report.reportId + "`\n" +
                "See: "
                // TODO FIXME :||remove
//                + CM.report.show.cmd.report(report.reportId + "").toSlashCommand(true)
                ;
    }

    @Command(desc = "Remove a report of a nation or user")
    public String removeReport(ReportManager reportManager, @Me JSONObject command, @Me IMessageIO io, @Me DBNation me, @Me User author, @Me GuildDB db, @ReportPerms ReportManager.Report report, @Switch("f") boolean force) {
        if (!report.hasPermission(me, author, db)) {
            return "You do not have permission to remove this report: `#" + report.reportId +
                    "` (owned by nation:" + DNS.getName(report.reporterNationId, false) + ")\n" +
                    "To add a comment: " + CM.report.comment.add.cmd.toSlashMention();
        }
        if (!force) {
            String title = "Remove " + report.type + " report";
            StringBuilder body = new StringBuilder(report.toMarkdown(true));
            io.create().confirmation(title, body.toString(), command).send();
            return null;
        }
        reportManager.deleteReport(report.reportId);
        return "Report removed.";
    }

    @Command(desc = "Remove a comment on a report")
    public String removeComment(ReportManager reportManager, @Me JSONObject command, @Me IMessageIO io, @Me DBNation me, @Me User author, @ReportPerms ReportManager.Report report, @Default DBNation nationCommenting, @Switch("f") boolean force) {
        if (nationCommenting == null) nationCommenting = me;
        if (nationCommenting.getNation_id() != me.getNation_id() && !Roles.INTERNAL_AFFAIRS_STAFF.hasOnRoot(author)) {
            return "You do not have permission to remove another nation's comment on report: `#" + report.reportId +
                    "` (owned by nation:" + DNS.getName(nationCommenting.getNation_id(), false) + ")\n" +
                    "To add a comment: " + CM.report.comment.add.cmd.toSlashMention();
        }
        ReportManager.Comment comment = reportManager.loadCommentsByReportNation(report.reportId, nationCommenting.getNation_id());
        if (comment == null) {
            return "No comment found for nation: " + DNS.getName(nationCommenting.getNation_id(), false) + " on report: `#" + report.reportId + "`";
        }

        if (!force) {
            String title = "Remove comment by nation:" + DNS.getName(comment.nationId, false);
            StringBuilder body = new StringBuilder();
            body.append("See report: #" + report.reportId + " | " + CM.report.show.cmd.toSlashMention() + "\n");
            body.append(comment.toMarkdown());
            io.create().confirmation(title, body.toString(), command).send();
            return null;
        }
        reportManager.deleteComment(report.reportId, comment.nationId);
        return "Comment removed.";
    }

    @Command(desc = "Approv a report for a nation or user")
    @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.INTERNAL_AFFAIRS}, root = true, any = true)
    public String approveReport(ReportManager reportManager, @Me JSONObject command, @Me IMessageIO io, @ReportPerms ReportManager.Report report, @Switch("f") boolean force) {
        if (report.approved) {
            return "Report #" + report.reportId + " is already approved.";
        }
        if (!force) {
            String title = "Verify " + report.type + " report";
            StringBuilder body = new StringBuilder(report.toMarkdown(false));
            io.create().confirmation(title, body.toString(), command).send();
            return null;
        }
        report.approved = true;
        reportManager.saveReport(report);
        return "Verified report #" + report.reportId;
    }

    @Command(desc = "Add a short comment to a report")
    public String comment(ReportManager reportManager, @Me JSONObject command, @Me IMessageIO io, @Me DBNation me, @Me User author, ReportManager.Report report, String comment, @Switch("f") boolean force) {
        Map.Entry<String, Long> ban = reportManager.getBan(me);
        if (ban != null) {
            return "You were banned from reporting on " + DiscordUtil.timestamp(ban.getValue(), null) + " for `" + ban.getKey() + "`";
        }
        ReportManager.Comment existing = reportManager.loadCommentsByReportNation(report.reportId, me.getNation_id());

        if (!force) {
            String title = "Comment on " + report.getTitle();
            StringBuilder body = new StringBuilder(report.toMarkdown(false));

            String bodyString = "Your comment\n```\n" + comment + "\n```\n" + body.toString();
            if (existing != null) {
                bodyString = "Overwrite your existing comment:\n```\n" + existing.comment + "\n```\n" + bodyString;
            }

            io.create().confirmation(title, bodyString, command).send();
            return null;
        }

        boolean isNewComment = existing == null;

        existing = new ReportManager.Comment(report.reportId, me.getNation_id(), author.getIdLong(), comment, System.currentTimeMillis());

        reportManager.saveComment(existing);

        ReportManager.Comment finalExisting = existing;
        AlertUtil.forEachChannel(f -> true, REPORT_ALERT_CHANNEL, new BiConsumer<MessageChannel, GuildDB>() {
            @Override
            public void accept(MessageChannel channel, GuildDB db) {
                String title = (isNewComment ? "New " : "Updating ") + " comment";
                String body = "Report: #" + report.reportId + " " + report.getTitle() + "\n\n" + finalExisting.toMarkdown();
                new DiscordChannelIO(channel).create().embed(title, body).sendWhenFree();
            }
        });

        return "Added comment to #" + report.reportId +
                "\nSee: " + CM.report.show.cmd.toSlashMention();
    }

    @Command(desc = "Mass delete reports about or submitted by a user or nation")
    @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.INTERNAL_AFFAIRS}, root = true, any = true)
    public String purgeReports(@Me IMessageIO io, @Me JSONObject command, ReportManager reportManager, @Switch("n") Integer nationIdReported, @Switch("d") Long userIdReported, @Switch("i") Integer reportingNation, @Switch("u") Long reportingUser, @Switch("f") boolean force) {
        List<ReportManager.Report> reports = reportManager.loadReports(nationIdReported, userIdReported, reportingNation, reportingUser);
        if (reports.isEmpty()) {
            return "No reports found";
        }
        if (!force) {
            String title = "Purge " + reports.size() + " reports";
            StringBuilder body = new StringBuilder();
            // append each variable used
            if (nationIdReported != null) {
                DBNation nation = DBNation.getById(nationIdReported);
                String aaName = nation == null ? "None" : nation.getAllianceUrlMarkup(true);
                body.append("Nation reported: ").append(DNS.getMarkdownUrl(nationIdReported, false) + " | " + aaName).append("\n");
            }
            if (userIdReported != null) {
                body.append("User reported: ").append("<@" + userIdReported + ">").append("\n");
            }
            if (reportingNation != null) {
                DBNation nation = DBNation.getById(reportingNation);
                String aaName = nation == null ? "None" : nation.getAllianceUrlMarkup(true);
                body.append("Reporting nation: ").append(DNS.getMarkdownUrl(reportingNation, false) + " | " + aaName).append("\n");
            }
            if (reportingUser != null) {
                body.append("Reporting user: ").append("<@" + reportingUser + ">").append("\n");
            }
            body.append("\n");
            io.create().confirmation(title, body.toString(), command).send();
            return null;
        }
        for (ReportManager.Report report : reports) {
            reportManager.deleteReport(report.reportId);
        }
        return "Deleted " + reports.size() + " reports";
    }

    @Command(desc = "Mass delete reports about or submitted by a user or nation")
    @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.INTERNAL_AFFAIRS}, root = true, any = true)
    public String purgeComments(@Me IMessageIO io, @Me JSONObject command, ReportManager reportManager, @Switch("r") ReportManager.Report report, @Switch("i") Integer nation_id, @Switch("u") Long discord_id, @Switch("f") boolean force) {
        if (nation_id != null && discord_id != null) {
            return "Cannot specify both a nation and a user. Pick one";
        }
        if (report != null && (nation_id != null || discord_id != null)) {
            return "Cannot specify both a report and a nation/user. Pick one";
        }
        List<ReportManager.Comment> comments;
        if (report != null) {
            comments = reportManager.loadCommentsByReport(report.reportId);
        } else if (nation_id != null) {
            comments = reportManager.loadCommentsByNation(nation_id);
        } else if (discord_id != null) {
            comments = reportManager.loadCommentsByUser(discord_id);
        } else {
            return "Please specify either `report` or `nation_id` or `discord_id`";
        }
        if (comments.isEmpty()) {
            return "No comments found";
        }
        if (!force) {
            String title = "Purge " + comments.size() + " comments";
            StringBuilder body = new StringBuilder();
            // append each variable used
            if (nation_id != null) {
                DBNation nation = DBNation.getById(nation_id);
                String aaName = nation == null ? "None" : nation.getAllianceUrlMarkup(true);
                body.append("Nation commenting: ").append(DNS.getMarkdownUrl(nation_id, false) + " | " + aaName).append("\n");
            }
            if (discord_id != null) {
                body.append("User commenting: ").append("<@" + discord_id + ">").append("\n");
            }
            body.append("\n");
            io.create().confirmation(title, body.toString(), command).send();
            return null;
        }
        for (ReportManager.Comment comment : comments) {
            reportManager.deleteComment(comment.report_id, comment.nationId);
        }
        return "Deleted " + comments.size() + " comments";
    }

    @Command(desc = "Ban a nation from submitting new reports\n" +
            "Reports they have already submitted will remain\n" +
            "Use the purge command to delete existing reports")
    @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.INTERNAL_AFFAIRS}, root = true, any = true)
    public String ban(ReportManager reportManager, @Me IMessageIO io, @Me JSONObject command, DBNation nation, @Timestamp long timestamp, String reason, @Switch("f") boolean force) throws IOException {
        if (!force) {
            String title = "Ban " + nation.getName() + " from reporting";
            StringBuilder body = new StringBuilder();
            body.append("Nation: ").append(nation.getNationUrlMarkup(true) + " | " + nation.getAllianceUrlMarkup(true)).append("\n");
            body.append("Reason:\n```\n").append(reason).append("\n```\n");
            body.append("Until: ").append(DiscordUtil.timestamp(timestamp, null)).append("\n");

            Map.Entry<String, Long> ban = reportManager.getBan(nation);
            if (ban != null) {
                body.append("Existing ban:\n```\n").append(ban.getKey()).append("\n```\nuntil ").append(DiscordUtil.timestamp(ban.getValue(), null)).append("\n");
            }

            io.create().confirmation(title, body.toString(), command).send();
            return null;
        }
        reportManager.setBanned(nation, timestamp, reason);
        return "Banned " + nation.getName() + " from submitting nation reports";
    }

    @Command(desc = "Remove a ban on a nation submitting new reports")
    @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.INTERNAL_AFFAIRS}, root = true, any = true)
    public String unban(ReportManager reportManager, @Me IMessageIO io, @Me JSONObject command, DBNation nation, @Switch("f") boolean force) throws IOException {
        Map.Entry<String, Long> ban = reportManager.getBan(nation);
        if (ban == null) {
            return "No ban found for " + nation.getName();
        }
        if (!force) {
            String title = "Unban " + nation.getName() + " from reporting";
            StringBuilder body = new StringBuilder();
            body.append("Nation: ").append(nation.getNationUrlMarkup(true) + " | " + nation.getAllianceUrlMarkup(true)).append("\n");
            if (ban != null) {
                body.append("Existing ban:\n```\n").append(ban.getKey()).append("\n```\nuntil ").append(DiscordUtil.timestamp(ban.getValue(), null)).append("\n");
            }
            io.create().confirmation(title, body.toString(), command).send();
            return null;
        }
        nation.deleteMeta(NationMeta.REPORT_BAN);
        return "Unbanned " + nation.getName() + " from submitting nation reports";
    }

//    @Command
//    public String viewReports(ReportManager reportManager, @Switch("n") DBNation nation, @Switch("d") Long discordId, @Switch("u") User discordUser) {
//        if (discordUser != null && discordId != null) {
//            // can only do one, throw exception, pick one
//        }
//
//        if (nation == null && discordUser == null && discordId == null) {
//            // must do at least one, pick,. throw error
//        }
//        Integer nationId = nation == null ? null : nation.getNation_id();
//        if (discordUser != null) discordId = discordUser.getIdLong();
//        List<ReportManager.Report> reports = reportManager.loadReportsByNationOrUser(nationId, discordId);
//        if (reports.isEmpty()) {
//            return "No reports found";
//        }
//
//        StringBuilder response = new StringBuilder();
//        // append each report incl comments
//
//        return null;
//    }

    // report search
    @Command(desc = "List all reports about or submitted by a nation or user")
    public String searchReports(ReportManager reportManager, @Switch("n") Integer nationIdReported, @Switch("d") Long userIdReported, @Switch("i") Integer reportingNation, @Switch("u") Long reportingUser) {
        List<ReportManager.Report> reports = reportManager.loadReports(nationIdReported, userIdReported, reportingNation, reportingUser);
        // list reports matching
        if (reports.isEmpty()) {
            return "No reports found";
        }
        StringBuilder response = new StringBuilder();
        response.append("# " + reports.size()).append(" reports found:\n");
        for (ReportManager.Report report : reports) {
            response.append("### ").append(report.toMarkdown(false)).append("\n");
        }
        return response.toString();
    }

    // report show, incl comments
    @Command(desc = "View a report and its comments")
    public String showReport(ReportManager.Report report) {
        return "### " + report.toMarkdown(true);
    }

    @Command(desc = "Show an analysis of a nation's risk factors including: Reports, loans, discord & game bans, money trades and proximity with blacklisted nations, multi info, user account age, inactivity predictors")
    public String riskFactors(@Me IMessageIO io, ReportManager reportManager, DBNation nation) {
        StringBuilder response = new StringBuilder();
        // Nation | Alliance (#AA Rank) | Position
        response.append(nation.getNationUrlMarkup(true) + " | " + nation.getAllianceUrlMarkup(true));
        if (nation.getAlliance_id() > 0) {
            DBAlliance alliance = nation.getAlliance();
            response.append(" (#").append(alliance.getRank()).append(")");
            response.append(" " + nation.getPositionEnum().name());
        }

        response.append("\n");

        // Get reports by same nation/discord
        List<ReportManager.Report> reports = reportManager.loadReports(nation.getId(), nation.getUserId(), null, null);
        if (reports.size() > 0) {
            int approved = (int) reports.stream().filter(report -> report.approved).count();
            int pending = reports.size() - approved;
            response.append("Reports: " + reports.size() + " (" + approved + " approved, " + pending + " pending) "
                    // TODO FIXME :||remove
//                    + CM.report.search.cmd.toSlashMention() + "\n"
            );
        }


        //- server bans
        User user = nation.getUser();
        if (user == null) {
            response.append("`Nation is not registered to a discord user`\n");
        } else {
            //- account age
            long dateCreated = user.getTimeCreated().toEpochSecond() * 1000L;
            response.append("Discord created: " + DiscordUtil.timestamp(dateCreated, null) + "\n");

            List<DiscordBan> discordBans = Locutus.imp().getDiscordDB().getBans(user.getIdLong());
            if (!discordBans.isEmpty()) {
                response.append("Discord bans:\n");
                for (DiscordBan ban : discordBans) {
                    response.append("- " + DiscordUtil.getGuildName(ban.server) + " on " + DiscordUtil.timestamp(ban.date, null) + "\n");
                }
            }
        }

        Map<Integer, List<Map.Entry<Long, Long>>> history = reportManager.getCachedHistory(nation.getNation_id());

        Map<Integer, Long> sameAAProximity = reportManager.getBlackListProximity(nation, history);
        Map<Integer, Set<ReportManager.ReportType>> reportTypes = reportManager.getApprovedReportTypesByNation(sameAAProximity.keySet());
        if (!sameAAProximity.isEmpty()) {
            response.append("Proximity to reported individuals:\n");
            for (Map.Entry<Integer, Long> entry : sameAAProximity.entrySet()) {
                int nationId = entry.getKey();
                String nationName = DNS.getMarkdownUrl(nationId, false);
                String reportListStr = reportTypes.get(nationId).stream().map(ReportManager.ReportType::name).collect(Collectors.joining(","));
                response.append("- " + nationName + " | " + reportListStr + ": " + TimeUtil.secToTime(TimeUnit.MILLISECONDS, entry.getValue()) + "\n");
            }
            response.append("See: " + CM.nation.departures.cmd.toSlashMention() + "\n");
        }
        if (response.length() < 4000) {
            io.create().embed(nation.getNation() + " Analysis", response.toString()).send();
        } else {
            io.send(response.toString());
        }
        return null;
    }
}
