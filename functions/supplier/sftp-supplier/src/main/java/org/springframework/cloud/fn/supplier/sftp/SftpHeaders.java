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

package org.springframework.cloud.fn.supplier.sftp;

/**
 * @author David Turanski
 **/
public abstract class SftpHeaders {
	/**
	 * The SFTP host.
	 */
	static String SFTP_HOST_PROPERTY_KEY = "sftp_host";

	/**
	 * The SFTP port.
	 */
	static String SFTP_PORT_PROPERTY_KEY = "sftp_port";

	/**
	 * The SFTP username.
	 */
	static String SFTP_USERNAME_PROPERTY_KEY = "sftp_username";

	/**
	 * The SFTP password.
	 */
	static String SFTP_PASSWORD_PROPERTY_KEY = "sftp_password";

	/**
	 * The SFTP selected server if multi-source.
	 */
	static String SFTP_SELECTED_SERVER_PROPERTY_KEY = "sftp_selectedServer";
}
