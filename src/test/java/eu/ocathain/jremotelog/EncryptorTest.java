package eu.ocathain.jremotelog;

import java.math.BigInteger;
import java.security.GeneralSecurityException;

import javax.xml.bind.DatatypeConverter;

import org.junit.Assert;
import org.junit.Test;

public class EncryptorTest {
	private static final String AES_KEY_HEX = "DEADBEEFCAFEDEADBEEFCAFEDEADBEEF";

	private static final byte[] INITIALIZATION_VECTOR = DatatypeConverter
			.parseHexBinary("ABCDEF0123456789ABCDEF0123456789");

	private static final String CIPHERTEXT_BASE64 = "/AGzDhTxYg3ol9bP7rVPVU4PCfEdvzjikdUGsYhR+Wu3TxEV7hd+v4zMaAoa6qS8+Gt3XFzETzNv54UyyztcDoiqj7uME+A=";

	private static final String CIPHERTEXT_WITH_PADDING_BASE64 = "/AGzDhTxYg3ol9bP7rVPVU4PCfEdvzjikdUGsYhR+Wu3TxEV7hd+v4zMaAoa6qS8+Gt3XFzET055h0aQOOSZ+PpO1CxblbsWknZ5eSS5XmCwEyfB16ejD9yN9Ti7CPMPHP/aUQy9l/wiA3C46qGnMlVi8poWTHqNqWJuT+oHdM/MtQS2wrnwcD7V4QlN9ncM0pKnFjpd3xIsqFeQeeMD+wtv8oIlhg/YEXtbHri7JwX4TMPqHxsFVioqGr8+WQimKl7x4ZHRV5SeRhVGXwKqGJDl6yjbK5co";

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
