import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by Caleb on 10/23/2018.
 */
public class Main {

    public static JDA jda;

    public static void main (String[] args) {

        try {
            String token = Files.readAllLines(Paths.get("token.txt"), Charset.forName("UTF-8")).get(0);
            jda = new JDABuilder(token).build();
            jda.addEventListener(new ReadyListener());
        } catch (LoginException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
