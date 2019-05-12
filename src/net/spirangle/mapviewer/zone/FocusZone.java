package net.spirangle.mapviewer.zone;

public final class FocusZone {
	private final String name;
	private final int sx;
	private final int sy;
	private final int ex;
	private final int ey;
	private final int type;
	private int height;

	public FocusZone(final String name,final int sx,final int sy,final int ex,final int ey,final int type) {
		this.name    = name;
		this.sx      = sx;
		this.sy      = sy;
		this.ex      = ex;
		this.ey      = ey;
		this.type    = type;
		this.height  = 0;
	}

	public String getName() {
		return this.name;
	}

	public int getSx() {
		return this.sx;
	}

	public int getSy() {
		return this.sy;
	}

	public int getEx() {
		return this.ex;
	}

	public int getEy() {
		return this.ey;
	}

	public int getX() {
		return (this.sx+this.ex)/2;
	}

	public int getY() {
		return (this.sy+this.ey)/2;
	}

	public int getType() {
		return this.type;
	}

	public int getHeight() {
		return this.height;
	}

	public void setHeight(final int height) {
		this.height = height;
	}

	public boolean contains(final int x, final int y) {
		return x>=sx && y>=sy && x<=ex && y<=ey;
	}
}
