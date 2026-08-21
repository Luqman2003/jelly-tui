package tui.dto;

import api.dto.MediaItem;

import java.util.List;

public record ScreenState(String title, List<MediaItem> items, String parentId) {}
