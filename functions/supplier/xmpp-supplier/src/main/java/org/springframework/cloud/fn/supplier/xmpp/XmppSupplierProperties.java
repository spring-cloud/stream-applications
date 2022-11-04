/*
 * Copyright 2014-2022 the original author or authors.
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

package org.springframework.cloud.fn.supplier.xmpp;

import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.expression.Expression;
import org.springframework.validation.annotation.Validated;

/**
 *
 * @author Daniel Frey
 * @since 4.0.0
 */
@ConfigurationProperties("xmpp.supplier")
@Validated
public class XmppSupplierProperties {

	private StanzaFilter stanzaFilter = StanzaTypeFilter.MESSAGE;

	private Expression payloadExpression;

	public void setStanzaFilter(StanzaFilter stanzaFilter) {
		this.stanzaFilter = stanzaFilter;
	}

	public StanzaFilter getStanzaFilter() {
		return stanzaFilter;
	}

	public void setPayloadExpression(Expression payloadExpression) {
		this.payloadExpression = payloadExpression;
	}
	public Expression getPayloadExpression() {
		return payloadExpression;
	}

}
