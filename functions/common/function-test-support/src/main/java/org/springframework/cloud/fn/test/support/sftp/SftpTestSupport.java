/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.cloud.fn.test.support.sftp;

import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Collections;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.springframework.cloud.fn.test.support.file.remote.RemoteFileTestSupport;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Base64Utils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * Provides an embedded SFTP Server for test cases.
 *
 * @author David Turanski
 * @author Gary Russell
 * @author Artem Bilan
 */
public class SftpTestSupport extends RemoteFileTestSupport {

	private static SshServer server;

	@Override
	public String prefix() {
		return "sftp";
	}

	@BeforeAll
	public static void createServer() throws Exception {
		server = SshServer.setUpDefaultServer();
		server.setPasswordAuthenticator((username, password, session) ->
				StringUtils.hasText(password) && !"badPassword".equals(password)); // fail if pub key validation failed
		server.setPublickeyAuthenticator((username, key, session) -> key.equals(decodePublicKey("id_rsa_pp.pub")));
		server.setPort(0);
		server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser").toPath()));
		server.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
		server.setFileSystemFactory(new VirtualFileSystemFactory(remoteTemporaryFolder));
		server.start();
		System.setProperty("sftp.factory.port", String.valueOf(server.getPort()));
		System.setProperty("sftp.consumer.localDir",
				localTemporaryFolder + File.separator + "localTarget");
	}

	@AfterAll
	public static void stopServer() throws Exception {
		server.stop();
		File hostkey = new File("hostkey.ser");
		if (hostkey.exists()) {
			hostkey.delete();
		}
		System.clearProperty("sftp.factory.port");
		System.clearProperty("sftp.consumer.localDir");
	}

	private static PublicKey decodePublicKey(String key) {
		try {
			InputStream stream = new ClassPathResource(key).getInputStream();
			byte[] keyBytes = FileCopyUtils.copyToByteArray(stream);
			// strip any newline chars
			while (keyBytes[keyBytes.length - 1] == 0x0a || keyBytes[keyBytes.length - 1] == 0x0d) {
				keyBytes = Arrays.copyOf(keyBytes, keyBytes.length - 1);
			}
			byte[] decodeBuffer = Base64Utils.decode(keyBytes);
			ByteBuffer bb = ByteBuffer.wrap(decodeBuffer);
			int len = bb.getInt();
			byte[] type = new byte[len];
			bb.get(type);
			if ("ssh-rsa".equals(new String(type))) {
				BigInteger e = decodeBigInt(bb);
				BigInteger m = decodeBigInt(bb);
				RSAPublicKeySpec spec = new RSAPublicKeySpec(m, e);
				return KeyFactory.getInstance("RSA").generatePublic(spec);

			}
			else {
				throw new IllegalArgumentException("Only supports RSA");
			}
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to determine the test public key", e);
		}
	}

	private static BigInteger decodeBigInt(ByteBuffer bb) {
		int len = bb.getInt();
		byte[] bytes = new byte[len];
		bb.get(bytes);
		return new BigInteger(bytes);
	}
}
