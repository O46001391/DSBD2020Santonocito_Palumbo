package dsbd2020.lab.paymentmicroservice.repository;

import dsbd2020.lab.paymentmicroservice.model.Payment;
import org.bson.types.ObjectId;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * Creazione dell'interfaccia per avere a disposizione un Reactive CRUD Repository:
 * Repository che effettua operazioni CRUD sul DB in modo reattivo (l'esito dell'operazione
 * sul DB è il valore pubblicato da un publisher di tipo Mono o Flux in reazione alla
 * sottoscrizione di un subscriber) => ATTENZIONE: QUANDO SI USANO SUBSCRIBER PERSONALIZZATI
 * OCCORRE TENERE IN CONSIDERAZIONE CHE SE IL PUBLISHER FA PARTE DI UNA PIPE REATTIVA
 * A CUI SI SOTTOSCRIVERA' UN SOTTOSCRITTORE DI DEFAULT (AD ESEMPIO QUANDO FA PARTE
 * DI UNA PIPE CHE PORTA ALLA CREAZIONE DI UNA MONO<SERVER-RESPONSE>) TALI SUBSCRIBER
 * IMPORRANNO UN'OPERAZIONE SUL DB CIASCUNO (QUINDI SE E' UN SAVE OTTERREMO DUE SAVE,
 * AD ESEMPIO).
 */
public interface PaymentRepository extends ReactiveCrudRepository<Payment, ObjectId> {

    /**
     * Permettiamo l'autogenerazione di un metodo reattivo per il recupero dell'elenco
     * di pagamenti tra un UnixTimestamp di "from" ed uno di "to".
     * @param from
     * @param to
     * @return
     */
    Flux<Payment> findPaymentsByUnixTimestampBetween(long from, long to);

}
