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

package org.springframework.cloud.fn.object.detection;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.BiFunction;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.fn.common.tensorflow.deprecated.GraphicsUtils;
import org.springframework.cloud.fn.object.detection.domain.ObjectDetection;
import org.springframework.util.CollectionUtils;

/**
 * Augment the input image fromMemory detected object bounding boxes and categories.
 * For mask models and withMask set to true it draws the instance segmentation image as well.
 *
 * @author Christian Tzolov
 */
public class ObjectDetectionImageAugmenter implements BiFunction<byte[], List<ObjectDetection>, byte[]> {

	private static final Log logger = LogFactory.getLog(ObjectDetectionImageAugmenter.class);

	/** Make checkstyle happy. **/
	public static final String DEFAULT_IMAGE_FORMAT = "jpg";

	private String imageFormat = DEFAULT_IMAGE_FORMAT;

	private final boolean withMask;
	private boolean agnosticColors = false;

	public ObjectDetectionImageAugmenter() {
		this(false);
	}

	public ObjectDetectionImageAugmenter(boolean withMask) {
		this.withMask = withMask;
	}

	public boolean isAgnosticColors() {
		return agnosticColors;
	}

	public void setAgnosticColors(boolean agnosticColors) {
		this.agnosticColors = agnosticColors;
	}

	public String getImageFormat() {
		return imageFormat;
	}

	public void setImageFormat(String imageFormat) {
		this.imageFormat = imageFormat;
	}

	@Override
	public byte[] apply(byte[] imageBytes, List<ObjectDetection> objectDetections) {

		if (!CollectionUtils.isEmpty(objectDetections)) {
			try {
				BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));

				for (ObjectDetection od : objectDetections) {
					int y1 = (int) (od.getY1() * (float) bufferedImage.getHeight());
					int x1 = (int) (od.getX1() * (float) bufferedImage.getWidth());
					int y2 = (int) (od.getY2() * (float) bufferedImage.getHeight());
					int x2 = (int) (od.getX2() * (float) bufferedImage.getWidth());

					int cid = od.getCid();

					String labelName = od.getName();
					int probability = (int) (100 * od.getConfidence());
					String title = labelName + ": " + probability + "%";

					GraphicsUtils.drawBoundingBox(bufferedImage, cid, title, x1, y1, x2, y2, this.agnosticColors);

					if (this.withMask && od.getMask() != null) {
						float[][] mask = od.getMask();
						if (mask != null) {
							Color maskColor = this.agnosticColors ? null : GraphicsUtils.getClassColor(cid);
							BufferedImage maskImage = GraphicsUtils.createMaskImage(
									mask, x2 - x1, y2 - y1, maskColor);
							GraphicsUtils.overlayImages(bufferedImage, maskImage, x1, y1);
						}
					}
				}

				imageBytes = GraphicsUtils.toImageByteArray(bufferedImage, this.getImageFormat());
			}
			catch (IOException e) {
				logger.error(e);
			}
		}

		// Null mend that QR image is found and not output message will be send.
		return imageBytes;
	}
}
