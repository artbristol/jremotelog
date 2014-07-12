package eu.ocathain.jremotelog;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.junit.Assert;
import org.junit.Test;

public class IvManagerTest {

    public static final String TEST_IV_FILE = "target/test-classes/ivFile";
    public static final String TEST_IV_FILE_FORTEST = TEST_IV_FILE + "forTest";

    @Test
	public void testIvManager() throws IOException {
        Files.copy(new File(TEST_IV_FILE).toPath(), new File(TEST_IV_FILE_FORTEST).toPath(),
                StandardCopyOption.REPLACE_EXISTING);

		IvManager ivManager = new IvManager(TEST_IV_FILE_FORTEST);

		Assert.assertArrayEquals(
				ivManager.toByteArrayForIv(BigInteger.valueOf(101000)),
				ivManager.next());

		Assert.assertArrayEquals(
				ivManager.toByteArrayForIv(BigInteger.valueOf(101001)),
				ivManager.next());
	}

	@Test
	public void testWithEmptyFile() {
		IvManager ivManager = new IvManager("target/test-classes/emptyIvFile");
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

		appendLotsAndCheck(temp, ivManager, "4000");

		appendLotsAndCheck(temp, ivManager, "5000");
	}

	private void appendLotsAndCheck(File temp, IvManager ivManager,
			String expected) throws IOException {
		for (int i = 0; i < 1001; i++) {
			ivManager.next();
		}

		List<String> lines = Files.readAllLines(temp.toPath(),
				StandardCharsets.US_ASCII);

		Assert.assertEquals(expected, lines.get(lines.size() - 1));
	}

}
