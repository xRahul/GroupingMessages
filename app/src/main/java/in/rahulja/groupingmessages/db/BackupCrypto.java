package in.rahulja.groupingmessages.db;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Versioned encrypted container for database backups.
 *
 * <p>Format: {@code "GMBK1"}, a big-endian iteration count, a 16-byte salt, a
 * 12-byte GCM IV, then AES-256-GCM ciphertext and tag. The passphrase is never
 * stored. Plaintext SQLite files are still accepted by the import path for
 * backward compatibility; this class only handles the encrypted form.
 */
public final class BackupCrypto {

  private static final byte[] MAGIC = { 'G', 'M', 'B', 'K', '1' };
  private static final int SALT_LENGTH = 16;
  private static final int IV_LENGTH = 12;
  private static final int ITERATIONS = 210_000;
  private static final int MAX_ITERATIONS = 1_000_000;
  private static final int KEY_BITS = 256;
  private static final int TAG_BITS = 128;
  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA256";

  private BackupCrypto() {
  }

  /**
   * Returns true when the stream starts with our header. The stream is advanced;
   * reopen it before calling {@link #decrypt}.
   */
  public static boolean isEncrypted(InputStream in) throws IOException {
    byte[] header = new byte[MAGIC.length];
    int offset = 0;
    while (offset < header.length) {
      int read = in.read(header, offset, header.length - offset);
      if (read < 0) {
        break;
      }
      offset += read;
    }
    return offset == MAGIC.length && Arrays.equals(header, MAGIC);
  }

  public static void encrypt(InputStream plain, OutputStream out, char[] passphrase)
      throws IOException, GeneralSecurityException {
    byte[] salt = randomBytes(SALT_LENGTH);
    byte[] iv = randomBytes(IV_LENGTH);
    out.write(MAGIC);
    writeInt(out, ITERATIONS);
    out.write(salt);
    out.write(iv);

    Cipher cipher = Cipher.getInstance(TRANSFORMATION);
    cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt, ITERATIONS),
        new GCMParameterSpec(TAG_BITS, iv));
    writeCipher(cipher, plain, out);
  }

  public static void decrypt(InputStream in, OutputStream out, char[] passphrase)
      throws IOException, GeneralSecurityException {
    byte[] magic = new byte[MAGIC.length];
    readFully(in, magic);
    if (!Arrays.equals(magic, MAGIC)) {
      throw new IOException("Not an encrypted Grouping Messages backup");
    }
    int iterations = readInt(in);
    if (iterations <= 0 || iterations > MAX_ITERATIONS) {
      throw new IOException("Unsupported backup KDF iteration count");
    }
    byte[] salt = new byte[SALT_LENGTH];
    byte[] iv = new byte[IV_LENGTH];
    readFully(in, salt);
    readFully(in, iv);

    Cipher cipher = Cipher.getInstance(TRANSFORMATION);
    cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt, iterations),
        new GCMParameterSpec(TAG_BITS, iv));
    writeCipher(cipher, in, out);
  }

  private static void writeCipher(Cipher cipher, InputStream in, OutputStream out)
      throws IOException, GeneralSecurityException {
    byte[] buffer = new byte[8192];
    int read;
    while ((read = in.read(buffer)) != -1) {
      byte[] chunk = cipher.update(buffer, 0, read);
      if (chunk != null) {
        out.write(chunk);
      }
    }
    byte[] finalChunk = cipher.doFinal();
    if (finalChunk != null) {
      out.write(finalChunk);
    }
    out.flush();
  }

  private static SecretKeySpec deriveKey(char[] passphrase, byte[] salt, int iterations)
      throws GeneralSecurityException {
    PBEKeySpec spec = new PBEKeySpec(passphrase, salt, iterations, KEY_BITS);
    try {
      byte[] key = SecretKeyFactory.getInstance(KEY_ALGORITHM)
          .generateSecret(spec)
          .getEncoded();
      return new SecretKeySpec(key, "AES");
    } finally {
      spec.clearPassword();
    }
  }

  private static byte[] randomBytes(int length) {
    byte[] bytes = new byte[length];
    new SecureRandom().nextBytes(bytes);
    return bytes;
  }

  private static void writeInt(OutputStream out, int value) throws IOException {
    out.write((value >>> 24) & 0xFF);
    out.write((value >>> 16) & 0xFF);
    out.write((value >>> 8) & 0xFF);
    out.write(value & 0xFF);
  }

  private static int readInt(InputStream in) throws IOException {
    byte[] bytes = new byte[4];
    readFully(in, bytes);
    return ((bytes[0] & 0xFF) << 24)
        | ((bytes[1] & 0xFF) << 16)
        | ((bytes[2] & 0xFF) << 8)
        | (bytes[3] & 0xFF);
  }

  private static void readFully(InputStream in, byte[] target) throws IOException {
    int offset = 0;
    while (offset < target.length) {
      int read = in.read(target, offset, target.length - offset);
      if (read < 0) {
        throw new EOFException("Encrypted backup is truncated");
      }
      offset += read;
    }
  }
}
