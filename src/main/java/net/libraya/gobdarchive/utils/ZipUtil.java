package net.libraya.gobdarchive.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtil {
	
	public static void zipFolder(Path sourceFolder, Path zipFile) throws IOException {
		if (!Files.exists(sourceFolder)) {
            throw new IOException("Source folder does not exist: " + sourceFolder);
        }
		
		// Ensure parent directory exists
        if (zipFile.getParent() != null) {
            Files.createDirectories(zipFile.getParent());
        }

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            Path basePath = sourceFolder.toAbsolutePath();

            Files.walk(sourceFolder).forEach(path -> {
                try {
                    if (Files.isDirectory(path)) {
                        return; // skip directories
                    }

                    // Compute relative path inside ZIP
                    Path relative = basePath.relativize(path.toAbsolutePath());
                    ZipEntry entry = new ZipEntry(relative.toString().replace("\\", "/"));
                    zos.putNextEntry(entry);

                    try (InputStream is = Files.newInputStream(path)) {
                        is.transferTo(zos);
                    }

                    zos.closeEntry();

                } catch (IOException e) {
                    throw new RuntimeException("Error zipping file: " + path, e);
                }
            });
        }

	}
}	
