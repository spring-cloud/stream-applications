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

package org.springframework.cloud.fn.image.recognition.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/**
 * Create a text file mapping label id to human readable string.
 *
 * Produces a text file where every line represents single category. The line number represents the category id, while
 * the line text is human-readable names for the categories fromMemory this imagenet id.
 *
 * Based on https://github.com/tensorflow/models/blob/master/research/slim/datasets/imagenet.py#L66
 *
 *   We retrieve a synset file, which contains a list of valid synset labels used
 *   by ILSVRC competition. There is one synset one per line, eg.
 *           #   n01440764
 *           #   n01443537
 *   We also retrieve a synset_to_human_file, which contains a mapping from synsets
 *   to human-readable names for every synset in Imagenet. These are stored in a
 *   tsv format, as follows:
 *           #   n02119247    black fox
 *           #   n02119359    silver fox
 *   We assign each synset (in alphabetical order) an integer, starting from 1
 *   (since 0 is reserved for the background class)
 *
 * @author Christian Tzolov
 */
public final class ImageNetReadableNamesWriter {

	private ImageNetReadableNamesWriter() {
	}

	/** BASE_URL. */
	public final static String BASE_URL = "https://raw.githubusercontent.com/tensorflow/models/master/research/inception/inception/data/";
	/** SYNSET_URI. */
	public final static String SYNSET_URI = BASE_URL + "imagenet_lsvrc_2015_synsets.txt";
	/** SYNSET_TO_HUMAN_URI. */
	public final static String SYNSET_TO_HUMAN_URI = BASE_URL + "imagenet_metadata.txt";

	public static void main(String[] args) {
		Charset utf8 = Charset.forName("UTF-8");

		try (InputStream synsetIs = toResource(SYNSET_URI).getInputStream();
			InputStream synsetToHumanIs = toResource(SYNSET_TO_HUMAN_URI).getInputStream()) {

			List<String> synsetList = Arrays.asList(StreamUtils.copyToString(synsetIs, utf8)
					.split("\n")).stream().map(l -> l.trim()).collect(Collectors.toList());
			Assert.notNull(synsetList, "Failed to initialize the labels list");
			Assert.isTrue(synsetList.size() == 1000, "Labels list is expected to be of " +
					"size 1000 but was:" + synsetList.size());

			Map<String, String> synsetToHuman = Arrays.asList(StreamUtils.copyToString(synsetToHumanIs, utf8)
					.split("\n")).stream().map(s2h -> s2h.split("\t")).collect(Collectors.toMap(s -> s[0], s -> s[1]));
			Assert.notNull(synsetToHuman, "Failed to initialize the synsetToHuman");
			Assert.isTrue(synsetToHuman.size() == 21842, "synsetToHuman is expected to be of " +
					"size 21842 but was:" + synsetToHuman.size());

			List<String> l = synsetList.stream().map(id -> synsetToHuman.get(id)).collect(Collectors.toList());

			List<String> ll = new ArrayList<>();
			ll.add("dummy");
			ll.addAll(l);
			System.out.println(ll.get(389));
			FileUtils.writeLines(new File("labels.txt"), ll);

		}
		catch (IOException e) {
			throw new RuntimeException("Failed to initialize the Vocabulary", e);
		}
	}

	public static Resource toResource(String uri) {
		return new DefaultResourceLoader().getResource(uri);
	}
}
