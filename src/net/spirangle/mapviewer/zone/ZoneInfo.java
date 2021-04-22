package net.spirangle.mapviewer.zone;

import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.players.Permissions;
import com.wurmonline.server.support.JSONException;
import com.wurmonline.server.support.JSONObject;
import com.wurmonline.server.support.JSONTokener;
import net.spirangle.mapviewer.Config;
import net.spirangle.mapviewer.Renderer;

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

    private static final Logger logger = Logger.getLogger(ZoneInfo.class.getName());

    private static long PLANTED = 1<<Permissions.Allow.PLANTED.getBit();

    // Num-Pad: 1,2,3,4,6,7,8,9
    private static final int[] adjacentX = { -1,0,1,-1,1,-1,0,1 };
    private static final int[] adjacentY = { 1,1,1,0,0,-1,-1,-1 };

    private Server server;
    private final Map<String,PlayerData> playersData;
    private final Map<Long,PlayerData> playersDataById;
    private final List<Deed> deeds;
    private final List<FocusZone> focusZones;
    private final List<Kingdom> kingdoms;
    private final List<GuardTower> guardTowers;
    private final Map<Tile,BridgePart> bridgeParts;
    private final List<HighwayNode> highwayNodes;
    private final List<HighwayNode> bridgeNodes;
    private final List<HighwayNode> tunnelNodes;
    private final List<Sign> signs;

    private ZoneInfo() {
        this.server = null;
        this.playersData = new HashMap<>();
        this.playersDataById = new HashMap<>();
        this.deeds = new ArrayList<>();
        this.focusZones = new ArrayList<>();
        this.kingdoms = new ArrayList<>();
        this.guardTowers = new ArrayList<>();
        this.bridgeParts = new HashMap<>();
        this.highwayNodes = new ArrayList<>();
        this.bridgeNodes = new ArrayList<>();
        this.tunnelNodes = new ArrayList<>();
        this.signs = new ArrayList<>();
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

    public Map<Tile,BridgePart> getBridgeParts() {
        return this.bridgeParts;
    }

    public List<HighwayNode> getHighwayNodes() {
        return this.highwayNodes;
    }

    public List<HighwayNode> getBridgeNodes() {
        return this.bridgeNodes;
    }

    public List<HighwayNode> getTunnelNodes() {
        return this.tunnelNodes;
    }

    public List<Sign> getSigns() {
        return this.signs;
    }

    public static ZoneInfo load(Config config) throws SQLException, IOException {
        Path temp = Paths.get("temp",new String[0]);
        if(Files.notExists(temp,new LinkOption[0])) {
            Files.createDirectory(temp,new FileAttribute[0]);
        }

        logger.log(Level.INFO,"Copying login database");
        Path loginTable = temp.resolve("wurmlogin.db");
        Files.deleteIfExists(loginTable);
        Files.copy(config.getMapDirectory().resolve("sqlite/wurmlogin.db"),loginTable,new CopyOption[0]);
        logger.log(Level.INFO,"Copying zones database");
        Path zonesTable = temp.resolve("wurmzones.db");
        Files.deleteIfExists(zonesTable);
        Files.copy(config.getMapDirectory().resolve("sqlite/wurmzones.db"),zonesTable,new CopyOption[0]);
        logger.log(Level.INFO,"Copying items database");
        Path itemsTable = temp.resolve("wurmitems.db");
        Files.deleteIfExists(itemsTable);
        Files.copy(config.getMapDirectory().resolve("sqlite/wurmitems.db"),itemsTable,new CopyOption[0]);
        logger.log(Level.INFO,"Copying modsupport database");
        Path modsupportTable = temp.resolve("modsupport.db");
        Files.deleteIfExists(modsupportTable);
        Files.copy(config.getMapDirectory().resolve("sqlite/modsupport.db"),modsupportTable,new CopyOption[0]);

        ZoneInfo info = new ZoneInfo();
        Server server = null;
        Map<String,PlayerData> playersData = info.getPlayersData();
        Map<Long,PlayerData> playersDataById = info.getPlayersDataById();
        Map<Tile,BridgePart> bridges = info.getBridgeParts();
        List<Deed> deeds = info.getDeeds();
        List<FocusZone> focusZones = info.getFocusZones();
        List<Kingdom> kingdoms = info.getKingdoms();
        List<GuardTower> guardTowers = info.getGuardTowers();
        List<Sign> signs = info.getSigns();

        Connection loginConnection = DriverManager.getConnection("jdbc:sqlite:temp/wurmlogin.db");
        Connection zonesConnection = DriverManager.getConnection("jdbc:sqlite:temp/wurmzones.db");
        Connection itemsConnection = DriverManager.getConnection("jdbc:sqlite:temp/wurmitems.db");
        Connection modsupportConnection = DriverManager.getConnection("jdbc:sqlite:temp/modsupport.db");

        if(config.usePlayerSettings()) {
            logger.log(Level.INFO,"Loading players data");
            PreparedStatement playersDataStatement = modsupportConnection.prepareStatement("SELECT WURMID,NAME,DATA FROM PLAYERSDATA;");
            ResultSet playersDataResultSet = playersDataStatement.executeQuery();
            while(playersDataResultSet.next()) {
                long wurmId = playersDataResultSet.getLong("WURMID");
                String name = playersDataResultSet.getString("NAME");
                String data = playersDataResultSet.getString("DATA");
                try {
                    JSONTokener jt = new JSONTokener(data);
                    JSONObject json = new JSONObject(jt);
                    PlayerData pd = new PlayerData(wurmId,name,json);
                    playersData.put(name,pd);
                    playersDataById.put(wurmId,pd);
                } catch(JSONException e) {
                    logger.log(Level.WARNING,"Failed to load data for "+name+": "+e.getMessage(),e);
                }
            }
        }

        logger.log(Level.INFO,"Loading server");
        String serversSql = "SELECT * FROM SERVERS";
        if(config.getServerId()>0) serversSql += " WHERE `SERVER`=?";
        PreparedStatement serverStatement = loginConnection.prepareStatement(serversSql+";");
        if(config.getServerId()>0) serverStatement.setInt(1,config.getServerId());
        ResultSet serverResultSet = serverStatement.executeQuery();
        if(serverResultSet.next()) {
            int id = serverResultSet.getInt("SERVER");
            String name = serverResultSet.getString("NAME");
            boolean pvp = serverResultSet.getInt("PVP")==1? true : false;
            server = new Server(id,name,pvp);
            info.server = server;
        }

        logger.log(Level.INFO,"Loading bridges");
        PreparedStatement bridgeStatement = zonesConnection.prepareStatement("SELECT * FROM BRIDGEPARTS;");
        ResultSet bridgeResultSet = bridgeStatement.executeQuery();
        while(bridgeResultSet.next()) {
            int x = bridgeResultSet.getInt("TILEX");
            int y = bridgeResultSet.getInt("TILEY");
            int material = bridgeResultSet.getInt("MATERIAL");
            int heightOffset = bridgeResultSet.getInt("HEIGHTOFFSET");
            int slope = bridgeResultSet.getInt("SLOPE");
            Tile tile = new Tile(x,y);
            BridgePart bridgePart = new BridgePart(material,heightOffset,slope);
            bridges.put(tile,bridgePart);
        }

        logger.log(Level.INFO,"Loading focus zones");
        PreparedStatement focusZoneStatement = zonesConnection.prepareStatement("SELECT * FROM FOCUSZONES;");
        ResultSet focusZonesResultSet = focusZoneStatement.executeQuery();
        while(focusZonesResultSet.next()) {
            String name = focusZonesResultSet.getString("NAME");
            int sx = focusZonesResultSet.getInt("STARTX");
            int sy = focusZonesResultSet.getInt("STARTY");
            int ex = focusZonesResultSet.getInt("ENDX");
            int ey = focusZonesResultSet.getInt("ENDY");
            int type = focusZonesResultSet.getInt("TYPE");
            focusZones.add(new FocusZone(name,sx,sy,ex,ey,type));
        }

        logger.log(Level.INFO,"Loading deeds and tokens");
        PreparedStatement tokenStatement = itemsConnection.prepareStatement("SELECT * FROM ITEMS WHERE `WURMID`=?;");
        PreparedStatement deedStatement = zonesConnection.prepareStatement("SELECT * FROM VILLAGES WHERE `DISBANDED`=0;");
        ResultSet deedResultSet = deedStatement.executeQuery();
        while(deedResultSet.next()) {
            String name = deedResultSet.getString("NAME");
            String founder = deedResultSet.getString("FOUNDER");
            String mayor = deedResultSet.getString("MAYOR");
            long creationDate = deedResultSet.getLong("CREATIONDATE");
            boolean democracy = deedResultSet.getInt("DEMOCRACY")==1;
            int kingdom = deedResultSet.getInt("KINGDOM");
            int sx = deedResultSet.getInt("STARTX");
            int sy = deedResultSet.getInt("STARTY");
            int ex = deedResultSet.getInt("ENDX");
            int ey = deedResultSet.getInt("ENDY");
            int p = deedResultSet.getInt("PERIMETER");
            int visibility = 0;
            int x = (sx+ex)/2;
            int y = (sy+ey)/2;
            long tokenId = deedResultSet.getLong("TOKEN");
            tokenStatement.setLong(1,tokenId);
            ResultSet tokenResultSet = tokenStatement.executeQuery();
            while(tokenResultSet.next()) {
                x = (int)(tokenResultSet.getFloat("POSX")/4.0f);
                y = (int)(tokenResultSet.getFloat("POSY")/4.0f);
            }
            boolean permanent = deedResultSet.getInt("PERMANENT")==1;
            boolean pvp = server.isPvp();
            for(FocusZone focusZone : focusZones) {
                if(focusZone.contains(x,y)) {
                    int type = focusZone.getType();
                    if(server.isPvp() && type==5) pvp = false;
                    else if(!server.isPvp() && (type==2 || type==6 || type==7)) pvp = true;
                }
            }
            PlayerData pd = playersData.get(mayor);
            if(pd!=null) {
                visibility = pd.getJson().optInt("deedVisibility",0);
            }
            deeds.add(new Deed(name,founder,mayor,creationDate,democracy,kingdom,x,y,permanent,pvp,sx,sy,ex,ey,p,visibility));
        }

        logger.log(Level.INFO,"Loading kingdoms");
        PreparedStatement kingdomsStatement = zonesConnection.prepareStatement("SELECT k.KINGDOM,k.KINGDOMNAME,e.KINGSNAME FROM KINGDOMS AS k LEFT JOIN KING_ERA AS e ON e.KINGDOM=k.KINGDOM AND e.CURRENT=1;");
        ResultSet kingdomsResultSet = kingdomsStatement.executeQuery();
        while(kingdomsResultSet.next()) {
            int kingdom = kingdomsResultSet.getInt("KINGDOM");
            String name = kingdomsResultSet.getString("KINGDOMNAME");
            String king = kingdomsResultSet.getString("KINGSNAME");
            kingdoms.add(new Kingdom(kingdom,name,king));
        }

        logger.log(Level.INFO,"Loading guard towers");
        int[] guardTowerIDs = { ItemList.guardTower,ItemList.guardTowerHots,ItemList.guardTowerMol,ItemList.guardTowerFreedom };
        /*if(config.getGuardTowerIDs()!=null) {
            int[] xids = config.getGuardTowerIDs();
            int[] ids = new int[guardTowerIDs.length+xids.length];
            System.arraycopy(guardTowerIDs,0,ids,0,guardTowerIDs.length);
            System.arraycopy(xids,0,ids,guardTowerIDs.length,xids.length);
            guardTowerIDs = ids;
        }
        StringBuilder guardTowersSQL = new StringBuilder();
        guardTowersSQL.append("SELECT * FROM ITEMS WHERE TEMPLATEID IN (");
        for(int i=0; i<guardTowerIDs.length; ++i) {
            if(i>0) guardTowersSQL.append(',');
            guardTowersSQL.append('?');
        }
        guardTowersSQL.append(");");
        PreparedStatement guardTowerItemsStatement = itemsConnection.prepareStatement(guardTowersSQL.toString());
        for(int i=0; i<guardTowerIDs.length; ++i)
            guardTowerItemsStatement.setInt(i+1,guardTowerIDs[i]);*/
        ResultSet guardTowerItemsResultSet = getItemsResultSet(itemsConnection,guardTowerIDs,config.getGuardTowerIDs());//guardTowerItemsStatement.executeQuery();
        while(guardTowerItemsResultSet.next()) {
            int x = guardTowerItemsResultSet.getInt("POSX")/4;
            int y = guardTowerItemsResultSet.getInt("POSY")/4;
            int z = (int)Math.ceil(guardTowerItemsResultSet.getFloat("POSZ")*10.0f);
            long creationDate = guardTowerItemsResultSet.getLong("CREATIONDATE");
            long lastOwnerId = guardTowerItemsResultSet.getLong("LASTOWNERID");
            int a = guardTowerItemsResultSet.getInt("AUXDATA");
            String desc = guardTowerItemsResultSet.getString("DESCRIPTION");
            String owner = "";
            PlayerData pd = playersDataById.get(lastOwnerId);
            if(pd!=null) owner = pd.getName();
            guardTowers.add(new GuardTower(owner,creationDate,a,x,y,z,desc));
        }

        logger.log(Level.INFO,"Loading signs");
        int[] signIDs = { ItemList.signLarge,ItemList.signSmall,ItemList.signPointing };
        ResultSet signItemsResultSet = getItemsResultSet(itemsConnection,signIDs,null);
        while(signItemsResultSet.next()) {
            long settings = signItemsResultSet.getLong("SETTINGS");
            if((settings&Permissions.Allow.PLANTED.getValue())==0) continue;
            int x = signItemsResultSet.getInt("POSX")/4;
            int y = signItemsResultSet.getInt("POSY")/4;
            if(info.getDeed(x,y)!=null) continue;
            int z = (int)Math.ceil(signItemsResultSet.getFloat("POSZ")*10.0f);
            long creationDate = signItemsResultSet.getLong("CREATIONDATE");
            long lastOwnerId = signItemsResultSet.getLong("LASTOWNERID");
            int a = signItemsResultSet.getInt("AUXDATA");
            String desc = signItemsResultSet.getString("DESCRIPTION");
            String owner = "";
            PlayerData pd = playersDataById.get(lastOwnerId);
            if(pd!=null) owner = pd.getName();
            signs.add(new Sign(owner,creationDate,a,x,y,z,desc));
        }

        modsupportConnection.close();
        itemsConnection.close();
        zonesConnection.close();
        loginConnection.close();
        return info;
    }

    private static ResultSet getItemsResultSet(Connection itemsConnection,int[] itemIds,int[] configIds) throws SQLException {
        int[] ids = itemIds;
        if(configIds!=null) {
            ids = new int[itemIds.length+configIds.length];
            System.arraycopy(itemIds,0,ids,0,itemIds.length);
            System.arraycopy(configIds,0,ids,itemIds.length,configIds.length);
        }
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ITEMS WHERE TEMPLATEID IN (");
        for(int i=0; i<ids.length; ++i) {
            if(i>0) sql.append(',');
            sql.append('?');
        }
        sql.append(");");
        PreparedStatement statement = itemsConnection.prepareStatement(sql.toString());
        for(int i=0; i<ids.length; ++i)
            statement.setInt(i+1,ids[i]);
        return statement.executeQuery();
    }

    public void loadHighways(Renderer renderer) throws SQLException {
        HighwayManager highwayManager = new HighwayManager(highwayNodes);
        HighwayManager bridgeManager = new HighwayManager(bridgeNodes);
        HighwayManager tunnelManager = new HighwayManager(tunnelNodes);
        Map<Tile,HighwayNode> hwNodesMap = new HashMap<>();

        Connection itemsConnection = DriverManager.getConnection("jdbc:sqlite:temp/wurmitems.db");

        logger.log(Level.INFO,"Loading highways");
        PreparedStatement highwayItemsStatement = itemsConnection.prepareStatement("SELECT TEMPLATEID,POSX,POSY,POSZ,AUXDATA,ONBRIDGE,SETTINGS FROM ITEMS WHERE TEMPLATEID IN (?,?);");
        highwayItemsStatement.setLong(1,ItemList.catseye);
        highwayItemsStatement.setLong(2,ItemList.waystone);
        ResultSet highwayItemsResultSet = highwayItemsStatement.executeQuery();
        while(highwayItemsResultSet.next()) {
            int t = highwayItemsResultSet.getInt("TEMPLATEID");
            int x = highwayItemsResultSet.getInt("POSX")/4;
            int y = highwayItemsResultSet.getInt("POSY")/4;
            int z = (int)Math.ceil(highwayItemsResultSet.getFloat("POSZ")*10.0f);
            int a = highwayItemsResultSet.getInt("AUXDATA");
            long b = highwayItemsResultSet.getInt("ONBRIDGE");
            long s = highwayItemsResultSet.getLong("SETTINGS");
            if(x>0 && y>0 & a!=0 && (s&PLANTED)!=0L) {
                Tile tile = new Tile(x,y);
                HighwayNode node = new HighwayNode(x,y,z,t==ItemList.waystone,b!=-10L,renderer.isOnSurface(x,y,z));
                hwNodesMap.put(tile,node);
                if(!node.surfaced) tunnelNodes.add(node);
                else if(node.onBridge) bridgeNodes.add(node);
                else highwayNodes.add(node);
            }
        }
        logger.log(Level.INFO,"Linking all highway nodes");
        for(Map.Entry<Tile,HighwayNode> entry : hwNodesMap.entrySet()) {
            HighwayNode n = entry.getValue();
            for(int i=0; i<8; ++i) {
                HighwayNode a = hwNodesMap.get(new Tile(n.x+adjacentX[i],n.y+adjacentY[i]));
                if(a!=null && n.onBridge==a.onBridge && n.surfaced==a.surfaced) {
                    n.nodes[i] = a;
                    n.nodeCount++;
                }
            }
        }
        highwayManager.unlinkCornerNodes();
        highwayManager.unlinkNodesInLine();
        highwayManager.removeDoubleOrUnlinkedNodes();

        bridgeManager.unlinkCornerNodes();
        bridgeManager.unlinkNodesInLine();
        bridgeManager.removeDoubleOrUnlinkedNodes();

        tunnelManager.unlinkCornerNodes();
        tunnelManager.unlinkNodesInLine();
        tunnelManager.removeDoubleOrUnlinkedNodes();

        itemsConnection.close();
    }

    private Deed getDeed(int x,int y) {
        return deeds.stream()
                    .filter(d -> x>=d.getSx() && y>=d.getSy() && x<=d.getEx() && y<=d.getEy())
                    .findFirst().orElse(null);
    }
}
