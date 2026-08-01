package com.pixelbeastmode.madagascar.worldgen;

import com.pixelbeastmode.madagascar.MadagascarMod;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * The shape of Madagascar, loaded from a greyscale outline image.
 * <p>
 * White is land, black is sea. The image is a plain PNG in the mod jar, so you
 * can redraw the coastline in any paint program without touching Java - just
 * keep it greyscale and keep the same dimensions.
 * <p>
 * The image is traced from real longitude/latitude, and its aspect ratio matches
 * the real bounding box (810 km x 1580 km), so the island is not stretched.
 */
public final class IslandShape {

	/** World blocks per mask pixel. Raising this makes the whole island bigger. */
	public static final int BLOCKS_PER_PIXEL = 14;

	private static final String MASK_PATH = "/madagascar/island_mask.png";

	/** Below this the pixel counts as sea. */
	private static final float SEA_LEVEL = 0.5f;

	/** Between SEA_LEVEL and this, we are on the coastal fringe. */
	private static final float BEACH_EDGE = 0.62f;

	/**
	 * Where the central plateau's axis sits across the island, 0 being the west
	 * coast and 1 the east. Madagascar's is east of centre, which is why the
	 * eastern escarpment is steep and the western plains are broad.
	 */
	private static final float SPINE_ACROSS = 0.66f;

	private static final IslandShape INSTANCE = load();

	private final int width;
	private final int height;

	/** Land amount per pixel, 0 (sea) to 1 (land). */
	private final float[] land;

	/** For each north-south row, the west and east edge of the island. -1 when the row is all sea. */
	private final int[] rowWest;
	private final int[] rowEast;

	/** Which part of Madagascar a position belongs to. */
	public enum Region {
		OCEAN,
		BEACH,
		RAINFOREST,
		HIGHLANDS,
		DRY_WEST,
		SPINY_SOUTH,
		TSINGY,
		ISALO
	}

	public static IslandShape get() {
		return INSTANCE;
	}

	private IslandShape(int width, int height, float[] land) {
		this.width = width;
		this.height = height;
		this.land = land;
		this.rowWest = new int[height];
		this.rowEast = new int[height];

		// Precompute how far the island reaches east and west on each row.
		// Madagascar is curved, so a fixed "east half of the map" test would put
		// the rainforest in the sea near the south. Measuring per row keeps the
		// east coast on the actual east coast all the way down.
		for (int y = 0; y < height; y++) {
			int west = -1;
			int east = -1;
			for (int x = 0; x < width; x++) {
				if (land[y * width + x] >= SEA_LEVEL) {
					if (west < 0) {
						west = x;
					}
					east = x;
				}
			}
			rowWest[y] = west;
			rowEast[y] = east;
		}

		// Rows off the ends of the island have no land at all. Carry the nearest
		// real extent into them so interpolation never meets a -1.
		for (int y = 1; y < height; y++) {
			if (rowWest[y] < 0) {
				rowWest[y] = rowWest[y - 1];
				rowEast[y] = rowEast[y - 1];
			}
		}
		for (int y = height - 2; y >= 0; y--) {
			if (rowWest[y] < 0) {
				rowWest[y] = rowWest[y + 1];
				rowEast[y] = rowEast[y + 1];
			}
		}
	}

	/**
	 * The island's west and east edge at a fractional row.
	 * <p>
	 * These interpolate between rows on purpose. Reading {@code rowWest[(int) pz]}
	 * directly quantises the edges to whole mask rows, which is 14 blocks, so the
	 * across-island position jumps every 14 blocks and the terrain grows hard
	 * terraces and straight creases running across the hillsides.
	 */
	private float westAt(float pz) {
		return interpolateRow(rowWest, pz);
	}

	private float eastAt(float pz) {
		return interpolateRow(rowEast, pz);
	}

	private float interpolateRow(int[] rows, float pz) {
		float clamped = Math.max(0.0f, Math.min(pz, height - 1.0f));
		int row = (int) clamped;
		int next = Math.min(row + 1, height - 1);
		// Eased for the same reason as sample(): a plain lerp is continuous but
		// its slope still jumps at each row, and that slope drives the terrain.
		return lerp(rows[row], rows[next], smoothstep(clamped - row));
	}

	private static IslandShape load() {
		try (InputStream in = IslandShape.class.getResourceAsStream(MASK_PATH)) {
			if (in == null) {
				throw new IllegalStateException("Island mask not found on the classpath at " + MASK_PATH);
			}

			BufferedImage image = ImageIO.read(in);
			if (image == null) {
				throw new IllegalStateException("Island mask at " + MASK_PATH + " is not a readable image");
			}

			int width = image.getWidth();
			int height = image.getHeight();
			float[] land = new float[width * height];

			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					// Greyscale, so the red channel is enough.
					land[y * width + x] = ((image.getRGB(x, y) >> 16) & 0xFF) / 255.0f;
				}
			}

