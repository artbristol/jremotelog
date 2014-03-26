package eu.ocathain.jremotelog.upload;

import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.input.TailerListenerAdapter;

public class LogFileTailer extends TailerListenerAdapter {
	private final BlockingQueue<String> logLines;

	private final Logger logger = Logger.getAnonymousLogger();

	public LogFileTailer(BlockingQueue<String> logLines) {
		super();
		this.logLines = logLines;
	}

	@Override
	public void handle(Exception ex) {
		Logger.getAnonymousLogger().log(Level.SEVERE, "Exception tailing log",
				ex);
	}

	@Override
	public void handle(String line) {
		logger.fine("Handled a line");
		logLines.add(line);

	}

	@Override
	public void fileNotFound() {
		System.err.println("File not found!");
		try {
			/*
			 * Throttle the rate at which we complain about this, because the
			 * file might be recreated soon.
			 */
			Thread.sleep(100);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}