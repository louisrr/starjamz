package com.play.stream.Starjams.PaymentService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class KafkaPublisherService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void publishScyllaDbResultsToKafka(ResultSet resultSet, String topicName) {
        for (Row row : resultSet) {
            // Convert the row to a String or JSON format
            String message = convertRowToMessage(row);
            // Publish the message to the Kafka topic
            kafkaTemplate.send(topicName, message);
        }
    }

    private String convertRowToMessage(Row row) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();

        // Assuming the row has columns: id, name, email, etc.
        // Adjust the field names and types according to your actual row structure
        if (row != null) {
            node.put("id", row.getUuid("id").toString());
            node.put("name", row.getString("name"));
            node.put("email", row.getString("email"));
            // Add other fields as needed
        }

        try {
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            e.printStackTrace();
            return "{}"; // Return an empty JSON object in case of an error
        }
    }
}
