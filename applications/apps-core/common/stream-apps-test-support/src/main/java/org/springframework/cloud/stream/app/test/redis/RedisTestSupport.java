///*
// * Copyright 2016 the original author or authors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package org.springframework.cloud.stream.app.test.redis;
//
//import org.springframework.cloud.stream.test.junit.AbstractExternalResourceTestSupport;
//import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
//
///**
// * Porting from https://github.com/spring-cloud/spring-cloud-stream/blob/1.0.x/spring-cloud-stream-test-support-internal
// *
// * @author Soby Chacko
// */
//public class RedisTestSupport extends AbstractExternalResourceTestSupport<LettuceConnectionFactory> {
//
//	public RedisTestSupport() {
//		super("REDIS");
//	}
//	@Override
//	protected void cleanupResource() throws Exception {
//		resource.destroy();
//	}
//
//	@Override
//	protected void obtainResource() throws Exception {
//		resource = new LettuceConnectionFactory();
//		resource.afterPropertiesSet();
//		resource.getConnection().close();
//	}
//}
