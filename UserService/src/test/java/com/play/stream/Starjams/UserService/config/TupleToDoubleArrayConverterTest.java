package com.play.stream.Starjams.UserService.config;

import com.datastax.oss.driver.api.core.data.TupleValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class TupleToDoubleArrayConverterTest {

    @Mock
    private TupleValue mockTupleValue;

    private TupleToDoubleArrayConverter converter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        converter = new TupleToDoubleArrayConverter();
    }

    @Test
    public void convert_ValidTupleValue_ReturnsDoubleArray() {
        // Setup mock to return specific doubles
        when(mockTupleValue.getDouble(0)).thenReturn(12.34);
        when(mockTupleValue.getDouble(1)).thenReturn(56.78);

        // Execute the converter with the mock TupleValue
        double[] result = converter.convert(mockTupleValue);

        // Verify the result
        assertArrayEquals(new double[]{12.34, 56.78}, result, "The converted array does not match the expected values.");
    }
}
