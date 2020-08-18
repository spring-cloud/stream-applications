/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.cloud.fn.supplier.syslog;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.Supplier;

import javax.net.SocketFactory;

import reactor.core.publisher.Flux;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.integration.syslog.inbound.UdpSyslogReceivingChannelAdapter;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(properties = "syslog.supplier.port = 0")
@DirtiesContext
public class AbstractSyslogSupplierTests {

	protected static final String RFC3164_PACKET = "<157>JUL 26 22:08:35 WEBERN TESTING[70729]: TEST SYSLOG MESSAGE";

	protected static final String RFC5424_PACKET =
			"<14>1 2014-06-20T09:14:07+00:00 loggregator d0602076-b14a-4c55-852a-981e7afeed38 DEA - "
					+ "[exampleSDID@32473 iut=\\\"3\\\" eventSource=\\\"Application\\\" eventID=\\\"1011\\\"]"
					+ "[exampleSDID@32473 iut=\\\"3\\\" eventSource=\\\"Application\\\" eventID=\\\"1011\\\"] "
					+ "Removing instance";

	@Autowired
	Supplier<Flux<Message<?>>> syslogSupplier;

	@Autowired
	protected SyslogSupplierProperties properties;

	@Autowired(required = false)
	protected AbstractServerConnectionFactory connectionFactory;

	@Autowired(required = false)
	protected UdpSyslogReceivingChannelAdapter udpAdapter;

	protected void sendTcp(String syslog) throws Exception {
		int port = getPort();
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write(syslog.getBytes());
		socket.close();
	}

	private int getPort() throws Exception {
		int n = 0;
		while (n++ < 100 && !this.connectionFactory.isListening()) {
			Thread.sleep(100);
		}
		assertThat(this.connectionFactory.isListening()).isTrue();
		int port = this.connectionFactory.getPort();
		assertThat(port > 0).isTrue();
		return port;
	}

	protected void sendUdp(String syslog) throws Exception {
		int port = waitUdp();
		DatagramSocket socket = new DatagramSocket();
		DatagramPacket packet = new DatagramPacket(syslog.getBytes(), syslog.length());
		packet.setSocketAddress(new InetSocketAddress("localhost", port));
		socket.send(packet);
		socket.close();
	}

	private int waitUdp() throws Exception {
		int n = 0;
		DirectFieldAccessor dfa = new DirectFieldAccessor(this.udpAdapter);
		while (n++ < 100 && !((UnicastReceivingChannelAdapter) dfa.getPropertyValue("udpAdapter")).isListening()) {
			Thread.sleep(100);
		}
		return ((UnicastReceivingChannelAdapter) dfa.getPropertyValue("udpAdapter")).getPort();
	}

	@SpringBootApplication
	public static class SyslogSupplierTestApplication {

	}

}
