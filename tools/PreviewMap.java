import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Renders the region layout so we can eyeball it without launching Minecraft.
 * The geometry below is copied verbatim from IslandShape - if that changes,
 * this must be re-copied or the preview lies.
 */
public class PreviewMap {

	static final int BLOCKS_PER_PIXEL = 14;
	static final float SEA_LEVEL = 0.5f;
	static final float BEACH_EDGE = 0.62f;

	static int width, height;
	static float[] land;
	static int[] rowWest, rowEast;

	enum Region { OCEAN, BEACH, RAINFOREST, HIGHLANDS, DRY_WEST, SPINY_SOUTH, TSINGY, ISALO }

	public static void main(String[] args) throws Exception {
		BufferedImage mask = ImageIO.read(new File(args[0]));
		width = mask.getWidth();
		height = mask.getHeight();
		land = new float[width * height];
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++)
				land[y * width + x] = ((mask.getRGB(x, y) >> 16) & 0xFF) / 255.0f;

		rowWest = new int[height];
		rowEast = new int[height];
		for (int y = 0; y < height; y++) {
			int west = -1, east = -1;
			for (int x = 0; x < width; x++) {
				if (land[y * width + x] >= SEA_LEVEL) {
					if (west < 0) west = x;
					east = x;
				}
			}
			rowWest[y] = west;
			rowEast[y] = east;
		}
		for (int y = 1; y < height; y++)
			if (rowWest[y] < 0) { rowWest[y] = rowWest[y - 1]; rowEast[y] = rowEast[y - 1]; }
		for (int y = height - 2; y >= 0; y--)
			if (rowWest[y] < 0) { rowWest[y] = rowWest[y + 1]; rowEast[y] = rowEast[y + 1]; }

		// Render at 3x so regions are legible.
		int scale = 3;
		BufferedImage out = new BufferedImage(width * scale, height * scale, BufferedImage.TYPE_INT_RGB);
		int[] counts = new int[Region.values().length];

		for (int py = 0; py < height * scale; py++) {
			for (int px = 0; px < width * scale; px++) {
				int blockX = Math.round(((float) px / scale - width * 0.5f) * BLOCKS_PER_PIXEL);
				int blockZ = Math.round(((float) py / scale - height * 0.5f) * BLOCKS_PER_PIXEL);
				Region r = regionAt(blockX, blockZ);
				counts[r.ordinal()]++;
				out.setRGB(px, py, colour(r));
			}
		}

		ImageIO.write(out, "png", new File(args[1]));

		// Elevation map, mirroring MadagascarTerrain.compute().
		BufferedImage elev = new BufferedImage(width * scale, height * scale, BufferedImage.TYPE_INT_RGB);
		double maxY = 0, minLandY = 999;
		long landSamples = 0;
		double sumY = 0;
		for (int py = 0; py < height * scale; py++) {
			for (int px = 0; px < width * scale; px++) {
				int bx = Math.round(((float) px / scale - width * 0.5f) * BLOCKS_PER_PIXEL);
				int bz = Math.round(((float) py / scale - height * 0.5f) * BLOCKS_PER_PIXEL);
				double y = surfaceY(bx, bz);
				if (y > maxY) maxY = y;
				if (y > 63) { landSamples++; sumY += y; if (y < minLandY) minLandY = y; }
				elev.setRGB(px, py, shade(y));
			}
		}
		ImageIO.write(elev, "png", new File(args[2]));
		System.out.printf("%nelevation: peak y=%.0f, mean land y=%.0f, lowest land y=%.0f%n",
			maxY, sumY / Math.max(1, landSamples), minLandY);
		for (int[] p : new int[][] {{0,0},{350,0},{-400,0},{0,-1200},{0,1500},{600,0}})
			System.out.printf("  surface at (%6d,%6d) -> y=%.0f%n", p[0], p[1], surfaceY(p[0], p[1]));

