package eu.ocathain.jremotelog;

import java.math.BigInteger;

public class EncryptedOutput {
	public final BigInteger iv;
	public final String encryptedBase64;

	public EncryptedOutput(BigInteger iv, String encryptedBase64) {
		super();
		this.iv = iv;
		this.encryptedBase64 = encryptedBase64;
	}

	public static EncryptedOutput fromString(String encryptedStringIncludingIv) {
		String[] dataSplit = encryptedStringIncludingIv.split(",");
		BigInteger iv = new BigInteger(dataSplit[0]);
		return new EncryptedOutput(iv, dataSplit[1]);
	}

	@Override
	public String toString() {
		return "EncryptedOutput [iv=" + iv + ", encryptedBase64="
				+ encryptedBase64 + "]";
	}

}