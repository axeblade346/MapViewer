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
import java.util.logging.Logger;


public final class MapViewer {
    private static final Logger logger = Logger.getLogger(MapViewer.class.getName());

    private static final String[] resources = {
        "bg.png",
        "logo.png",
        "main.css",
        "map.js",
        "pointer.png",
        "search.png",
        "tower.png"
    };

    public static void main(final String[] args) throws IOException, SQLException {
        logger.info("Loading config");
        String suffix = args.length>=1? "-"+args[0] : "";
        Config config = Config.getInstance();
        config.load(Paths.get("mapviewer.properties",new String[0]),suffix);

        final Renderer renderer = new Renderer(config);
        if(Files.notExists(config.getOutputDirectory(),new LinkOption[0])) {
            Files.createDirectory(config.getOutputDirectory(),new FileAttribute[0]);
        }
        copyResources(config);
        logger.info("Loading map data");
        final ZoneInfo zoneInfo = ZoneInfo.load(config);
        final int size = renderer.load(zoneInfo);
        saveIndex(config,zoneInfo,size);
        saveConfig(config,zoneInfo,size);
        logger.info("Saving terrain map");
        renderer.save(config.getOutputDirectory().resolve("map-terrain.png"),Renderer.Type.TERRAIN);
        logger.info("Saving topographic map");
        renderer.save(config.getOutputDirectory().resolve("map-topographic.png"),Renderer.Type.TOPOGRAPHIC);
        logger.info("Saving isometric map");
        renderer.save(config.getOutputDirectory().resolve("map-isometric.png"),Renderer.Type.ISOMETRIC);
        logger.info("Done");
    }

    private static void copyResources(final Config config) throws IOException {
        for(final String res : MapViewer.resources) {
            final Path path = config.getOutputDirectory().resolve(res);
            if(!Files.exists(path,new LinkOption[0])) {
                try(final InputStream in = MapViewer.class.getResourceAsStream("/res/"+res)) {
                    logger.info("Copying "+res+" to "+path);
                    Files.copy(in,path,new CopyOption[0]);
                }
            }
        }
    }

    private static void saveIndex(final Config config,final ZoneInfo zoneInfo,final int size) throws IOException {
        boolean php = config.usePhpOutput();
        String fileName = php? "index.php" : "index.html";
        Path path = config.getOutputDirectory();
        if(Files.notExists(path,new LinkOption[0])) {
            Files.createDirectory(path,new FileAttribute[0]);
        }
        path = path.resolve(fileName);
        logger.info("Saving index file");
        StringBuilder sb = new StringBuilder();
        String serverName = config.getServerName().replace("\"","&quot;");
        if(php) {
            sb.append("<?php\n\n")
              .append("$serverName = \"").append(serverName).append("\";\n")
              .append("$mapSize = ").append(size).append(";\n")
              .append("\n?>");
        }
        sb.append("<!DOCTYPE html>\n")
          .append("<html>\n")
          .append("<head>\n");
        if(php) {
            sb.append("<title><?= $serverName ?> - Map</title>\n");
        } else {
            sb.append("<title>"+serverName+" - Map</title>\n");
        }
        sb.append("<meta charset=\"UTF-8\">\n")
          .append("<link rel=\"stylesheet\" type=\"text/css\" href=\"main.css\"/>\n")
          .append("</head>\n")
          .append("<body>\n")
          .append("<main id=\"container\">\n");
        if(php) {
            sb.append("  <canvas id=\"map\" width=\"<?= $mapSize ?>\" height=\"<?= $mapSize ?>\"></canvas>\n");
        } else {
            sb.append("  <canvas id=\"map\" width=\""+size+"\" height=\""+size+"\"></canvas>\n");
        }
        sb.append("  <div id=\"markers\"></div>\n")
          .append("  <div id=\"sidebar\">\n");
        if(php) {
            sb.append("    <h2><a href=\"/\"><?= $serverName ?></a></h2>\n");
        } else {
            sb.append("    <h2><a href=\"/\">"+serverName+"</a></h2>\n");
        }
        sb.append("    <div id=\"zoom\" class=\"panel\">\n")
          .append("      <h3>Zoom</h3>\n")
          .append("      <div id=\"zoom-in\">+</div>\n")
          .append("      <div id=\"zoom-out\">-</div>\n")
          .append("      <p id=\"zoom-scale\"></p>\n")
          .append("    </div>\n")
          .append("    <div id=\"map-type\" class=\"panel\">\n")
          .append("      <h3>Map Type</h3>\n")
          .append("      <div id=\"map-terrain\" class=\"selected\">Terrain</div>\n")
          .append("      <div id=\"map-topographic\">Topographic</div>\n")
          .append("      <div id=\"map-isometric\">Isometric</div>\n")
          .append("    </div>\n")
          .append("    <div id=\"coords\" class=\"panel\">\n")
          .append("      <h3>Coordinates</h3>\n")
          .append("      <p id=\"coords-pointer\" style=\"display:none\"></p>\n")
          .append("      <p id=\"coords-mouse\">0, 0</p>\n")
          .append("      <p id=\"coords-distance\" style=\"display:none\"></p>\n")
          .append("    </div>\n")
          .append("    <div class=\"panel\">\n")
          .append("      <h3>Layers</h3>\n")
          .append("      <label><input type=\"checkbox\" id=\"layer-deeds\" />Deeds</label>\n")
          .append("      <label><input type=\"checkbox\" id=\"layer-guardtowers\" />Guard towers</label>\n")
          .append("      <label><input type=\"checkbox\" id=\"layer-highways\" disabled />Highways</label>\n")
          .append("      <label><input type=\"checkbox\" id=\"layer-bridges\" disabled />Bridges</label>\n")
          .append("      <label><input type=\"checkbox\" id=\"layer-tunnels\" disabled />Tunnels</label>\n")
          .append("    </div>\n")
          .append("    <div class=\"panel\">\n")
          .append("      <h3>Info</h3>\n")
          .append("      <p id=\"info\"></p>\n")
          .append("    </div>\n")
          .append("    <footer>\n")
          .append("      <div id=\"timestamp\"></div>\n")
          .append("      <a id=\"map-file\" href=\"#\" target=\"_blank\">Map file</a>\n")
          .append("    </footer>\n")
          .append("  </div>\n")
          .append("  <div id=\"search\">\n")
          .append("    <input type=\"text\" id=\"searchbox\" placeholder=\"Search deeds and locations...\" />\n")
          .append("    <div id=\"searchbutton\"></div>\n")
          .append("    <div id=\"autocomplete\" style=\"display: none;\"></div>\n")
          .append("  </div>\n")
          .append("</main>\n")
          .append("<script type=\"text/javascript\" src=\"config.js\"></script>\n")
          .append("<script type=\"text/javascript\" src=\"map.js\"></script>\n")
          .append("</body>\n")
          .append("</html>\n");
        try(final OutputStream out = Files.newOutputStream(path,new OpenOption[0])) {
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void saveConfig(final Config config,final ZoneInfo zoneInfo,final int size) throws IOException {
        logger.info("Saving config file");
        final Path path = config.getOutputDirectory();
        final List<Deed> deeds = zoneInfo.getDeeds();
        final List<FocusZone> focusZones = zoneInfo.getFocusZones();
        final List<Kingdom> kingdoms = zoneInfo.getKingdoms();
        final List<GuardTower> guardTowers = zoneInfo.getGuardTowers();
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
            Files.createDirectory(path,new FileAttribute[0]);
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
}
