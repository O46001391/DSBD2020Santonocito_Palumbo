package dsbd2020.lab.paymentmicroservice.mongo;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import dsbd2020.lab.paymentmicroservice.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

/**
 * Classe di configurazione per Mongo. Nello specifico definiremo un @Bean necessario
 * per fornire all'ambiente un'istanza specifica di TransactionManager da utilizzare
 * per la nostra applicazione.
 */
@Configuration
@EnableReactiveMongoRepositories(basePackageClasses= PaymentRepository.class)
public class MongoConfiguration extends AbstractReactiveMongoConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoDBUri;

    @Value("${mongoDBName}")
    private String mongoDBName;

    /**
     * Definendo un Bean per l'istanziazione del TransactionManager da utilizzare, stiamo
     * stabilendo che tipologie di transazioni dovranno essere gestite (nel nostro caso
     * transazioni basate su esiti di operazioni legate alla pubblicazione di uno
     * specifico publisher immerso in pipe reattive). Le nostre transazioni copriranno tutte
     * le operazioni reattive, al patto di seguire uno dei seguenti 3 approcci:
     * 1) NON DICHIARATIVO:
     *    - Si utilizza un'istanza di un TransactionalOperator creato a partire da un Reactive
     *      Transaction Manager in modo esplicito usando il metodo:
     *      (publisher sotto controllo della transazione) ...
     *      ...transactionalOperator.execute(publisher da considerare nella transazione);
     *    - Si utilizza un'istanza di un TransactionalOperator creato a partire da un Reactive
     *      Transaction Manager in modo esplicito usando il metodo:
     *      (publisher da considerare nella transazione).as(transactionalOperator::Transactional).
     * 2) DICHIARATIVO (QUELLO SEGUITO IN QUESTA APPLICAZIONE):
     *    - Non si usa esplitamente un Transactional Operator ma si annota con @Transactional
     *      direttamente il metodo (o la classe come insieme di metodi) da considerare
     *      transazionali (è stato scelto poiché più compatto, veloce e simile a quanto fatto
     *      negli esempi in aula).
     * @param rmdf
     * @return
     */
    @Bean
    ReactiveMongoTransactionManager transactionManager(ReactiveMongoDatabaseFactory rmdf) {
        return new ReactiveMongoTransactionManager(rmdf);
    }

    @Override
    protected String getDatabaseName() {
        return this.mongoDBName;
    }

    @Override
    @Bean
    public MongoClient reactiveMongoClient() {
        return MongoClients.create(this.mongoDBUri);
    }

    /*
	@Bean
	TransactionalOperator transactionalOperator(ReactiveTransactionManager rtm) {
		return TransactionalOperator.create(rtm);
	}*/

}
