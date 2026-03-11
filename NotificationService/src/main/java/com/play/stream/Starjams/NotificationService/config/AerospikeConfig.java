package com.play.stream.Starjams.NotificationService.config;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.policy.ClientPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AerospikeConfig {

    @Value("${aerospike.host:localhost}")
    private String host;

    @Value("${aerospike.port:3000}")
    private int port;

    @Bean(destroyMethod = "close")
    public IAerospikeClient aerospikeClient() {
        ClientPolicy policy = new ClientPolicy();
        policy.timeout = 5000;
        return new AerospikeClient(policy, host, port);
    }
}
