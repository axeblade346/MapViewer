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
        "ls.js",
        "menu.png",
        "pointer.png",
        "search.png",
        "sign.png",
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
        zoneInfo.loadHighways(renderer);
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
          .append("<head>\n")
          .append("<title>");
        if(php) sb.append("<?= $serverName ?>");
        else sb.append(serverName);
        sb.append(" - Map</title>\n")
          .append("<meta charset=\"UTF-8\">\n")
          .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\">\n")
          .append("<link rel=\"stylesheet\" type=\"text/css\" href=\"main.css\"/>\n")
          .append("</head>\n")
          .append("<body>\n")
          .append("<main id=\"container\" class=\"no-ui\">\n")
          .append("  <canvas id=\"map\" width=\"");
        if(php) sb.append("<?= $mapSize ?>");
        else sb.append(size);
        sb.append("\" height=\"");
        if(php) sb.append("<?= $mapSize ?>").append("\"></canvas>\n");
        else sb.append(size);
        sb.append("\"></canvas>\n");
        sb.append("  <div id=\"markers\"></div>\n")
          .append("  <aside id=\"sidebar\">\n")
          .append("    <div>\n")
          .append("      <header>\n")
          .append("        <h2><a href=\"").append(config.getWebPageURL()).append("\">");
        if(php) sb.append("<?= $serverName ?>");
        else sb.append(serverName);
        sb.append("</a></h2>\n")
          .append("      </header>\n")
          .append("      <section id=\"zoom\" class=\"panel\">\n")
          .append("        <h3>Zoom</h3>\n")
          .append("        <div id=\"zoom-in\">+</div>\n")
          .append("        <div id=\"zoom-out\">-</div>\n")
          .append("        <div id=\"zoom-scale\"></div>\n")
          .append("      </section>\n")
          .append("      <section id=\"map-type\" class=\"panel\">\n")
          .append("        <h3>Map Type</h3>\n")
          .append("        <div id=\"map-terrain\" class=\"selected\">Terrain</div>\n")
          .append("        <div id=\"map-topographic\">Topographic</div>\n")
          .append("        <div id=\"map-isometric\">Isometric</div>\n")
          .append("      </section>\n")
          .append("      <section id=\"coords\" class=\"panel\">\n")
          .append("        <h3>Coordinates</h3>\n")
          .append("        <div id=\"coords-pointer\"></div>\n")
          .append("        <div id=\"coords-mouse\">0, 0</div>\n")
          .append("        <div id=\"coords-distance\"></div>\n")
          .append("      </section>\n");
        if(config.showDeeds() || config.showGuardTowers() || config.showKingdoms() ||
           config.showHighways() || config.showSigns()) {
            sb.append("      <section class=\"panel\">\n")
              .append("        <h3>Layers</h3>\n");
            if(config.showDeeds()) {
                sb.append("        <label><input type=\"checkbox\" id=\"layer-deeds\" />Deeds</label>\n")
                  .append("        <label><input type=\"checkbox\" id=\"layer-perimeters\" />Perimeters</label>\n");
            }
            if(config.showGuardTowers()) {
                sb.append("        <label><input type=\"checkbox\" id=\"layer-guardtowers\" />Guard towers</label>\n");
            }
            if(config.showKingdoms()) {
                sb.append("        <label><input type=\"checkbox\" id=\"layer-kingdoms\" />Kingdoms</label>\n");
            }
            if(config.showHighways()) {
                sb.append("        <label><input type=\"checkbox\" id=\"layer-highways\" />Highways</label>\n");
            }
            if(config.showSigns()) {
                sb.append("        <label><input type=\"checkbox\" id=\"layer-signs\" />Signs</label>\n");
            }
            sb.append("      </section>\n");
        }
        
            if(config.showlsa()) {
		    sb.append("<! –– part of the LSA Addon by Axeblade -->")
            sb.append("    <div class=\"panel\">")
            sb.append("      <h3>Locate Soul Assistant</h3>")
            sb.append("	  <label>Face NORTH when using this</label>")
            sb.append("		<label for=\"XI\">X</label>")
            sb.append("		<input type=\"number\" id=\"XI\" name=\"XI\" min=\"1\" max=\"8192\">")
            sb.append("		<label for=\"YI\">Y</label>")
            sb.append("		<input type=\"number\" id=\"YI\" name=\"YI\" min=\"1\" max=\"8192\">")
            sb.append("		<label for=\"EI\">Paste event message here</label>")
            sb.append("		<input type=\"text\" id=\"EI\" name=\"EI\" size=\"15\">")
            sb.append("		<button onclick=\"lsacall()\">Add search area</button>")
            sb.append("	</div>")
            sb.append("	<script type=\"text/javascript\" src=\"lsa.js\"></script>")
            sb.append("	<script>")
            sb.append("			function lsacall() {")
            sb.append("				ls(document.getElementById(\"XI\").value,document.getElementById(\"YI\").value,document.getElementById(\"EI\").value)")
            sb.append("			}")
            sb.append("		</script>")
            sb.append("	<! –– part of the LSA Addon by Axeblade -->");
             }

        sb.append("      <section class=\"panel\">\n")
          .append("        <h3>Info</h3>\n")
          .append("        <div id=\"info\"></div>\n")
          .append("      </section>\n")
          .append("    </div>\n")
          .append("    <footer>\n")
          .append("      <div id=\"timestamp\"></div>\n")
          .append("      <a id=\"map-file\" href=\"#\" target=\"_blank\">Map file</a>\n")
          .append("    </footer>\n")
          .append("  </aside>\n")
          .append("  <button id=\"sidebar-toggle\"><i></i></button>\n")
          .append("  <div id=\"search\">\n")
          .append("    <input type=\"text\" id=\"searchbox\" placeholder=\"Search deeds and locations...\" />\n")
          .append("    <div id=\"searchbutton\"></div>\n")
          .append("  </div>\n")
          .append("  <div id=\"autocomplete\" style=\"display: none;\"></div>\n")
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
        Path path = config.getOutputDirectory();
        List<Deed> deeds = zoneInfo.getDeeds();
        List<FocusZone> focusZones = zoneInfo.getFocusZones();
        List<Kingdom> kingdoms = zoneInfo.getKingdoms();
        List<GuardTower> guardTowers = zoneInfo.getGuardTowers();
        List<Sign> signs = zoneInfo.getSigns();
        List<HighwayNode> highwayNodes = zoneInfo.getHighwayNodes();
        List<HighwayNode> bridgeNodes = zoneInfo.getBridgeNodes();
        List<HighwayNode> tunnelNodes = zoneInfo.getTunnelNodes();
        int y;
        int x = y = size/2;
        for(Deed deed : deeds) {
            if(deed.isPermanent()) {
                x = deed.getX();
                y = deed.getY();
                break;
            }
        }
        if(Files.notExists(path,new LinkOption[0])) {
            Files.createDirectory(path,new FileAttribute[0]);
        }
        String serverName = config.getServerName().replace("\"","&quot;");
        String neutralLandName = config.getNeutralLandName().replace("\"","&quot;");
        StringBuilder sb = new StringBuilder();
        sb.append("\nvar config = {\n")
          .append("   serverName: \"").append(serverName).append("\",\n")
          .append("   size: ").append(size).append(",\n")
          .append("   x: ").append(x).append(",\n")
          .append("   y: ").append(y).append(",\n")
          .append("   mode: \"terrain\",\n")
          .append("   showDeedBordersIn3dMode: ").append(config.showDeedBorders3d()).append(",\n")
          .append("   showDeedBordersInFlatMode: ").append(config.showDeedBordersFlat()).append(",\n")
          .append("   neutralLandName: \"").append(neutralLandName).append("\"\n")
          .append("};\n")
          .append("function Deed(name,founder,mayor,creationDate,democracy,kingdom,x,y,height,permanent,sx,sy,ex,ey,p) {\n")
          .append("   this.name          = name;\n")
          .append("   this.founder       = founder;\n")
          .append("   this.mayor         = mayor;\n")
          .append("   this.creationDate  = creationDate;\n")
          .append("   this.democracy     = democracy;\n")
          .append("   this.kingdom       = kingdom;\n")
          .append("   this.x             = x;\n")
          .append("   this.y             = y;\n")
          .append("   this.sx            = sx;\n")
          .append("   this.sy            = sy;\n")
          .append("   this.ex            = ex;\n")
          .append("   this.ey            = ey;\n")
          .append("   this.p             = p;\n")
          .append("   this.height        = height;\n")
          .append("   this.permanent     = permanent;\n")
          .append("}\n\n")
          .append("function FocusZone(name,x,y,height,type,sx,sy,ex,ey) {\n")
          .append("   this.name    = name;\n")
          .append("   this.x       = x;\n")
          .append("   this.y       = y;\n")
          .append("   this.sx      = sx;\n")
          .append("   this.sy      = sy;\n")
          .append("   this.ex      = ex;\n")
          .append("   this.ey      = ey;\n")
          .append("   this.height  = height;\n")
          .append("   this.type    = type;\n")
          .append("}\n\n")
          .append("function Kingdom(kingdom,name,king,color) {\n")
          .append("   this.kingdom = kingdom;\n")
          .append("   this.name    = name;\n")
          .append("   this.king    = king;\n")
          .append("   this.color   = color;\n")
          .append("}\n\n")
          .append("function GuardTower(owner,creationDate,kingdom,x,y,z,description) {\n")
          .append("   this.owner         = owner;\n")
          .append("   this.creationDate  = creationDate;\n")
          .append("   this.kingdom       = kingdom;\n")
          .append("   this.x             = x;\n")
          .append("   this.y             = y;\n")
          .append("   this.z             = z;\n")
          .append("   this.description   = description;\n")
          .append("}\n\n")
          .append("function Sign(owner,creationDate,x,y,z,message) {\n")
          .append("   this.owner         = owner;\n")
          .append("   this.creationDate  = creationDate;\n")
          .append("   this.x             = x;\n")
          .append("   this.y             = y;\n")
          .append("   this.z             = z;\n")
          .append("   this.message       = message;\n")
          .append("}\n\n")
          .append("var deeds = [];\n")
          .append("var focusZones = [];\n")
          .append("var kingdoms = [];\n")
          .append("var guardTowers = [];\n")
          .append("var signs = [];\n");
        if(config.showDeeds()) {
            for(Deed deed : deeds) {
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
                  .append(deed.getEy()).append(",")
                  .append(deed.getPerimeter()).append("));\n");
            }
        }
        for(FocusZone focusZone : focusZones) {
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
        for(Kingdom kingdom : kingdoms) {
            String king = kingdom.getKing()!=null? kingdom.getKing().replace("'","\\'") : "";
            String color = config.getKingdomColor(kingdom.getKingdom());
            sb.append("kingdoms[").append(kingdom.getKingdom()).append("] = new Kingdom(")
              .append(kingdom.getKingdom()).append(",'")
              .append(kingdom.getName().replace("'","\\'")).append("','")
              .append(king).append("','")
              .append(color==null? "" : color).append("');\n");
        }
        if(config.showGuardTowers() || config.showKingdoms()) {
            for(GuardTower guardTower : guardTowers) {
                sb.append("guardTowers.push(new GuardTower('")
                  .append(guardTower.getOwner().replace("'","\\'")).append("',")
                  .append(guardTower.getCreationDate()).append(",")
                  .append(guardTower.getKingdom()).append(",")
                  .append(guardTower.getX()).append(",")
                  .append(guardTower.getY()).append(",")
                  .append(guardTower.getZ()).append(",'")
                  .append(guardTower.getDescription().replace("'","\\'")).append("'));\n");
            }
        }
        if(config.showSigns()) {
            for(Sign sign : signs) {
                sb.append("signs.push(new Sign('")
                  .append(sign.getOwner().replace("'","\\'")).append("',")
                  .append(sign.getCreationDate()).append(",")
                  .append(sign.getX()).append(",")
                  .append(sign.getY()).append(",")
                  .append(sign.getZ()).append(",'")
                  .append(sign.getMessage().replace("'","\\'")).append("'));\n");
            }
        }
        sb.append("highwayNodes = [");
        if(config.showHighways()) {
            writeHighwayNodes(highwayNodes,sb);
        }
        sb.append("\n];\n")
          .append("bridgeNodes = [");
        if(config.showHighways()) {
            writeHighwayNodes(bridgeNodes,sb);
        }
        sb.append("\n];\n")
          .append("tunnelNodes = [");
        if(config.showHighways()) {
            writeHighwayNodes(tunnelNodes,sb);
        }
        sb.append("\n];\n")
          .append("var timestamp = ").append(System.currentTimeMillis()).append(";\n");
        try(final OutputStream out = Files.newOutputStream(path.resolve("config.js"),new OpenOption[0])) {
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void writeHighwayNodes(List<HighwayNode> highwayNodes,StringBuilder sb) {
        int nodes = 0;
        for(final HighwayNode node : highwayNodes) {
            if(node.nodeCount==0) {
                if(nodes>0) sb.append(',');
                if(nodes%5==0) sb.append("\n   ");
                sb.append("[").append(node.x).append(",").append(node.y).append(",").append(node.z).append(",1]");
                ++nodes;
            } else {
                for(int i = 0; i<8; ++i)
                    if(node.nodes[i]!=null) {
                        HighwayNode next = node.nodes[i];
                        if(nodes>0) sb.append(',');
                        if(nodes%5==0) sb.append("\n   ");
                        sb.append("[")
                          .append(node.x).append(",").append(node.y).append(",").append(node.z).append(",")
                          .append(next.x).append(",").append(next.y).append(",").append(next.z);
                        if(node.waystone) sb.append(",1");
                        sb.append("]");
                        ++nodes;
                    }
            }
        }
    }
}
