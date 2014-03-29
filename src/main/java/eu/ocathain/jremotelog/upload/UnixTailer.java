package eu.ocathain.jremotelog.upload;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TailerListener;

/**
 * Native version of org.apache.commons.io.input.Tailer, that doesn't suffer
 * from https://issues.apache.org/jira/browse/IO-279.
 * 
 * @author art
 * 
 */
public class UnixTailer implements Runnable {

	private final File file;
	private final TailerListener tailerListener;

	private Process tailProcess;
	private ExecutorService executorService;

	public UnixTailer(File file, TailerListener tailerListener, boolean end,
			ExecutorService executorService) {
		this.file = file;
		this.tailerListener = tailerListener;
		this.executorService = executorService;
		if (!end) {
			throw new UnsupportedOperationException("Can only tail the end");
		}
	}

	@Override
	public void run() {
		if (!file.exists()) {
			tailerListener.fileNotFound();
			return;
		}

		try {
			tailProcess = new ProcessBuilder().command("tail", "-c0", "-F",
					file.getPath()).start();
		} catch (IOException e) {
			tailerListener.handle(e);
			return;
		}

		executorService.submit(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				IOUtils.copy(tailProcess.getErrorStream(), System.err);
				return null;
			}
		});

		executorService.submit(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				String line = null;
				BufferedReader br = new BufferedReader(new InputStreamReader(
						tailProcess.getInputStream()));
				while ((line = br.readLine()) != null) {
					tailerListener.handle(line);
				}
				return null;
			}
		});

		try {
			// Tail process should never finish.
			int exitCode = tailProcess.waitFor();
			Logger.getAnonymousLogger()
					.log(Level.SEVERE,
							"The tail process finished unexpectedly. Exit code {0}. Exiting!",
							exitCode);
			System.exit(1);
		} catch (InterruptedException e) {
			return;
		}
	}
}
