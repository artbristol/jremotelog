package eu.ocathain.jremotelog.upload;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import eu.ocathain.jremotelog.EncryptedOutput;
import eu.ocathain.jremotelog.Encryptor;

public class LogMessageBatcherTest {

	private final BlockingQueue<String> logLines = new LinkedBlockingQueue<>();
	private final RestOperations mockRest = Mockito.mock(RestOperations.class);
	private final Encryptor mockEncryptor = Mockito.mock(Encryptor.class);
	private final LogUploaderConfig config;
	private final LogMessageBatcher batcher;
	private final Semaphore batchPostSemaphore = new Semaphore(0);

    private Thread batcherThread;

	public LogMessageBatcherTest() {
		try {
			config = LogUploaderConfig.loadFromFile(new File(
					"src/test/resources/loguploaderconfigtest.properties"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		batcher = new LogMessageBatcher(config, logLines, mockRest,
				mockEncryptor, 1);
	}

    @Before
    public void startBatcher() {
        batcherThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    batcher.call();
                } catch (InterruptedException e) {
                    // expected
                }

            }
        });
        batcherThread.start();
    }

    @After
    public void stopBatcher() {
        batcherThread.interrupt();
    }

	@Test
	public void testSingleMessages() throws InterruptedException {

		ArgumentCaptor<String> messageMatcher = ArgumentCaptor
				.forClass(String.class);

		appendOneMessageAndCheckPosted(messageMatcher, BigInteger.ZERO,
				"ciphertext1", "log message 1");
		appendOneMessageAndCheckPosted(messageMatcher, BigInteger.ONE,
				"ciphertext2", "log message 2");
	}

    @Test
    public void testMultipleMessages() throws InterruptedException {

        ArgumentCaptor<String> messageMatcher = ArgumentCaptor
                .forClass(String.class);

        appendOneMessageAndCheckPosted(messageMatcher, BigInteger.ZERO,
                "ciphertext1", "log message 1");

        // These two messages should get batched into a single post
        String message2 = "log message 2";
        String message3 = "log message 3";

        expectEncrypt(message2, new EncryptedOutput(BigInteger.valueOf(1),
                "ciphertext2"));
        expectEncrypt(message3, new EncryptedOutput(BigInteger.valueOf(2),
                "ciphertext3"));
        expectPost(messageMatcher);

        logLines.add(message2);
        logLines.add(message3);
        batchPostSemaphore.acquire();
        Assert.assertEquals("1,ciphertext2\n" + "2,ciphertext3\n",
                messageMatcher.getValue());
    }

    @Test
    public void testMultipleMessagesWithException() throws InterruptedException {

        appendOneMessageAndCheckPosted(ArgumentCaptor
                .forClass(String.class), BigInteger.ZERO,
                "ciphertext1", "log message 1");

        String message2 = "log message 2 will fail";
        expectEncrypt(message2, new EncryptedOutput(BigInteger.valueOf(1),
                "ciphertext2"));

        Mockito.when(
                mockRest.postForObject(Matchers.eq(config.logglyUrl),
                        ArgumentCaptor
                                .forClass(String.class).capture(), Matchers.eq(String.class)))
                        .thenAnswer(new Answer<Object>() {
                            @Override
                            public Object answer(InvocationOnMock invocation) throws Throwable {
                                batchPostSemaphore.release();
                                throw new RestClientException("Imaginary communication problem");
                            }
                        });

        logLines.add(message2);
        batchPostSemaphore.acquire();

        // This message should be batched with message 2 which is resent due to failure
        String message3 = "log message 3";

        expectEncrypt(message3, new EncryptedOutput(BigInteger.valueOf(2),
                "ciphertext3"));

        ArgumentCaptor<String> messageMatcher = ArgumentCaptor
                .forClass(String.class);
        expectPost(messageMatcher);

        logLines.add(message3);
        batchPostSemaphore.acquire();

        Assert.assertEquals("1,ciphertext2\n" + "2,ciphertext3\n",
                messageMatcher.getValue());
    }


    private void expectPost(ArgumentCaptor<String> messageMatcher) {
		Mockito.when(
				mockRest.postForObject(Matchers.eq(config.logglyUrl),
						messageMatcher.capture(), Matchers.eq(String.class)))
				.thenAnswer(new Answer<String>() {

					@Override
					public String answer(InvocationOnMock invocation)
							throws Throwable {
						batchPostSemaphore.release();
						return null;
					}
				});
	}

	private void expectEncrypt(String logMessage,
			EncryptedOutput mockEncryptedOutput) {
		Mockito.when(
				mockEncryptor.encrypt(Matchers.eq(logMessage),
						Matchers.anyInt())).thenReturn(mockEncryptedOutput);
	}

	private void setupMocksForPostedMessage(String logMessage,
			ArgumentCaptor<String> messageMatcher,
			EncryptedOutput mockEncryptedOutput) {
		expectEncrypt(logMessage, mockEncryptedOutput);
		expectPost(messageMatcher);
	}

	private void appendOneMessageAndCheckPosted(
			ArgumentCaptor<String> messageMatcher, BigInteger iv,
			String mockCiphertext, String logMessage)
			throws InterruptedException {
		EncryptedOutput mockEncryptedOutput = new EncryptedOutput(iv,
				mockCiphertext);
		setupMocksForPostedMessage(logMessage, messageMatcher,
				mockEncryptedOutput);
		logLines.add(logMessage);
		batchPostSemaphore.acquire();
		Assert.assertEquals(iv.toString() + "," + mockCiphertext + "\n",
				messageMatcher.getValue());
	}


}
