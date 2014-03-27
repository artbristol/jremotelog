package eu.ocathain.jremotelog.viewer;

import java.io.File;
import java.util.Map;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import eu.ocathain.jremotelog.EncryptedOutput;
import eu.ocathain.jremotelog.Encryptor;
import eu.ocathain.jremotelog.StartupChecks;

public class ExistingLogViewer {

	private static final int DEFAULT_HOURS_TO_RETRIEVE = 12;

	private static final int PAGE_SIZE = 1000;

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.err
					.println("usage: ExistingLogViewer properties-file-location [hours of logs to retrieve]");
			System.exit(1);
		}

		File propertiesFile = new File(args[0]);
		StartupChecks.checkPropertiesFileExistence(args, propertiesFile);

		Integer hours = DEFAULT_HOURS_TO_RETRIEVE;

		if (args.length > 1) {
			hours = Integer.valueOf(args[1]);
		}

		LogViewerConfig config = LogViewerConfig.loadFromFile(propertiesFile);
		new ExistingLogViewer(config).retrieveLastNHours(hours);
	}

	private final LogViewerConfig config;

	private final Encryptor encryptor;

	private final RestTemplate restTemplate;

	public ExistingLogViewer(LogViewerConfig config) {
		this.config = config;
		encryptor = new Encryptor(config);
		restTemplate = create(config);
	}

	void retrieveLastNHours(Integer hours) {

		Map<?, ?> response = restTemplate.getForObject("https://"
				+ config.logglyRetrievalHost + "/apiv2/fields?q=*&from=-"
				+ hours + "h&until=now&order=asc&size=" + PAGE_SIZE, Map.class);

		Map<?, ?> rsid = (Map<?, ?>) response.get("rsid");
		String idForRetrieve = (String) rsid.get("id");

		Map<?, ?> queryData = retrievePage(idForRetrieve, 0);
		int totalEvents = (Integer) queryData.get("total_events");
		if (totalEvents > PAGE_SIZE) {
			for (int pageId = 1; pageId * PAGE_SIZE < totalEvents; pageId++) {
				retrievePage(idForRetrieve, pageId);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Map<?, ?> retrievePage(String idForRetrieve, int pageId) {
		Map<?, ?> queryData = restTemplate.getForObject(
				"https://" + config.logglyRetrievalHost + "/apiv2/events?rsid="
						+ Long.valueOf(idForRetrieve) + "&page=" + pageId,
				Map.class);

		Object events = queryData.get("events");
		for (Map<?, ?> event : (Iterable<Map<?, ?>>) events) {
			String logmsg = (String) event.get("logmsg");
			System.out.println(encryptor.decrypt(EncryptedOutput
					.fromString(logmsg)));

		}
		return queryData;
	}

	private static RestTemplate create(LogViewerConfig config) {
		CredentialsProvider credentials = new BasicCredentialsProvider();
		credentials.setCredentials(
				new AuthScope(config.logglyRetrievalHost, -1),
				new UsernamePasswordCredentials(config.logglyUsername,
						config.logglyPassword));
		return new RestTemplate(new HttpComponentsClientHttpRequestFactory(
				HttpClientBuilder.create()
						.setDefaultCredentialsProvider(credentials).build()));
	}
}
