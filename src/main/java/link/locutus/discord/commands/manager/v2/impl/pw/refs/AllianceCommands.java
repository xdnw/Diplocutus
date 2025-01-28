package link.locutus.discord.commands.manager.v2.impl.pw.refs;
import link.locutus.discord.commands.manager.v2.command.AutoRegister;
import link.locutus.discord.commands.manager.v2.command.CommandRef;
public class AllianceCommands {
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBAlliance.class,method="countNations")
        public static class countNations extends CommandRef {
            public static final countNations cmd = new countNations();
        public countNations filter(String value) {
            return set("filter", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBAlliance.class,method="getAlliance_id")
        public static class getAlliance_id extends CommandRef {
            public static final getAlliance_id cmd = new getAlliance_id();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBAlliance.class,method="getAverage")
        public static class getAverage extends CommandRef {
            public static final getAverage cmd = new getAverage();
        public getAverage attribute(String value) {
            return set("attribute", value);
        }

        public getAverage filter(String value) {
            return set("filter", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBAlliance.class,method="getAveragePer")
        public static class getAveragePer extends CommandRef {
            public static final getAveragePer cmd = new getAveragePer();
        public getAveragePer attribute(String value) {
            return set("attribute", value);
        }

        public getAveragePer per(String value) {
            return set("per", value);
        }

        public getAveragePer filter(String value) {
            return set("filter", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBAlliance.class,method="getDateCreated")
        public static class getDateCreated extends CommandRef {
            public static final getDateCreated cmd = new getDateCreated();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBAlliance.class,method="getDef")
        public static class getDef extends CommandRef {
            public static final getDef cmd = new getDef();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBAlliance.class,method="getExpectedWars")
        public static class getExpectedWars extends CommandRef {
            public static final getExpectedWars cmd = new getExpectedWars();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBAlliance.class,method="getId")
        public static class getId extends CommandRef {
            public static final getId cmd = new getId();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBAlliance.class,method="getName")
        public static class getName extends CommandRef {
            public static final getName cmd = new getName();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBAlliance.class,method="getNumWars")
        public static class getNumWars extends CommandRef {
            public static final getNumWars cmd = new getNumWars();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBAlliance.class,method="getNumWarsSince")
        public static class getNumWarsSince extends CommandRef {
            public static final getNumWarsSince cmd = new getNumWarsSince();
        public getNumWarsSince date(String value) {
            return set("date", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBAlliance.class,method="getOff")
        public static class getOff extends CommandRef {
            public static final getOff cmd = new getOff();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBAlliance.class,method="getRank")
        public static class getRank extends CommandRef {
            public static final getRank cmd = new getRank();
        public getRank filter(String value) {
            return set("filter", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.binding.DefaultPlaceholders.class,method="getResource")
        public static class getResource extends CommandRef {
            public static final getResource cmd = new getResource();
        public getResource resources(String value) {
            return set("resources", value);
        }

        public getResource resource(String value) {
            return set("resource", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.binding.DefaultPlaceholders.class,method="getResourceValue")
        public static class getResourceValue extends CommandRef {
            public static final getResourceValue cmd = new getResourceValue();
        public getResourceValue resources(String value) {
            return set("resources", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBAlliance.class,method="getRevenue")
        public static class getRevenue extends CommandRef {
            public static final getRevenue cmd = new getRevenue();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBAlliance.class,method="getScore")
        public static class getScore extends CommandRef {
            public static final getScore cmd = new getScore();
        public getScore filter(String value) {
            return set("filter", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBAlliance.class,method="getSheetUrl")
        public static class getSheetUrl extends CommandRef {
            public static final getSheetUrl cmd = new getSheetUrl();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBAlliance.class,method="getTotal")
        public static class getTotal extends CommandRef {
            public static final getTotal cmd = new getTotal();
        public getTotal attribute(String value) {
            return set("attribute", value);
        }

        public getTotal filter(String value) {
            return set("filter", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBAlliance.class,method="getTreatiedAllies")
        public static class getTreatiedAllies extends CommandRef {
            public static final getTreatiedAllies cmd = new getTreatiedAllies();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBAlliance.class,method="getTreatyOrdinal")
        public static class getTreatyOrdinal extends CommandRef {
            public static final getTreatyOrdinal cmd = new getTreatyOrdinal();
        public getTreatyOrdinal alliance(String value) {
            return set("alliance", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBAlliance.class,method="getTreatyType")
        public static class getTreatyType extends CommandRef {
            public static final getTreatyType cmd = new getTreatyType();
        public getTreatyType alliance(String value) {
            return set("alliance", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBAlliance.class,method="getUrl")
        public static class getUrl extends CommandRef {
            public static final getUrl cmd = new getUrl();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBAlliance.class,method="hasDefensiveTreaty")
        public static class hasDefensiveTreaty extends CommandRef {
            public static final hasDefensiveTreaty cmd = new hasDefensiveTreaty();
        public hasDefensiveTreaty alliances(String value) {
            return set("alliances", value);
        }

        }

}
