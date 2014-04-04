package eu.ocathain.jremotelog.upload;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.web.client.RestTemplate;

import eu.ocathain.jremotelog.EncryptedOutput;
import eu.ocathain.jremotelog.Encryptor;

class LogMessageBatcher implements Callable<Void> {

	private static final int MIN_LINE_LENGTH = 80;

	Logger logger = Logger.getAnonymousLogger();

	private final RestTemplate restTemplate = new RestTemplate();

	private final LogUploaderConfig config;
	private final Encryptor encryptor;

	private final BlockingQueue<String> logLines;

	public LogMessageBatcher(LogUploaderConfig config,
			BlockingQueue<String> logLines) {
		this.config = config;
		this.encryptor = new Encryptor(config);
		this.logLines = logLines;
	}

	long timeForNextBatch = System.currentTimeMillis();

	private Thread batchingThread;
	private final Queue<EncryptedOutput> lines = new ConcurrentLinkedQueue<>();

	@Override
	public Void call() throws InterruptedException {
		batchingThread = Thread.currentThread();
		while (true) {
			logger.fine("Starting poll of lines");

			addToBatch(logLines.take());

			while (millisTillNextBatch() > 0) {
				pollForNextLineUntilNextBatch();
			}

			logger.fine("Finished waiting next batch");
			if (lines.isEmpty()) {
				throw new IllegalStateException("How did we end up here?");
			}
			sendBatch();
			timeForNextBatch = System.currentTimeMillis()
					+ config.batchIntervalMs;
		}
	}

	public void sendImmediatelyAndFinish() {
		logger.log(Level.INFO, "Shutting down");
		batchingThread.interrupt();
		if (!lines.isEmpty()) {
			logger.log(Level.INFO, "Trying to send last batch");
			sendBatch();
		}
	}

	private void sendBatch() {
		String batch = createBatchMessage(lines);
		logger.log(Level.FINE, "Posting: [{0}]", batch);
		restTemplate.postForObject(config.logglyUrl, batch, String.class);
		lines.clear();
	}

	private String createBatchMessage(Collection<EncryptedOutput> lines) {
		StringBuilder builder = new StringBuilder();

		for (EncryptedOutput encrypted : lines) {
			builder.append(encrypted.counter).append(',')
					.append(encrypted.encryptedBase64).append("\n");
		}

		return builder.toString();
	}

	private void pollForNextLineUntilNextBatch()
			throws InterruptedException {
		logger.log(
				Level.FINE,
				"Awaiting next batch; millisecondsUntilNextBatch {0}, lines size {1} ",
				new Object[] { millisTillNextBatch(), lines.size() });

		String newLogLine = logLines.poll(millisTillNextBatch(),
				TimeUnit.MILLISECONDS);

		logger.log(Level.FINE, "NewLogLine: [{0}]", newLogLine);
		if (newLogLine != null) {
			addToBatch(newLogLine);
		}
	}

	private void addToBatch(String newLogLine) {
		lines.add(encryptor.encrypt(newLogLine, MIN_LINE_LENGTH));
	}

	private long millisTillNextBatch() {
		return timeForNextBatch - System.currentTimeMillis();
	}
}