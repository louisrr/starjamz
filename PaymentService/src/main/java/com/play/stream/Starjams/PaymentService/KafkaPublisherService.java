package com.play.stream.Starjams.PaymentService;


import org.springframework.beans.factory.annotation.Autowired;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Properties;


@Service
public class KafkaPublisherService {

    @Autowired
    private Producer<String, String> producer;

    public KafkaPublisherService() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        this.producer = new KafkaProducer<>(props);
    }

    public void publishScyllaDbResultsToKafka(ResultSet resultSet, String topicName) {
        ObjectMapper mapper = new ObjectMapper();

        for (Row row : resultSet) {
            try {
                String message = mapper.writeValueAsString(row); // Simplifying conversion
                producer.send(new ProducerRecord<>(topicName, message));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Ensure to close the producer to free resources
    public void closeProducer() {
        if (producer != null) {
            producer.close();
        }
    }
}
