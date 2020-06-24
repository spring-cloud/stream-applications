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

package org.springframework.cloud.fn.common.twitter.util;


import java.io.IOException;
import java.nio.charset.Charset;
import java.util.function.Function;

import twitter4j.conf.ConfigurationBuilder;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.StreamUtils;

/**
 * @author Christian Tzolov
 */
public class TwitterTestUtils {

	public Function<ConfigurationBuilder, ConfigurationBuilder> mockTwitterUrls(String baseUrl) {
		return configBuilder -> {
			configBuilder.setRestBaseURL(baseUrl + "/");
			configBuilder.setStreamBaseURL(baseUrl + "/stream/");
			configBuilder.setUserStreamBaseURL(baseUrl + "/user/");
			configBuilder.setSiteStreamBaseURL(baseUrl + "/site/");
			configBuilder.setUploadBaseURL(baseUrl + "/upload/");

			configBuilder.setOAuthAccessTokenURL(baseUrl + "/oauth/access_token");
			configBuilder.setOAuthAuthenticationURL(baseUrl + "/oauth/authenticate");
			configBuilder.setOAuthAuthorizationURL(baseUrl + "/oauth/authorize");
			configBuilder.setOAuthRequestTokenURL(baseUrl + "/oauth/request_token");
			configBuilder.setOAuth2TokenURL(baseUrl + "/oauth2/token");
			configBuilder.setOAuth2InvalidateTokenURL(baseUrl + "/oauth2/invalidate_token");

			return configBuilder;
		};
	}

	/**
	 * Load Spring Resource as String.
	 * @param resourcePath Resource path (accepts file:// , classpath:// and http:// uri schemas)
	 * @return Returns text (UTF8) representation of the resource pointed by the resourcePath
	 */
	public static String asString(String resourcePath) {
		try {
			return StreamUtils.copyToString(new DefaultResourceLoader().getResource(resourcePath).getInputStream(),
					Charset.forName("UTF-8"));
		}
		catch (IOException e) {
			throw new RuntimeException("Can not load resource:" + resourcePath, e);
		}
	}

}
