package eu.ocathain.jremotelog;

import java.math.BigInteger;

public class EncryptedOutput {
	public final BigInteger counter;
	public final String encryptedBase64;

	public EncryptedOutput(BigInteger counter, String encryptedBase64) {
		super();
		this.counter = counter;
		this.encryptedBase64 = encryptedBase64;
	}

	public static EncryptedOutput fromString(String encryptedStringIncludingIv) {
		String[] dataSplit = encryptedStringIncludingIv.split(",");
		BigInteger counter = new BigInteger(dataSplit[0]);
		return new EncryptedOutput(counter, dataSplit[1]);
	}

	@Override
	public String toString() {
		return "EncryptedOutput [counter=" + counter + ", encryptedBase64="
				+ encryptedBase64 + "]";
	}

}