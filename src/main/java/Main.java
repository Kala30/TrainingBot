import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Caleb on 10/23/2018.
 */
public class Main {

    public static JDA jda;
    public static Map<String, Integer> prestige = new HashMap<String, Integer>();

    public static void main (String[] args) {

        try {
            setPrestige();

            String token = Files.readAllLines(Paths.get("token.txt"), Charset.forName("UTF-8")).get(0);
            jda = new JDABuilder(token).setGame(Game.playing("!help")).build();
            jda.addEventListener(new ReadyListener());
        } catch (LoginException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void setPrestige() throws IOException {
        JsonElement root = new JsonParser().parse(readFile("prestige.json", Charset.forName("UTF-8")));
        JsonObject obj = root.getAsJsonObject();

        for (Map.Entry<String, JsonElement> entry: obj.entrySet()) {
            prestige.put(entry.getKey(), entry.getValue().getAsInt());
        }
    }

    private static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
