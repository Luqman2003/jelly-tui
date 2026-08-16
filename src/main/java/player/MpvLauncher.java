package player;


import java.io.IOException;

public class MpvLauncher {


    //ProcessBuilder(command, args...)
    //  → .inheritIO()
    //  → .start()  → returns a Process
    //  → process.waitFor()  → blocks until it exits, returns the exit code
    public int play(String streamUrl) throws InterruptedException {
        try {
            return new ProcessBuilder("mpv", streamUrl)
                    .inheritIO()
                    .start()
                    .waitFor();
        } catch (IOException e) {
            System.err.println("Error: mpv is not installed or not found in PATH.");
            System.err.println("Install mpv to play media: https://mpv.io/installation/");
            return -1;
        }
    }
}
