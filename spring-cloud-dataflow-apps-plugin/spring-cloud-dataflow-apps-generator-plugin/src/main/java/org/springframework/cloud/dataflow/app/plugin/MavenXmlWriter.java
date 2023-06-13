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

package org.springframework.cloud.dataflow.app.plugin;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.MXSerializer;
import org.codehaus.plexus.util.xml.pull.XmlSerializer;

/**
 * Uses the private MavenXpp3Writer write methods to convert Model elements into XML strings.
 *
 * @author Christian Tzolov
 */
public final class MavenXmlWriter {

	private MavenXmlWriter() {

	}

	/**
	 * Serializes any instance of a e.g. org.apache.maven.model.XXXX class into XML text.
	 * Via reflections calls the private MavenXpp3Writer#writeXXXX(XXXX, String, XmlSerializer) method.
	 * As xml tag uses the lower case of the XXXX class name (e.g. xxxx).
	 * <p>
	 * If above name conventions don't hold or additional processing is required implement a dedicated overload method.
	 * For example see {@link #toXml(Plugin)}
	 *
	 * @param element instance of a org.apache.maven.model.* class.
	 * @param <T> element type.
	 * @return XML text serialization of the element.
	 */
	public static <T> String toXml(T element) {
		String privateMethodName = "write" + element.getClass().getSimpleName();
		String xmlTagName = element.getClass().getSimpleName().toLowerCase();
		return write(serializer -> invokeMavenXppWriteMethod(element, privateMethodName, xmlTagName, serializer));
	}

	/**
	 * Serializes an {@link Plugin} instance into XML text, using reflection to invoke the private
	 * MavenXpp3Writer#writePlugin(Plugin, String, XmlSerializer) method. The Plugin Configuration block (when present)
	 * requires special handling to convert String values into Xpp3Dom object before writing.
	 * <p>
	 * For Plugin Configuration you must nest the configuration content into a CDATA block like shown here:
	 * <pre>{@code
	 * <additionalPlugins>
	 *   <plugin>
	 *     <groupId>com.google.cloud.tools</groupId>
	 *     <artifactId>jib-maven-plugin</artifactId>
	 *     <version>3.3.0</version>
	 *     <configuration>
	 *       <![CDATA[
	 *         <from><image>springcloud/baseimage:1.0.4</image></from>
	 *         <to>
	 *           <image>springcloudstream:${project.artifactId}</image>
	 *           <tags><tag>latest</tag></tags>
	 *         </to>
	 *         <container>
	 *           <format>Docker</format>
	 *         </container>
	 *       ]]>
	 *     </configuration>
	 *   </plugin>
	 * </additionalPlugins>
	 * }</pre>
	 *
	 * @param plugin Plugin instance to serialize.
	 * @return POM/XML serialization of the plugin.
	 */
	public static String toXml(Plugin plugin) {
		try {
			Object configuration = plugin.getConfiguration();
			if (configuration != null && !(configuration instanceof Xpp3Dom)) {
				plugin.setConfiguration(Xpp3DomBuilder.build(new StringReader(
						"<configuration>" + configuration.toString() + "</configuration>")));
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Issue creating config for additional plugin", ex);
		}

		return write(serializer -> invokeMavenXppWriteMethod(
				plugin, "writePlugin", "plugin", serializer));
	}

	/**
	 * Template method that helps generating well-formed and valid XML segments. It uses the {@link XmlSerializer}
	 * to create the create the XML structure and delegates to the #elementWriter callback to fill in the content.
	 *
	 * @param elementWriter Callback handler that contributes the XML content.
	 * @return Returns well-formed XML element.
	 */
	public static String write(Consumer<XmlSerializer> elementWriter) {
		try {
			Writer writer = new StringWriter();

			XmlSerializer serializer = new MXSerializer();
			serializer.setProperty("http://xmlpull.org/v1/doc/properties.html#serializer-indentation", "  ");
			serializer.setProperty("http://xmlpull.org/v1/doc/properties.html#serializer-line-separator", "\n");
			serializer.setOutput(writer);

			serializer.startDocument("UTF-8", null);
			elementWriter.accept(serializer);
			serializer.endDocument();

			String result = writer.toString();
			return result.substring(result.indexOf('\n') + 1); // remove first line (e.g. remove the <?xml ... ?>)
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Uses the private MavenXpp3Writer methods to convert Model elements into XML strings.
	 *
	 * @param modelElementToWrite a parameterizable org.apache.maven.model.T instance to serialize.
	 * @param <T>                 The actual org.apache.maven.model's class type.
	 * @param writeMethodName     Name of the private {@link MavenXpp3Writer} method to call.
	 * @param xmlTagName          Name for the XML tag to surround the serialized model instance.
	 * @param serializer          The XmlSerializer used to write the xml.
	 */
	public static <T> void invokeMavenXppWriteMethod(T modelElementToWrite, String writeMethodName,
			String xmlTagName, XmlSerializer serializer) {

		try {
			MavenXpp3Writer pomWriter = new MavenXpp3Writer();
			Method method = pomWriter.getClass().getDeclaredMethod(
					writeMethodName, modelElementToWrite.getClass(), String.class, XmlSerializer.class);
			method.setAccessible(true); // allow invoking private method.
			method.invoke(pomWriter, modelElementToWrite, xmlTagName, serializer);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Indent the input string by the provided  indentation steps.
	 * @param input String to indent.
	 * @param indentation Number of indentation steps.
	 * @return Returns the formatted string.
	 */
	public static String indent(String input, int indentation) {
		String indentPrefix = "\n" + IntStream.range(0, indentation).mapToObj(i -> " ").collect(Collectors.joining());
		String indentedInput = input.replace("\n", indentPrefix);
		return indentedInput.substring(0, indentedInput.lastIndexOf(indentPrefix)); // remove the last empty line.
	}
}
