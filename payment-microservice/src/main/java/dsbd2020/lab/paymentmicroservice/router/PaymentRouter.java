package dsbd2020.lab.paymentmicroservice.router;

import dsbd2020.lab.paymentmicroservice.adapter.RESTAdapter;
import dsbd2020.lab.paymentmicroservice.simulator.adapter.RESTAdapterHeartBeatSink;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Classe di configurazione per un router che instrada al nostro ADAPTER Reactive REST
 * le richieste ai vari ENDPOINT esposti dal nostro servizio.
 */
@Configuration
public class PaymentRouter {

    @Value("${heartBeatEndPoint}")
    private String heartBeatEndPoint;

    @Bean
    public RouterFunction<ServerResponse> paymentRoutes(RESTAdapter paymentAdapter, RESTAdapterHeartBeatSink restAdapterHeartBeatSink) {
        return RouterFunctions.route(
                RequestPredicates.POST("/test")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                paymentAdapter::savePayment
        ).andRoute(
                RequestPredicates.POST(heartBeatEndPoint)
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                restAdapterHeartBeatSink::heartBeatHandle
        ).andRoute(
                RequestPredicates.POST("/ipn")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                paymentAdapter::ipn
        ).andRoute(
                RequestPredicates.GET("/transactions"),
                paymentAdapter::getTransactions
        );
    }

}
