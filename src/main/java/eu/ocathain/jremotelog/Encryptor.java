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

	private static final int IV_SIZE_BYTES = 16;

	private static final int GCM_TAG_SIZE_BITS = 128;

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	private BigInteger currentIv;

	private final Cipher aes;

	private final SecretKeySpec aesKeySpec;

	// private - use static create methods
	private Encryptor(Cipher aes, SecretKeySpec aesKeySpec, BigInteger iv) {
		this.aes = aes;
		this.aesKeySpec = aesKeySpec;
		this.currentIv = iv;
	}

	public EncryptedOutput encrypt(String message, int padLength) {
		if (padLength < 0 || padLength > 255 /* arbitrary maximum */) {
			throw new IllegalArgumentException("Pad length " + padLength
					+ " should be between 0 and 255");
		}

		try {
			currentIv = currentIv.add(BigInteger.ONE);
			byte[] iv = toByteArrayForIv(currentIv);

			aes.init(Cipher.ENCRYPT_MODE, aesKeySpec, new GCMParameterSpec(
					GCM_TAG_SIZE_BITS, iv));
			String paddedString = message;
			if (padLength > 0) {
				paddedString = String.format("%1$-" + padLength + "s", message);
			}

			byte[] encrypted = aes.doFinal(paddedString
					.getBytes(StandardCharsets.UTF_8));

			String encryptedBase64 = DatatypeConverter
					.printBase64Binary(encrypted);
			return new EncryptedOutput(currentIv, encryptedBase64);
		} catch (GeneralSecurityException ex) {
			throw new RuntimeException(ex);
		}
	}

	private byte[] toByteArrayForIv(BigInteger iv) {
		return Arrays.copyOfRange(iv.toByteArray(), 0, IV_SIZE_BYTES);
	}

	public String decrypt(EncryptedOutput output)
			throws GeneralSecurityException {
		byte[] encrypted = DatatypeConverter
				.parseBase64Binary(output.encryptedBase64);

		byte[] iv = toByteArrayForIv(output.iv);
		aes.init(Cipher.DECRYPT_MODE, aesKeySpec, new GCMParameterSpec(
				GCM_TAG_SIZE_BITS, iv));

		byte[] decrypted = aes.doFinal(encrypted);
		return new String(decrypted, StandardCharsets.UTF_8);
	}

	public static Encryptor createWithRandomizedIv(AesKeyProvider config) {
		byte[] iv = new byte[IV_SIZE_BYTES];
		new SecureRandom().nextBytes(iv);
		return create(config, iv);
	}

	public static Encryptor create(AesKeyProvider config, byte[] ivBytes) {
		byte[] aesKeyBytes = DatatypeConverter.parseHexBinary(config
				.getAesKeyHexBinary());

		if (aesKeyBytes.length > ivBytes.length) {
			throw new IllegalArgumentException("AES key length ("
					+ (aesKeyBytes.length * 8)
					+ " bits) should be equal to IV length (" + (ivBytes.length * 8)
					+ " bits)");
		}

		BigInteger iv = new BigInteger(ivBytes);
		Cipher aes;
		try {
			aes = Cipher.getInstance("AES/GCM/NoPadding");
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
		SecretKeySpec aesKeySpec = new SecretKeySpec(aesKeyBytes, "AES");
		return new Encryptor(aes, aesKeySpec, iv);
	}
}
