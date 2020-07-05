package net.spirangle.mapviewer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
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
    private int serverId;
    private Path mapDirectory;
    private Path originalMapDirectory;
    private Path outputDirectory;
    private OutputFormat outputFormat;
    private boolean showDeeds;
    private boolean showDeedBorders3d;
    private boolean showDeedBordersFlat;

    private Config() {

    }

    public void load(final Path path,String suffix) throws IOException {
        if(!Files.exists(path)) return;
        try(InputStream stream = Files.newInputStream(path)) {
            Properties properties = new Properties();
            properties.load(stream);
            serverName = getProperty(properties,"server-name",suffix);
            logger.info("Server Name: "+serverName);
            serverId = Integer.parseInt(getProperty(properties,"server-id",suffix));
            logger.info("Server ID: "+serverId);
            mapDirectory = Paths.get(getProperty(properties,"map-directory",suffix),new String[0]);
            logger.info("Map directory: "+mapDirectory);
            originalMapDirectory = Paths.get(getProperty(properties,"original-map-directory",suffix),new String[0]);
            logger.info("Map directory (original): "+originalMapDirectory);
            outputDirectory = Paths.get(getProperty(properties,"output-directory",suffix),new String[0]);
            logger.info("Output directory: "+outputDirectory);
            String of = getProperty(properties,"output-format",suffix);
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
            showDeeds = Boolean.parseBoolean(getProperty(properties,"show-deeds",suffix));
            logger.info("Show deeds: "+showDeeds);
            showDeedBorders3d = Boolean.parseBoolean(getProperty(properties,"show-deed-borders-in-3d-mode",suffix));
            logger.info("Show deed borders (3D): "+showDeedBorders3d);
            showDeedBordersFlat = Boolean.parseBoolean(getProperty(properties,"show-deed-borders-in-flat-mode",suffix));
            logger.info("Show deed borders (flat): "+showDeedBordersFlat);
        }
    }

    private String getProperty(Properties properties,String key,String suffix) {
        String value = properties.getProperty(key+suffix);
        if((value==null || value.isEmpty()) && suffix!=null && !suffix.isEmpty())
            value = properties.getProperty(key);
        return value;
    }

    public String getServerName() {
        return this.serverName;
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

    public boolean showDeedBorders3d() {
        return this.showDeedBorders3d;
    }

    public boolean showDeedBordersFlat() {
        return this.showDeedBordersFlat;
    }
}