		// Creases from row quantisation show up as a curvature spike every
		// BLOCKS_PER_PIXEL blocks. Bucket curvature by z mod 14: if one offset
		// dominates, the grid is still showing through.
		// Where to actually fly to, per region: the centroid of each region's blocks.
		System.out.println("\ngo here for each region:");
		long[] sx = new long[Region.values().length], sz = new long[Region.values().length];
		long[] cn = new long[Region.values().length];
		for (int bz = -1900; bz <= 1900; bz += 8)
			for (int bx = -980; bx <= 980; bx += 8) {
				Region r = regionAt(bx, bz);
				int k = r.ordinal();
				sx[k] += bx; sz[k] += bz; cn[k]++;
			}
		for (Region r : Region.values()) {
			int k = r.ordinal();
			if (cn[k] == 0) { System.out.printf("  %-12s (none)%n", r); continue; }
			int cx = (int) (sx[k] / cn[k]), cz = (int) (sz[k] / cn[k]);
			// Nudge to a spot actually inside the region, since a centroid can miss.
			int fx = cx, fz = cz;
			outer:
			for (int rad = 0; rad <= 900; rad += 16)
				for (int dz = -rad; dz <= rad; dz += 16)
					for (int dx = -rad; dx <= rad; dx += 16)
						if (regionAt(cx + dx, cz + dz) == r) { fx = cx + dx; fz = cz + dz; break outer; }
			System.out.printf("  %-12s /tp %d ~ %d   (surface y=%.0f)%n", r, fx, fz, surfaceY(fx, fz));
		}

		System.out.println("\ncrease check (curvature bucketed by z mod " + BLOCKS_PER_PIXEL + "):");
		for (int x : new int[] {-300, 0, 300}) {
			double[] bucket = new double[BLOCKS_PER_PIXEL];
			int[] n = new int[BLOCKS_PER_PIXEL];
			double worst = 0;
			for (int z = -1400; z <= 1400; z++) {
				double a = surfaceY(x, z - 1), b = surfaceY(x, z), c = surfaceY(x, z + 1);
				double curv = Math.abs(a - 2 * b + c);
				int k = Math.floorMod(z, BLOCKS_PER_PIXEL);
				bucket[k] += curv; n[k]++;
				if (curv > worst) worst = curv;
			}
			double mean = 0, peak = 0;
			for (int k = 0; k < BLOCKS_PER_PIXEL; k++) { bucket[k] /= n[k]; mean += bucket[k] / BLOCKS_PER_PIXEL; peak = Math.max(peak, bucket[k]); }
			System.out.printf("  x=%5d  worst single curvature %.3f, peak/mean bucket ratio %.2f %s%n",
				x, worst, peak / mean, peak / mean > 1.5 ? "<-- GRID VISIBLE" : "(no grid bias)");
		}

		int total = width * scale * height * scale;
		System.out.println("region coverage:");
		for (Region r : Region.values())
			System.out.printf("  %-12s %6.2f%%%n", r, 100.0 * counts[r.ordinal()] / total);
		System.out.printf("island spans %d x %d blocks%n", width * BLOCKS_PER_PIXEL, height * BLOCKS_PER_PIXEL);

