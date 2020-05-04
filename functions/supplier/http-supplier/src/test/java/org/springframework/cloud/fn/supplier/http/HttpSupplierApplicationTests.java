package org.springframework.cloud.fn.supplier.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.integration.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

/**
 * The test for HTTP Supplier.
 *
 * @author Artem Bilan
 */
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"server.ssl.key-store=classpath:test.jks",
				"server.ssl.key-password=password",
				"server.ssl.trust-store=classpath:test.jks",
				"server.ssl.client-auth=want"
		})
public class HttpSupplierApplicationTests {

	@Autowired
	private Supplier<Flux<Message<byte[]>>> httpSupplier;

	@LocalServerPort
	private int port;

	@Test
	public void testHttpSupplier() {
		Flux<Message<byte[]>> messageFlux = this.httpSupplier.get();

		StepVerifier stepVerifier =
				StepVerifier.create(messageFlux)
						.assertNext((message) ->
								assertThat(message)
										.satisfies((msg) -> assertThat(msg)
												.extracting(Message::getPayload)
												.isEqualTo("test1".getBytes()))
										.satisfies((msg) -> assertThat(msg.getHeaders())
												.containsEntry(MessageHeaders.CONTENT_TYPE,
														new MediaType("text", "plain", StandardCharsets.UTF_8))
												.extractingByKey(HttpHeaders.REQUEST_URL).asString()
												.startsWith("https://"))
						)
						.assertNext((message) ->
								assertThat(message)
										.extracting(Message::getPayload)
										.isEqualTo("{\"name\":\"test2\"}".getBytes()))
						.assertNext((message) ->
								assertThat(message)
										.extracting(Message::getPayload)
										.isEqualTo("{\"name\":\"test3\"}".getBytes()))
						.thenCancel()
						.verifyLater();

		HttpClient httpClient =
				HttpClient.create()
						.secure(sslSpec ->
								sslSpec.sslContext(SslContextBuilder.forClient()
										.sslProvider(SslProvider.JDK)
										.trustManager(InsecureTrustManagerFactory.INSTANCE)));

		WebClient webClient =
				WebClient.builder()
						.clientConnector(new ReactorClientHttpConnector(httpClient))
						.baseUrl("https://localhost:" + port)
						.build();

		WebClient.RequestBodySpec requestBodySpec = webClient.post().uri("/");
		requestBodySpec.bodyValue("test1").exchange().block(Duration.ofSeconds(10));
		requestBodySpec.bodyValue(new TestPojo("test2")).exchange().block(Duration.ofSeconds(10));
		requestBodySpec.bodyValue(new TestPojo("test3")).exchange().block(Duration.ofSeconds(10));

		stepVerifier.verify();
	}

	private static class TestPojo {

		private String name;

		public TestPojo() {
		}

		public TestPojo(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	@SpringBootApplication
	static class TestApplication { }

}
