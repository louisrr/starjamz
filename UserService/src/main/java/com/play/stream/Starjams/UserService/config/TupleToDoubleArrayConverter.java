package com.play.stream.Starjams.UserService.config;

import com.datastax.oss.driver.api.core.data.TupleValue;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

// Converter from TupleValue to double[]
@ReadingConverter
public class TupleToDoubleArrayConverter implements Converter<TupleValue, double[]> {
    @Override
    public double[] convert(TupleValue source) {
        return new double[]{source.getDouble(0), source.getDouble(1)};
    }
}
