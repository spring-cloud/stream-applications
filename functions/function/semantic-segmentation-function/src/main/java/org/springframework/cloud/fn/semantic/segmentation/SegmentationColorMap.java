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

package org.springframework.cloud.fn.semantic.segmentation;

import java.io.InputStream;
import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.core.io.DefaultResourceLoader;

/**
 *
 * Visualizes the segmentation results via specified color map.
 * Color maps helping to visualize the semantic segmentation results for the different datasets.
 *
 * Supported colormaps are:
 * - ADE20K (http://groups.csail.mit.edu/vision/datasets/ADE20K/).
 * - Cityscapes dataset (https://www.cityscapes-dataset.com).
 * - Mapillary Vistas (https://research.mapillary.com).
 * - PASCAL VOC 2012 (http://host.robots.ox.ac.uk/pascal/VOC/).
 *
 * Based on: https://github.com/tensorflow/models/blob/master/research/deeplab/utils/get_dataset_colormap.py
 *
 * @author Christian Tzolov
 */
public final class SegmentationColorMap {

	private SegmentationColorMap() {

	}

	/** MAPILLARY_COLORMAP . */
	public static final int[][] MAPILLARY_COLORMAP = new int[][] {
			{ 165, 42, 42 },
			{ 0, 192, 0 },
			{ 196, 196, 196 },
			{ 190, 153, 153 },
			{ 180, 165, 180 },
			{ 102, 102, 156 },
			{ 102, 102, 156 },
			{ 128, 64, 255 },
			{ 140, 140, 200 },
			{ 170, 170, 170 },
			{ 250, 170, 160 },
			{ 96, 96, 96 },
			{ 230, 150, 140 },
			{ 128, 64, 128 },
			{ 110, 110, 110 },
			{ 244, 35, 232 },
			{ 150, 100, 100 },
			{ 70, 70, 70 },
			{ 150, 120, 90 },
			{ 220, 20, 60 },
			{ 255, 0, 0 },
			{ 255, 0, 0 },
			{ 255, 0, 0 },
			{ 200, 128, 128 },
			{ 255, 255, 255 },
			{ 64, 170, 64 },
			{ 128, 64, 64 },
			{ 70, 130, 180 },
			{ 255, 255, 255 },
			{ 152, 251, 152 },
			{ 107, 142, 35 },
			{ 0, 170, 30 },
			{ 255, 255, 128 },
			{ 250, 0, 30 },
			{ 0, 0, 0 },
			{ 220, 220, 220 },
			{ 170, 170, 170 },
			{ 222, 40, 40 },
			{ 100, 170, 30 },
			{ 40, 40, 40 },
			{ 33, 33, 33 },
			{ 170, 170, 170 },
			{ 0, 0, 142 },
			{ 170, 170, 170 },
			{ 210, 170, 100 },
			{ 153, 153, 153 },
			{ 128, 128, 128 },
			{ 0, 0, 142 },
			{ 250, 170, 30 },
			{ 192, 192, 192 },
			{ 220, 220, 0 },
			{ 180, 165, 180 },
			{ 119, 11, 32 },
			{ 0, 0, 142 },
			{ 0, 60, 100 },
			{ 0, 0, 142 },
			{ 0, 0, 90 },
			{ 0, 0, 230 },
			{ 0, 80, 100 },
			{ 128, 64, 64 },
			{ 0, 0, 110 },
			{ 0, 0, 70 },
			{ 0, 0, 192 },
			{ 32, 32, 32 },
			{ 0, 0, 0 },
			{ 0, 0, 0 },
	};

