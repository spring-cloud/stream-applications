/*
 * Copyright 2020-2020 the original author or authors.
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

package org.springframework.cloud.fn.supplier.mail;

import java.io.UnsupportedEncodingException;
import java.util.List;

import com.icegreen.greenmail.util.GreenMailUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.integration.mail.MailHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "mail.supplier.url=imap://user:pw@localhost:${test.mail.server.imap.port}/INBOX")
public class ImapPassTests extends AbstractMailSupplierTests {


	@Test
	public void testSimpleTest() throws UnsupportedEncodingException, MessagingException {
		// given
		MimeMessage mailMessage = GreenMailUtil.createTextEmail("bar@foo", "test@test", "test", "foo", mailServer.getSmtp().getServerSetup());
		mailMessage.addRecipients(jakarta.mail.Message.RecipientType.TO, new InternetAddress[]{new InternetAddress("foo@bar", "Foo")});
		mailMessage.addRecipients(jakarta.mail.Message.RecipientType.CC, new InternetAddress[]{new InternetAddress("a@b"), new InternetAddress("c@d")});
		mailMessage.addRecipients(jakarta.mail.Message.RecipientType.BCC, new InternetAddress[]{new InternetAddress("e@f"), new InternetAddress("g@h")});
		mailUser.deliver(mailMessage);
		// when
		final Flux<Message<?>> messageFlux = mailSupplier.get();
		// then
		StepVerifier.create(messageFlux)
			.assertNext((message) -> {
					assertThat(((String) message.getPayload())).isEqualTo("foo");
					MessageHeaders headers = message.getHeaders();
					assertThat(headers.get(MailHeaders.TO)).isInstanceOf(List.class);
					assertThat(headers.get(MailHeaders.CC)).isInstanceOf(List.class);
					assertThat(headers.get(MailHeaders.BCC)).isInstanceOf(List.class);
					assertThat(headers.get(MailHeaders.TO).toString()).isEqualTo("[bar@foo, Foo <foo@bar>]");
					assertThat(headers.get(MailHeaders.CC).toString()).isEqualTo("[a@b, c@d]");
					assertThat(headers.get(MailHeaders.BCC).toString()).isEqualTo("[e@f, g@h]");
				}
			)
			.thenCancel()
			.verify();
	}
}
