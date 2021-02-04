package dsbd2020.lab.paymentmicroservice.adapter;

import dsbd2020.lab.paymentmicroservice.model.Payment;
import dsbd2020.lab.paymentmicroservice.service.PaymentService;
import dsbd2020.lab.paymentmicroservice.subscriber.ServerResponseSubscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapter REST Reactive per l'implementazione del nostro servizio.
 */
@Component
public class RESTAdapter {

    @Autowired
    PaymentService paymentService;

    @Value("${kafkaHTTPError}")
    private String kafkaHTTPErrorKey;

    @Value("${kafkaLoggingTopic}")
    private String loggingTopic;

    @Value("${kafkaBadIpnError}")
    private String kafkaBadIpnError;

    /**
     * Per il caricamento nel DB di Payments per la fase di testing.
     * @param serverRequest
     * @return
     */
    public Mono<ServerResponse> savePayment(ServerRequest serverRequest) {
        return ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(serverRequest.bodyToMono(Payment.class)
                        .flatMap(payment -> paymentService.savePayment(payment)), Payment.class);
    }


    /**
     * Metodo per la gestione dell'Istant Payment Notification di Paypal
     * (nel nostro caso l'IPN Simulator di Sandbox).
     * @param serverRequest
     * @return
     */
    public Mono<ServerResponse> ipn(ServerRequest serverRequest) {

        // Recuperiamo il "Content-Type" dall'intestazione della richiesta.
        Optional<MediaType> contentType = serverRequest.headers().contentType();

        // Dobbiamo gestire il caso in cui l'header "Content-Type" : "application/x-www-form-urlencoded"
        // non sia presente ( o magari sia presente una tipologia di contenuto differente).
        if (!contentType.isPresent() || !contentType.get().isCompatibleWith(MediaType.parseMediaType(MediaType.APPLICATION_FORM_URLENCODED_VALUE))) {
            // Siamo certi che la richiesta ricevuta non sia ottimale per il nostro
            // servizio.
            Logger.getGlobal().log(Level.WARNING, "Error -> (400): Bad Request...");

            // Creiamo un opportuno publisher Mono<ServerResponse> andando a sfruttare la
            // seguente pipe reattiva.
            // REACTIVE PIPE DI RISPOSTA:
            // 1) Creiamo una Server Response;
            // 2) Ne settiamo lo stato a 400;
            // 3.a) Creiamo un Mono<ServerResponse> a partire da un Mono<Void> che
            //      viene restituito dal metodo "sendLog".
            // 3.b) Specifichiamo che, in caso di fallimento, vogliamo che venga
            //      restituito un Mono<Void> comunque, cosicché anche in caso di fallimento
            //      della "sendLog" siamo certi che la ServerResponse 400 venga gestita
            //      dal codice reattivo che si sottoscriverà ad essa senza problemi.
            return ServerResponse
                    .ok()
                    .build(paymentService
                            .sendLog(
                                    serverRequest.remoteAddress().get().getHostName(),
                                    serverRequest.path(),
                                    serverRequest.method().toString(),
                                    "400",
                                    new Exception(),
                                    loggingTopic,
                                    kafkaBadIpnError)
                            .onErrorResume((err) -> Mono.empty()));
        }
        // Dobbiamo gestire il caso in cui il body della richiesta
        // non è conforme a Payment.class (che è la nostra classe di Serializzazione/
        // Deserializzazione creata appositamente). Oltre alla gestione di tale
        // caso dobbiamo prevedere la creazione di un opportuno publisher Mono<ServerResponse>
        // che restituisca, indipendentemente dai casi, una HTTP response 200-OK (anche se
        // dietro le quinte effettua opportuni log di eventuali errori verificatisi).
        // REACTIVE PIPE DI RISPOSTA:
        // 1) Partiamo dall'estrazione del body della ServerRequest da gestire;
        // 2) Estriamo il body sotto forma di Mono<MultiValueMap<String,String>> attraverso
        //    l'uso di "formData()" -> (questo è possibile poiché sappiamo che il body
        //    è formattato come x-www-form-urlencode);
        // 3) Lo scopo della pipe è quello di arrivare ad una Mono<ServerResponse>, quindi
        //    immaginando che i valori del Mono ottenuti al punto (2) siano pubblicati
        //    immediatamente appena effettuiamo la successiva invocazione nella pipe ("formData()"
        //    recupera valori già presenti nella ServerRequest), utilizziamo "flatMap" con una
        //    lambda function per creare una Mono<ServerResponse> e contestualmente sottoscrivere
        //    a tale publisher un nostro sottoscittore che utilizzerà il servizio IPN all'interno
        //    del nostro PaymentService -> l'handler del nostro sottoscrittore verrà invocato
        //    non appena la ServerResponse verrà pubblicata dal sottoscrittore di default del
        //    Reactor.Core.
        // 4) Alla nostra Mono<ServerResponse> aggiungiamo un metodo di gestione dell'errore che
        //    può presentarsi nell'estrazione effettuata da "formData()" (che prevede una serializzazione
        //    opportuna in Map dei valori del body della richiesta). Anche in questo caso, come da specifica,
        //    la response HTTP è sempre 200-OK ma il reale esito è registrato sui log di kafka.
        // N.B. => Il nostro subscriber avrà un handler che si occuperà di completare l'interazione
        //         a 4 vie con il sistema di IPN:
        //         Viste qui abbiamo le prime due:
        //         (1) IPN ----------------------------> /ipn
        //         (2) /ipn ---------------------------> IPN
        //         Il subscriber al completamento delle due (in modo strettamente sincrono) eseguirà le ultime due fasi:
        //         (3) subscriber ---------------------> IPN
        //         (4) IPN ----------------------------> subscriber
        //         (5) => Opportuno logging su kafka in base all'esito della IPN.
        return serverRequest
                .formData()
                .flatMap(params -> {
                    Mono<ServerResponse> sr = ServerResponse.status(200).build();
                    sr.subscribe(new ServerResponseSubscriber<>(serverRequest, params, paymentService));
                    return sr;
                })
                .onErrorResume((e) -> {
                    Logger.getGlobal().log(Level.WARNING, "Error -> (400): Bad Request...");
                    return ServerResponse
                            .ok()
                            .build(paymentService
                                    .sendLog(
                                            serverRequest.remoteAddress().get().getAddress().toString(),
                                            serverRequest.path(),
                                            serverRequest.method().toString(),
                                            "400",
                                            new Exception(),
                                            loggingTopic,
                                            kafkaHTTPErrorKey)
                                    .onErrorResume((err) -> Mono.empty()));
                });
    }


