import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.HierarchyException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.net.ssl.HttpsURLConnection;
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
                    channel.sendMessage("```**Stats:** !stats <battletag>\n**Set role:** !setprofile <battletag>```").queue();
                    break;
                case "stats":
                    if (args.length > 1) {
                        try {
                            channel.sendTyping().queue();
                            channel.sendMessage(getProfile(args[1])).queue();
                        } catch (Exception e) {
                            channel.sendMessage(args[1] + " was not found").queue();
                        }
                    } else {
                        String nickname = event.getGuild().getMember(event.getAuthor()).getNickname();
                        try {
                            channel.sendTyping().queue();
                            channel.sendMessage(getProfile(nickname)).queue();
                        } catch (Exception e) {
                            if (nickname != null)
                                channel.sendMessage(nickname + " was not found").queue();
                            else
                                channel.sendMessage(event.getAuthor().getAsMention() + " Please set your nickname to your Battletag").queue();
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
                                channel.sendMessage("Player not found.").queue();
                            }
                        } else {
                            channel.sendMessage(event.getAuthor().getAsMention() + " Please set your nickname to your Battletag").queue();
                        }
                    } else {
                        try {
                            channel.sendTyping().queue();
                            channel.sendMessage(setProfile(args[1], event)).queue();
                        } catch (Exception e) {
                            e.printStackTrace();
                            channel.sendMessage("Player not found.").queue();
                        }
                        break;
                    }

            }
        }
    }


    public static MessageEmbed getProfile(String battletag) throws IOException {

        String url = "https://ow-api.com/v1/stats/pc/us/" + battletag.replace('#', '-') + "/profile";
        HttpsURLConnection con = (HttpsURLConnection) new URL(url).openConnection();
        if (con.getResponseCode() == 200) {
            InputStream responseBody = new BufferedInputStream(con.getInputStream());

            JsonParser jsonParser = new JsonParser();
            JsonObject stats = jsonParser.parse(new InputStreamReader(responseBody, "UTF-8")).getAsJsonObject();

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle(battletag, "https://playoverwatch.com/en-us/career/pc/" + battletag.replace('#', '-'));
            eb.setColor(new Color(0x2196F3));
            eb.setImage(stats.get("icon").getAsString());
            if (!stats.get("ratingIcon").getAsString().equals(""))
                eb.setThumbnail(stats.get("ratingIcon").getAsString());
            eb.addField("Level", Integer.toString(stats.get("prestige").getAsInt() * 100 + stats.get("level").getAsInt()), true);
            if (!stats.get("gamesWon").getAsString().equals("0"))
                eb.addField("Games Won", stats.get("gamesWon").getAsString(), true);
            eb.addField("Endorsement Lvl", stats.get("endorsement").getAsString(), true);
            if (!stats.get("rating").getAsString().equals("0"))
                eb.addField("SR", stats.get("rating").getAsString(), true);

            responseBody.close();
            con.disconnect();
            return eb.build();
        } else {
            con.disconnect();
            EmbedBuilder eb = new EmbedBuilder();
            eb.setDescription("ERROR");
            return eb.build();
        }
    }

    public static String setProfile(String battletag, MessageReceivedEvent event) throws IOException {

        String url = "https://ow-api.com/v1/stats/pc/us/" + battletag.replace('#', '-') + "/profile";
        HttpsURLConnection con = (HttpsURLConnection) new URL(url).openConnection();
        if (con.getResponseCode() == 200) {
            InputStream responseBody = new BufferedInputStream(con.getInputStream());

            JsonParser jsonParser = new JsonParser();
            JsonObject stats = jsonParser.parse(new InputStreamReader(responseBody, "UTF-8")).getAsJsonObject();
            int rating = stats.get("rating").getAsInt();
            responseBody.close();
            con.disconnect();
            String[] ranks = {"Grandmaster", "Master", "Diamond", "Platinum", "Gold", "Silver", "Bronze"};
            String role = null;
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
                    event.getChannel().sendMessage(event.getAuthor().getAsMention() + " You are too high a role.").queue();
                }
                return "Added " + event.getAuthor().getAsMention() + " to **" + role + "**";
            } else
                return event.getAuthor().getAsMention() + " You are currently unranked.";
        } else {
            con.disconnect();
            return "Error";
        }
    }

}
