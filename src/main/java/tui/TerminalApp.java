package tui;

import api.JellyfinClient;
import api.dto.ItemsResponse;
import api.dto.MediaItem;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.graphics.Theme;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import player.MpvLauncher;
import tui.dto.ScreenState;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class TerminalApp {

    private JellyfinClient client;
    private boolean quitSelected = false;
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

                if (quitSelected) {
                    break;
                }

                if (selected == null) {
                    // back was chosen
                    ScreenState prev = stack.pop();
                    title = prev.title();
                    currItems = prev.items();
                    continue;
                }

                if (selected.type().equals("Movie")) {
                    String streamUrl = client.getStreamUrl(selected.id());
                    player.play(streamUrl);
                    screen.clear();
                    screen.refresh(Screen.RefreshType.COMPLETE);
                    continue;
                }

                if (selected.type().equals("Episode")) {
                    MediaItem episodeToPlay = selected;

                    while (episodeToPlay != null) {
                        String streamUrl = client.getStreamUrl(episodeToPlay.id());
                        player.play(streamUrl);
                        screen.clear();
                        screen.refresh(Screen.RefreshType.COMPLETE);

                        // Find index of current episode and check for next
                        int currentIndex = currItems.indexOf(episodeToPlay);
                        if (currentIndex < 0 || currentIndex + 1 >= currItems.size()) break;

                        MediaItem next = currItems.get(currentIndex + 1);
                        if (!next.type().equals("Episode")) break;

                        String nextLabel = "Episode " + next.indexNumber() + " - " + next.name();
                        MessageDialogButton result = new MessageDialogBuilder()
                                .setTitle("Next Episode")
                                .setText("Play " + nextLabel + "?")
                                .addButton(MessageDialogButton.Yes)
                                .addButton(MessageDialogButton.No)
                                .build()
                                .showDialog(gui);

                        if (result != MessageDialogButton.Yes) break;
                        episodeToPlay = next;
                    }
                    continue;
                }

                // drill in
                ItemsResponse nextItems = client.getItems(selected.id());
                if (nextItems == null || nextItems.mediaItems() == null) {
                    new MessageDialogBuilder()
                            .setTitle("Error")
                            .setText("Failed to load items. Please check your connection and try again.")
                            .addButton(MessageDialogButton.OK)
                            .build()
                            .showDialog(gui);
                    continue;
                }
                stack.push(new ScreenState(title, currItems));
                title = selected.name();
                currItems = nextItems.mediaItems();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            screen.stopScreen();
        }
    }

    public MediaItem showListScreen(String title, List<MediaItem> items, WindowBasedTextGUI gui, boolean showBackOption) throws IOException {
        Panel panel = new Panel(new BorderLayout());
        ActionListBox listBox = new ActionListBox();
        BasicWindow window = new BasicWindow(title);
        AtomicReference<MediaItem> selected = new AtomicReference<>();
        TextBox searchBox = new TextBox();

        rebuildList(listBox, items, "", selected, window, showBackOption);

        String helpText = "[/] Search  [Enter] Select [q] Quit" + (showBackOption ? "  [Esc] Back" : "");
        Label helpLabel = new Label(helpText);

        panel.addComponent(listBox, BorderLayout.Location.CENTER);
        panel.addComponent(helpLabel, BorderLayout.Location.BOTTOM);
        window.setComponent(panel);
        window.setHints(Collections.singletonList(Window.Hint.FULL_SCREEN));
        gui.setTheme(theme);

        window.addWindowListener(new WindowListenerAdapter() {
            @Override
            public void onInput(Window basePane, KeyStroke keyStroke, AtomicBoolean deliverEvent) {
                if (keyStroke.getCharacter() != null && keyStroke.getCharacter() == 'q' && !window.getFocusedInteractable().equals(searchBox)) {
                    System.out.println(keyStroke.getCharacter());
                    window.close();
                    quitSelected = true;
                }

                if (keyStroke.getCharacter() != null && keyStroke.getCharacter() == '/') {
                    panel.removeComponent(helpLabel);
                    panel.addComponent(searchBox, BorderLayout.Location.BOTTOM);
                    searchBox.takeFocus();
                    deliverEvent.set(false);
                }
                if (keyStroke.getKeyType() == KeyType.Escape) {
                    panel.removeComponent(searchBox);
                    searchBox.setText("");
                    panel.addComponent(helpLabel, BorderLayout.Location.BOTTOM);
                    listBox.takeFocus();
                    deliverEvent.set(false);
                }
                if (keyStroke.getKeyType() == KeyType.Enter && window.getFocusedInteractable() == searchBox) {
                    if (listBox.getItemCount() > 1) {
                        listBox.setSelectedIndex(1);
                        listBox.takeFocus();
                    } else if (listBox.getItemCount() > 0 && listBox.getItemCount() < 2) {
                        listBox.setSelectedIndex(0);
                        listBox.takeFocus();
                    }
                    deliverEvent.set(false);
                }
            }
        });

        searchBox.setTextChangeListener((newText, byUser) -> {
            rebuildList(listBox, items, newText, selected, window, showBackOption);
        });
        gui.addWindowAndWait(window);

        return selected.get();
    }

    private void rebuildList(ActionListBox listBox, List<MediaItem> items, String filter,
                             AtomicReference<MediaItem> selected, BasicWindow window, boolean showBackOption) {
        listBox.clearItems();
        if (showBackOption) {
            listBox.addItem("...", window::close);
        }
        for (MediaItem item : items) {
            String displayName = item.type().equals("Episode") ? "Episode " + item.indexNumber() : item.name();
            // replace with isSubsequenceMatch once it's implemented for fuzzy search
            if (filter.isEmpty() || isSubsequenceMatch(filter, displayName)) {
                listBox.addItem(displayName, () -> {
                    selected.set(item);
                    window.close();
                });
            }
        }
    }

    private boolean isSubsequenceMatch(String search, String target) {
        search = search.toLowerCase();
        target = target.toLowerCase();
        int i = 0;
        for (char c : target.toCharArray()) {
            if (i < search.length() && c == search.toCharArray()[i]) {
                i++;
            }
        }
        return i == search.length();
    }
}
