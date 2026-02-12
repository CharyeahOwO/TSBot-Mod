package com.example.tsbotmod;

/**
 * 音乐搜索结果数据类。
 */
public class MusicSearchResult {
    private final String id;
    private final String name;
    private final String artist;

    public MusicSearchResult(String id, String name, String artist) {
        this.id = id;
        this.name = name;
        this.artist = artist;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getArtist() {
        return artist;
    }

    /** 格式化显示：歌曲名 - 歌手 */
    public String getDisplayName() {
        return name + " - " + artist;
    }
}
