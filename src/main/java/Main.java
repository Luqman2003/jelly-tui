import api.JellyfinClient;
import api.dto.AuthResponse;
import api.dto.ItemsResponse;
import api.dto.User;
import config.Config;
import config.ConfigManager;
import tui.TerminalApp;

import java.io.Console;
import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length > 0) {
            switch (args[0]) {
                case "--help", "-h" -> {
                    printHelp();
                    return;
                }
                case "--version", "-v" -> {
                    System.out.println("jelly-cli v0.1.0");
                    return;
                }
                case "--logout" -> {
                    ConfigManager cm = new ConfigManager();
                    if (cm.delete()) {
                        System.out.println("Logged out. Saved credentials cleared.");
                    } else {
                        System.out.println("No saved session found.");
                    }
                    return;
                }
                default -> {
                    System.err.println("Unknown option: " + args[0]);
                    printHelp();
                    System.exit(1);
                }
            }
        }

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
        Console console = System.console();
        Config config = null;
        String username;
        String password;
        String serverUrl;

        if (console != null) {
            username = console.readLine("Username: ");
            password = new String(console.readPassword("Password: "));
            serverUrl = console.readLine("Server URL: ");
        } else {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Username: ");
            username = scanner.nextLine();
            System.out.print("Password: ");
            password = scanner.nextLine();
            System.out.print("Server URL: ");
            serverUrl = scanner.nextLine();
        }

        username = username.trim();
        serverUrl = serverUrl.trim();

        if (!serverUrl.isEmpty() && !serverUrl.matches("(?i)^https?://.*")) {
            serverUrl = "http://" + serverUrl;
            System.out.println("No protocol specified, assuming " + serverUrl);
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

    private static void printHelp() {
        System.out.println("jelly-cli - A terminal UI for Jellyfin");
        System.out.println();
        System.out.println("Usage: jelly-cli [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h, --help      Show this help message");
        System.out.println("  -v, --version   Show version");
        System.out.println("  --logout        Clear saved credentials and log out");
    }
}
