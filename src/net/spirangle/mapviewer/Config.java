package net.spirangle.mapviewer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Config {
    private final Path mapDirectory;
    private final Path originalMapDirectory;
    private final Path outputDirectory;
    private final int serverId;
    private final boolean showDeeds;
    private final boolean showDeedBorders3d;
    private final boolean showDeedBordersFlat;

    private Config(final Path mapDirectory,final Path originalMapDirectory,final Path outputDirectory,final int serverId,
                   final boolean showDeeds,final boolean showDeedBorders3d,final boolean showDeedBordersFlat) {
        this.mapDirectory = mapDirectory;
        this.originalMapDirectory = originalMapDirectory;
        this.outputDirectory = outputDirectory;
        this.serverId = serverId;
        this.showDeeds = showDeeds;
        this.showDeedBorders3d = showDeedBorders3d;
        this.showDeedBordersFlat = showDeedBordersFlat;
    }

    public Path getMapDirectory() {
        return this.mapDirectory;
    }

    public Path getOriginalMapDirectory() {
        return this.originalMapDirectory;
    }

    public Path getOutputDirectory() {
        return this.outputDirectory;
    }

    public int getServerId() {
        return this.serverId;
    }

    public boolean showDeeds() {
        return this.showDeeds;
    }

    public boolean showDeedBorders3d() {
        return this.showDeedBorders3d;
    }

    public boolean showDeedBordersFlat() {
        return this.showDeedBordersFlat;
    }

    public static Config load(final Path path) throws IOException {
        try(final BufferedReader reader = Files.newBufferedReader(path)) {
            Path mapDirectory = null;
            Path originalMapDirectory = null;
            Path outputDirectory = null;
            int serverId = 0;
            boolean showDeeds = true;
            boolean showDeedBorders3d = false;
            boolean showDeedBordersFlat = true;
            for(String line = reader.readLine(); line!=null; line = reader.readLine()) {
                if(!line.startsWith("#")) {
                    final String[] parts = line.split("\\s+=\\s+",2);
                    if(parts.length==2) {
                        final String s2;
                        final String s = s2 = parts[0];
                        switch(s2) {
                            case "MAP_DIRECTORY": {
                                mapDirectory = Paths.get(parts[1],new String[0]);
                                break;
                            }
                            case "ORIGINAL_MAP_DIRECTORY": {
                                originalMapDirectory = Paths.get(parts[1],new String[0]);
                                break;
                            }
                            case "SHOW_DEED_BORDERS_IN_3D_MODE": {
                                showDeedBorders3d = Boolean.parseBoolean(parts[1]);
                                break;
                            }
                            case "OUTPUT_DIRECTORY": {
                                outputDirectory = Paths.get(parts[1],new String[0]);
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
                            case "SHOW_DEED_BORDERS_IN_FLAT_MODE": {
                                showDeedBordersFlat = Boolean.parseBoolean(parts[1]);
                                break;
                            }
                        }
                    }
                }
            }
            return new Config(mapDirectory,originalMapDirectory,outputDirectory,serverId,showDeeds,showDeedBorders3d,showDeedBordersFlat);
        }
    }
}
