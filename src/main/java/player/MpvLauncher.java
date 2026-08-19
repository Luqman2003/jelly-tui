package player;


import java.io.IOException;

public class MpvLauncher {


    //ProcessBuilder(command, args...)
    //  → .inheritIO()
    //  → .start()  → returns a Process
    //  → process.waitFor()  → blocks until it exits, returns the exit code
    public int play(String streamUrl) throws IOException, InterruptedException {
        return new ProcessBuilder("mpv", streamUrl)
                .inheritIO()
                .start()
                .waitFor();
    }

    public int play(String streamUrl, String title) throws IOException, InterruptedException {
        return new ProcessBuilder("mpv", "--title=" + title, streamUrl)
                .inheritIO()
                .start()
                .waitFor();
    }
}
