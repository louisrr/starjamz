package com.play.stream.Starjams.GatewayService.Services;

import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaPublisherService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper mapper;

    public KafkaPublisherService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper mapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.mapper = mapper;
    }

    public void publishScyllaDbResultsToKafka(ResultSet resultSet, String topicName) {
        for (Row row : resultSet) {
            try {
                String message = mapper.writeValueAsString(row.getFormattedContents());
                kafkaTemplate.send(new ProducerRecord<>(topicName, message));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to publish Scylla row to Kafka", e);
            }
        }
    }
}