	/**
	 * Label colormap used in ADE20K segmentation benchmark.
	 */
	public static final int[][] ADE20K_COLORMAP = new int[][] {
			{ 0, 0, 0 },
			{ 120, 120, 120 },
			{ 180, 120, 120 },
			{ 6, 230, 230 },
			{ 80, 50, 50 },
			{ 4, 200, 3 },
			{ 120, 120, 80 },
			{ 140, 140, 140 },
			{ 204, 5, 255 },
			{ 230, 230, 230 },
			{ 4, 250, 7 },
			{ 224, 5, 255 },
			{ 235, 255, 7 },
			{ 150, 5, 61 },
			{ 120, 120, 70 },
			{ 8, 255, 51 },
			{ 255, 6, 82 },
			{ 143, 255, 140 },
			{ 204, 255, 4 },
			{ 255, 51, 7 },
			{ 204, 70, 3 },
			{ 0, 102, 200 },
			{ 61, 230, 250 },
			{ 255, 6, 51 },
			{ 11, 102, 255 },
			{ 255, 7, 71 },
			{ 255, 9, 224 },
			{ 9, 7, 230 },
			{ 220, 220, 220 },
			{ 255, 9, 92 },
			{ 112, 9, 255 },
			{ 8, 255, 214 },
			{ 7, 255, 224 },
			{ 255, 184, 6 },
			{ 10, 255, 71 },
			{ 255, 41, 10 },
			{ 7, 255, 255 },
			{ 224, 255, 8 },
			{ 102, 8, 255 },
			{ 255, 61, 6 },
			{ 255, 194, 7 },
			{ 255, 122, 8 },
			{ 0, 255, 20 },
			{ 255, 8, 41 },
			{ 255, 5, 153 },
			{ 6, 51, 255 },
			{ 235, 12, 255 },
			{ 160, 150, 20 },
			{ 0, 163, 255 },
			{ 140, 140, 140 },
			{ 250, 10, 15 },
			{ 20, 255, 0 },
			{ 31, 255, 0 },
			{ 255, 31, 0 },
			{ 255, 224, 0 },
			{ 153, 255, 0 },
			{ 0, 0, 255 },
			{ 255, 71, 0 },
			{ 0, 235, 255 },
			{ 0, 173, 255 },
			{ 31, 0, 255 },
			{ 11, 200, 200 },
			{ 255, 82, 0 },
			{ 0, 255, 245 },
			{ 0, 61, 255 },
			{ 0, 255, 112 },
			{ 0, 255, 133 },
			{ 255, 0, 0 },
			{ 255, 163, 0 },
			{ 255, 102, 0 },
			{ 194, 255, 0 },
			{ 0, 143, 255 },
			{ 51, 255, 0 },
			{ 0, 82, 255 },
			{ 0, 255, 41 },
			{ 0, 255, 173 },
			{ 10, 0, 255 },
			{ 173, 255, 0 },
			{ 0, 255, 153 },
			{ 255, 92, 0 },
			{ 255, 0, 255 },
			{ 255, 0, 245 },
			{ 255, 0, 102 },
			{ 255, 173, 0 },
			{ 255, 0, 20 },
			{ 255, 184, 184 },
			{ 0, 31, 255 },
			{ 0, 255, 61 },
			{ 0, 71, 255 },
			{ 255, 0, 204 },
			{ 0, 255, 194 },
			{ 0, 255, 82 },
			{ 0, 10, 255 },
			{ 0, 112, 255 },
			{ 51, 0, 255 },
			{ 0, 194, 255 },
			{ 0, 122, 255 },
			{ 0, 255, 163 },
			{ 255, 153, 0 },
			{ 0, 255, 10 },
			{ 255, 112, 0 },
			{ 143, 255, 0 },
			{ 82, 0, 255 },
			{ 163, 255, 0 },
			{ 255, 235, 0 },
			{ 8, 184, 170 },
			{ 133, 0, 255 },
			{ 0, 255, 92 },
			{ 184, 0, 255 },
			{ 255, 0, 31 },
			{ 0, 184, 255 },
			{ 0, 214, 255 },
			{ 255, 0, 112 },
			{ 92, 255, 0 },
			{ 0, 224, 255 },
			{ 112, 224, 255 },
			{ 70, 184, 160 },
			{ 163, 0, 255 },
			{ 153, 0, 255 },
			{ 71, 255, 0 },
			{ 255, 0, 163 },
			{ 255, 204, 0 },
			{ 255, 0, 143 },
			{ 0, 255, 235 },
			{ 133, 255, 0 },
			{ 255, 0, 235 },
			{ 245, 0, 255 },
			{ 255, 0, 122 },
			{ 255, 245, 0 },
			{ 10, 190, 212 },
			{ 214, 255, 0 },
			{ 0, 204, 255 },
			{ 20, 0, 255 },
			{ 255, 255, 0 },
			{ 0, 153, 255 },
			{ 0, 41, 255 },
			{ 0, 255, 204 },
			{ 41, 0, 255 },
			{ 41, 255, 0 },
			{ 173, 0, 255 },
			{ 0, 245, 255 },
			{ 71, 0, 255 },
			{ 122, 0, 255 },
			{ 0, 255, 184 },
			{ 0, 92, 255 },
			{ 184, 255, 0 },
			{ 0, 133, 255 },
			{ 255, 214, 0 },
			{ 25, 194, 194 },
			{ 102, 255, 0 },
			{ 92, 0, 255 },
	};

	/** BLACK_WHITE_COLORMAP . */
	public static int[][] BLACK_WHITE_COLORMAP = new int[][] {
			{ 0, 0, 0 },
			{ 127, 127, 127 },
			{ 255, 255, 255 },
	};

	/** CITYMAP_COLORMAP . */
	public static final int[][] CITYMAP_COLORMAP = new int[255][3];

	static {

		// Initialize citymap
		int[][] _CITYMAP_COLORMAP = new int[][] {
				{ 128, 64, 128 },
				{ 244, 35, 232 },
				{ 70, 70, 70 },
				{ 102, 102, 156 },
				{ 190, 153, 153 },
				{ 153, 153, 153 },
				{ 250, 170, 30 },
				{ 220, 220, 0 },
				{ 107, 142, 35 },
				{ 152, 251, 152 },
				{ 70, 130, 180 },
				{ 220, 20, 60 },
				{ 255, 0, 0 },
				{ 0, 0, 142 },
				{ 0, 0, 70 },
				{ 0, 60, 100 },
				{ 0, 80, 100 },
				{ 0, 0, 230 },
				{ 119, 11, 32 }
		};

		for (int i = 0; i < _CITYMAP_COLORMAP.length; i++) {
			System.arraycopy(_CITYMAP_COLORMAP[i], 0, CITYMAP_COLORMAP[i], 0, _CITYMAP_COLORMAP[i].length);
		}
	}

	public static int[][] loadColorMap(String resourceUri) {
		try {
			InputStream colorMapIs = new DefaultResourceLoader().getResource(resourceUri).getInputStream();
			ColorMap colorMap = new ObjectMapper().readValue(colorMapIs, ColorMap.class);
			return colorMap.getColormap();
		}
		catch (Exception exception) {
			throw new RuntimeException(exception);
		}
	}

	public static class ColorMap {
		private String name;
		private String info;
		private int[][] colormap;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getInfo() {
			return info;
		}

		public void setInfo(String info) {
			this.info = info;
		}

		public int[][] getColormap() {
			return colormap;
		}

		public void setColormap(int[][] colormap) {
			this.colormap = colormap;
		}

		@Override
		public String toString() {
			return "ColorMap{" +
					"name='" + name + '\'' +
					"info='" + info + '\'' +
					", colormap=" + Arrays.deepToString(colormap) +
					'}';
		}
	}
}
