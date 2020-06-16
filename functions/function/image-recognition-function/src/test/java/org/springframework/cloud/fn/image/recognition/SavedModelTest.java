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

package org.springframework.cloud.fn.image.recognition;

import java.util.Map;


import com.google.protobuf.InvalidProtocolBufferException;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.SignatureDef;

/**
 * @author Christian Tzolov
 */
public final class SavedModelTest {

	private SavedModelTest() {

	}

	/**
	 * https://medium.com/@jsflo.dev/saving-and-loading-a-tensorflow-model-using-the-savedmodel-api-17645576527
	 *
	 * https://www.tensorflow.org/alpha/guide/saved_model
	 *
	 */
	public static void main(String[] args) throws InvalidProtocolBufferException {
		SavedModelBundle savedModelBundle =
				SavedModelBundle.load("/Users/ctzolov/Downloads/ssd_mobilenet_v1_coco_2017_11_17/saved_model", "serve");
				//SavedModelBundle.load("/Users/ctzolov/Downloads/aiy_vision_classifier_plants_V1_1/", "serve");
		//SavedModelBundle savedModelBundle =
		//		SavedModelBundle.load("/Users/ctzolov/Downloads/mnasnet-a1/saved_model", "serve");

		MetaGraphDef meta = MetaGraphDef.parseFrom(savedModelBundle.metaGraphDef());

		Map<String, SignatureDef> signatures = meta.getSignatureDefMap();

		System.out.println(signatures);

		savedModelBundle.session();

		//Iterator<Operation> itr = savedModelBundle.graph().operations();
		//
		//while (itr.hasNext()) {
		//	System.out.println("Operation: " + itr.next());
		//}
	}
}
