package dsbd2020.lab.paymentmicroservice.simulator.adapter;

import dsbd2020.lab.paymentmicroservice.entities.Ack;
import dsbd2020.lab.paymentmicroservice.entities.Beat;
import dsbd2020.lab.paymentmicroservice.simulator.service.BeatResponseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Adapter Reactive REST per la gestione dell'Heart-Beat ACK:
 * E' il nostro stesso servizio l'Heart-Beat Monitor (oltre ad
 * essere normalmente un Heart-Beat Source).
 */
@Component
public class RESTAdapterHeartBeatSink {

    @Autowired
    BeatResponseService beatResponseService;

    /**
     * Metodo per la gestione dell'heartbeat in arrivo all'endpoint
     * di heartbeat sink. Tale metodo si occupa di costruire, all'arrivo di
     * un heartbeat, una server response 200 OK contenente nel body un publisher
     * di tipo Mono<Ack> restituito dal metodo "responseBeat".
     * @param serverRequest
     * @return
     */
    public Mono<ServerResponse> heartBeatHandle(ServerRequest serverRequest) {
        return ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(serverRequest.bodyToMono(Beat.class)
                        .flatMap(beat -> beatResponseService.responseBeat(beat)), Ack.class);
    }

}
