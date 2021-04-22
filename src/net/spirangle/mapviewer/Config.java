package net.spirangle.mapviewer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Config {
    private static final Logger logger = Logger.getLogger(Config.class.getName());

    public static Config instance = null;

    public static Config getInstance() {
        if(instance==null) instance = new Config();
        return instance;
    }

    private enum OutputFormat {
        HTML,
        PHP
    }

    private String serverName;
    private String webPageUrl;
    private int serverId;
    private Path mapDirectory;
    private Path originalMapDirectory;
    private Path outputDirectory;
    private OutputFormat outputFormat;
    private boolean showDeeds;
    private boolean showGuardTowers;
    private boolean showKingdoms;
    private boolean showHighways;
    private boolean showSigns;
    private boolean showDeedBorders3d;
    private boolean showDeedBordersFlat;
    private boolean usePlayerSettings;
    private int[] guardTowerIDs;
    private String neutralLandName;
    private String[] kingdomColors;

    private Config() {

    }

    public void load(final Path path,String suffix) throws IOException {
        if(!Files.exists(path)) return;
        try(InputStream stream = Files.newInputStream(path)) {
            Properties properties = new Properties();
            properties.load(stream);
            serverName = getProperty(properties,"server-name",suffix,"Wurm Server");
            logger.info("Server Name: "+serverName);
            webPageUrl = getProperty(properties,"web-page-url",suffix,"/");
            logger.info("Web Page URL: "+webPageUrl);
            serverId = Integer.parseInt(getProperty(properties,"server-id",suffix,"-10"));
            logger.info("Server ID: "+serverId);
            mapDirectory = Paths.get(getProperty(properties,"map-directory",suffix,serverName),new String[0]);
            logger.info("Map directory: "+mapDirectory);
            originalMapDirectory = Paths.get(getProperty(properties,"original-map-directory",suffix,serverName+"_original"),new String[0]);
            logger.info("Map directory (original): "+originalMapDirectory);
            outputDirectory = Paths.get(getProperty(properties,"output-directory",suffix,"mapviewer"),new String[0]);
            logger.info("Output directory: "+outputDirectory);
            String of = getProperty(properties,"output-format",suffix,"HTML");
            outputFormat = OutputFormat.HTML;
            if(of!=null) {
                switch(of.toUpperCase()) {
                    case "HTML":
                        outputFormat = OutputFormat.HTML;
                        break;
                    case "PHP":
                        outputFormat = OutputFormat.PHP;
                        break;
                }
            }
            logger.info("Output Format: "+outputFormat);
            showDeeds = Boolean.parseBoolean(getProperty(properties,"show-deeds",suffix,"true"));
            logger.info("Show deeds: "+showDeeds);
            showGuardTowers = Boolean.parseBoolean(getProperty(properties,"show-guard-towers",suffix,"true"));
            logger.info("Show guard towers: "+showGuardTowers);
            showKingdoms = Boolean.parseBoolean(getProperty(properties,"show-kingdoms",suffix,"true"));
            logger.info("Show kingdoms: "+showKingdoms);
            showHighways = Boolean.parseBoolean(getProperty(properties,"show-highways",suffix,"true"));
            logger.info("Show highways: "+showHighways);
            showSigns = Boolean.parseBoolean(getProperty(properties,"show-signs",suffix,"true"));
            logger.info("Show signs: "+showSigns);
            showDeedBorders3d = Boolean.parseBoolean(getProperty(properties,"show-deed-borders-in-3d-mode",suffix,"false"));
            logger.info("Show deed borders (3D): "+showDeedBorders3d);
            showDeedBordersFlat = Boolean.parseBoolean(getProperty(properties,"show-deed-borders-in-flat-mode",suffix,"true"));
            logger.info("Show deed borders (flat): "+showDeedBordersFlat);
            usePlayerSettings = Boolean.parseBoolean(getProperty(properties,"use-player-settings",suffix,"false"));
            logger.info("Use player settings: "+usePlayerSettings);
            String gtids = getProperty(properties,"guard-tower-ids",suffix,null);
            guardTowerIDs = parseIntArray(gtids);
            logger.info("Guard tower IDs: "+gtids);
            neutralLandName = getProperty(properties,"neutral-land-name",suffix,serverName);
            logger.info("Neutral Land Name: "+neutralLandName);
            String kcols = getProperty(properties,"kingdom-colors",suffix,null);
            kingdomColors = parseArray(kcols);
            logger.info("Kingdom colors: "+kcols);
        }
    }

    private String getProperty(Properties properties,String key,String suffix,String defaultValue) {
        String value = properties.getProperty(key+suffix);
        if((value==null || value.isEmpty()) && suffix!=null && !suffix.isEmpty())
            value = properties.getProperty(key);
        if(value==null || value.isEmpty())
            value = defaultValue;
        return value;
    }

    private String[] parseArray(String value) {
        if(value==null) return null;
        String[] strArray = value.split(",");
        for(int i=0; i<strArray.length; ++i)
            strArray[i] = strArray[i].trim();
        return strArray;
    }

    private int[] parseIntArray(String value) {
        if(value==null) return null;
        String[] strArray = value.split(",");
        int[] intArray = new int[strArray.length];
        for(int i=0; i<strArray.length; ++i) {
            try {
                intArray[i] = Integer.parseInt(strArray[i]);
            } catch(NumberFormatException e) {
                logger.log(Level.SEVERE,"Guard tower IDs: "+e.getMessage(),e);
                return null;
            }
        }
        return intArray;
    }

    public String getServerName() {
        return this.serverName;
    }

    public String getWebPageURL() {
        return this.webPageUrl;
    }

    public int getServerId() {
        return this.serverId;
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

    public boolean useHtmlOutput() {
        return OutputFormat.HTML.equals(this.outputFormat);
    }

    public boolean usePhpOutput() {
        return OutputFormat.PHP.equals(this.outputFormat);
    }

    public boolean showDeeds() {
        return this.showDeeds;
    }

    public boolean showGuardTowers() {
        return this.showGuardTowers;
    }

    public boolean showKingdoms() {
        return this.showKingdoms;
    }

    public boolean showHighways() {
        return this.showHighways;
    }

    public boolean showSigns() {
        return this.showSigns;
    }

    public boolean showDeedBorders3d() {
        return this.showDeedBorders3d;
    }

    public boolean showDeedBordersFlat() {
        return this.showDeedBordersFlat;
    }

    public boolean usePlayerSettings() {
        return this.usePlayerSettings;
    }

    public int[] getGuardTowerIDs() {
        return this.guardTowerIDs;
    }

    public String getNeutralLandName() {
        return this.neutralLandName;
    }

    public String getKingdomColor(int kingdom) {
        return kingdomColors!=null && kingdom>=0 && kingdom<kingdomColors.length? kingdomColors[kingdom] : null;
    }
}
