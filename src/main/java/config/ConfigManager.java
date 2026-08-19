package config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;

public class ConfigManager {

    private ObjectMapper mapper;
    private Path path;

    public ConfigManager() {
        this.mapper = new ObjectMapper();
        // ~/.config/jelly-tui/config.json
        this.path = Paths.get(System.getProperty("user.home"), ".config", "jelly-tui", "config.json");
    }

    public Config load() throws IOException {
        // get config if it exists, return null if it doesn't
        // client defines what actions to take if there doesn't exist a config
        try {
            if (Files.exists(this.path)) {
                Config config = mapper.readValue(this.path.toFile(), Config.class);
                return config;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean save(Config config) throws IOException {
        try {
            Files.createDirectories(this.path.getParent());
            mapper.writeValue(this.path.toFile(), config);
            // config.json contains a plaintext access token, so restrict it to the owner only
            if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                Files.setPosixFilePermissions(this.path, PosixFilePermissions.fromString("rw-------"));
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
