package dsbd2020.lab.paymentmicroservice.service;

import com.mongodb.reactivestreams.client.MongoClient;
import dsbd2020.lab.paymentmicroservice.entities.Ack;
import dsbd2020.lab.paymentmicroservice.entities.Beat;
import org.bson.Document;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Componente che contiene del codice che implementa il servizio di Heart-Beat su
 * un Thread separato sganciato dagli altri.
 */
@Component
public class HeartBeatService implements ApplicationListener<ContextRefreshedEvent>, Runnable{

    @Autowired
    MongoClient mongoClient;

    @Value("${heartBeatHost}")
    private String heartBeatHost;

    @Value("${heartBeatPort}")
    private String heartBeatPort;

    @Value("${heartBeatEndPoint}")
    private String heartBeatEndPoint;

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${heartBeatPeriod}")
    private String beatPeriod;

    @Value("${mongoDBName}")
    private String mongoDBName;

    private final Thread thread;

    private Boolean exitCondition;

    public HeartBeatService() {
        this.thread = new Thread(this);
        this.exitCondition = false;
    }

    /**
     * Soltanto quando il contesto dell'applicazione è pronto permettiamo
     * l'avvio del nostro thread per la gestione dell'heart-beat (altrimenti
     * rischiamo che venga mandato in RUN prima che le application-properties
     * siano state settate e dunque facciano parte del contesto dell'applicazione).
     * Non facciamo altro che fare in modo che la nostra classe che si occupa
     * del Thread in background crei il Thread quando i componenti vengono
     * istanziati (essendo annotata come @Component) ma, rendendola listener
     * dell' ApplicationEvent, posticipiamo la messa in RUN del Thread soltanto
     * quando siamo certi che l'applicazione è pronta.
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        this.thread.start();
    }

    /**
     * Metodo run() che viene eseguito al momento della messa in RUN del thread.
     * Tale metodo si occupa periodicamente di inviare un messaggio di heartbeat
     * all'endpoint sink di destinazione (nel nostro caso il beat verrà inviato
     * sulla stessa porta in cui gira il microservizio non avendo a disposizione un
     * microservizio per la gestione dell'healthcheck).
     * Per semplicità, il beat inviato conterrà sempre lo stato "up" per il
     * nostro microservizio, mentre per lo stato del database si effettuerà un check
     * tramite ping sul server di Mongo.
     */
    @Override
    public void run() {

        // Siamo certi che le application.properties che utilizziamo
        // siano state settate. Generiamo quindi il messaggio di beat.
        Beat beat = new Beat(serviceName, "up", "up");

        // Verifichiamo se il periodo di beating sia stato settato
        // (interpretiamo questo setting come l'esplicita volontà di
        // voler utilizzare l'heart-beat service).
        if(beatPeriod.compareToIgnoreCase("disabled") != 0) {

            while(!exitCondition) {
                // Verifichiamo che il DB sia UP sfruttando il servizio di
                // PING offerto dal MongoDB-Server.
                Map<String, String> command = new HashMap<>();
                command.put("ping", "1");
                JSONObject jsonCommand = new JSONObject(command);
                ReactiveMongoTemplate mongoTemplate = new ReactiveMongoTemplate(mongoClient, mongoDBName);
                Mono<Document> monoDoc = mongoTemplate.executeCommand(jsonCommand.toString());

                // Utilizziamo un Hook ad onErrorDropped per gestire il caso in cui
                // il DB non sia raggiungibile. In questa eventualità modifichiamo lo
                // stato del DB all'interno del beat-message e lo inviamo contestualmente.
                Hooks.onErrorDropped(error -> {
                    Logger.getGlobal().log(Level.WARNING, "Error -> (DB DOWN): Exception happened... ", error);
                    beat.setDBStatus("down");
                    sendHeartBeat(beat);
                });

                // Sottoscriviamo un'opportuna LAMBDA Function per gestire
                // l'esito positivo del PING inviato in precedenza al DB.
                monoDoc.subscribe((Document doc) -> {
                    System.out.println(doc);
                    beat.setDBStatus("up");
                    sendHeartBeat(beat);
                });

                try {
                    // Attendiamo prima di inviare un nuovo Beat.
                    Thread.sleep(Long.parseLong(beatPeriod));
                } catch (InterruptedException e) {
                     // Se ci troviamo qui significa che il nostro thread è stato interrotto
                     // esternamente nella sua fase di sleeping.
                    Logger.getGlobal().log(Level.WARNING, "Error: unexpected thread termination...");
                }
            }

        } else {
            // Se non viene settato il periodo di invio dell'heart-beat diamo
            // per scontato che questo servizio non viene utilizzato.
            Logger.getGlobal().log(Level.WARNING, "Error: Heart-Beat Mode OFF...");
            exitCondition = true;
        }

    }

    /**
     * Metodo per il sending sincrono dell'Hear-Beat all'endpoint sink di destinazione.
     * @param beat
     */
    private void sendHeartBeat(Beat beat) {

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JSONObject beatJSON = new JSONObject(beat.getBeatMap());

        Logger.getGlobal().log(Level.INFO, "Beat" + beatJSON);

        HttpEntity<String> request = new HttpEntity<>(beatJSON.toString(), headers);
        Ack receivedAck = restTemplate.postForObject(
                "http://" + heartBeatHost + ":" + heartBeatPort + heartBeatEndPoint,
                request,
                Ack.class);

        // Logghiamo sulla console l'avvenuta ricezione dell'ACK
        Logger.getGlobal().log(Level.INFO, receivedAck.toString());

    }

}
