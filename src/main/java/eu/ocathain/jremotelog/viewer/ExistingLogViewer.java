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

	private static final int PAGE_SIZE = 1000;

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Expected 1 argument: properties file location");
			System.exit(1);
		}

		File propertiesFile = new File(args[0]);
		StartupChecks.checkPropertiesFileExistence(args, propertiesFile);

		LogViewerConfig config = LogViewerConfig.loadFromFile(propertiesFile);
		new ExistingLogViewer(config).retrieveLast12Hours();
	}

	private final LogViewerConfig config;

	private final Encryptor encryptor;

	private final RestTemplate restTemplate;

	public ExistingLogViewer(LogViewerConfig config) {
		this.config = config;
		encryptor = new Encryptor(config);
		restTemplate = create(config);
	}

	void retrieveLast12Hours() {

		Map<?, ?> response = restTemplate.getForObject("https://"
				+ config.logglyRetrievalHost
				+ "/apiv2/fields?q=*&from=-12h&until=now&order=asc&size="
				+ PAGE_SIZE, Map.class);

		Map<?, ?> rsid = (Map<?, ?>) response.get("rsid");
		String idForRetrieve = (String) rsid.get("id");

		Map<?, ?> queryData = retrievePage(config, restTemplate, idForRetrieve,
				0, encryptor);
		int totalEvents = (Integer) queryData.get("total_events");
		if (totalEvents > PAGE_SIZE) {
			for (int pageId = 1; pageId * PAGE_SIZE < totalEvents; pageId++) {
				retrievePage(config, restTemplate, idForRetrieve, pageId,
						encryptor);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<?, ?> retrievePage(LogViewerConfig config,
			RestTemplate restTemplate, String idForRetrieve, int pageId,
			Encryptor encryptor) {
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
