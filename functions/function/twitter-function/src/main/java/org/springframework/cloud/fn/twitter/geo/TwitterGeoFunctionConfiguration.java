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

package org.springframework.cloud.fn.twitter.geo;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import twitter4j.GeoLocation;
import twitter4j.GeoQuery;
import twitter4j.Place;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.common.twitter.TwitterConnectionConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;

/**
 *
 * @author Christian Tzolov
 */
@Configuration
@EnableConfigurationProperties(TwitterGeoFunctionProperties.class)
@Import(TwitterConnectionConfiguration.class)
public class TwitterGeoFunctionConfiguration {

	private static final Log logger = LogFactory.getLog(TwitterGeoFunctionConfiguration.class);

	@Bean
	public Function<Message<?>, GeoQuery> messageToGeoQueryFunction(TwitterGeoFunctionProperties geoProperties) {
		return message -> {
			String ip = null;
			if (geoProperties.getSearch().getIp() != null) {
				ip = geoProperties.getSearch().getIp().getValue(message, String.class);
			}
			GeoLocation geoLocation = null;
			if (geoProperties.getLocation().getLat() != null && geoProperties.getLocation().getLon() != null) {
				Double lat = geoProperties.getLocation().getLat().getValue(message, Double.class);
				Double lon = geoProperties.getLocation().getLon().getValue(message, Double.class);
				geoLocation = new GeoLocation(lat, lon);
			}

			String query = null;
			if (geoProperties.getSearch().getQuery() != null) {
				query = geoProperties.getSearch().getQuery().getValue(message, String.class);
			}
			GeoQuery geoQuery = new GeoQuery(query, ip, geoLocation);

			geoQuery.setMaxResults(geoProperties.getMaxResults());
			geoQuery.setAccuracy(geoProperties.getAccuracy());
			geoQuery.setGranularity(geoProperties.getGranularity());

			return geoQuery;
		};
	}

	@Bean
	@ConditionalOnProperty(name = "twitter.geo.search.type", havingValue = "search", matchIfMissing = true)
	public Function<GeoQuery, List<Place>> twitterSearchPlacesFunction(Twitter twitter) {
		return geoQuery -> {
			try {
				return twitter.searchPlaces(geoQuery);
			}
			catch (TwitterException e) {
				logger.error("Places Search failed!", e);
			}
			return null;
		};
	}

	@Bean
	@ConditionalOnProperty(name = "twitter.geo.search.type", havingValue = "reverse")
	public Function<GeoQuery, List<Place>> twitterReverseGeocodeFunction(Twitter twitter) {
		return geoQuery -> {
			try {
				return twitter.reverseGeoCode(geoQuery);
			}
			catch (TwitterException e) {
				logger.error("Reverse Geocode failed!", e);
			}
			return null;
		};
	}

	@Bean
	public Function<Message<?>, Message<byte[]>> twitterGeoFunction(
			Function<Message<?>, GeoQuery> toGeoQuery,
			Function<GeoQuery, List<Place>> places,
			Function<Object, Message<byte[]>> managedJson) {

		return toGeoQuery.andThen(places).andThen(managedJson)::apply;
	}
}
