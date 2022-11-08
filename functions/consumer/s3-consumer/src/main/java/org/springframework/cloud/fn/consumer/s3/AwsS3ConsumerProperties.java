/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.cloud.fn.consumer.s3;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import jakarta.validation.constraints.AssertTrue;
import org.hibernate.validator.constraints.Length;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;
import org.springframework.validation.annotation.Validated;

/**
 * @author Artem Bilan
 */
@ConfigurationProperties("s3.consumer")
@Validated
public class AwsS3ConsumerProperties {

	/**
	 * AWS bucket for target file(s) to store.
	 */
	private String bucket;

	/**
	 * Expression to evaluate AWS bucket name.
	 */
	private Expression bucketExpression;

	/**
	 * Expression to evaluate S3 Object key.
	 */
	private Expression keyExpression;

	/**
	 * S3 Object access control list.
	 */
	private CannedAccessControlList acl;

	/**
	 * Expression to evaluate S3 Object access control list.
	 */
	private Expression aclExpression;

	@Length(min = 3)
	public String getBucket() {
		return this.bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public Expression getBucketExpression() {
		return this.bucketExpression;
	}

	public void setBucketExpression(Expression bucketExpression) {
		this.bucketExpression = bucketExpression;
	}

	public Expression getKeyExpression() {
		return this.keyExpression;
	}

	public void setKeyExpression(Expression keyExpression) {
		this.keyExpression = keyExpression;
	}

	public CannedAccessControlList getAcl() {
		return this.acl;
	}

	public void setAcl(CannedAccessControlList acl) {
		this.acl = acl;
	}

	public Expression getAclExpression() {
		return this.aclExpression;
	}

	public void setAclExpression(Expression aclExpression) {
		this.aclExpression = aclExpression;
	}

	@AssertTrue(message = "Exactly one of 'bucket' or 'bucketExpression' must be set")
	public boolean isMutuallyExclusiveBucketAndBucketExpression() {
		return (this.bucket != null && this.bucketExpression == null) ||
				(this.bucket == null && this.bucketExpression != null);
	}

	@AssertTrue(message = "Only one of 'acl' or 'aclExpression' must be set")
	public boolean isMutuallyExclusiveAclAndAclExpression() {
		return this.acl == null || this.aclExpression == null;
	}
}
