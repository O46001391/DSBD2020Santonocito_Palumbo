package dsbd2020.lab.paymentmicroservice.service;

import com.google.gson.Gson;
import dsbd2020.lab.paymentmicroservice.entities.Log;
import dsbd2020.lab.paymentmicroservice.model.Payment;
import dsbd2020.lab.paymentmicroservice.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


/**
 * Classe che modella il servizio di pagamento (la nostra core business logic).
 * Tutti i metodi che restituiscono Mono<Void> sono metodi in cui il chiamante
 * non è interessato ad un valore specifico prodotto ma soltanto all'evento di
 * corretta (o meno) pubblicazione da parte del publisher.
 */
@Service
public class PaymentService {

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    private ReactiveKafkaProducerTemplate<String, String> reactiveKafkaTemplate;

    @Value("${myPaypalAccount}")
    private String businessAccount;

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${sandboxEndpoint}")
    private String sandboxEndpoint;

    @Value("${kafkaOrdersTopic}")
    private String ordersTopic;

    @Value("${kafkaLoggingTopic}")
    private String loggingTopic;

    @Value("${kafkaOrderPaidKey}")
    private String kafkaOrderPaidKey;

    @Value("${kafkaBadIpnError}")
    private String kafkaBadIpnError;

    @Value("${kafkaBadBusinessEmail}")
    private String kafkaBadBusinessEmail;


    /**
     * ### TEST ###
     * @param payments
     * @return
     * Servizio utilizzato solo per testare il
     * corretto funzionamento delle transazioni.
     */
    @Transactional
    public Flux<Payment> savePayments(Payment... payments) {
        Flux<Payment> paymentFlux = Flux.just(payments)
                .map(payment -> new Payment(payment.getOrderId(), payment.getUserId(), payment.getAmountPaid(), payment.getBusinessEmail()))
                .flatMap(this.paymentRepository::save)
                .doOnNext(payment -> Assert.isTrue(payment.getBusinessEmail().contains("@"), "ERROR: The email must contain @..."));
        return paymentFlux;
    }

    /**
     * Effettua il salvataggio di un Payment nel database.
     * @param payment
     * @return
     */
    @Transactional
    public Mono<Payment> savePayment(Payment payment) {
        Mono<Payment> paymentMono = paymentRepository.save(payment);
        return paymentMono;

    }

    /**
     * Effettua la verifica e le opportune operazioni
     * di salvataggio necessarie per confermare o
     * meno una transazione Paypal derivante dall'IPN
     * simulator.
     * @param sourceIp
     * @param path
     * @param method
     * @param params
     * @return
     */
    @Transactional
    public Mono<Void> ipn(String sourceIp, String path, String method, MultiValueMap<String, String> params) {

        /**
         * ######################### FASE 1 #########################
         * Verifichiamo che la richiesta di IPN provenga da PayPal
         * e che contenga i campi necessari per poter essere memorizzata
         * eventualmente nel nostro database.
         */
        Payment payment = new Payment();

        try {
            // Verifichiamo la correttezza dei parametri della richiesta.
            String userId = params.get("payer_id").get(0);
            String orderId = params.get("invoice").get(0);
            String amountPaid = params.get("mc_gross").get(0);
            String businessMail = params.get("receiver_email").get(0);

            payment = new Payment(orderId, userId, Double.parseDouble(amountPaid), businessMail);

        } catch(Exception e) {
            // Logging in caso di richiesta non conforme.
            return sendLog(
                    sourceIp,
                    path,
                    method,
                    "400",
                    new Exception("(400): Bad Request..."),
                    loggingTopic,
                    kafkaBadIpnError);
        }

        // Verifica con servizio IPN Sandbox Paypal.
        ResponseEntity<String> response = verifyRequest(params);

        if(!response.getBody().contains("VERIFIED")) {
            // Logging in caso di fallita verifica della transazione.
            return sendLog(
                    sourceIp,
                    path,
                    method,
                    "500",
                    new Exception("(500): Transaction NOT Verified..."),
                    loggingTopic,
                    kafkaBadIpnError);

        }

        /**
         * ######################### FASE 2 #########################
         * Verifichiamo che la business email contenuta nel messaggio di IPN
         * sia conforme alla email del venditore impostata dal microservizio di
         * gestione dei pagamenti. In caso affermativo, salviamo il pagamento sul
         * DB e pubblichiamo sul topic "orders" di kafka il messaggio di avvenuto
         * pagamento.
         */
        // Verifica della business mail.
        if(payment.getBusinessEmail().compareToIgnoreCase(businessAccount) == 0) {
            // Tutti i controlli sono andati a buon fine.
            // Inviamo il messaggio sul topic kafka.
            Mono<Void> kafkaPublisher = sendLog(
                    sourceIp,
                    path,
                    method,
                    "200", new Exception("(200): OK... Transaction Received..."),
                    ordersTopic, kafkaOrderPaidKey);

            // Salviamo il pagamento nel nostro DB.
            Mono<Void> mongoPublisher = savePayment(payment)
                    .then();

            // Restituiamo un Mono<Void> dato dalla concatenazione
            // dei due publisher di Kafka e Mongo restituiti in precedenza.
            return kafkaPublisher
                    .concatWith(mongoPublisher)
                    .then();

        } else {
            // Logging in caso di errore sul conto destinatario del pagamento.
            return sendLog(
                    sourceIp,
                    path,
                    method,
                    "400",
                    new Exception("(400): Receiver Mail NOT Verified..."),
                    loggingTopic,
                    kafkaBadBusinessEmail);

        }
    }

