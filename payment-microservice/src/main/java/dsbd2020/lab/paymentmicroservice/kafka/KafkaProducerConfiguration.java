package dsbd2020.lab.paymentmicroservice.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.kafka.sender.SenderOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe di configurazione del produttore Kafka (utilizzato per
 * la pubblicazione sui vari topic dalla nostra applicazione).
 */
@Configuration
public class KafkaProducerConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafkaOrdersTopic}")
    private String ordersTopic;

    @Value("${kafkaLoggingTopic}")
    private String loggingTopic;

    /**
     * Bean per la creazione della configurazione del
     * produttore.
     * @return
     */
    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);
        return properties;
    }

    /**
     * Bean per la creazione del Kafka template da utilizzare (nel nostro caso
     * reactive) per la pubblicazione reattiva dei messaggi su Kafka.
     * @return
     */
    @Bean
    public ReactiveKafkaProducerTemplate<String, String> reactiveKafkaTemplate() {
        return new ReactiveKafkaProducerTemplate<>(SenderOptions.create(producerConfigs()));
    }

    /**
     * Bean per la creazione (se non esiste) del topic "orders" su Kafka.
     * @return
     */
    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name(this.ordersTopic).build();
    }

    /**
     * Bean per la creazione (se non esiste) del topic "logging" su Kafka.
     * @return
     */
    @Bean
    public NewTopic loggingTopic() {
        return TopicBuilder.name(this.loggingTopic).build();
    }

}
