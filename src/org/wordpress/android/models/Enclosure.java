package org.wordpress.android.models;

public class Enclosure {

    private final String url;
    private final int length;
    private final String type;

    public Enclosure(String url, int length, String type) {
        this.url = url;
        this.length = length;
        this.type = type;
    }

    public String getUrl() {
        return this.url;
    }

    public int getLength() {
        return this.length;
    }

    public String getType() {
        return this.type;
    }
}
