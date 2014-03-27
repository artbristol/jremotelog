package eu.ocathain.jremotelog;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Encryptor {

	private static final int IV_SIZE_BYTES = 8;

	private static final int GCM_TAG_SIZE_BITS = 128;

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	private BigInteger counter;

	private final Cipher aes;

	final SecretKeySpec aesKeySpec;

	public Encryptor(AesKeyProvider config) {
		try {
			aes = Cipher.getInstance("AES/GCM/NoPadding");
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}

		byte[] iv = new byte[IV_SIZE_BYTES];
		new SecureRandom().nextBytes(iv);

		counter = new BigInteger(iv);

		aesKeySpec = new SecretKeySpec(DatatypeConverter.parseHexBinary(config
				.getAesKeyHexBinary()), "AES");
	}

	public EncryptedOutput encrypt(String message) {
		try {
			counter = counter.add(BigInteger.ONE);
			byte[] iv = toByteArrayForIv(counter);

			aes.init(Cipher.ENCRYPT_MODE, aesKeySpec, new GCMParameterSpec(
					GCM_TAG_SIZE_BITS, iv));
			byte[] encrypted = aes.doFinal(message
					.getBytes(StandardCharsets.UTF_8));

			String encryptedBase64 = DatatypeConverter
					.printBase64Binary(encrypted);
			return new EncryptedOutput(counter, encryptedBase64);
		} catch (GeneralSecurityException ex) {
			throw new RuntimeException(ex);
		}
	}

	private byte[] toByteArrayForIv(BigInteger counter) {
		return Arrays.copyOfRange(counter.toByteArray(), 0, IV_SIZE_BYTES);
	}

	public String decrypt(EncryptedOutput output) {
		byte[] encrypted = DatatypeConverter
				.parseBase64Binary(output.encryptedBase64);

		byte[] iv = toByteArrayForIv(output.counter);
		try {
			aes.init(Cipher.DECRYPT_MODE, aesKeySpec, new GCMParameterSpec(
					GCM_TAG_SIZE_BITS, iv));

			byte[] decrypted = aes.doFinal(encrypted);
			return new String(decrypted, StandardCharsets.UTF_8);
		} catch (GeneralSecurityException ex) {
			throw new RuntimeException(ex);
		}
	}
}
