package dsbd2020.lab.paymentmicroservice.simulator.service;

import dsbd2020.lab.paymentmicroservice.entities.Ack;
import dsbd2020.lab.paymentmicroservice.entities.Beat;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Servizio che simula l'Heart-Beat Monitor.
 */
@Service
public class BeatResponseService {

    /**
     * Metodo per la verifica dell'heartbeat e l'invio
     * di un Ack (sotto forma di publisher Mono<Ack>) di conferma
     * per segnalare lo stato di attività del microservizio e del database.
     * @param beat
     * @return
     */
    public Mono<Ack> responseBeat(Beat beat) {
        Ack ack = new Ack("ALL-UP", "Beat Received!");
        if(beat.getDBStatus().compareToIgnoreCase("up") != 0
                ||  beat.getServiceStatus().compareToIgnoreCase("up") != 0) {
            ack.setStatus("DOWN");
        }
        Mono<Ack> ackMono = Mono.just(ack);
        return ackMono;
    }

}
