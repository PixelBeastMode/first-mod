import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.Inflater;

/**
 * Counts block names inside a Minecraft region file. Block palettes store names
 * as plain UTF-8 inside the NBT, so once a chunk is decompressed we can just
 * look for the strings without a full NBT parser.
 */
public class ScanRegion {
	public static void main(String[] args) throws Exception {
		String[] needles = Arrays.copyOfRange(args, 1, args.length);
		Map<String, Integer> counts = new LinkedHashMap<>();
		for (String n : needles) counts.put(n, 0);

		int chunksScanned = 0;
		for (Path p : listRegions(args[0])) {
			byte[] file = Files.readAllBytes(p);
			if (file.length < 8192) continue;
			for (int i = 0; i < 1024; i++) {
				int off = ((file[i * 4] & 0xFF) << 16) | ((file[i * 4 + 1] & 0xFF) << 8) | (file[i * 4 + 2] & 0xFF);
				int sectors = file[i * 4 + 3] & 0xFF;
				if (off == 0 || sectors == 0) continue;
				int start = off * 4096;
				if (start + 5 > file.length) continue;
				int len = ((file[start] & 0xFF) << 24) | ((file[start + 1] & 0xFF) << 16)
					| ((file[start + 2] & 0xFF) << 8) | (file[start + 3] & 0xFF);
				int compression = file[start + 4] & 0xFF;
				if (compression != 2 || len <= 1 || start + 5 + len - 1 > file.length) continue;

				byte[] raw = inflate(file, start + 5, len - 1);
				if (raw == null) continue;
				chunksScanned++;
				String text = new String(raw, "ISO-8859-1");
				for (String n : needles)
					if (text.contains(n)) counts.merge(n, 1, Integer::sum);
			}
		}

		System.out.println("chunks scanned: " + chunksScanned);
		for (Map.Entry<String, Integer> e : counts.entrySet())
			System.out.printf("  %-34s present in %4d chunks (%.1f%%)%n",
				e.getKey(), e.getValue(), 100.0 * e.getValue() / Math.max(1, chunksScanned));
	}

	static List<Path> listRegions(String dir) throws IOException {
		try (var s = Files.list(Paths.get(dir))) {
			return s.filter(p -> p.toString().endsWith(".mca")).sorted().toList();
		}
	}

	static byte[] inflate(byte[] data, int off, int len) {
		Inflater inf = new Inflater();
		inf.setInput(data, off, len);
		ByteArrayOutputStream out = new ByteArrayOutputStream(1 << 16);
		byte[] buf = new byte[1 << 16];
		try {
			while (!inf.finished()) {
				int n = inf.inflate(buf);
				if (n == 0) break;
				out.write(buf, 0, n);
			}
		} catch (Exception e) {
			return null;
		} finally {
			inf.end();
		}
		return out.toByteArray();
	}
}
