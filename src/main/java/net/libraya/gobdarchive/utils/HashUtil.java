package net.libraya.gobdarchive.utils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {
	
	public static String sha256(Path path) {
        try {
            byte[] data = Files.readAllBytes(path);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        } catch (Exception e) {
            throw new RuntimeException("Hashing fehlgeschlagen", e);
        }
    }
	
	public static String sha256(String text) throws RuntimeException {
	    try {
			return hash(text, "sha-256");
		} catch (NoSuchAlgorithmException ignore) {}
	    return null;
	}
	
	public static String hash(String text, String instance) throws RuntimeException, NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance(instance.toUpperCase());
        byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();

	}
}
