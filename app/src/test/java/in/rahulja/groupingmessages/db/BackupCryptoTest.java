package in.rahulja.groupingmessages.db;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import org.junit.Test;

public class BackupCryptoTest {

  private static final char[] PASSPHRASE = "correct horse battery staple".toCharArray();

  private static byte[] payload() {
    byte[] data = new byte[64 * 1024];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) (i * 31 + 7);
    }
    return data;
  }

  @Test
  public void encryptedRoundTripRestoresExactBytes() throws Exception {
    byte[] plain = payload();
    ByteArrayOutputStream encrypted = new ByteArrayOutputStream();
    BackupCrypto.encrypt(new ByteArrayInputStream(plain), encrypted, PASSPHRASE);

    assertTrue(BackupCrypto.isEncrypted(
        new ByteArrayInputStream(encrypted.toByteArray())));
    assertFalse(Arrays.equals(plain, encrypted.toByteArray()));

    ByteArrayOutputStream decrypted = new ByteArrayOutputStream();
    BackupCrypto.decrypt(new ByteArrayInputStream(encrypted.toByteArray()),
        decrypted, PASSPHRASE);
    assertArrayEquals(plain, decrypted.toByteArray());
  }

  @Test
  public void wrongPassphraseFailsClosed() throws Exception {
    ByteArrayOutputStream encrypted = new ByteArrayOutputStream();
    BackupCrypto.encrypt(new ByteArrayInputStream(payload()), encrypted, PASSPHRASE);

    try {
      BackupCrypto.decrypt(new ByteArrayInputStream(encrypted.toByteArray()),
          new ByteArrayOutputStream(), "wrong passphrase".toCharArray());
      fail("decrypt should reject a wrong passphrase");
    } catch (GeneralSecurityException expected) {
      // GCM authentication failure
    }
  }

  @Test
  public void tamperedCiphertextFailsClosed() throws Exception {
    ByteArrayOutputStream encrypted = new ByteArrayOutputStream();
    BackupCrypto.encrypt(new ByteArrayInputStream(payload()), encrypted, PASSPHRASE);
    byte[] tampered = encrypted.toByteArray();
    tampered[tampered.length - 1] ^= 0x01;

    try {
      BackupCrypto.decrypt(new ByteArrayInputStream(tampered),
          new ByteArrayOutputStream(), PASSPHRASE);
      fail("decrypt should reject tampered ciphertext");
    } catch (GeneralSecurityException expected) {
      // GCM authentication failure
    }
  }

  @Test
  public void plaintextIsNotMistakenForEncrypted() throws Exception {
    assertFalse(BackupCrypto.isEncrypted(
        new ByteArrayInputStream("SQLite format 3\u0000".getBytes())));
  }
}
