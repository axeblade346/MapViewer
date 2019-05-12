package net.spirangle.mapviewer;

import java.io.IOException;
import java.io.BufferedReader;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Config {
    private final Path mapDirectory;
    private final Path outputDirectory;
    private final int sectionSize;
    private final int serverId;
    private final boolean showDeeds;
    private final boolean showPvpDeeds;
    private final boolean showDeedBorders3d;
    private final boolean showDeedBordersFlat;
    
    private Config(final Path mapDirectory, final Path outputDirectory, final int sectionSize, final int serverId,
                   final boolean showDeeds, final boolean showPvpDeeds, final boolean showDeedBorders3d, final boolean showDeedBordersFlat) {
        this.mapDirectory = mapDirectory;
        this.outputDirectory = outputDirectory;
        this.sectionSize = sectionSize;
        this.serverId = serverId;
        this.showDeeds = showDeeds;
        this.showPvpDeeds = showPvpDeeds;
        this.showDeedBorders3d = showDeedBorders3d;
        this.showDeedBordersFlat = showDeedBordersFlat;
    }
    
    public Path getMapDirectory() {
        return this.mapDirectory;
    }
    
    public Path getOutputDirectory() {
        return this.outputDirectory;
    }
    
    public int getSectionSize() {
        return this.sectionSize;
    }
    
    public int getServerId() {
        return this.serverId;
    }
    
    public boolean showDeeds() {
        return this.showDeeds;
    }
    
    public boolean showPvpDeeds() {
        return this.showPvpDeeds;
    }
    
    public boolean showDeedBorders3d() {
        return this.showDeedBorders3d;
    }
    
    public boolean showDeedBordersFlat() {
        return this.showDeedBordersFlat;
    }
    
    public static Config load(final Path path) throws IOException {
        try (final BufferedReader reader = Files.newBufferedReader(path)) {
            Path mapDirectory = null;
            Path outputDirectory = null;
            int sectionSize = 256;
            int serverId = 0;
            boolean showDeeds = true;
            boolean showPvpDeeds = true;
            boolean showDeedBorders3d = false;
            boolean showDeedBordersFlat = true;
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (!line.startsWith("#")) {
                    final String[] parts = line.split("\\s+=\\s+", 2);
                    if (parts.length == 2) {
                        final String s2;
                        final String s = s2 = parts[0];
                        switch (s2) {
                            case "SHOW_DEED_BORDERS_IN_3D_MODE": {
                                showDeedBorders3d = Boolean.parseBoolean(parts[1]);
                                break;
                            }
                            case "OUTPUT_DIRECTORY": {
                                outputDirectory = Paths.get(parts[1], new String[0]);
                                break;
                            }
                            case "SERVER_ID": {
                                serverId = Integer.parseInt(parts[1]);
                                break;
                            }
                            case "SHOW_DEEDS": {
                                showDeeds = Boolean.parseBoolean(parts[1]);
                                break;
                            }
                            case "SHOW_PVP_DEEDS": {
                                showPvpDeeds = Boolean.parseBoolean(parts[1]);
                                break;
                            }
                            case "SECTION_SIZE": {
                                sectionSize = Integer.parseInt(parts[1]);
                                break;
                            }
                            case "SHOW_DEED_BORDERS_IN_FLAT_MODE": {
                                showDeedBordersFlat = Boolean.parseBoolean(parts[1]);
                                break;
                            }
                            case "MAP_DIRECTORY": {
                                mapDirectory = Paths.get(parts[1], new String[0]);
                                break;
                            }
                        }
                    }
                }
            }
            return new Config(mapDirectory, outputDirectory, sectionSize, serverId, showDeeds, showPvpDeeds, showDeedBorders3d, showDeedBordersFlat);
        }
    }
}
