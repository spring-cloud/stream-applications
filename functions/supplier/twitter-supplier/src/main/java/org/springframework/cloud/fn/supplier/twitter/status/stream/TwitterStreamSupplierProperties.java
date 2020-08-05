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

package org.springframework.cloud.fn.supplier.twitter.status.stream;

import java.util.ArrayList;
import java.util.List;

import twitter4j.FilterQuery;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;

/**
 * @author Christian Tzolov
 */
@ConfigurationProperties("twitter.stream")
@Validated
public class TwitterStreamSupplierProperties {

	public enum StreamType {
		/** Starts listening on random sample of all public statuses. The default access level provides a small
		 * proportion of the Firehose. */
		sample,

		/** Start consuming public statuses that match one or more filter predicates. At least one predicate parameter,
		 * follow, locations, or track must be specified. Multiple parameters may be specified which allows most
		 * clients to use a single connection to the Streaming API. Placing long parameters in the URL may cause the
		 * request to be rejected for excessive URL length.<br>
		 * The default access level allows up to 200 track keywords, 400 follow userids and 10 1-degree location boxes.
		 * Increased access levels allow 80,000 follow userids ('shadow' role), 400,000 follow userids ('birddog' role),
		 * 10,000 track keywords ('restricted track' role),  200,000 track keywords ('partner track' role),
		 * and 200 10-degree location boxes ('locRestricted' role). Increased track access levels also pass a higher
		 * proportion of statuses before limiting the stream.*/
		filter,

		/** tarts listening on all public statuses. Available only to approved parties and requires a signed agreement
		 * to access. */
		firehose,

		/** Starts listening on all public statuses containing links.
		 * Available only to approved parties and requires a signed agreement to access.*/
		link
	}

	private StreamType type = StreamType.sample;

	private Filter filter = new Filter();

	public Filter getFilter() {
		return filter;
	}

	public StreamType getType() {
		return type;
	}

	public void setType(StreamType type) {
		this.type = type;
	}

	public static class Filter {

		public enum FilterLevel {
			/** filter level. */
			all, none, low, medium
		}

		/**
		 * Indicates the number of previous statuses to stream before transitioning to the live stream.
		 */
		private int count = 0;

		/**
		 * Specifies the users, by ID, to receive public tweets from.
		 */
		private List<Long> follow;

		/**
		 * Specifies keywords to track.
		 */
		private List<String> track;


		/**
		 * Locations to track. Internally represented as 2D array.
		 * Bounding box is invalid: 52.38, 4.90, 51.51, -0.12.  The first pair must be the SW corner of the box
		 */
		private List<BoundingBox> locations = new ArrayList<>();


		/**
		 * Specifies the tweets language of the stream.
		 */
		private List<String> language;

		/**
		 * The filter level limits what tweets appear in the stream to those with a minimum filterLevel attribute value.
		 * One of either none, low, or medium.
		 */
		private FilterLevel filterLevel = FilterLevel.all;

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public List<Long> getFollow() {
			return follow;
		}

		public void setFollow(List<Long> follow) {
			this.follow = follow;
		}

		public List<String> getTrack() {
			return track;
		}

		public void setTrack(List<String> track) {
			this.track = track;
		}

		public List<String> getLanguage() {
			return language;
		}

		public void setLanguage(List<String> language) {
			this.language = language;
		}

		public List<BoundingBox> getLocations() {
			return locations;
		}

		public FilterLevel getFilterLevel() {
			return filterLevel;
		}

		public void setFilterLevel(FilterLevel filterLevel) {
			this.filterLevel = filterLevel;
		}

		public FilterQuery toFilterQuery() {

			FilterQuery filterQuery = new FilterQuery();

			filterQuery.count(this.count);

			if (!CollectionUtils.isEmpty(this.track)) {
				filterQuery.track(String.join(",", this.track));
			}

			if (!CollectionUtils.isEmpty(this.follow)) {
				long[] followIds = new long[this.follow.size()];
				for (int i = 0; i < this.follow.size(); i++) {
					followIds[i] = this.follow.get(i);
				}
				filterQuery.follow(followIds);
			}

			if (!CollectionUtils.isEmpty(this.language)) {
				filterQuery.language(this.language.toArray(new String[this.language.size()]));
			}

			if (!CollectionUtils.isEmpty(this.locations)) {
				double[][] bboxLocations = new double[this.locations.size() * 2][2];
				for (int i = 0; i < this.locations.size(); i = i + 2) {
					//SW lat, lon
					bboxLocations[i][0] = this.locations.get(i).getSw().getLat();
					bboxLocations[i][1] = this.locations.get(i).getSw().getLon();
					//NE lat, lon
					bboxLocations[i + 1][0] = this.locations.get(i).getNe().getLat();
					bboxLocations[i + 1][1] = this.locations.get(i).getNe().getLon();
				}
				filterQuery.locations(bboxLocations);
			}

			if (this.filterLevel != FilterLevel.all) {
				filterQuery.filterLevel(this.filterLevel.name());
			}

			return filterQuery;
		}

		public boolean isValid() {
			return count > 0 || !CollectionUtils.isEmpty(this.track) || !CollectionUtils.isEmpty(this.follow)
					|| !CollectionUtils.isEmpty(this.language) || this.filterLevel != FilterLevel.all;
		}

		public static class BoundingBox {

			/**
			 * Bounding Box's South-West point (e.g. bottom-left).
			 */
			private Geocode sw;

			/**
			 * Bounding Box's North-East point (e.g. top-right).
			 */
			private Geocode ne;

			public Geocode getSw() {
				return sw;
			}

			public void setSw(Geocode sw) {
				this.sw = sw;
			}

			public Geocode getNe() {
				return ne;
			}

			public void setNe(Geocode ne) {
				this.ne = ne;
			}
		}

		public static class Geocode {

			/**
			 * latitude.
			 */
			private double lat = -1;

			/**
			 * longitude.
			 */
			private double lon = -1;


			public double getLat() {
				return lat;
			}

			public void setLat(double lat) {
				this.lat = lat;
			}

			public double getLon() {
				return lon;
			}

			public void setLon(double lon) {
				this.lon = lon;
			}
		}
	}
}
