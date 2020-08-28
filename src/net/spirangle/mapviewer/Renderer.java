package net.spirangle.mapviewer;

import com.wurmonline.mesh.Tiles;
import net.spirangle.mapviewer.zone.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.*;
import java.util.logging.Logger;

public final class Renderer {

    private static final Logger logger = Logger.getLogger(Renderer.class.getName());

    private static final int[] colors = new int[256];

    static {
        for(final Tiles.Tile tile : Tiles.Tile.values())
            colors[tile.id&0xFF] = tile.getColor().getRGB();
        colors[Tiles.Tile.TILE_CLAY.id] = 0x505774;
        colors[Tiles.Tile.TILE_FIELD.id] = 0x5f7424;
        colors[Tiles.Tile.TILE_FIELD2.id] = 0x5f7424;
    }

    public enum Type {
        TERRAIN,
        TOPOGRAPHIC,
        ISOMETRIC;
    }

    private final Config config;
    private final List<Deed> deeds;
    private final Map<Tile,BridgePart> bridgeParts;
    private int size;
    private int[][] typeData;
    private int[][] metaData;
    private int[][] heightData;
    private int[][] originalTypeData;
    private int[][] originalMetaData;
    private int[][] originalHeightData;

    public Renderer(final Config config) {
        this.deeds = new ArrayList<>();
        this.bridgeParts = new HashMap<>();
        this.size = -1;
        this.config = config;
    }