    /**
     * Permette all'utente ADMIN (UserID=0) di ottenere le transazioni che sono
     * state correttamente registrate nel DB in uno specifico intervallo di tempo.
     * @param serverRequest
     * @return
     */
    public Mono<ServerResponse> getTransactions(ServerRequest serverRequest) {

        // Vengono recuperati i parametri URL della HTTP GET:
        Optional<String> from = serverRequest.queryParam("fromTimestamp");
        Optional<String> to = serverRequest.queryParam("endTimestamp");
        List<String> xUserID = new ArrayList<>();

        try {
            //Recuperiamo i valori dell'header di nostro interesse
            xUserID = serverRequest.headers().header("X-User-ID");

            // Controlliamo che il campo contenente lo user ID sia presente, e
            // se presente che contenga l'ID dell'amministratore.
            if(xUserID.isEmpty() || xUserID.get(0).compareToIgnoreCase("0") != 0) {
                throw new Exception("Error -> (401): Unauthorized...");
            }

            // Verifichiamo che la richiesta GET contenga gli opportuni
            // parametri all'interno dell'URL.
            if(!from.isPresent() || !to.isPresent()) {
                throw new Exception("Error -> (400) : Bad Request...");
            }

        } catch (Exception e) {

            // Logghiamo il problema che si è verificato con la richiesta.
            Logger.getGlobal().log(Level.WARNING, e.getMessage());

            // Controlliamo la tipologia di eccezione che è stata generata
            // nell'analisi della richiesta HTTP.
            if(e.getMessage().compareToIgnoreCase("Error -> (401): Unauthorized...") == 0) {
                // REACTIVE PIPE DI RISPOSTA IN CASO DI ERRORE 401:
                // 1) Creiamo una Server Response;
                // 2) Ne settiamo lo stato a 401;
                // 3.a) Creiamo un Mono<ServerResponse> a partire da un Mono<Void> che
                //      viene restituito dal metodo "sendLog".
                // 3.b) Specifichiamo che, in caso di fallimento, vogliamo che venga
                //      restituito un Mono<Void> comunque, cosicché anche in caso di fallimento
                //      della "sendLog" siamo certi che la ServerResponse 401 venga gestita
                //      dal codice reattivo che si sottoscriverà ad essa senza problemi.
                return ServerResponse
                        .status(401)
                        .build(paymentService
                                .sendLog(
                                        serverRequest.remoteAddress().get().getAddress().toString(),
                                        serverRequest.path(),
                                        serverRequest.method().toString(),
                                        "401",
                                        e,
                                        loggingTopic,
                                        kafkaHTTPErrorKey)
                                .onErrorResume((err) -> Mono.empty()));
            }
            // REACTIVE PIPE DI RISPOSTA IN CASO DI ERRORE 400:
            // 1) Creiamo una Server Response;
            // 2) Ne settiamo lo stato a 400;
            // 3.a) Creiamo un Mono<ServerResponse> a partire da un Mono<Void> che
            //      viene restituito dal metodo "sendLog".
            // 3.b) Specifichiamo che, in caso di fallimento, vogliamo che venga
            //      restituito un Mono<Void> comunque, cosicché anche in caso di fallimento
            //      della "sendLog" siamo certi che la ServerResponse 400 venga gestita
            //      dal codice reattivo che si sottoscriverà ad essa senza problemi.
            return ServerResponse
                    .status(400)
                    .build(paymentService
                            .sendLog(
                                    serverRequest.remoteAddress().get().getAddress().toString(),
                                    serverRequest.path(),
                                    serverRequest.method().toString(),
                                    "400",
                                    e,
                                    loggingTopic,
                                    kafkaHTTPErrorKey)
                            .onErrorResume((err) -> Mono.empty()));

        }
        // Se tutto va a buon fine allora invochiamo il nostro
        // servizio di recupero delle transazioni.
        // REACTIVE PIPE DI RISPOSTA IN CASO DI SUCCESSO:
        // 1) Creiamo una Server Response;
        // 2) Ne settiamo lo stato a 200;
        // 3) Ne settiamo il "Content-Type" ad "application/json";
        // 4.a) Creiamo la Mono<ServerResponse> specificando che come body
        //      della ServerResponse avremo un Flux<Payment>;
        // 4.b.a) Invochiamo il nostro servizio di recupero delle transazioni
        //        per ottenere un Flux<Payment> a cui il sottoscrittore di default
        //        fornito dal Reactor.Core si sottoscriverà per noi;
        // 4.c.b) Specifichiamo cosa fare in caso di errore generato dall'operazione
        //        asincrona di recupero delle transazioni sul DB (ci interessa che venga
        //        fatto un logging opportuno su Kafka);
        // 4.d.c) Facciamo il cast del Mono<Void> proveniente dalla "sendLog" alla classe "Payment"
        //        in modo da restituire un Mono<Payment>, il cui il Payment è vuoto, in modo tale che
        //        sia compatibile con il Flux<Payment>, la cui pubblicazione abbiamo scelto di inserire
        //        come body della ServerResponse, cosicchè il sottoscrittore di default possa sottoscriversi correttamente
        //        anche a questa tipologia di publisher nel body e, dunque, permettere realmente di effettuare
        //        l'operazione asincrona di sending del log su kafka.
        return ServerResponse
                .status(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body(paymentService.getTransactions(Long.parseLong(from.get()), Long.parseLong(to.get()))
                                .onErrorResume(e -> paymentService
                                        .sendLog(
                                                serverRequest.remoteAddress().get().getAddress().toString(),
                                                serverRequest.path(),
                                                serverRequest.method().toString(),
                                                "500",
                                                e,
                                                loggingTopic,
                                                kafkaHTTPErrorKey)
                                        .cast(Payment.class)
                                ),
                        Payment.class);
    }

}
