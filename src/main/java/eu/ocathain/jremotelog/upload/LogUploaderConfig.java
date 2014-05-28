package eu.ocathain.jremotelog.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

import eu.ocathain.jremotelog.AesKeyProvider;

public class LogUploaderConfig implements AesKeyProvider {
	public static final String defaultPropertiesLocation = "/etc/jremotelog.properties";

	public final String logglyUrl;
	public final String aesKeyHexBinary;
	public final String logFileToTail;
	public final int batchIntervalMs;
	public final String ivFile;

	public LogUploaderConfig(Properties props) {
		logglyUrl = props.getProperty("logglyUrl");
		aesKeyHexBinary = props.getProperty("aesKeyHexBinary");
		logFileToTail = props.getProperty("logFileToTail");
		String batchIntervalMsString = props.getProperty("batchIntervalMs");
		ivFile = props.getProperty("ivFile");
		Objects.requireNonNull(logglyUrl);
		Objects.requireNonNull(aesKeyHexBinary);
		Objects.requireNonNull(logFileToTail);
		Objects.requireNonNull(batchIntervalMsString);
		Objects.requireNonNull(ivFile);
		batchIntervalMs = Integer.valueOf(batchIntervalMsString);
	}

	public static LogUploaderConfig loadFromFile(File propertiesFile)
			throws IOException, FileNotFoundException {
		Properties props = new Properties();
		props.load(new FileInputStream(propertiesFile));

		return new LogUploaderConfig(props);
	}

	@Override
	public String getAesKeyHexBinary() {
		return aesKeyHexBinary;
	}

}
