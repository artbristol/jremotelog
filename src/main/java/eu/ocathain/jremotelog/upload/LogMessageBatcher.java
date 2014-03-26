package eu.ocathain.jremotelog.upload;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.web.client.RestTemplate;

import eu.ocathain.jremotelog.EncryptedOutput;
import eu.ocathain.jremotelog.Encryptor;

class LogMessageBatcher implements Callable<Void> {

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

	private final AtomicBoolean sendNow = new AtomicBoolean(false);
	private Thread batchingThread;

	@Override
	public Void call() {
		batchingThread = Thread.currentThread();
		while (true) {
			logger.fine("Starting poll of lines");

			List<EncryptedOutput> lines = new ArrayList<>();

			try {
				addToBatch(lines, logLines.take());

				while (millisTillNextBatch() > 0) {
					pollForNextLineUntilNextBatch(lines);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				if (sendNow.get()) {
					logger.severe("Sending immediately");
					if (!lines.isEmpty()) {
						logger.severe("Batch is here");
						sendBatch(lines);
					} else {
						logger.severe("Nothing to send");
					}
					return null;
				} else {
					throw new RuntimeException(e);
				}
			}

			logger.fine("Finished waiting next batch");
			if (lines.isEmpty()) {
				throw new IllegalStateException("How did we end up here?");
			}
			sendBatch(lines);
			timeForNextBatch = System.currentTimeMillis()
					+ config.batchIntervalMs;

		}
	}

	public void sendImmediatelyAndFinish() {
		sendNow.set(true);
		batchingThread.interrupt();
	}

	private void sendBatch(List<EncryptedOutput> lines) {
		String batch = createBatchMessage(lines);
		logger.log(Level.FINE, "Posting: [{0}]", batch);
		restTemplate.postForObject(config.logglyUrl, batch, String.class);
	}

	private String createBatchMessage(List<EncryptedOutput> lines) {
		StringBuilder builder = new StringBuilder();

		ListIterator<EncryptedOutput> it = lines.listIterator();
		while (it.hasNext()) {
			EncryptedOutput encrypted = it.next();
			builder.append(encrypted.counter).append(',')
					.append(encrypted.encryptedBase64).append("\n");
		}

		return builder.toString();
	}

	private void pollForNextLineUntilNextBatch(List<EncryptedOutput> lines)
			throws InterruptedException {
		logger.log(
				Level.FINE,
				"Awaiting next batch; millisecondsUntilNextBatch {0}, lines size {1} ",
				new Object[] { millisTillNextBatch(), lines.size() });

		String newLogLine = logLines.poll(millisTillNextBatch(),
				TimeUnit.MILLISECONDS);

		logger.log(Level.FINE, "NewLogLine: [{0}]", newLogLine);
		if (newLogLine != null) {
			addToBatch(lines, newLogLine);
		}
	}

	private void addToBatch(List<EncryptedOutput> lines, String newLogLine) {
		lines.add(encryptor.encrypt(newLogLine));
	}

	private long millisTillNextBatch() {
		return timeForNextBatch - System.currentTimeMillis();
	}
}