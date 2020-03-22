package net.spirangle.mapviewer.zone;

import com.wurmonline.server.support.JSONException;
import com.wurmonline.server.support.JSONObject;
import com.wurmonline.server.support.JSONTokener;
import net.spirangle.mapviewer.Config;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ZoneInfo {
    private static final Logger LOGGER;
    private Server server;
    private final Map<String,PlayerData> playersData;
    private final Map<Long,PlayerData> playersDataById;
    private final List<Deed> deeds;
    private final List<FocusZone> focusZones;
    private final List<Kingdom> kingdoms;
    private final List<GuardTower> guardTowers;
    private final Map<Tile,BridgePart> bridgeParts;
    private final List<HighwayNode> hwNodes;
    private static final int HW_NODE = 16711680;
    private static final int HW_WAYSTONE = 16753920;

    private ZoneInfo() {
        this.server = null;
        this.playersData = new HashMap<>();
        this.playersDataById = new HashMap<>();
        this.deeds = new ArrayList<>();
        this.focusZones = new ArrayList<>();
        this.kingdoms = new ArrayList<>();
        this.guardTowers = new ArrayList<>();
        this.bridgeParts = new HashMap<>();
        this.hwNodes = new ArrayList<>();
    }

    public Map<String,PlayerData> getPlayersData() {
        return this.playersData;
    }

    public Map<Long,PlayerData> getPlayersDataById() {
        return this.playersDataById;
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

    public List<GuardTower> getGuardTowers() {
        return this.guardTowers;
    }

    public List<HighwayNode> getHwNodes() {
        return this.hwNodes;
    }

    public Map<Tile,BridgePart> getBridgeParts() {
        return this.bridgeParts;
    }

    public static ZoneInfo load(final Config config) throws SQLException, IOException {
        final Path temp = Paths.get("temp",new String[0]);
        if(Files.notExists(temp,new LinkOption[0])) {
            Files.createDirectory(temp,(FileAttribute<?>[])new FileAttribute[0]);
        }

        ZoneInfo.LOGGER.log(Level.INFO,"Copying login database");
        final Path loginTable = temp.resolve("wurmlogin.db");
        Files.deleteIfExists(loginTable);
        Files.copy(config.getMapDirectory().resolve("sqlite/wurmlogin.db"),loginTable,new CopyOption[0]);
        ZoneInfo.LOGGER.log(Level.INFO,"Copying zones database");
        final Path zonesTable = temp.resolve("wurmzones.db");
        Files.deleteIfExists(zonesTable);
        Files.copy(config.getMapDirectory().resolve("sqlite/wurmzones.db"),zonesTable,new CopyOption[0]);
        ZoneInfo.LOGGER.log(Level.INFO,"Copying items database");
        final Path itemsTable = temp.resolve("wurmitems.db");
        Files.deleteIfExists(itemsTable);
        Files.copy(config.getMapDirectory().resolve("sqlite/wurmitems.db"),itemsTable,new CopyOption[0]);
        ZoneInfo.LOGGER.log(Level.INFO,"Copying modsupport database");
        final Path modsupportTable = temp.resolve("modsupport.db");
        Files.deleteIfExists(modsupportTable);
        Files.copy(config.getMapDirectory().resolve("sqlite/modsupport.db"),modsupportTable,new CopyOption[0]);

        final ZoneInfo info = new ZoneInfo();
        Server server = null;
        final Map<String,PlayerData> playersData = info.getPlayersData();
        final Map<Long,PlayerData> playersDataById = info.getPlayersDataById();
        final Map<Tile,BridgePart> bridges = info.getBridgeParts();
        final List<Deed> deeds = info.getDeeds();
        final List<FocusZone> focusZones = info.getFocusZones();
        final List<Kingdom> kingdoms = info.getKingdoms();
        final List<GuardTower> guardTowers = info.getGuardTowers();
        final List<HighwayNode> hwNodes = info.getHwNodes();

        final Connection loginConnection = DriverManager.getConnection("jdbc:sqlite:temp/wurmlogin.db");
        final Connection zonesConnection = DriverManager.getConnection("jdbc:sqlite:temp/wurmzones.db");
        final Connection itemsConnection = DriverManager.getConnection("jdbc:sqlite:temp/wurmitems.db");
        final Connection modsupportConnection = DriverManager.getConnection("jdbc:sqlite:temp/modsupport.db");

        ZoneInfo.LOGGER.log(Level.INFO,"Loading players data");
        final PreparedStatement playersDataStatement = modsupportConnection.prepareStatement("SELECT WURMID,NAME,DATA FROM PLAYERSDATA;");
        final ResultSet playersDataResultSet = playersDataStatement.executeQuery();
        while(playersDataResultSet.next()) {
            final long wurmId = playersDataResultSet.getLong("WURMID");
            final String name = playersDataResultSet.getString("NAME");
            final String data = playersDataResultSet.getString("DATA");
            try {
                JSONTokener jt = new JSONTokener(data);
                JSONObject json = new JSONObject(jt);
                final PlayerData pd = new PlayerData(wurmId,name,json);
                playersData.put(name,pd);
                playersDataById.put(wurmId,pd);
            } catch(JSONException e) {
                ZoneInfo.LOGGER.log(Level.WARNING,"Failed to load data for "+name+": "+e.getMessage(),e);
            }
        }

        ZoneInfo.LOGGER.log(Level.INFO,"Loading server");
        final PreparedStatement serverStatement = loginConnection.prepareStatement("SELECT * FROM SERVERS WHERE `SERVER` == ?;");
        serverStatement.setInt(1,config.getServerId());
        final ResultSet serverResultSet = serverStatement.executeQuery();
        if(serverResultSet.next()) {
            final int id = serverResultSet.getInt("SERVER");
            final String name = serverResultSet.getString("NAME");
            final boolean pvp = serverResultSet.getInt("PVP")==1? true : false;
            server = new Server(id,name,pvp);
            info.server = server;
        }

        ZoneInfo.LOGGER.log(Level.INFO,"Loading bridges");
        final PreparedStatement bridgeStatement = zonesConnection.prepareStatement("SELECT * FROM BRIDGEPARTS;");
        final ResultSet bridgeResultSet = bridgeStatement.executeQuery();
        while(bridgeResultSet.next()) {
            final int x = bridgeResultSet.getInt("TILEX");
            final int y = bridgeResultSet.getInt("TILEY");
            final int material = bridgeResultSet.getInt("MATERIAL");
            final int heightOffset = bridgeResultSet.getInt("HEIGHTOFFSET");
            final int slope = bridgeResultSet.getInt("SLOPE");
            final Tile tile = new Tile(x,y);
            final BridgePart bridgePart = new BridgePart(material,heightOffset,slope);
            bridges.put(tile,bridgePart);
        }

        ZoneInfo.LOGGER.log(Level.INFO,"Loading focus zones");
        final PreparedStatement focusZoneStatement = zonesConnection.prepareStatement("SELECT * FROM FOCUSZONES;");
        final ResultSet focusZonesResultSet = focusZoneStatement.executeQuery();
        while(focusZonesResultSet.next()) {
            final String name = focusZonesResultSet.getString("NAME");
            final int sx = focusZonesResultSet.getInt("STARTX");
            final int sy = focusZonesResultSet.getInt("STARTY");
            final int ex = focusZonesResultSet.getInt("ENDX");
            final int ey = focusZonesResultSet.getInt("ENDY");
            final int type = focusZonesResultSet.getInt("TYPE");
            focusZones.add(new FocusZone(name,sx,sy,ex,ey,type));
        }

        ZoneInfo.LOGGER.log(Level.INFO,"Loading deeds and tokens");
        final PreparedStatement tokenStatement = itemsConnection.prepareStatement("SELECT * FROM ITEMS WHERE `WURMID` == ?;");
        final PreparedStatement deedStatement = zonesConnection.prepareStatement("SELECT * FROM VILLAGES WHERE `DISBANDED` == 0;");
        final ResultSet deedResultSet = deedStatement.executeQuery();
        while(deedResultSet.next()) {
            final String name = deedResultSet.getString("NAME");
            final String founder = deedResultSet.getString("FOUNDER");
            final String mayor = deedResultSet.getString("MAYOR");
            final long creationDate = deedResultSet.getLong("CREATIONDATE");
            final boolean democracy = deedResultSet.getInt("DEMOCRACY")==1;
            final int kingdom = deedResultSet.getInt("KINGDOM");
            final int sx = deedResultSet.getInt("STARTX");
            final int sy = deedResultSet.getInt("STARTY");
            final int ex = deedResultSet.getInt("ENDX");
            final int ey = deedResultSet.getInt("ENDY");
            int visibility = 0;
            int x = (sx+ex)/2;
            int y = (sy+ey)/2;
            final long tokenId = deedResultSet.getLong("TOKEN");
            tokenStatement.setLong(1,tokenId);
            final ResultSet tokenResultSet = tokenStatement.executeQuery();
            while(tokenResultSet.next()) {
                x = (int)(tokenResultSet.getFloat("POSX")/4.0f);
                y = (int)(tokenResultSet.getFloat("POSY")/4.0f);
            }
            final boolean permanent = deedResultSet.getInt("PERMANENT")==1;
            boolean pvp = server.isPvp();
            for(final FocusZone focusZone : focusZones) {
                if(focusZone.contains(x,y)) {
                    final int type = focusZone.getType();
                    if(server.isPvp() && type==5) pvp = false;
                    else if(!server.isPvp() && (type==2 || type==6 || type==7)) pvp = true;
                }
            }
            final PlayerData pd = playersData.get(mayor);
            if(pd!=null) {
                visibility = pd.getJson().optInt("deedVisibility",0);
            }
            deeds.add(new Deed(name,founder,mayor,creationDate,democracy,kingdom,x,y,permanent,pvp,sx,sy,ex,ey,visibility));
        }

        ZoneInfo.LOGGER.log(Level.INFO,"Loading kingdoms");
        final PreparedStatement kingdomsStatement = zonesConnection.prepareStatement("SELECT k.KINGDOM,k.KINGDOMNAME,e.KINGSNAME FROM KINGDOMS AS k LEFT JOIN KING_ERA AS e ON e.KINGDOM=k.KINGDOM AND e.CURRENT=1;");
        final ResultSet kingdomsResultSet = kingdomsStatement.executeQuery();
        while(kingdomsResultSet.next()) {
            final int kingdom = kingdomsResultSet.getInt("KINGDOM");
            final String name = kingdomsResultSet.getString("KINGDOMNAME");
            final String king = kingdomsResultSet.getString("KINGSNAME");
            kingdoms.add(new Kingdom(kingdom,name,king));
        }

        ZoneInfo.LOGGER.log(Level.INFO,"Loading guard towers");
        final PreparedStatement guardTowerItemsStatement = itemsConnection.prepareStatement("SELECT TEMPLATEID,POSX,POSY,POSZ,CREATIONDATE,LASTOWNERID,AUXDATA,DESCRIPTION FROM ITEMS WHERE TEMPLATEID IN (384,430,528,638);");
        final ResultSet guardTowerItemsResultSet = guardTowerItemsStatement.executeQuery();
        while(guardTowerItemsResultSet.next()) {
            final int t = guardTowerItemsResultSet.getInt("TEMPLATEID");
            final int x = guardTowerItemsResultSet.getInt("POSX");
            final int y = guardTowerItemsResultSet.getInt("POSY");
            final float z = guardTowerItemsResultSet.getFloat("POSZ");
            final long creationDate = guardTowerItemsResultSet.getLong("CREATIONDATE");
            final long lastOwnerId = guardTowerItemsResultSet.getLong("LASTOWNERID");
            final int a = guardTowerItemsResultSet.getInt("AUXDATA");
            final String desc = guardTowerItemsResultSet.getString("DESCRIPTION");
            String owner = "";
            final PlayerData pd = playersDataById.get(lastOwnerId);
            if(pd!=null) owner = pd.getName();
            guardTowers.add(new GuardTower(owner,creationDate,a,x/4,y/4,Math.round(z*10.0f),desc));
        }

        ZoneInfo.LOGGER.log(Level.INFO,"Loading highways");
        final PreparedStatement highwayItemsStatement = itemsConnection.prepareStatement("SELECT TEMPLATEID,POSX,POSY,AUXDATA FROM ITEMS WHERE TEMPLATEID IN (1114,1112);");
        final ResultSet highwayItemsResultSet = highwayItemsStatement.executeQuery();
        while(highwayItemsResultSet.next()) {
            final int t = highwayItemsResultSet.getInt("TEMPLATEID");
            final int x = highwayItemsResultSet.getInt("POSX");
            final int y = highwayItemsResultSet.getInt("POSY");
            final int a = highwayItemsResultSet.getInt("AUXDATA");
            if(x>0 && (y>0&a!=0)) {
                hwNodes.add(new HighwayNode(x/4,y/4,(t==1112)? 16753920 : 16711680));
            }
        }

        modsupportConnection.close();
        itemsConnection.close();
        zonesConnection.close();
        loginConnection.close();
        return info;
    }

    static {
        LOGGER = Logger.getLogger(ZoneInfo.class.getName());
    }
}
