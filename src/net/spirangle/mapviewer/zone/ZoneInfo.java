package net.spirangle.mapviewer.zone;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.nio.file.CopyOption;
import java.util.logging.Level;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import net.spirangle.mapviewer.Config;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.logging.Logger;

public final class ZoneInfo {
    private static final Logger LOGGER;
    private Server server;
    private final List<Deed> deeds;
    private final List<FocusZone> focusZones;
    private final List<Kingdom> kingdoms;
    private final Map<Tile, BridgePart> bridgeParts;
    private final List<HighwayNode> hwNodes;
    private static final int HW_NODE = 16711680;
    private static final int HW_WAYSTONE = 16753920;
    
    private ZoneInfo() {
		  this.server = null;
        this.deeds = new ArrayList<Deed>();
        this.focusZones = new ArrayList<FocusZone>();
        this.kingdoms = new ArrayList<Kingdom>();
        this.bridgeParts = new HashMap<Tile, BridgePart>();
        this.hwNodes = new ArrayList<HighwayNode>();
    }
    
    public List<Deed> getDeeds() {
        return this.deeds;
    }
    
    public List<FocusZone> getFocusZones() {
        return this.focusZones;
    }
    
    public List<Kingdom> getKingdoms() {
        return this.kingdoms;
    }
    
    public List<HighwayNode> getHwNodes() {
        return this.hwNodes;
    }
    
    public Map<Tile, BridgePart> getBridgeParts() {
        return this.bridgeParts;
    }
    
