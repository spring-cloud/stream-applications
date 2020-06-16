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

package org.springframework.cloud.fn.object.detection.domain;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Christian Tzolov
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ObjectDetection {

	private String name;
	private float confidence;
	private float x1;
	private float y1;
	private float x2;
	private float y2;
	private float[][] mask;
	private int cid;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public float getConfidence() {
		return confidence;
	}

	public void setConfidence(float confidence) {
		this.confidence = confidence;
	}

	public float getX1() {
		return x1;
	}

	public void setX1(float x1) {
		this.x1 = x1;
	}

	public float getY1() {
		return y1;
	}

	public void setY1(float y1) {
		this.y1 = y1;
	}

	public float getX2() {
		return x2;
	}

	public void setX2(float x2) {
		this.x2 = x2;
	}

	public float getY2() {
		return y2;
	}

	public void setY2(float y2) {
		this.y2 = y2;
	}

	public int getCid() {
		return cid;
	}

	public void setCid(int cid) {
		this.cid = cid;
	}

	public float[][] getMask() {
		return mask;
	}

	public void setMask(float[][] mask) {
		this.mask = mask;
	}

	@Override
	public String toString() {
		return "ObjectDetection{" +
				"name='" + name + '\'' +
				", confidence=" + confidence +
				", x1=" + x1 +
				", y1=" + y1 +
				", x2=" + x2 +
				", y2=" + y2 +
				", mask=" + Arrays.toString(mask) +
				", cid=" + cid +
				'}';
	}
}
