package net.spirangle.mapviewer;

import net.spirangle.mapviewer.zone.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public final class MapViewer {
    private static final Logger LOGGER;
    private static final String[] RESOURCES;

    public static void main(final String[] args) throws IOException, SQLException {
        MapViewer.LOGGER.log(Level.INFO,"Loading config");
        Config config;
        config = Config.load(Paths.get("mapviewer.config",new String[0]));

        final Renderer renderer = new Renderer(config);
        if(Files.notExists(config.getOutputDirectory(),new LinkOption[0])) {
            Files.createDirectory(config.getOutputDirectory(),(FileAttribute<?>[])new FileAttribute[0]);
        }
        copyResources(config);
        MapViewer.LOGGER.log(Level.INFO,"Loading map data");
        final ZoneInfo zoneInfo = ZoneInfo.load(config);
        final int size = renderer.load(zoneInfo.getDeeds(),zoneInfo.getBridgeParts(),zoneInfo.getFocusZones(),zoneInfo.getHwNodes());
        saveConfig(config.getOutputDirectory(),zoneInfo.getDeeds(),zoneInfo.getFocusZones(),zoneInfo.getKingdoms(),zoneInfo.getGuardTowers(),config,size);
        MapViewer.LOGGER.log(Level.INFO,"Saving terrain map");
        renderer.save(config.getOutputDirectory().resolve("map-terrain.png"),Renderer.Type.TERRAIN);
//      renderer.save(config.getOutputDirectory().resolve("sections-flat"),Renderer.Type.FLAT);
        MapViewer.LOGGER.log(Level.INFO,"Saving topographic map");
        renderer.save(config.getOutputDirectory().resolve("map-topographic.png"),Renderer.Type.TOPOGRAPHIC);
//      renderer.save(config.getOutputDirectory().resolve("sections-roads"),Renderer.Type.ROADS);
        MapViewer.LOGGER.log(Level.INFO,"Saving isometric map");
        renderer.save(config.getOutputDirectory().resolve("map-isometric.png"),Renderer.Type.ISOMETRIC);
//      renderer.save(config.getOutputDirectory().resolve("sections-3d"),Renderer.Type.ISOMETRIC);
        MapViewer.LOGGER.log(Level.INFO,"Done");
    }

    private static void copyResources(final Config config) throws IOException {
        String[] resources;
        for(int length = (resources = MapViewer.RESOURCES).length, i = 0; i<length; ++i) {
            final String res = resources[i];
            final Throwable t = null;
            try(final InputStream in = MapViewer.class.getResourceAsStream("/res/"+res)) {
                final Path path = config.getOutputDirectory().resolve(res);
                if(!Files.exists(path,new LinkOption[0])) {
                    MapViewer.LOGGER.log(Level.INFO,"Copying "+res+" to "+path);
                    Files.copy(in,path,new CopyOption[0]);
                }
            }
        }
    }

    private static void saveConfig(final Path path,final List<Deed> deeds,final List<FocusZone> focusZones,final List<Kingdom> kingdoms,
                                   final List<GuardTower> guardTowers,final Config config,final int size) throws IOException {
        MapViewer.LOGGER.log(Level.INFO,"Saving config file");
        int y;
        int x = y = size/2;
        for(final Deed deed : deeds) {
            if(deed.isPermanent()) {
                x = deed.getX();
                y = deed.getY();
                break;
            }
        }
        if(Files.notExists(path,new LinkOption[0])) {
            Files.createDirectory(path,(FileAttribute<?>[])new FileAttribute[0]);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\nfunction Config() {}\n");
        sb.append("var config = new Config();\n");
        sb.append("config.size = ").append(size).append(";\n");
        sb.append("config.x = ").append(x).append(";\n");
        sb.append("config.y = ").append(y).append(";\n");
        sb.append("config.mode = \"terrain\";\n");
        sb.append("config.showDeedBordersIn3dMode = ").append(config.showDeedBorders3d()).append(";\n");
        sb.append("config.showDeedBordersInFlatMode = ").append(config.showDeedBordersFlat()).append(";\n\n");
        sb.append("function Deed(name,founder,mayor,creationDate,democracy,kingdom,x,y,height,permanent,sx,sy,ex,ey) {\n");
        sb.append("   this.name          = name;\n");
        sb.append("   this.founder       = founder;\n");
        sb.append("   this.mayor         = mayor;\n");
        sb.append("   this.creationDate  = creationDate;\n");
        sb.append("   this.democracy     = democracy;\n");
        sb.append("   this.kingdom       = kingdom;\n");
        sb.append("   this.x             = x;\n");
        sb.append("   this.y             = y;\n");
        sb.append("   this.sx            = sx;\n");
        sb.append("   this.sy            = sy;\n");
        sb.append("   this.ex            = ex;\n");
        sb.append("   this.ey            = ey;\n");
        sb.append("   this.height        = height;\n");
        sb.append("   this.permanent     = permanent;\n");
        sb.append("}\n\n");
        sb.append("function FocusZone(name,x,y,height,type,sx,sy,ex,ey) {\n");
        sb.append("   this.name    = name;\n");
        sb.append("   this.x       = x;\n");
        sb.append("   this.y       = y;\n");
        sb.append("   this.sx      = sx;\n");
        sb.append("   this.sy      = sy;\n");
        sb.append("   this.ex      = ex;\n");
        sb.append("   this.ey      = ey;\n");
        sb.append("   this.height  = height;\n");
        sb.append("   this.type    = type;\n");
        sb.append("}\n\n");
        sb.append("function Kingdom(kingdom,name,king) {\n");
        sb.append("   this.kingdom = kingdom;\n");
        sb.append("   this.name    = name;\n");
        sb.append("   this.king    = king;\n");
        sb.append("}\n\n");
        sb.append("function GuardTower(owner,creationDate,kingdom,x,y,z,description) {\n");
        sb.append("   this.owner         = owner;\n");
        sb.append("   this.creationDate  = creationDate;\n");
        sb.append("   this.kingdom       = kingdom;\n");
        sb.append("   this.x             = x;\n");
        sb.append("   this.y             = y;\n");
        sb.append("   this.z             = z;\n");
        sb.append("   this.description   = description;\n");
        sb.append("}\n\n");
        sb.append("var deeds = [];\n");
        sb.append("var focusZones = [];\n");
        sb.append("var kingdoms = [];\n");
        sb.append("var guardTowers = [];\n");
        if(config.showDeeds()) {
            for(final Deed deed : deeds) {
                if(deed.getVisibility() >= 2) continue;
                String name, founder, mayor;
                name = founder = mayor = "<hidden>";
                if(deed.getVisibility()==0) {
                    name = deed.getName().replace("'","\\'");
                    founder = deed.getFounder().replace("'","\\'");
                    mayor = deed.getMayor().replace("'","\\'");
                }
                sb.append("deeds.push(new Deed('")
                  .append(name).append("','")
                  .append(founder).append("','")
                  .append(mayor).append("',")
                  .append(deed.getCreationDate()).append(",")
                  .append(deed.isDemocracy()).append(",")
                  .append(deed.getKingdom()).append(",")
                  .append(deed.getX()).append(",")
                  .append(deed.getY()).append(",")
                  .append(deed.getHeight()).append(",")
                  .append(deed.isPermanent()).append(",")
                  .append(deed.getSx()).append(",")
                  .append(deed.getSy()).append(",")
                  .append(deed.getEx()).append(",")
                  .append(deed.getEy()).append("));\n");
            }
        }
        for(final FocusZone focusZone : focusZones) {
            sb.append("focusZones.push(new FocusZone('")
              .append(focusZone.getName().replace("'","\\'")).append("',")
              .append(focusZone.getX()).append(",")
              .append(focusZone.getY()).append(",")
              .append(focusZone.getHeight()).append(",")
              .append(focusZone.getType()).append(",")
              .append(focusZone.getSx()).append(",")
              .append(focusZone.getSy()).append(",")
              .append(focusZone.getEx()).append(",")
              .append(focusZone.getEy()).append("));\n");
        }
        for(final Kingdom kingdom : kingdoms) {
            String king = kingdom.getKing()!=null? kingdom.getKing().replace("'","\\'") : "";
            sb.append("kingdoms[").append(kingdom.getKingdom()).append("] = new Kingdom(")
              .append(kingdom.getKingdom()).append(",'")
              .append(kingdom.getName().replace("'","\\'")).append("','")
              .append(king).append("');\n");
        }
        for(final GuardTower guardTower : guardTowers) {
            sb.append("guardTowers.push(new GuardTower('")
              .append(guardTower.getOwner().replace("'","\\'")).append("',")
              .append(guardTower.getCreationDate()).append(",")
              .append(guardTower.getKingdom()).append(",")
              .append(guardTower.getX()).append(",")
              .append(guardTower.getY()).append(",")
              .append(guardTower.getZ()).append(",'")
              .append(guardTower.getDescription().replace("'","\\'")).append("'));\n");
        }
        sb.append("var timestamp = ").append(System.currentTimeMillis()).append(";\n");
        try(final OutputStream out = Files.newOutputStream(path.resolve("config.js"),new OpenOption[0])) {
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    static {
        LOGGER = Logger.getLogger(MapViewer.class.getName());
        RESOURCES = new String[]{"index.html","map.js","main.css","search.png","bg.png","logo.png"};
    }
}
