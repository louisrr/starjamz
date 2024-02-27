package com.play.stream.Starjams.UserService.config;

import com.datastax.oss.driver.api.core.data.TupleValue;
import com.datastax.oss.driver.api.core.type.TupleType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

// Converter from double[] to TupleValue
@WritingConverter
public class DoubleArrayToTupleConverter implements Converter<double[], TupleValue> {
    private TupleType tupleType = null; // You need to initialize this with the correct TupleType

    public DoubleArrayToTupleConverter(TupleType mockTupleType) {
        this.tupleType = tupleType;
    }

    @Override
    public TupleValue convert(double[] source) {
        return tupleType.newValue(source[0], source[1]);
    }
}
