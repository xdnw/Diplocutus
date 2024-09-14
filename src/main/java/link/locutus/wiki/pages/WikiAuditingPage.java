package link.locutus.wiki.pages;

import link.locutus.wiki.BotWikiGen;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.commands.manager.v2.impl.pw.CommandManager2;
import link.locutus.discord.user.Roles;

public class WikiAuditingPage extends BotWikiGen {
    public WikiAuditingPage(CommandManager2 manager) {
        super(manager, "auditing");
    }

    @Override
    public String generateMarkdown() {
        return build(
        """
        - Generate and send out audit reports
        - Create sheets of nation info you can review
        - player reports
        - And more!""",
        "# Auditing MMR",
        commandMarkdownSpoiler(CM.settings_audit.REQUIRED_MMR.cmd),
        commandMarkdownSpoiler(CM.settings_audit.addRequiredMMR.cmd),
        commandMarkdownSpoiler(CM.sheets_milcom.MMRSheet.cmd),
        "# Join Leave Alerts",
        commandMarkdownSpoiler(CM.settings_audit.MEMBER_LEAVE_ALERT_CHANNEL.cmd),
        "# Automatic Audits",
        "Set an opt out role on discord",
        CM.role.setAlias.cmd.locutusRole(Roles.AUDIT_ALERT_OPT_OUT.name()).discordRole("@audit_opt_out").toString(),
        commandMarkdownSpoiler(CM.settings_audit.MEMBER_AUDIT_ALERTS.cmd),
        commandMarkdownSpoiler(CM.alerts.audit.optout.cmd),
        "# Create or send audit reports",
        "Run audits on a nation, multiple nations, and optionally mail results",
        commandMarkdownSpoiler(CM.audit.run.cmd),
        commandMarkdownSpoiler(CM.settings_audit.DISABLED_MEMBER_AUDITS.cmd),
        "## Audit sheet",
        commandMarkdownSpoiler(CM.audit.sheet.cmd),
        "# New applicant auditing",
        commandMarkdownSpoiler(CM.report.analyze.cmd),
        commandMarkdownSpoiler(CM.nation.departures.cmd)
// TODO FIXME :||remove
//        "# Econ related auditing",
//        commandMarkdownSpoiler(CM.sheets_econ.revenueSheet.cmd),
//        commandMarkdownSpoiler(CM.sheets_econ.stockpileSheet.cmd),
//        commandMarkdownSpoiler(CM.sheets_econ.ProjectSheet.cmd),
//        commandMarkdownSpoiler(CM.project.slots.cmd),
//        commandMarkdownSpoiler(CM.nation.TurnTimer.cmd),
//        commandMarkdownSpoiler(CM.build.get.cmd),
//        commandMarkdownSpoiler(CM.sheets_econ.taxBracketSheet.cmd),
//        commandMarkdownSpoiler(CM.city.optimalBuild.cmd),
//        "# Espionage",
//        "## Nations not buying spies",
//        commandMarkdownSpoiler(CM.audit.hasNotBoughtSpies.cmd),
//        "## Nations not using spy ops",
//        commandMarkdownSpoiler(CM.spy.sheet.free_ops.cmd)
        );
    }
}
