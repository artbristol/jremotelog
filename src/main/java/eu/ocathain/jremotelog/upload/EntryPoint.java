package eu.ocathain.jremotelog.upload;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import org.springframework.web.client.RestTemplate;

import eu.ocathain.jremotelog.Encryptor;
import eu.ocathain.jremotelog.IvManager;
import eu.ocathain.jremotelog.StartupChecks;

public class EntryPoint {

	private static final int DEFAULT_PADDING = 120;

	private static final ExecutorService executor = Executors
			.newCachedThreadPool();

	private static final Logger logger = Logger.getAnonymousLogger();

	public static void main(String[] args) throws Exception {
		String propertiesLocation = decidePropertiesLocation(args);
		File propertiesFile = new File(propertiesLocation);
		StartupChecks.checkPropertiesFileExistence(args, propertiesFile);
		LogUploaderConfig config = LogUploaderConfig
				.loadFromFile(propertiesFile);

		StartupChecks.checkIvFile(config.ivFile);

		logger.info("Starting up; tailing " + config.logFileToTail);

		BlockingQueue<String> logLines = new LinkedBlockingQueue<String>();

		executor.submit(new UnixTailer(new File(config.logFileToTail),
				new LogFileTailerListener(logLines), true, executor));
		final LogMessageBatcher batcher = new LogMessageBatcher(config,
				logLines, new RestTemplate(), Encryptor.create(config,
						new IvManager(config.ivFile)), DEFAULT_PADDING);
		executor.submit(batcher);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
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