    public static ZoneInfo load(final Config config) throws SQLException, IOException {
        final Path temp = Paths.get("temp", new String[0]);
        if (Files.notExists(temp, new LinkOption[0])) {
            Files.createDirectory(temp, (FileAttribute<?>[])new FileAttribute[0]);
        }
        ZoneInfo.LOGGER.log(Level.INFO, "Copying login database");
        final Path loginTable = temp.resolve("wurmlogin.db");
        Files.deleteIfExists(loginTable);
        Files.copy(config.getMapDirectory().resolve("sqlite/wurmlogin.db"), loginTable, new CopyOption[0]);
        ZoneInfo.LOGGER.log(Level.INFO, "Copying zones database");
        final Path zonesTable = temp.resolve("wurmzones.db");
        Files.deleteIfExists(zonesTable);
        Files.copy(config.getMapDirectory().resolve("sqlite/wurmzones.db"), zonesTable, new CopyOption[0]);
        ZoneInfo.LOGGER.log(Level.INFO, "Copying items database");
        final Path itemsTable = temp.resolve("wurmitems.db");
        Files.deleteIfExists(itemsTable);
        Files.copy(config.getMapDirectory().resolve("sqlite/wurmitems.db"), itemsTable, new CopyOption[0]);
        final ZoneInfo info = new ZoneInfo();
        Server server = null;
        final Map<Tile, BridgePart> bridges = info.getBridgeParts();
        final List<Deed> deeds = info.getDeeds();
        final List<FocusZone> focusZones = info.getFocusZones();
        final List<Kingdom> kingdoms = info.getKingdoms();
        final List<HighwayNode> hwNodes = info.getHwNodes();
        final Connection loginConnection = DriverManager.getConnection("jdbc:sqlite:temp/wurmlogin.db");
        final Connection zonesConnection = DriverManager.getConnection("jdbc:sqlite:temp/wurmzones.db");
        final Connection itemsConnection = DriverManager.getConnection("jdbc:sqlite:temp/wurmitems.db");

        ZoneInfo.LOGGER.log(Level.INFO, "Loading server");
        final PreparedStatement serverStatement = loginConnection.prepareStatement("SELECT * FROM SERVERS WHERE `SERVER` == ?;");
        serverStatement.setInt(1,config.getServerId());
        final ResultSet serverResultSet = serverStatement.executeQuery();
        if (serverResultSet.next()) {
            final int id = serverResultSet.getInt("SERVER");
            final String name = serverResultSet.getString("NAME");
            final boolean pvp = serverResultSet.getInt("PVP")==1? true : false;
            server = new Server(id,name,pvp);
			   info.server = server;
        }

        ZoneInfo.LOGGER.log(Level.INFO, "Loading bridges");
        final PreparedStatement bridgeStatement = zonesConnection.prepareStatement("SELECT * FROM BRIDGEPARTS;");
        final ResultSet bridgeResultSet = bridgeStatement.executeQuery();
        while (bridgeResultSet.next()) {
            final int x = bridgeResultSet.getInt("TILEX");
            final int y = bridgeResultSet.getInt("TILEY");
            final int material = bridgeResultSet.getInt("MATERIAL");
            final int heightOffset = bridgeResultSet.getInt("HEIGHTOFFSET");
            final int slope = bridgeResultSet.getInt("SLOPE");
            final Tile tile = new Tile(x, y);
            final BridgePart bridgePart = new BridgePart(material, heightOffset, slope);
            bridges.put(tile, bridgePart);
        }

        ZoneInfo.LOGGER.log(Level.INFO, "Loading focus zones");
        final PreparedStatement focusZoneStatement = zonesConnection.prepareStatement("SELECT * FROM FOCUSZONES;");
        final ResultSet focusZonesResultSet = focusZoneStatement.executeQuery();
        while (focusZonesResultSet.next()) {
            final String name2 = focusZonesResultSet.getString("NAME");
            final int startX2 = focusZonesResultSet.getInt("STARTX");
            final int startY2 = focusZonesResultSet.getInt("STARTY");
            final int endX2 = focusZonesResultSet.getInt("ENDX");
            final int endY2 = focusZonesResultSet.getInt("ENDY");
            final int type = focusZonesResultSet.getInt("TYPE");
            focusZones.add(new FocusZone(name2, startX2, startY2, endX2, endY2, type));
        }

        ZoneInfo.LOGGER.log(Level.INFO, "Loading deeds and tokens");
        final PreparedStatement tokenStatement = itemsConnection.prepareStatement("SELECT * FROM ITEMS WHERE `WURMID` == ?;");
        final PreparedStatement deedStatement = zonesConnection.prepareStatement("SELECT * FROM VILLAGES WHERE `DISBANDED` == 0;");
        final ResultSet deedResultSet = deedStatement.executeQuery();
        while (deedResultSet.next()) {
            final String name = deedResultSet.getString("NAME");
            final String founder = deedResultSet.getString("FOUNDER");
            final String mayor = deedResultSet.getString("MAYOR");
            final long creationDate = deedResultSet.getLong("CREATIONDATE");
            final boolean democracy = deedResultSet.getInt("DEMOCRACY") == 1;
            final int kingdom = deedResultSet.getInt("KINGDOM");
            final int startX = deedResultSet.getInt("STARTX");
            final int startY = deedResultSet.getInt("STARTY");
            final int endX = deedResultSet.getInt("ENDX");
            final int endY = deedResultSet.getInt("ENDY");
            int x2 = (startX + endX) / 2;
            int y2 = (startY + endY) / 2;
            final long tokenId = deedResultSet.getLong("TOKEN");
            tokenStatement.setLong(1, tokenId);
            final ResultSet tokenResultSet = tokenStatement.executeQuery();
            while (tokenResultSet.next()) {
                x2 = (int)(tokenResultSet.getFloat("POSX") / 4.0f);
                y2 = (int)(tokenResultSet.getFloat("POSY") / 4.0f);
            }
            final boolean permanent = deedResultSet.getInt("PERMANENT") == 1;
            boolean pvp = server.isPvp();
            for (final FocusZone focusZone : focusZones) {
					if(focusZone.contains(x2,y2)) {
						final int type = focusZone.getType();
						if(server.isPvp() && type==5) pvp = false;
						else if(!server.isPvp() && (type==2 || type==6 || type==7)) pvp = true;
					}
				}
            deeds.add(new Deed(name, founder, mayor, creationDate, democracy, kingdom, x2, y2, permanent, pvp, startX, startY, endX, endY));
        }

        ZoneInfo.LOGGER.log(Level.INFO, "Loading kingdoms");
        final PreparedStatement kingdomsStatement = zonesConnection.prepareStatement("SELECT k.KINGDOM,k.KINGDOMNAME,e.KINGSNAME FROM KINGDOMS AS k LEFT JOIN KING_ERA AS e ON e.KINGDOM=k.KINGDOM AND e.CURRENT=1;");
        final ResultSet kingdomsResultSet = kingdomsStatement.executeQuery();
        while (kingdomsResultSet.next()) {
            final int kingdom = kingdomsResultSet.getInt("KINGDOM");
            final String name = kingdomsResultSet.getString("KINGDOMNAME");
            final String king = kingdomsResultSet.getString("KINGSNAME");
            kingdoms.add(new Kingdom(kingdom,name,king));
        }

        ZoneInfo.LOGGER.log(Level.INFO, "Loading highways");
        final PreparedStatement highwayItemsStatement = itemsConnection.prepareStatement("SELECT TEMPLATEID,POSX,POSY,AUXDATA FROM ITEMS WHERE TEMPLATEID IN (1114,1112);");
        final ResultSet highwayItemsResultSet = highwayItemsStatement.executeQuery();
        while (highwayItemsResultSet.next()) {
            final int t = highwayItemsResultSet.getInt("TEMPLATEID");
            final int x3 = highwayItemsResultSet.getInt("POSX");
            final int y3 = highwayItemsResultSet.getInt("POSY");
            final int a = highwayItemsResultSet.getInt("AUXDATA");
            if (x3 > 0 && (y3 > 0 & a != 0)) {
                hwNodes.add(new HighwayNode(x3 / 4, y3 / 4, (t == 1112) ? 16753920 : 16711680));
            }
        }
        itemsConnection.close();
        zonesConnection.close();
        loginConnection.close();
        return info;
    }
    
    static {
        LOGGER = Logger.getLogger(ZoneInfo.class.getName());
    }
}
