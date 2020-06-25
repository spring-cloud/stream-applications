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

package org.springframework.cloud.fn.common.tensorflow;

import java.util.Iterator;

import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.op.Ops;

import org.springframework.cloud.fn.common.tensorflow.util.CachedModelExtractor;
import org.springframework.cloud.fn.common.tensorflow.util.ModelExtractor;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

/**
 * @author Christian Tzolov
 */
public class ProtoBufGraphDefinition implements GraphDefinition {

	/**
	 * Location of the pre-trained model archive.
	 */
	private final Resource modelLocation;

	/**
	 * If set true the pre-trained model is cached on the local file system.
	 */
	private final boolean cacheModel;

	public ProtoBufGraphDefinition(String modelUri, boolean cacheModel) {
		this(new DefaultResourceLoader().getResource(modelUri), cacheModel);
	}

	public ProtoBufGraphDefinition(Resource modelLocation, boolean cacheModel) {
		this.modelLocation = modelLocation;
		this.cacheModel = cacheModel;
	}

	@Override
	public void defineGraph(Ops tf) {
		// Extract the pre-trained model as byte array.
		byte[] model = this.cacheModel ? new CachedModelExtractor().getModel(this.modelLocation)
				: new ModelExtractor().getModel(this.modelLocation);
		// Import the pre-trained model
		((Graph) tf.scope().env()).importGraphDef(model);
		//try {
		//	((Graph) tf.scope().env()).importGraphDef(GraphDef.parseFrom(model));
		//}
		//catch (InvalidProtocolBufferException e) {
		//	throw new RuntimeException(e);
		//}

		Graph graph = ((Graph) tf.scope().env());
		Iterator<Operation> ops = graph.operations();
		while (ops.hasNext()) {
			System.out.println(ops.next().name());
		}
	}
}
