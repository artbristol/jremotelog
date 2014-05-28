package eu.ocathain.jremotelog;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.junit.Assert;
import org.junit.Test;

public class IvManagerTest {

	@Test
	public void testIvManager() {
		IvManager ivManager = new IvManager("src/test/resources/ivFile");

		Assert.assertArrayEquals(
				ivManager.toByteArrayForIv(BigInteger.valueOf(101000)),
				ivManager.next());

		Assert.assertArrayEquals(
				ivManager.toByteArrayForIv(BigInteger.valueOf(101001)),
				ivManager.next());
	}

	@Test
	public void testWithEmptyFile() {
		IvManager ivManager = new IvManager("src/test/resources/emptyIvFile");
		Assert.assertSame(IvManager.IV_SIZE_BYTES, ivManager.next().length);
	}

	@Test
	public void testPersist() throws IOException {
		BigInteger initial = BigInteger.valueOf(2000);

		File temp = File.createTempFile(getClass().getName() + "junitTest",
				".txt");

		temp.deleteOnExit();

		try (Writer fw = new FileWriterWithEncoding(temp,
				StandardCharsets.US_ASCII)) {
			fw.append(initial.toString());
		}

		IvManager ivManager = new IvManager(temp.getPath());

		for (int i = 0; i < 1001; i++) {
			ivManager.next();
		}

		List<String> lines = Files.readAllLines(temp.toPath(),
				StandardCharsets.US_ASCII);

		Assert.assertEquals("4000", lines.get(lines.size() - 1));
	}

}
