package eu.ocathain.jremotelog;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.output.FileWriterWithEncoding;

public class IvManager {

	private static final int PERSIST_EVERY_N_COUNT = 1000;

	static final int IV_SIZE_BYTES = 12;

	private final File ivFile;

	private BigInteger currentIv;

	public IvManager(String ivFileLocation) {
		ivFile = new File(ivFileLocation);

		List<String> ivStrings;
		try {
			ivStrings = Files.readAllLines(ivFile.toPath(),
					StandardCharsets.US_ASCII);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		if (ivStrings.isEmpty()) {
			currentIv = new BigInteger(createNew());
		} else {
			currentIv = new BigInteger(ivStrings.get(ivStrings.size() - 1))
					.add(BigInteger.valueOf(PERSIST_EVERY_N_COUNT));
		}

		persist();
	}

	public byte[] next() {

		byte[] byteArrayForIv = toByteArrayForIv(currentIv);
		currentIv = currentIv.add(BigInteger.ONE);
		if (currentIv.mod(BigInteger.valueOf(PERSIST_EVERY_N_COUNT)).equals(
				BigInteger.ZERO)) {
			persist();
		}
		return byteArrayForIv;
	}

	private void persist() {
		Logger.getAnonymousLogger().log(Level.FINE,
				"Writing the IV to the file [" + ivFile + "]");
		try (FileWriterWithEncoding fw = new FileWriterWithEncoding(ivFile,
				StandardCharsets.US_ASCII)) {
			fw.append(currentIv.toString() + "\n");
		} catch (IOException e) {
			Logger.getAnonymousLogger().log(
					Level.SEVERE,
					"There was an error writing the IV to the file [" + ivFile
							+ "]", e);
		}
	}

	byte[] toByteArrayForIv(BigInteger iv) {
		return Arrays.copyOfRange(iv.toByteArray(), 0, IV_SIZE_BYTES);
	}

	private byte[] createNew() {
		byte[] iv = new byte[IV_SIZE_BYTES];
		new SecureRandom().nextBytes(iv);
		return iv;
	}

}
