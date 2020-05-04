package org.springframework.cloud.fn.consumer.counter;

import java.util.function.Function;

import org.springframework.core.convert.converter.Converter;

/**
 * @author Christian Tzolov
 */
public class ConverterFunctionAdapter<S, T>  implements Converter<S, T> {

	private Function<S, T> function;

	public ConverterFunctionAdapter(Function<S, T> function) {
		this.function = function;
	}

	@Override
	public T convert(S s) {
		return this.function.apply(s);
	}
}
