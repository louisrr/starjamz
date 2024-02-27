package com.play.stream.Starjams.UserService.config;

import com.datastax.oss.driver.api.core.type.TupleType;
import org.springframework.data.cassandra.core.convert.CassandraCustomConversions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Arrays;

// Configuration class to register the converters
@Configuration
public class CassandraConfig {

    @Bean
    public CassandraCustomConversions cassandraCustomConversions() {
        TupleType mockTupleType = null;
        return new CassandraCustomConversions(Arrays.asList(new TupleToDoubleArrayConverter(), new DoubleArrayToTupleConverter(/* Pass the TupleType here */mockTupleType)));
    }
}
