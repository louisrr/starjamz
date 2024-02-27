package com.play.stream.Starjams.UserService.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.cassandra.core.convert.CassandraCustomConversions;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = CassandraConfig.class)
public class CassandraConfigTest {

    @Autowired
    private ApplicationContext context;

    @Test
    public void cassandraCustomConversionsBeanExists() {
        assertTrue(context.containsBean("cassandraCustomConversions"),
                "The cassandraCustomConversions bean should be defined in the application context");

        CassandraCustomConversions conversions = context.getBean(CassandraCustomConversions.class);
        assertNotNull(conversions, "The cassandraCustomConversions bean should not be null");
    }
}
