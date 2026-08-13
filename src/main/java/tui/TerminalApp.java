package tui;

import api.JellyfinClient;
import api.dto.ItemsResponse;
import api.dto.MediaItem;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.graphics.Theme;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import player.MpvLauncher;
import tui.dto.ScreenState;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class TerminalApp {

    private JellyfinClient client;
    Deque<ScreenState> stack;
    private final Theme theme = SimpleTheme.makeTheme(
            true, // activeIsBold
            TextColor.ANSI.DEFAULT, // baseForeground
            TextColor.ANSI.DEFAULT, // baseBackground
            TextColor.ANSI.DEFAULT, // editableForeground
            TextColor.ANSI.DEFAULT, // editableBackground
            TextColor.ANSI.BLUE, // selectedForeground
            TextColor.ANSI.DEFAULT, // selectedBackground
            TextColor.ANSI.DEFAULT  // guiBackground
    );
    MpvLauncher player;

    public TerminalApp(JellyfinClient client) {
        this.client = client;
        this.stack = new ArrayDeque<>();
        this.player = new MpvLauncher();
    }

    public void run(ItemsResponse library) throws IOException {
        Screen screen = new DefaultTerminalFactory().createScreen();
        screen.startScreen();
        WindowBasedTextGUI gui = new MultiWindowTextGUI(screen);
        String title = "Jellyfin TUI";
        MediaItem selected;
        List<MediaItem> currItems = library.mediaItems();

        try {
            while (true) {
                selected = showListScreen(title, currItems, gui, !stack.isEmpty());

                if (selected == null) {
                    // back was chosen
                    ScreenState prev = stack.pop();
                    title = prev.title();
                    currItems = prev.items();
                    continue;
                }

                if (selected.type().equals("Episode")) {
                    String streamUrl = client.getStreamUrl(selected.id());
                    player.play(streamUrl);
                    screen.clear();
                    screen.refresh(Screen.RefreshType.COMPLETE);
                    continue; //time to play
                }

                // drill in
                stack.push(new ScreenState(title, currItems));
                title = selected.name();
                currItems = client.getItems(selected.id()).mediaItems();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            screen.stopScreen();
        }
    }

    public MediaItem showListScreen(String title, List<MediaItem> items, WindowBasedTextGUI gui, boolean showBackOption) throws IOException {
        Panel panel = new Panel();
        ActionListBox listBox = new ActionListBox();
        BasicWindow window = new BasicWindow(title);
        AtomicReference<MediaItem> selected = new AtomicReference<>();
        String itemName;
        int count = 1;

        for (MediaItem item : items) {
            itemName = item.name();
            if (item.type().equals("Episode")) {
                itemName = "Episode " + item.indexNumber();
            }
            listBox.addItem(itemName, () -> {
                selected.set(item);
                window.close();
            });
            count++;
        }

        if (showBackOption) {
            listBox.addItem("..", window::close);
        }

        panel.addComponent(listBox);
        window.setComponent(panel);
        gui.setTheme(theme);
        gui.addWindowAndWait(window);

        return selected.get();
    }
}
