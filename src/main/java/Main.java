import api.JellyfinClient;
import api.dto.AuthResponse;
import api.dto.ItemsResponse;
import api.dto.User;
import config.Config;
import config.ConfigManager;
import tui.TerminalApp;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        ConfigManager configManager = new ConfigManager();
        JellyfinClient client = new JellyfinClient();

        Config config = configManager.load();

        if (config == null) {
            // we don't have a saved config
            // prompt user to login and enter serverUrl
            // run jellyfinAuth, if successful, persist config to disk
            config = authenticate(client, configManager);

        } else {
            if (!client.healthCheck(config.serverUrl(), config.accessToken())) {
                System.out.println("Session expired, please log in again");
                config = authenticate(client, configManager);
            } else {
                client.loadSession(config);
            }
        }

        // At this point, config is correct under both cases and client is fine
        ItemsResponse library = client.getLibraries();
        TerminalApp terminalApp = new TerminalApp(client);
        terminalApp.run(library);

    }

    private static Config authenticate(JellyfinClient client, ConfigManager configManager) {
        Scanner scanner = new Scanner(System.in);
        Config config = null;
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        System.out.print("Server URL: ");
        String serverUrl = scanner.nextLine();
        while (serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }

        try {
            AuthResponse authResponse = client.auth(username, password, serverUrl);
            if (authResponse == null) { // bad credentials
                System.out.println("Bad credentials");
                System.exit(1);
            }
            // successful jellyfinauth
            // persist config to disk now
            User user = authResponse.user();
            System.out.println("Logged in as " + user.name());
            config = new Config(user.id(), username, authResponse.accessToken(), serverUrl);
            configManager.save(config);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return config;
    }
}
