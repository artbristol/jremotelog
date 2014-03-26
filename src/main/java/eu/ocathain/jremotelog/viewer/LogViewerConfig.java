package eu.ocathain.jremotelog.viewer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

import eu.ocathain.jremotelog.AesKeyProvider;

public class LogViewerConfig implements AesKeyProvider {
	public static final String defaultPropertiesLocation = "/etc/jremotelog-retrieve.properties";

	public final String logglyRetrievalHost;
	public final String logglyUsername;
	public final String logglyPassword;
	public final String aesKeyHexBinary;

	public LogViewerConfig(Properties props) {
		logglyRetrievalHost = props.getProperty("logglyRetrievalHost");
		logglyUsername = props.getProperty("logglyUsername");
		logglyPassword = props.getProperty("logglyPassword");
		aesKeyHexBinary = props.getProperty("aesKeyHexBinary");

		Objects.requireNonNull(logglyRetrievalHost);
		Objects.requireNonNull(logglyUsername);
		Objects.requireNonNull(logglyPassword);
		Objects.requireNonNull(aesKeyHexBinary);
	}

	public static LogViewerConfig loadFromFile(File propertiesFile)
			throws IOException, FileNotFoundException {
		Properties props = new Properties();
		props.load(new FileInputStream(propertiesFile));
		return new LogViewerConfig(props);
	}

	@Override
	public String getAesKeyHexBinary() {
		return aesKeyHexBinary;
	}

}
