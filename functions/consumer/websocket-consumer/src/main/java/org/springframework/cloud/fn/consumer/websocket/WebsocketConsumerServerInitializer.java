/*
 * Copyright 2014-2020 the original author or authors.
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

package org.springframework.cloud.fn.consumer.websocket;

import java.security.cert.CertificateException;

import javax.net.ssl.SSLException;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.fn.consumer.websocket.trace.InMemoryTraceRepository;

/**
 * Does some basic initialization and setup.
 *
 * <ul>
 * <li>Configure the {@link SslContext} based on {@link WebsocketConsumerProperties#ssl}</li>
 * <li>add the {@link WebsocketConsumerServerHandler} to the underlying {@link ChannelPipeline}</li>
 * </ul>
 *
 * @author Oliver Moser
 * @author Gary Russell
 * @author Artem Bilan
 */
public class WebsocketConsumerServerInitializer extends ChannelInitializer<SocketChannel> {

	/**
	 * Max content length.
	 */
	public static final int MAX_CONTENT_LENGTH = 65536;

	private final InMemoryTraceRepository traceRepository;

	@Autowired
	private WebsocketConsumerProperties properties;

	@Value("${endpoints.websocketsinktrace.enabled:false}")
	private boolean traceEnabled;

	public WebsocketConsumerServerInitializer(InMemoryTraceRepository traceRepository) {
		this.traceRepository = traceRepository;
	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();

		final SslContext sslCtx = configureSslContext();
		if (sslCtx != null) {
			pipeline.addLast(sslCtx.newHandler(ch.alloc()));
		}

		pipeline.addLast(new HttpServerCodec());
		pipeline.addLast(new HttpObjectAggregator(MAX_CONTENT_LENGTH));
		pipeline.addLast(new WebsocketConsumerServerHandler(this.traceRepository, this.properties, this.traceEnabled));
	}

	private SslContext configureSslContext() throws CertificateException, SSLException {
		if (this.properties.isSsl()) {
			SelfSignedCertificate ssc = new SelfSignedCertificate();
			return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
		}
		else {
			return null;
		}
	}
}
