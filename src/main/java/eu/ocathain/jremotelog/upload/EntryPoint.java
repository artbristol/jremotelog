package eu.ocathain.jremotelog.upload;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import org.apache.commons.io.input.Tailer;

import eu.ocathain.jremotelog.StartupChecks;

public class EntryPoint {

	private static final ExecutorService executor = Executors
			.newCachedThreadPool();

	private static final Logger logger = Logger.getAnonymousLogger();

	public static void main(String[] args) throws Exception {
		String propertiesLocation = decidePropertiesLocation(args);
		File propertiesFile = new File(propertiesLocation);
		StartupChecks.checkPropertiesFileExistence(args, propertiesFile);
		LogUploaderConfig config = LogUploaderConfig
				.loadFromFile(propertiesFile);

		logger.info("Starting up; tailing " + config.logFileToTail);

		BlockingQueue<String> logLines = new LinkedBlockingQueue<String>();

		executor.submit(new Tailer(new File(config.logFileToTail),
				new LogFileTailer(logLines), 1000, true));
		final LogMessageBatcher batcher = new LogMessageBatcher(config,
				logLines);
		executor.submit(batcher);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				// This doesn't usually work, but it's worth a try.
				batcher.sendImmediatelyAndFinish();
			}
		});
	}

	private static String decidePropertiesLocation(String[] args) {
		if (args.length == 0) {
			return LogUploaderConfig.defaultPropertiesLocation;
		} else {
			return args[0];
		}
	}
}
