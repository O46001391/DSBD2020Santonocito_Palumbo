package dsbd2020.lab.paymentmicroservice.subscriber;

import dsbd2020.lab.paymentmicroservice.service.PaymentService;
import org.reactivestreams.Subscription;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;

/**
 * Classe che modella un Subscriber personalizzato. Nel nostro caso lo
 * useremo per sottoscriverci e gestire la pubblicazione della ServerResponse
 * associata all'Happy-Path del metodo ipn() all'interno del REST Reactive
 * Adapter.
 * @param <T>
 */
public class ServerResponseSubscriber<T> extends BaseSubscriber<T> {

    private PaymentService paymentService;

    private ServerRequest serverRequest;

    private MultiValueMap<String, String> params;

    public ServerResponseSubscriber(ServerRequest serverRequest, MultiValueMap<String, String> params, PaymentService paymentService) {
        this.params = params;
        this.serverRequest = serverRequest;
        this.paymentService = paymentService;
    }

    /**
     * Metodo eseguito all'atto della sottoscrizione di questo subscriber
     * ad un qualsiasi Mono o Flux.
     * @param subscription
     */
    @Override
    public void hookOnSubscribe(Subscription subscription) {
        request(1);
    }

    /**
     * Metodo eseguito all'atto della pubblicazione del publisher
     * a cui questo subscriber sarà sottoscritto.
     * @param value
     */
    @Override
    public void hookOnNext(T value) {
        // Richiediamo un dato dal publisher.
        request(1);
        // Verifichiamo che la serverRequest contenga ciò di cui abbiamo
        // bisogno.
        if(paymentService != null && serverRequest != null && params != null) {
            // Andiamo ad utilizzare il nostro servizio IPN.
            Mono<Void> response = paymentService
                    .ipn(
                            serverRequest.remoteAddress().get().getHostName(),
                            serverRequest.path(),
                            serverRequest.method().toString(),
                            params
                    );
            // Occorre effettuare una sottoscrizione per far sì che le chiamate
            // asincrone reattive di IPN vengano eseguite (in questo caso si tratta di un Mono<Void>
            // quindi di fatto ci sottoscriviamo semplicemente all'evento di pubblicazione
            // corretta da parte del publisher, ma non ci aspettiamo alcun valore specifico).
            response.subscribe();
        }
    }

}
