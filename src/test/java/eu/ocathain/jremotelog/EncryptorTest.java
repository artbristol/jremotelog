package eu.ocathain.jremotelog;

import java.math.BigInteger;
import java.security.GeneralSecurityException;

import javax.xml.bind.DatatypeConverter;

import org.junit.Assert;
import org.junit.Test;

public class EncryptorTest {
	private static final String AES_KEY_HEX = "DEADBEEFCAFEDEADBEEFCAFEDEADBEEF";

	private static final byte[] INITIALIZATION_VECTOR = DatatypeConverter
			.parseHexBinary("ABCDEF0123456789ABCDEF01");
	
	private static final String CIPHERTEXT_BASE64 = "V8/14IkVyBHrEOp1ELa01Ga3eMzPlUA5LJeTqxuC+/rUWAz1tnjzBlKBughldCJpJwaUPuaOX4A+2Btdkd9AcHLtsSMmNLk=";

	private static final String CIPHERTEXT_WITH_PADDING_BASE64 = "V8/14IkVyBHrEOp1ELa01Ga3eMzPlUA5LJeTqxuC+/rUWAz1tnjzBlKBughldCJpJwaUPuaOXwRwx3EYW71boFLIJXeyPnaETQwPPjMebNFYsASI0x0fJK+seFrrlYgE+jYK3H3/VaBNaICeKmVQTM4CnFr4XPAVgfrVVPLOuX6hPzSg1AS6Q0y6kvZa9CJ7Cv6ITXaaHqe5PyU5+t/g/32pEE7wVvUTw7QvW2GoZmq2MkRyWc4dyn8eJUskvncWxPj3Jb8GZkJzR9OkyqKjM+IihatR6f7+";
	
	private static final String PLAINTEXT = "Why bother encrypting your logs, they already have root";

	private static final AesKeyProvider AES_KEY_PROVIDER = new AesKeyProvider() {

		@Override
		public String getAesKeyHexBinary() {
			return AES_KEY_HEX;
		}
	};

	Encryptor encryptor = Encryptor.create(AES_KEY_PROVIDER,
			INITIALIZATION_VECTOR);

	@Test(expected = IllegalArgumentException.class)
	public void testEncryptorKIvLengthValidation() {
		byte[] tooShortIv = new byte[] { (byte) 0 };
		Encryptor.create(AES_KEY_PROVIDER, tooShortIv);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEncryptorKeyLengthValidation() {
		Encryptor.createWithRandomizedIv(new AesKeyProvider() {

			@Override
			public String getAesKeyHexBinary() {
				// Too short
				return "ABC";
			}
		});
	}

	@Test
	public void testEncrypt() {
		EncryptedOutput output = encryptor.encrypt(PLAINTEXT, 1);
		Assert.assertEquals(CIPHERTEXT_BASE64, output.encryptedBase64);
	}

	@Test
	public void testEncryptNoPadding() {
		EncryptedOutput output = encryptor.encrypt(PLAINTEXT, 0);
		Assert.assertEquals(CIPHERTEXT_BASE64, output.encryptedBase64);
	}

	@Test
	public void testEncrypt200Padding() throws GeneralSecurityException {
		EncryptedOutput output = encryptor.encrypt(PLAINTEXT, 200);
		Assert.assertEquals(CIPHERTEXT_WITH_PADDING_BASE64,
				output.encryptedBase64);

		String roundTripped = encryptor.decrypt(output);
		String padding = "                                                                                                                                                 ";
		Assert.assertEquals(PLAINTEXT + padding, roundTripped);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEncryptNegativePadding() {
		encryptor.encrypt(PLAINTEXT, -1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEncryptTooMuchPadding() {
		encryptor.encrypt(PLAINTEXT, 300);
	}

	@Test
	public void testDecrypt() throws GeneralSecurityException {
		String decryptedPlaintext = encryptor.decrypt(new EncryptedOutput(
				new BigInteger(INITIALIZATION_VECTOR).add(BigInteger.ONE),
				CIPHERTEXT_BASE64));
		Assert.assertEquals(PLAINTEXT, decryptedPlaintext);
	}

	@Test(expected = GeneralSecurityException.class)
	public void testDecryptWithTampering() throws GeneralSecurityException {
		String tamperedCiphertext = CIPHERTEXT_BASE64.replaceFirst("a", "b");
		encryptor
				.decrypt(new EncryptedOutput(new BigInteger(
						INITIALIZATION_VECTOR).add(BigInteger.ONE),
						tamperedCiphertext));
	}
}