    /**
     * Andiamo a recuperare tutti i pagamenti che sono
     * stati effettuati da-a ("da" e "a" inclusi).
     * @param from
     * @param to
     * @return
     */
    @Transactional
    public Flux<Payment> getTransactions(long from, long to) {
        Flux<Payment> paymentFlux = paymentRepository
                .findPaymentsByUnixTimestampBetween((from-1), (to+1));
        return paymentFlux;
    }


    /**
     * Metodo per l'invio dei Log in modo reattivo su Kafka.
     * @param sourceIp
     * @param path
     * @param method
     * @param code
     * @param e
     * @param topic
     * @param key
     * @return
     */
    public Mono<Void> sendLog(String sourceIp, String path, String method, String code, Throwable e, String topic, String key) {

        // Andiamo a generare il nostro log.
        Log log = new Log(
                sourceIp,
                serviceName,
                path + " Method: " + method,
                "Generic Error...");


        // Non sempre lo stack trace potrebbe essere disponibile.
        if(e == null) {
            // Se lo stack trace non dovesse essere
            // disponibile.
            log.setError("Stack-Trace not available...");
        } else {
            // Controlliamo quale sia il tipo di errore che si è
            // verificato.
            if (code.contains("50")) {
                // Siamo in presenza di un errore della famiglia 50x.
                log.setError("(50x): " + printStackTrace(e));
            } else if (code.contains("40")) {
                // Siamo in presenza di un errore della famiglia 40x.
                log.setError("(40x) - Status code: " + code);
            } else if (code.contains("20")) {
                // Siamo in presenza di un errore della famiglia 20x.
                log.setError("(20x) - Status code: " + e.getMessage());
            }
        }

        // Siamo in presenza di errori di altra natura oppure è tutto OK.
        // Inviamo il log su kafka (specificando come formato del messaggio il JSON)
        // e restituiamo un publisher di tipo Mono<Void>.
        return reactiveKafkaTemplate
                .send(topic, key, new Gson().toJson(log))
                .then();
    }

    /**
     * Metodo di utility per il printing dello stack trace.
     * @param e
     * @return
     */
    private String printStackTrace(Throwable e) {
        String s = "";
        for (StackTraceElement text : e.getStackTrace()) {
            s = s + text.toString();
        }
        return s;
    }

    /**
     * Metodo per lo scambio di messaggi sincrono (ultimi due passi dell'handshake
     * a 4 vie -> Vedere RESTAdapter per una visione completa dell'interazione) con l'IPN
     * endopoint di convalida di Paypal.
     * @param params
     * @return
     */
    private ResponseEntity<String> verifyRequest(MultiValueMap<String, String> params) {

        // Appendiamo al body il nostro ulteriore parametro.
        params.add("cmd", "_notify-validate");

        // Creiamo ed inviamo una nostra richiesta.
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(MediaType.APPLICATION_FORM_URLENCODED_VALUE));

        HttpEntity entity = new HttpEntity(params, headers);

        return restTemplate.exchange(
                sandboxEndpoint, HttpMethod.POST, entity, String.class);

    }

}
