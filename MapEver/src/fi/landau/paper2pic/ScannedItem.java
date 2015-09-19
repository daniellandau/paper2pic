package fi.landau.paper2pic;

import java.io.File;

public class ScannedItem {
    public final File full;
    public final File thumb;
    public final String name;

    public ScannedItem(File full, File thumb, String name) {
        this.full = full;
        this.thumb = thumb;
        this.name = name;
    }
}
