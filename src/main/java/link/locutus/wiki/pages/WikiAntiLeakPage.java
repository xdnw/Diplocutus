package link.locutus.wiki.pages;

import link.locutus.wiki.BotWikiGen;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.commands.manager.v2.impl.pw.CommandManager2;
import link.locutus.discord.user.Roles;

public class WikiAntiLeakPage extends BotWikiGen {
    public WikiAntiLeakPage(CommandManager2 manager) {
        super(manager, "announcements_and_opsec");
    }

    @Override
    public String generateMarkdown() {
        return build(
                """
                This bot has tools for bulk communication, and for tracking leaks.
                - Create announcements, discord embeds, direct messages and invites that are unique to each user
                - Use Optical Character Recognition (OCR) to parse screenshots,
                - Search for matching or similar messages or invites""",
                "# Prerequisite settings",
                "Set your alliance and api key",
                commandMarkdownSpoiler(CM.settings_default.registerAlliance.cmd),
                commandMarkdownSpoiler(CM.settings_default.registerApiKey.cmd),
                "# Sending unique announcements",
                "Supports sending dms, mail and clickable discord embeds",
                commandMarkdownSpoiler(CM.announcement.create.cmd),
                "# Sending unique invites",
                "Note: Recommended to use `allowCreation`",
                commandMarkdownSpoiler(CM.announcement.invite.cmd),
                "# Sending unique documents",
                commandMarkdownSpoiler(CM.announcement.document.cmd),
                "# Sending unique polls",
                """
                 Create a new spreadsheet, then add the following google app script:
                 - <https://gist.github.com/xdnw/10c64fb8f78c7676c581273f04ed5c1a>
                 
                 App-Script Tutorial: <https://www.benlcollins.com/apps-script/google-apps-script-beginner-guide/>
                 
                 How to use this script:
                 - Edit the `title` and `replacements` in the your script file (typically `Code.gs`)
                 - Add your nations to the `nations` tab
                 - Add your questions to the `questions` tab. See the `Set Example Questions` from the `Poll` menu. Refresh the page if you dont see the menu.
                 - Press the `Create Polls` button from the `Poll` menu
                 - View the created polls in the `nations` tab
                 - To send the polls via in-game mail, see the {mail_sheet} command
                 
                 Note: Polls will not be created if one already exists in the `nations` tab
                 """.replace("{mail_sheet}", CM.message.sheet.cmd.toString()),
                "# Converting a screenshot to text",
                commandMarkdownSpoiler(CM.announcement.ocr.cmd),
                "# Finding a nation from a message or invite",
                commandMarkdownSpoiler(CM.announcement.find.cmd),
                commandMarkdownSpoiler(CM.announcement.find_invite.cmd),
                "# Other announcement commands",
                commandMarkdownSpoiler(CM.announcement.archive.cmd),
                commandMarkdownSpoiler(CM.announcement.read.cmd),
                commandMarkdownSpoiler(CM.announcement.view.cmd)
        );
    }
}
