package net.spirangle.mapviewer;

import java.io.OutputStream;
import java.util.Iterator;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import net.spirangle.mapviewer.zone.FocusZone;
import net.spirangle.mapviewer.zone.Deed;
import java.util.List;
import java.nio.file.Path;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.sql.SQLException;
import java.io.IOException;
import net.spirangle.mapviewer.zone.ZoneInfo;
import net.spirangle.mapviewer.zone.Kingdom;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MapViewer {
    private static final Logger LOGGER;
    private static final String[] RESOURCES;
    
    public static void main(final String[] args) throws IOException, SQLException {
        MapViewer.LOGGER.log(Level.INFO, "Loading config");
        Config config;
        config = Config.load(Paths.get("mapviewer.config", new String[0]));

        final Renderer renderer = new Renderer(config);
        if (Files.notExists(config.getOutputDirectory(), new LinkOption[0])) {
            Files.createDirectory(config.getOutputDirectory(), (FileAttribute<?>[])new FileAttribute[0]);
        }
        copyResources(config);
        MapViewer.LOGGER.log(Level.INFO, "Loading map data");
        final ZoneInfo zoneInfo = ZoneInfo.load(config);
        final int size = renderer.load(zoneInfo.getDeeds(), zoneInfo.getBridgeParts(), zoneInfo.getFocusZones(), zoneInfo.getHwNodes()) / config.getSectionSize();
        saveConfig(config.getOutputDirectory(), zoneInfo.getDeeds(), zoneInfo.getFocusZones(), zoneInfo.getKingdoms(), config, size);
        MapViewer.LOGGER.log(Level.INFO, "Saving 3d map");
        renderer.save(config.getOutputDirectory().resolve("sections-3d"), Renderer.Type.NORMAL);
        MapViewer.LOGGER.log(Level.INFO, "Saving flat map");
        renderer.save(config.getOutputDirectory().resolve("sections-flat"), Renderer.Type.FLAT);
        MapViewer.LOGGER.log(Level.INFO, "Saving road map");
        renderer.save(config.getOutputDirectory().resolve("sections-roads"), Renderer.Type.ROADS);
        MapViewer.LOGGER.log(Level.INFO, "Done");
    }
    
    private static void copyResources(final Config config) throws IOException {
        String[] resources;
        for (int length = (resources = MapViewer.RESOURCES).length, i = 0; i < length; ++i) {
            final String res = resources[i];
            final Throwable t = null;
            try (final InputStream in = MapViewer.class.getResourceAsStream("/res/" + res)) {
                final Path path = config.getOutputDirectory().resolve(res);
                if (!Files.exists(path, new LinkOption[0])) {
                    MapViewer.LOGGER.log(Level.INFO, "Copying " + res + " to " + path);
                    Files.copy(in, path, new CopyOption[0]);
                }
            }
        }
    }
    
    private static void saveConfig(final Path path, final List<Deed> deeds, final List<FocusZone> focusZones, final List<Kingdom> kingdoms,
                                   final Config config, final int size) throws IOException {
        MapViewer.LOGGER.log(Level.INFO, "Saving config file");
        int y;
        int x = y = size * config.getSectionSize() / 2;
        for (final Deed deed : deeds) {
            if (deed.isPermanent()) {
                x = deed.getX();
                y = deed.getY();
                break;
            }
        }
        if (Files.notExists(path, new LinkOption[0])) {
            Files.createDirectory(path, (FileAttribute<?>[])new FileAttribute[0]);
        }
        try (final OutputStream out = Files.newOutputStream(path.resolve("config.js"), new OpenOption[0])) {
            out.write("\nfunction Config() {}\n".getBytes(StandardCharsets.UTF_8));
            out.write("var config = new Config();\n".getBytes(StandardCharsets.UTF_8));
            out.write(("config.size = " + size + ";\n").getBytes(StandardCharsets.UTF_8));
            out.write(("config.step = " + config.getSectionSize() + ";\n").getBytes(StandardCharsets.UTF_8));
            out.write(("config.x = " + x + ";\n").getBytes(StandardCharsets.UTF_8));
            out.write(("config.y = " + y + ";\n").getBytes(StandardCharsets.UTF_8));
            out.write("config.mode = \"flat\";\n".getBytes(StandardCharsets.UTF_8));
            out.write(("config.showDeedBordersIn3dMode = " + config.showDeedBorders3d() + ";\n").getBytes(StandardCharsets.UTF_8));
            out.write(("config.showDeedBordersInFlatMode = " + config.showDeedBordersFlat() + ";\n").getBytes(StandardCharsets.UTF_8));
            out.write("\n".getBytes(StandardCharsets.UTF_8));
            out.write("function Deed(name,founder,mayor,creationDate,democracy,kingdom,x,y,height,permanent,sx,sy,ex,ey) {\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.name          = name;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.founder       = founder;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.mayor         = mayor;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.creationDate  = creationDate;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.democracy     = democracy;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.kingdom       = kingdom;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.x             = x;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.y             = y;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.sx            = sx;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.sy            = sy;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.ex            = ex;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.ey            = ey;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.height        = height;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.permanent     = permanent;\n".getBytes(StandardCharsets.UTF_8));
            out.write("}\n".getBytes(StandardCharsets.UTF_8));
            out.write("\n".getBytes(StandardCharsets.UTF_8));
            out.write("function FocusZone(name,x,y,height,type,sx,sy,ex,ey) {\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.name    = name;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.x       = x;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.y       = y;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.sx      = sx;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.sy      = sy;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.ex      = ex;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.ey      = ey;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.height  = height;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.type    = type;\n".getBytes(StandardCharsets.UTF_8));
            out.write("}\n".getBytes(StandardCharsets.UTF_8));
            out.write("\n".getBytes(StandardCharsets.UTF_8));
            out.write("function Kingdom(kingdom,name,king) {\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.kingdom = kingdom;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.name    = name;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.king    = king;\n".getBytes(StandardCharsets.UTF_8));
            out.write("}\n".getBytes(StandardCharsets.UTF_8));
            out.write("\n".getBytes(StandardCharsets.UTF_8));
            out.write("var deeds = [];\n".getBytes(StandardCharsets.UTF_8));
            out.write("var focusZones = [];\n".getBytes(StandardCharsets.UTF_8));
            out.write("var kingdoms = [];\n".getBytes(StandardCharsets.UTF_8));
            if (config.showDeeds()) {
                for (final Deed deed2 : deeds) {
						  if(!config.showPvpDeeds() && deed2.isPvp() && !deed2.isPermanent()) continue;
                    out.write(("deeds.push(new Deed('" + deed2.getName().replace("'", "\\'") + "','" +
                                                         deed2.getFounder().replace("'", "\\'") + "','" +
                                                         deed2.getMayor().replace("'", "\\'") + "'," + 
                                                         deed2.getCreationDate() + "," + 
                                                         deed2.isDemocracy() + "," + 
                                                         deed2.getKingdom() + "," + 
                                                         deed2.getX() + "," + 
                                                         deed2.getY() + "," + 
                                                         deed2.getHeight() + "," + 
                                                         deed2.isPermanent() + "," + 
                                                         deed2.getSx() + "," + 
                                                         deed2.getSy() + "," + 
                                                         deed2.getEx() + "," + 
                                                         deed2.getEy() + "));\n").getBytes(StandardCharsets.UTF_8));
                }
                for (final FocusZone focusZone : focusZones) {
                    out.write(("focusZones.push(new FocusZone('" + focusZone.getName().replace("'", "\\'") + "'," + 
                                                                   focusZone.getX() + "," + 
                                                                   focusZone.getY() + "," + 
                                                                   focusZone.getHeight() + "," + 
                                                                   focusZone.getType() + "," + 
                                                                   focusZone.getSx() + "," + 
                                                                   focusZone.getSy() + "," + 
                                                                   focusZone.getEx() + "," + 
                                                                   focusZone.getEy() + "));\n").getBytes(StandardCharsets.UTF_8));
                }
                for (final Kingdom kingdom : kingdoms) {
						 String king = kingdom.getKing()!=null? kingdom.getKing().replace("'", "\\'") : "";
                    out.write(("kingdoms["+kingdom.getKingdom()+"] = new Kingdom("+kingdom.getKingdom()+",'" + kingdom.getName().replace("'", "\\'") + "','" + 
                                                                                      king + "');\n").getBytes(StandardCharsets.UTF_8));
                }
            }
			  out.write(("var timestamp = "+System.currentTimeMillis()+";\n").getBytes(StandardCharsets.UTF_8));
        }
    }
    
    static {
        LOGGER = Logger.getLogger(MapViewer.class.getName());
        RESOURCES = new String[] { "index.html", "map.js", "main.css", "search.png", "bg.png", "logo.png" };
    }
}