			MadagascarMod.LOGGER.info("Loaded island mask: {}x{} px, {} blocks across",
				width, height, width * BLOCKS_PER_PIXEL);
			return new IslandShape(width, height, land);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read the island mask", e);
		}
	}

	/** Total east-west size of the island's bounding box, in blocks. */
	public int worldWidth() {
		return width * BLOCKS_PER_PIXEL;
	}

	/** Total north-south size of the island's bounding box, in blocks. */
	public int worldLength() {
		return height * BLOCKS_PER_PIXEL;
	}

	/**
	 * How much land is at this block position: 0 is open sea, 1 is solidly inland.
	 * The island is centred on the world origin.
	 */
	public float landAt(int blockX, int blockZ) {
		return sample(pixelX(blockX), pixelZ(blockZ));
	}

	public Region regionAt(int blockX, int blockZ) {
		float px = pixelX(blockX);
		float pz = pixelZ(blockZ);

		// Wobble the coastline so it gains bays and headlands instead of following
		// the traced polygon exactly. Roughly 17-block features.
		float coastX = px + noise(px * 0.15f, pz * 0.15f, 3) * 1.2f;
		float coastZ = pz + noise(px * 0.15f, pz * 0.15f, 4) * 1.2f;

		float amount = sample(coastX, coastZ);
		if (amount < SEA_LEVEL) {
			return Region.OCEAN;
		}
		if (amount < BEACH_EDGE) {
			return Region.BEACH;
		}

		float west = westAt(pz);
		float east = eastAt(pz);

		// 0 at the west coast, 1 at the east coast, on this row.
		float across = east > west ? (px - west) / (east - west) : 0.5f;
		// 0 at the northern tip, 1 at the southern tip.
		float down = pz / height;

		// Without this every region boundary is a straight line across the island.
		across += noise(px * 0.08f, pz * 0.08f, 1) * 0.05f;
		down += noise(px * 0.08f, pz * 0.08f, 2) * 0.025f;

		// Order matters: the specific landmarks win over the broad climate bands.

		// Spiny forest of Androy, south of about 23.5 S.
		if (down > 0.83f) {
			return Region.SPINY_SOUTH;
		}
		// Isalo massif, about 22.5 S, inland of the south-west coast.
		if (inBlob(across, down, 0.42f, 0.76f, 0.16f, 0.05f)) {
			return Region.ISALO;
		}
		// Tsingy de Bemaraha, about 18.7 S, hard against the west coast.
		if (inBlob(across, down, 0.18f, 0.49f, 0.15f, 0.055f)) {
			return Region.TSINGY;
		}
		if (across > 0.72f) {
			return Region.RAINFOREST;
		}
		if (across < 0.32f) {
			return Region.DRY_WEST;
		}
		return Region.HIGHLANDS;
	}

	/**
	 * Large-scale elevation, 0 at the coastal plains up to 1 on the highest
	 * massifs. This is what makes the island mountainous rather than a flat
	 * green table - hill noise alone only adds small bumps to one base height.
	 * <p>
	 * Madagascar's plateau runs the length of the island but sits east of
	 * centre. It falls away sharply to the east coast, which is why that coast
	 * is a narrow strip, and slopes gently west into the plains.
	 */
	public float spineElevation(int blockX, int blockZ) {
		float px = pixelX(blockX);
		float pz = pixelZ(blockZ);

		float west = westAt(pz);
		float east = eastAt(pz);
		if (east <= west) {
			return 0.0f;
		}

		// Clamped because a point can read as land while sitting just outside the
		// interpolated row extents. A negative value here would reach Math.pow
		// below and return NaN, which the chunk generator turns into missing
		// terrain rather than an error.
		float across = clamp01((px - west) / (east - west));
		float down = pz / height;

		// The two sides of the plateau behave very differently.
		float profile;
		if (across <= SPINE_ACROSS) {
			// West: broad low plains that only climb near the plateau. The power
			// curve is what keeps the western third genuinely lowland.
			profile = (float) Math.pow(across / SPINE_ACROSS, 1.8);
		} else {
			// East: the escarpment. High right up to a narrow strip, then a plunge
			// to the coast - which is why the eastern coastal plain is so thin.
			profile = smoothstep(clamp01((1.0f - across) / 0.20f));
		}

		// The plateau tapers away towards both tips of the island.
		float tips = clamp01(Math.min(down / 0.12f, (1.0f - down) / 0.18f));
		profile *= smoothstep(tips);

		// Three real massifs standing above the plateau.
		float massif = blob(across, down, 0.60f, 0.12f, 0.20f, 0.06f);      // Tsaratanana, north
		massif = Math.max(massif, blob(across, down, 0.62f, 0.50f, 0.18f, 0.07f)); // Ankaratra, centre
		massif = Math.max(massif, blob(across, down, 0.66f, 0.72f, 0.16f, 0.05f)); // Andringitra, south

		// The plateau is deliberately well below the massifs, so the peaks read as
		// mountains rather than as bumps on an already-high table.
		return clamp01(profile * 0.55f + massif * 0.55f);
	}

	/** 1 at the centre of an elliptical patch, easing to 0 at its edge. */
	private static float blob(float across, float down, float centreAcross, float centreDown,
			float radiusAcross, float radiusDown) {
		float dx = (across - centreAcross) / radiusAcross;
		float dz = (down - centreDown) / radiusDown;
		float distSq = dx * dx + dz * dz;
		if (distSq >= 1.0f) {
			return 0.0f;
		}
		return smoothstep(1.0f - (float) Math.sqrt(distSq));
	}

	/**
	 * Rolling hill detail for the terrain, in the range -1..1.
	 * Three octaves, so hills have both broad shape and smaller bumps.
	 */
	public float terrainNoise(int blockX, int blockZ) {
		float x = blockX * 0.006f;
		float z = blockZ * 0.006f;
		float total = noise(x, z, 11);
		total += noise(x * 2.3f, z * 2.3f, 12) * 0.5f;
		total += noise(x * 5.1f, z * 5.1f, 13) * 0.25f;
		return total / 1.75f;
	}

	/** Elliptical patch test, so landmarks are blobs rather than rectangles. */
	private static boolean inBlob(float across, float down, float centreAcross, float centreDown,
			float radiusAcross, float radiusDown) {
		float dx = (across - centreAcross) / radiusAcross;
		float dz = (down - centreDown) / radiusDown;
		return dx * dx + dz * dz < 1.0f;
	}

	/** Cheap deterministic value noise in the range -1..1. */
	private static float noise(float x, float z, int seed) {
		int x0 = (int) Math.floor(x);
		int z0 = (int) Math.floor(z);
		float fx = x - x0;
		float fz = z - z0;

		// Smoothstep, so the noise has no sharp creases at cell edges.
		fx = fx * fx * (3.0f - 2.0f * fx);
		fz = fz * fz * (3.0f - 2.0f * fz);

		float top = lerp(hash(x0, z0, seed), hash(x0 + 1, z0, seed), fx);
		float bottom = lerp(hash(x0, z0 + 1, seed), hash(x0 + 1, z0 + 1, seed), fx);
		return lerp(top, bottom, fz);
	}

	private static float lerp(float a, float b, float t) {
		return a + (b - a) * t;
	}

	private static float hash(int x, int z, int seed) {
		int n = x * 374761393 + z * 668265263 + seed * 1274126177;
		n = (n ^ (n >>> 13)) * 1274126177;
		return ((n ^ (n >>> 16)) & 0xFFFF) / 32768.0f - 1.0f;
	}

	private float pixelX(int blockX) {
		return (float) blockX / BLOCKS_PER_PIXEL + width * 0.5f;
	}

	private float pixelZ(int blockZ) {
		return (float) blockZ / BLOCKS_PER_PIXEL + height * 0.5f;
	}

	/**
	 * Samples the mask smoothly.
	 * <p>
	 * The fractions are eased before interpolating. Plain bilinear is continuous
	 * but its slope is not: the gradient jumps at every cell edge, which shows up
	 * in terrain as a crease every 14 blocks. Easing flattens the slope at the
	 * edges so neighbouring cells meet without a seam.
	 */
	private float sample(float px, float pz) {
		int x0 = (int) Math.floor(px);
		int z0 = (int) Math.floor(pz);
		float fx = smoothstep(px - x0);
		float fz = smoothstep(pz - z0);

		float topLeft = at(x0, z0);
		float topRight = at(x0 + 1, z0);
		float bottomLeft = at(x0, z0 + 1);
		float bottomRight = at(x0 + 1, z0 + 1);

		float top = topLeft + (topRight - topLeft) * fx;
		float bottom = bottomLeft + (bottomRight - bottomLeft) * fx;
		return top + (bottom - top) * fz;
	}

	/** Anything outside the image is open ocean. */
	private float at(int x, int z) {
		if (x < 0 || z < 0 || x >= width || z >= height) {
			return 0.0f;
		}
		return land[z * width + x];
	}

	private static float clamp01(float value) {
		if (value < 0.0f) {
			return 0.0f;
		}
		return Math.min(value, 1.0f);
	}

	/** Eases both ends of a 0..1 ramp so slopes meet flat ground smoothly. */
	private static float smoothstep(float t) {
		return t * t * (3.0f - 2.0f * t);
	}
}
