import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.HierarchyException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.net.ssl.HttpsURLConnection;
import java.net.URLConnection;
import java.util.List;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Caleb on 10/23/2018.
 */
public class ReadyListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        if (event.getMessage().getContentRaw().startsWith("!")) {

            if (event.getAuthor().isBot())
                return;

            Message message = event.getMessage();
            String[] args = message.getContentRaw().replaceFirst("!", "").split(" ");
            MessageChannel channel = event.getChannel();

            switch (args[0]) {
                case "ping":
                    channel.sendMessage("Pong!").queue();
                    break;
                case "help":
                    channel.sendMessage(helpEmbed()).queue();
                    break;
                case "stats":
                    if (args.length > 1) {
                        try {
                            channel.sendTyping().queue();
                            channel.sendMessage(getProfile(args[1])).queue();
                        } catch (Exception e) {
                            e.printStackTrace();
                            channel.sendMessage(errorEmbed(args[1] + " not found \n*" + e.toString() + "*")).queue();
                        }
                    } else {
                        String nickname = event.getGuild().getMember(event.getAuthor()).getNickname();
                        try {
                            channel.sendTyping().queue();
                            channel.sendMessage(getProfile(nickname)).queue();
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (nickname != null)
                                channel.sendMessage(errorEmbed(nickname + " not found \n*" + e.toString() + "*")).queue();
                            else
                                channel.sendMessage(errorEmbed("Please set your nickname to your Battletag")).queue();
                        }
                    }
                    break;
                case "setprofile":
                    if (args.length == 1) {
                        String nickname = event.getGuild().getMember(event.getAuthor()).getNickname();
                        if (nickname != null) {
                            try {
                                channel.sendTyping().queue();
                                channel.sendMessage(setProfile(nickname, event)).queue();
                            } catch (Exception e) {
                                e.printStackTrace();
                                channel.sendMessage(errorEmbed("Player not found \n*" + e.toString() + "*")).queue();
                            }
                        } else {
                            channel.sendMessage(errorEmbed("Please set your nickname to your Battletag.")).queue();
                        }
                    } else {
                        try {
                            channel.sendTyping().queue();
                            channel.sendMessage(setProfile(args[1], event)).queue();
                        } catch (Exception e) {
                            e.printStackTrace();
                            channel.sendMessage(errorEmbed("Player not found \n*" + e.toString() + "*")).queue();
                        }
                        break;
                    }

            }
        }
    }

    public static MessageEmbed getProfile(String battletag) throws IOException {
        String url = "https://playoverwatch.com/career/pc/" + battletag.replace('#', '-');
        Document document = Jsoup.connect(url).maxBodySize(0).get();
        Element player = document.selectFirst(".masthead-player");

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(battletag, "https://playoverwatch.com/career/pc/" + battletag.replace('#', '-'));
        eb.setColor(new Color(0x2196F3));

        //eb.setImage(player.selectFirst("img").attr("src"));
        eb.setThumbnail(player.selectFirst("img").attr("src"));

        //String desc = "Level " + player.selectFirst(".player-level").text();
        //System.out.println(document.html().indexOf("window.app.career.init("));

        //String[] userId = document.select("script").last().html().split("[(,]");

        String desc = "Level " + player.selectFirst(".player-level").text();
        desc += "\nEndorsement Level " + player.selectFirst(".EndorsementIcon-tooltip").text();
        eb.setDescription(desc);

        String qp = "";
        String comp ="";

        try {
            String time = document.selectFirst("tr[data-stat-id=\"0x0860000000000026\"]").text().split(" ")[2];
            qp += "\nTime Played: **" + time + "**";

            String gamesWon = document.selectFirst("tr[data-stat-id=\"0x08600000000003F5\"]").text().split(" ")[2];
            qp += "\nGames Won: **" + gamesWon + "**";

            Element mostPlayed = document.selectFirst(".ProgressBar-container");
            qp += "\nMost Played: **" + mostPlayed.selectFirst(".ProgressBar-title").text() + " (" + mostPlayed.selectFirst(".ProgressBar-description").text() + ")**";

        } catch (NullPointerException e) {}

        eb.addField("Quick Play", qp, true);

        try {

            //eb.setThumbnail(player.selectFirst(".competitive-rank").selectFirst("img").attr("src"));

            // Tank
            try {
                comp += "\nTank: **" + player.selectFirst("div[data-ow-tooltip-text=\"Tank Skill Rating\"]").parent().selectFirst(".competitive-rank-level").text() + " SR**";
            } catch (NullPointerException e) {}
            // Damage
            try {
                comp += "\nDamage: **" + player.selectFirst("div[data-ow-tooltip-text=\"Damage Skill Rating\"]").parent().selectFirst(".competitive-rank-level").text() + " SR**";
            } catch (NullPointerException e) {}
            // Support
            try {
                comp += "\nSupport: **" + player.selectFirst("div[data-ow-tooltip-text=\"Support Skill Rating\"]").parent().selectFirst(".competitive-rank-level").text() + " SR**";
            } catch (NullPointerException e) {}

            comp = comp.substring(1);

            Element compDiv = document.selectFirst("#competitive");

            String time = compDiv.selectFirst("tr[data-stat-id=\"0x0860000000000026\"]").text().split(" ")[2];
            comp += "\nTime Played: **" + time + "**";

            String gamesWon = compDiv.selectFirst("div[data-group-id=\"stats\"]").selectFirst("tr[data-stat-id=\"0x08600000000003F5\"]").text().split(" ")[2];
            String gamesPlayed = compDiv.selectFirst("div[data-group-id=\"stats\"]").selectFirst("tr[data-stat-id=\"0x0860000000000385\"]").text().split(" ")[2];
            double won = Double.parseDouble(gamesWon);
            double played = Double.parseDouble(gamesPlayed);
            String winRate = Double.toString(round(won / played * 100, 2)) + "%";
            comp += "\nGames Won: **" + gamesWon + "**";
            comp += "\nWin Rate: **" + winRate + "**";

            Element mostPlayed = compDiv.selectFirst(".ProgressBar-container");
            comp += "\nMost Played: **" + mostPlayed.selectFirst(".ProgressBar-title").text() + " ("
                    + mostPlayed.selectFirst(".ProgressBar-description").text() + ")**";

        } catch (NullPointerException e) {
            //e.printStackTrace();
        }

        if(!comp.equals(""))
            eb.addField("Competitive", comp, true);



        return eb.build();
    }

    public static MessageEmbed setProfile(String battletag, MessageReceivedEvent event) throws IOException {

        String url = "https://playoverwatch.com/career/pc/" + battletag.replace('#', '-');
        Document document = Jsoup.connect(url).maxBodySize(0).get();
        Element player = document.selectFirst(".masthead-player");
        String msg = "";
        try {
            int rating = Integer.parseInt( player.selectFirst(".competitive-rank").text() );

            String[] ranks = {"Grandmaster", "Master", "Diamond", "Platinum", "Gold", "Silver", "Bronze"};
            String role;
            if (rating != 0) {
                if (rating >= 4000)
                    role = "Grandmaster";
                else if (rating >= 3500)
                    role = "Master";
                else if (rating >= 3000)
                    role = "Diamond";
                else if (rating >= 2500)
                    role = "Platinum";
                else if (rating >= 2000)
                    role = "Gold";
                else if (rating >= 1500)
                    role = "Silver";
                else
                    role = "Bronze";

                Guild guild = event.getGuild();
                List<Role> group = guild.getRolesByName(role, true);
                ArrayList<Role> remove = new ArrayList<>();
                for (String rank : ranks) {
                    if (!rank.equals(role))
                        remove.addAll(guild.getRolesByName(rank, true));
                }
                guild.getController().modifyMemberRoles(guild.getMember(event.getAuthor()), group, remove).queue();
                try {
                    guild.getController().setNickname(guild.getMember(event.getAuthor()), battletag).queue();
                } catch (HierarchyException e) {
                    msg += "Could not set **" + event.getAuthor().getName() + "**'s nickname (Your role is too high)";
                }
                msg += "\nAdded " + event.getAuthor().getAsMention() + " to **" + role + "**";
            } else
                msg += event.getAuthor().getAsMention() + " You are currently unranked.";
        } catch (Exception e) {
            return errorEmbed(e.toString());
        }

        return infoEmbed("Set Profile", msg);
    }


    private static MessageEmbed errorEmbed(String msg) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Error");
        eb.setColor(new Color(0xf44336));
        eb.setDescription(msg);
        return eb.build();
    }

    private static MessageEmbed helpEmbed() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Help");
        eb.setColor(new Color(0x009688));
        eb.addField("Stats", "`!stats <battletag or use nick>`", false);
        eb.addField("Set Profile", "`!setprofile <battletag or use nick>`", false);
        eb.setFooter("Created by Kala30", "https://avatars2.githubusercontent.com/u/13771555");
        return eb.build();
    }

    private static MessageEmbed infoEmbed(String title, String msg) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(title);
        eb.setColor(new Color(0x2196F3));
        eb.setDescription(msg);
        return eb.build();
    }

    static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

}