		// Spot-check a few real places (approximate positions on the mask).
		check("north tip",        0, -1800);
		check("east coast mid",  350,     0);
		check("centre",            0,     0);
		check("west coast mid", -400,     0);
		check("deep south",        0,  1500);
		check("far out to sea", 4000,     0);
	}

	static void check(String label, int x, int z) {
		System.out.printf("  %-16s (%6d,%6d) -> %s%n", label, x, z, regionAt(x, z));
	}

	static int colour(Region r) {
		return switch (r) {
			case OCEAN       -> 0x1B4B7A;
			case BEACH       -> 0xE8DCA0;
			case RAINFOREST  -> 0x1E6B2E;
			case HIGHLANDS   -> 0xB5563A;
			case DRY_WEST    -> 0xC8A85A;
			case SPINY_SOUTH -> 0x8A6BA0;
			case TSINGY      -> 0xD0D0D8;
			case ISALO       -> 0xE07A3C;
		};
	}

	static Region regionAt(int blockX, int blockZ) {
		float px = (float) blockX / BLOCKS_PER_PIXEL + width * 0.5f;
		float pz = (float) blockZ / BLOCKS_PER_PIXEL + height * 0.5f;

		float coastX = px + noise(px * 0.15f, pz * 0.15f, 3) * 1.2f;
		float coastZ = pz + noise(px * 0.15f, pz * 0.15f, 4) * 1.2f;

		float amount = sample(coastX, coastZ);
		if (amount < SEA_LEVEL) return Region.OCEAN;
		if (amount < BEACH_EDGE) return Region.BEACH;

		float west = westAt(pz), east = eastAt(pz);
		float across = east > west ? (px - west) / (east - west) : 0.5f;
		float down = pz / height;

		across += noise(px * 0.08f, pz * 0.08f, 1) * 0.05f;
		down += noise(px * 0.08f, pz * 0.08f, 2) * 0.025f;

		if (down > 0.83f) return Region.SPINY_SOUTH;
		if (inBlob(across, down, 0.42f, 0.76f, 0.16f, 0.05f)) return Region.ISALO;
		if (inBlob(across, down, 0.18f, 0.49f, 0.15f, 0.055f)) return Region.TSINGY;
		if (across > 0.72f) return Region.RAINFOREST;
		if (across < 0.32f) return Region.DRY_WEST;
		return Region.HIGHLANDS;
	}

	// --- mirrors MadagascarTerrain ---
	static final double DEEP_SEA = -0.62, SHORELINE = -0.344, INLAND = -0.16;
	static final double HILL_HEIGHT = 0.18, MOUNTAIN_HEIGHT = 0.85;
	static final float SEA = 0.5f, INLAND_RAMP = 0.35f;

	static double surfaceY(int bx, int bz) {
		float px = (float) bx / BLOCKS_PER_PIXEL + width * 0.5f;
		float pz = (float) bz / BLOCKS_PER_PIXEL + height * 0.5f;
		float land = sample(px, pz);
		double offset;
		if (land < SEA) {
			offset = lerpD(DEEP_SEA, SHORELINE, smoothD(land / SEA));
		} else {
			double t = smoothD(Math.min((land - SEA) / INLAND_RAMP, 1.0));
			offset = lerpD(SHORELINE, INLAND, t)
				+ spineElevation(bx, bz) * MOUNTAIN_HEIGHT * t
				+ terrainNoise(bx, bz) * HILL_HEIGHT * t;
		}
		return 96.0 * (1.0 + offset);
	}

	static final float SPINE_ACROSS = 0.66f;

	static float spineElevation(int bx, int bz) {
		float px = (float) bx / BLOCKS_PER_PIXEL + width * 0.5f;
		float pz = (float) bz / BLOCKS_PER_PIXEL + height * 0.5f;
		float west = westAt(pz), east = eastAt(pz);
		if (east <= west) return 0f;
		float across = c01((px - west) / (east - west));
		float down = pz / height;
		float profile;
		if (across <= SPINE_ACROSS) profile = (float) Math.pow(across / SPINE_ACROSS, 1.8);
		else profile = smooth(c01((1f - across) / 0.20f));
		float tips = c01(Math.min(down / 0.12f, (1f - down) / 0.18f));
		profile *= smooth(tips);
		float massif = blobS(across, down, 0.60f, 0.12f, 0.20f, 0.06f);
		massif = Math.max(massif, blobS(across, down, 0.62f, 0.50f, 0.18f, 0.07f));
		massif = Math.max(massif, blobS(across, down, 0.66f, 0.72f, 0.16f, 0.05f));
		return c01(profile * 0.55f + massif * 0.55f);
	}

	static float blobS(float a, float d, float ca, float cd, float ra, float rd) {
		float dx = (a - ca) / ra, dz = (d - cd) / rd;
		float r2 = dx * dx + dz * dz;
		return r2 >= 1f ? 0f : smooth(1f - (float) Math.sqrt(r2));
	}

	static float terrainNoise(int bx, int bz) {
		float x = bx * 0.006f, z = bz * 0.006f;
		float t = noise(x, z, 11) + noise(x * 2.3f, z * 2.3f, 12) * 0.5f + noise(x * 5.1f, z * 5.1f, 13) * 0.25f;
		return t / 1.75f;
	}

	static float c01(float v) { return v < 0f ? 0f : Math.min(v, 1f); }
	static float smooth(float t) { return t * t * (3f - 2f * t); }
	static double smoothD(double t) { return t * t * (3.0 - 2.0 * t); }
	static double lerpD(double a, double b, double t) { return a + (b - a) * t; }

	static int shade(double y) {
		if (y <= 63) {
			double d = Math.max(0, Math.min(1, (63 - y) / 30.0));
			int b = (int) (140 - 90 * d);
			return (20 << 16) | ((int) (70 - 30 * d) << 8) | b;
		}
		double t = Math.min(1.0, (y - 63) / 120.0);
		if (t < 0.35) { double u = t / 0.35; return (int) (90 + 60 * u) << 16 | (int) (150 - 20 * u) << 8 | 70; }
		if (t < 0.75) { double u = (t - 0.35) / 0.40; return (int) (150 + 40 * u) << 16 | (int) (130 - 40 * u) << 8 | (int) (70 + 10 * u); }
		double u = (t - 0.75) / 0.25;
		return (int) (190 + 60 * u) << 16 | (int) (90 + 150 * u) << 8 | (int) (80 + 160 * u);
	}

	static boolean inBlob(float across, float down, float ca, float cd, float ra, float rd) {
		float dx = (across - ca) / ra, dz = (down - cd) / rd;
		return dx * dx + dz * dz < 1.0f;
	}

	static float noise(float x, float z, int seed) {
		int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
		float fx = x - x0, fz = z - z0;
		fx = fx * fx * (3.0f - 2.0f * fx);
		fz = fz * fz * (3.0f - 2.0f * fz);
		float top = lerp(hash(x0, z0, seed), hash(x0 + 1, z0, seed), fx);
		float bottom = lerp(hash(x0, z0 + 1, seed), hash(x0 + 1, z0 + 1, seed), fx);
		return lerp(top, bottom, fz);
	}

	static float lerp(float a, float b, float t) { return a + (b - a) * t; }

	static float hash(int x, int z, int seed) {
		int n = x * 374761393 + z * 668265263 + seed * 1274126177;
		n = (n ^ (n >>> 13)) * 1274126177;
		return ((n ^ (n >>> 16)) & 0xFFFF) / 32768.0f - 1.0f;
	}

	static float westAt(float pz) { return interpRow(rowWest, pz); }
	static float eastAt(float pz) { return interpRow(rowEast, pz); }

	static float interpRow(int[] rows, float pz) {
		float c = Math.max(0f, Math.min(pz, height - 1f));
		int r = (int) c, n = Math.min(r + 1, height - 1);
		return rows[r] + (rows[n] - rows[r]) * smooth(c - r);
	}

	static float sample(float px, float pz) {
		int x0 = (int) Math.floor(px), z0 = (int) Math.floor(pz);
		float fx = smooth(px - x0), fz = smooth(pz - z0);
		float tl = at(x0, z0), tr = at(x0 + 1, z0), bl = at(x0, z0 + 1), br = at(x0 + 1, z0 + 1);
		float top = tl + (tr - tl) * fx, bottom = bl + (br - bl) * fx;
		return top + (bottom - top) * fz;
	}

	static float at(int x, int z) {
		if (x < 0 || z < 0 || x >= width || z >= height) return 0.0f;
		return land[z * width + x];
	}
}