    public int load(final ZoneInfo zoneInfo) throws IOException {
        final List<Deed> deeds = zoneInfo.getDeeds();
        final Map<Tile,BridgePart> bridgeParts = zoneInfo.getBridgeParts();
        final List<FocusZone> focusZones = zoneInfo.getFocusZones();
        this.deeds.addAll(deeds);
        this.bridgeParts.putAll(bridgeParts);
        final Path temp = Paths.get("temp",new String[0]);
        if(Files.notExists(temp,new LinkOption[0])) {
            Files.createDirectory(temp,new FileAttribute[0]);
        }
        final Path map = temp.resolve("top_layer.map");
        Files.deleteIfExists(map);
        Files.copy(this.config.getMapDirectory().resolve("top_layer.map"),map,new CopyOption[0]);
        try(final DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(map,new OpenOption[0]),65536))) {
            in.readLong();
            in.readByte();
            this.size = 1<<in.readByte();
            in.skipBytes(1014);
            this.typeData = new int[this.size][this.size];
            this.metaData = new int[this.size][this.size];
            this.heightData = new int[this.size][this.size];
            for(int y = 0; y<this.size; ++y) {
                for(int x = 0; x<this.size; ++x) {
                    final int tileType = in.read()&0xFF;
                    final int meta = in.read()&0xFF;
                    final int height = in.readShort();
                    this.typeData[x][y] = tileType;
                    this.metaData[x][y] = meta;
                    this.heightData[x][y] = height;
                }
            }
            for(final Deed deed : deeds) {
                deed.setHeight(this.heightData[deed.getX()][deed.getY()]);
            }
            for(final FocusZone focusZone : focusZones) {
                focusZone.setHeight(this.heightData[focusZone.getX()][focusZone.getY()]);
            }
        }

        final Path originalMap = temp.resolve("original_top_layer.map");
        Files.deleteIfExists(originalMap);
        Files.copy(this.config.getOriginalMapDirectory().resolve("top_layer.map"),originalMap,new CopyOption[0]);
        try(final DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(originalMap,new OpenOption[0]),65536))) {
            in.readLong();
            in.readByte();
            in.readByte();
            in.skipBytes(1014);
            this.originalTypeData = new int[this.size][this.size];
            this.originalMetaData = new int[this.size][this.size];
            this.originalHeightData = new int[this.size][this.size];
            for(int y = 0; y<this.size; ++y) {
                for(int x = 0; x<this.size; ++x) {
                    final int tileType = in.read()&0xFF;
                    final int meta = in.read()&0xFF;
                    final int height = in.readShort();
                    this.originalTypeData[x][y] = tileType;
                    this.originalMetaData[x][y] = meta;
                    this.originalHeightData[x][y] = height;
                }
            }
        }
        return this.size;
    }

    public BufferedImage render(final Type type) {
        final BufferedImage img = new BufferedImage(this.size,this.size,1);
        final int[] pixels = ((DataBufferInt)img.getRaster().getDataBuffer()).getData();
        Arrays.fill(pixels,3686001);
        for(int x = 0; x<this.size-1; ++x) {
            for(int y = 0; y<this.size-1; ++y) {
                renderTile(pixels,type,x,y,this.typeData,this.metaData,this.heightData);
            }
        }
        for(Deed deed : this.deeds) {
//            Renderer.LOGGER.log(Level.INFO,"Deed "+deed.getName()+", visibility: "+deed.getVisibility());
            if(deed.getVisibility() >= 3) {
                for(int x = deed.getSx()-5; x<=deed.getEx()+5; ++x) {
                    for(int y = deed.getSy()-5; y<=deed.getEy()+5; ++y) {
                        renderTile(pixels,type,x,y,this.originalTypeData,this.originalMetaData,this.originalHeightData);
                    }
                }
            }
        }
        return img;
    }

    private void renderTile(final int[] pixels,final Type type,final int x,final int y,final int[][] typeData,final int[][] metaData,final int[][] heightData) {
        int colour = Renderer.colors[typeData[x][y]];
        if(type.equals(Type.ISOMETRIC)) {
            float r = (float)(colour >>> 16&0xFF);
            float g = (float)(colour >>> 8&0xFF);
            float b = (float)(colour&0xFF);
            final float tileHeight = heightData[x][y]/40.0f;
            final float tileHeightN = heightData[x+1][y+1]/40.0f;
            final float tileHeightB = heightData[x][y+1]/40.0f;
            final float delta = tileHeightN-tileHeight;
            final float factor = 1.0f+(float)Math.tanh(delta/4.0f);
            r *= factor;
            g *= factor;
            b *= factor;
            if(tileHeight<0.0f) {
                r = r/5.0f+41.0f;
                g = r/5.0f+51.0f;
                b = r/5.0f+102.0f;
            }
            if(r<0.0f) r = 0.0f;
            if(r>255.0f) r = 255.0f;
            if(g<0.0f) g = 0.0f;
            if(g>255.0f) g = 255.0f;
            if(b<0.0f) b = 0.0f;
            if(b>255.0f) b = 255.0f;
            int start = y-(int)tileHeight;
            if(tileHeightB-1.0f>tileHeight) {
                start = y-(int)tileHeightB;
            }
            int threshold = Integer.MAX_VALUE;
            if(tileHeight >= 0.0f && tileHeightB<0.0f) {
                threshold = y+1;
            }
            for(int qy = start; qy<=start+Math.abs(delta)+9.0f; ++qy) {
                if(qy >= 0 && qy<this.size) {
                    if(qy==threshold) {
                        r = r/5.0f+41.0f;
                        g = r/5.0f+51.0f;
                        b = r/5.0f+102.0f;
                    }
                    pixels[qy*this.size+x] = ((int)r<<16|(int)g<<8|(int)b);
                }
            }
        } else {
            if(heightData[x][y]<0) {
                int r = (colour >>> 16&0xFF), g, b;
                r = r/5+41;
                g = r/5+51;
                b = r/5+102;
                colour = (r<<16|g<<8|b);
            }
            if(type.equals(Type.TOPOGRAPHIC) && x>0 && y>0 && x<this.size-1 && y<this.size-1) {
                int l = 100;
                int h = heightData[x][y];
                int h1 = heightData[x][y-1];
                int h2 = heightData[x][y+1];
                int h3 = heightData[x-1][y];
                int h4 = heightData[x+1][y];
                h = h<0? h/l-1 : h/l;
                h1 = h1<0? h1/l-1 : h1/l;
                h2 = h2<0? h2/l-1 : h2/l;
                h3 = h3<0? h3/l-1 : h3/l;
                h4 = h4<0? h4/l-1 : h4/l;
                if((h>h1 && h<=h2) || (h>h2 && h<=h1) || (h>h3 && h<=h4) || (h>h4 && h<=h3)) {
                    int r = (colour >>> 16&0xFF)/2;
                    int g = (colour >>> 8&0xFF)/2;
                    int b = (colour&0xFF)/2;
                    colour = (r<<16|g<<8|b);
                }
            }
            pixels[y*this.size+x] = colour;
        }
        final Tile tile = new Tile(x,y);
        final BridgePart part = this.bridgeParts.get(tile);
        if(part!=null) {
            if(type.equals(Type.ISOMETRIC)) {
                int height = part.getHeightOffset();
                if(part.getSlope()<0) {
                    height += part.getSlope();
                }
                final int h = y-height/40;
                final int ha = y-part.getHeightOffset()/40;
                final int hb = y-(part.getHeightOffset()+part.getSlope())/40;
                if(h >= 0 && h<this.size-1) {
                    if(ha!=hb) {
                        pixels[(h-1)*this.size+x] = part.getColour();
                    }
                    pixels[h*this.size+x] = part.getColour();
                    pixels[(h+1)*this.size+x] = part.getShadowColour();
                }
            } else {
                pixels[y*this.size+x] = part.getColour();
            }
        }
    }

    public void save(final Path path,final Type type) throws IOException {
        final BufferedImage img = this.render(type);
        try(final OutputStream out = Files.newOutputStream(path,new OpenOption[0])) {
            ImageIO.write(img,"PNG",out);
        }
    }

    public boolean isOnSurface(final int xTile,final int yTile,final int z) {
        logger.info("isOnSurface("+heightData[xTile][yTile]+" <= "+z+")");
        return heightData[xTile][yTile]<=z || heightData[xTile+1][yTile]<=z ||
               heightData[xTile][yTile+1]<=z || heightData[xTile+1][yTile+1]<=z;
    }
}
