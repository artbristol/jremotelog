package eu.ocathain.jremotelog;

import java.io.File;

public class StartupChecks {

	public static void checkPropertiesFileExistence(String[] args,
			File propertiesFile) {
		if (!propertiesFile.exists()) {
			System.err.println("Expected to find a properties file at ["
					+ propertiesFile.getAbsolutePath() + "]");
			if (args.length == 0) {
				System.err
						.println("Specify a nondefault properties file location as the first parameter");
			}
			System.exit(1);
		}
	}
	public static void checkIvFile(String ivName) {
		File file = new File(ivName);
		if (!file.exists()) {
			System.err.println("Expected to find a iv file at ["
					+ ivName + "]");
			System.exit(1);
		}
		if (!file.canWrite()) {
			System.err.println("iv file at ["
					+ ivName + "] is not writeable");
			System.exit(1);
		}
	}

}
