/*
 * Copyright 2020-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.fn.supplier.http;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;

import javax.net.ssl.SSLException;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.AbstractJackson2Decoder;
import org.springframework.integration.http.HttpHeaders;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.webflux.inbound.WebFluxInboundEndpoint;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

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
				"server.ssl.client-auth=want",
				"spring.codec.max-in-memory-size=10MB"
		})
@DirtiesContext
public class HttpSupplierApplicationTests {

	@Autowired
	private Supplier<Flux<Message<byte[]>>> httpSupplier;

	@Autowired
	private WebFluxInboundEndpoint webFluxInboundEndpoint;

	@LocalServerPort
	private int port;

	@Test
	public void testHttpSupplier() throws SSLException {
		ServerCodecConfigurer codecConfigurer =
				TestUtils.getPropertyValue(this.webFluxInboundEndpoint, "codecConfigurer", ServerCodecConfigurer.class);

		final ServerCodecConfigurer.ServerDefaultCodecs serverDefaultCodecs = codecConfigurer.defaultCodecs();
		assertThat(TestUtils.getPropertyValue(serverDefaultCodecs, "maxInMemorySize", Integer.class))
				.isEqualTo(1024 * 1024 * 10);

		AbstractJackson2Decoder jackson2JsonDecoder =
				TestUtils.getPropertyValue(serverDefaultCodecs, "jackson2JsonDecoder", AbstractJackson2Decoder.class);

		assertThat(jackson2JsonDecoder).isNotNull()
				.extracting("maxInMemorySize")
				.isEqualTo(1024 * 1024 * 10);

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
						.assertNext((message) ->
								assertThat(message)
										.satisfies((msg) -> assertThat(msg)
												.extracting(Message::getPayload)
												.asInstanceOf(InstanceOfAssertFactories.MAP)
												.isEmpty())
										.satisfies((msg) -> assertThat(msg.getHeaders())
												.doesNotContainKey(MessageHeaders.CONTENT_TYPE)))
						.thenCancel()
						.verifyLater();

		SslContext sslContext =
				SslContextBuilder.forClient()
						.sslProvider(SslProvider.JDK)
						.trustManager(InsecureTrustManagerFactory.INSTANCE)
						.build();

		HttpClient httpClient =
				HttpClient.create()
						.secure(sslSpec -> sslSpec.sslContext(sslContext));

		WebClient webClient =
				WebClient.builder()
						.clientConnector(new ReactorClientHttpConnector(httpClient))
						.baseUrl("https://localhost:" + port)
						.build();

		webClient.post()
				.uri("/")
				.bodyValue("test1")
				.retrieve()
				.toBodilessEntity()
				.block(Duration.ofSeconds(10));

		webClient.post()
				.uri("/")
				.bodyValue(new TestPojo("test2"))
				.retrieve()
				.toBodilessEntity()
				.block(Duration.ofSeconds(10));

		webClient.post()
				.uri("/")
				.bodyValue(new TestPojo("test3"))
				.retrieve()
				.toBodilessEntity()
				.block(Duration.ofSeconds(10));

		webClient.post().uri("/")
				.retrieve()
				.toBodilessEntity()
				.block(Duration.ofSeconds(10));

		stepVerifier.verify();
	}

	private static class TestPojo {

		private String name;

		TestPojo() {
		}

		TestPojo(String name) {
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
	static class HttpSupplierTestApplication {

	}

}
