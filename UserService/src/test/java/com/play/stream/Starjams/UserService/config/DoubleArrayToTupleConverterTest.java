package com.play.stream.Starjams.UserService.config;

import com.datastax.oss.driver.api.core.data.TupleValue;
import com.datastax.oss.driver.api.core.type.TupleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

public class DoubleArrayToTupleConverterTest {

    @Mock
    private TupleType mockTupleType;

    @Mock
    private TupleValue mockTupleValue;

    private DoubleArrayToTupleConverter converter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockTupleType.newValue(anyDouble(), anyDouble())).thenReturn(mockTupleValue);
        converter = new DoubleArrayToTupleConverter(mockTupleType);
    }

    @Test
    public void convert_ValidDoubleArray_ReturnsTupleValue() {
        // Given a valid double array
        double[] source = new double[]{12.34, 56.78};

        // When converting
        TupleValue result = converter.convert(source);

        // Then a TupleValue should be returned with the correct values
        assertNotNull(result, "The result should not be null.");
        verify(mockTupleType).newValue(source[0], source[1]);
    }
}
