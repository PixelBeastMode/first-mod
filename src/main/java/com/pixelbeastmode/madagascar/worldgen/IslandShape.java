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

		int row = clamp((int) pz, 0, height - 1);
		int west = rowWest[row];
		int east = rowEast[row];

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

	/** Bilinear sample so the coastline is smooth rather than 14-block stair steps. */
	private float sample(float px, float pz) {
		int x0 = (int) Math.floor(px);
		int z0 = (int) Math.floor(pz);
		float fx = px - x0;
		float fz = pz - z0;

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

	private static int clamp(int value, int min, int max) {
		return value < min ? min : Math.min(value, max);
	}
}
