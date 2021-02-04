package dsbd2020.lab.paymentmicroservice.entities;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe che modella il concetto di Beat in relazione al
 * servizio di Heart-Beat.
 */
public class Beat {

    private String service;

    private String serviceStatus;

    private String DBStatus;

    public Beat(String service, String serviceStatus, String DBStatus) {
        this.service = service;
        this.serviceStatus = serviceStatus;
        this.DBStatus = DBStatus;
    }

    public String getService() {
        return service;
    }

    public Beat setService(String service) {
        this.service = service;
        return this;
    }

    public String getServiceStatus() {
        return serviceStatus;
    }

    public Beat setServiceStatus(String serviceStatus) {
        this.serviceStatus = serviceStatus;
        return this;
    }

    public String getDBStatus() {
        return DBStatus;
    }

    public Beat setDBStatus(String DBStatus) {
        this.DBStatus = DBStatus;
        return this;
    }

    @Override
    public String toString() {
        return "Beat{" +
                "service='" + service + '\'' +
                ", serviceStatus='" + serviceStatus + '\'' +
                ", DBStatus='" + DBStatus + '\'' +
                '}';
    }

    /*
    Metodo che restituisce l'oggetto Beat sotto forma di mappa.
    Tale metodo risulta essere utile nel momento in cui vogliamo
    effettuare la serializzazione di questo oggetto in formato JSON
    per inviarlo sul topic kafka.
     */
    public Map<String, String> getBeatMap() {
        Map<String, String> beatMap = new HashMap<>();
        beatMap.put("service", this.service);
        beatMap.put("serviceStatus", this.serviceStatus);
        beatMap.put("DBStatus", this.DBStatus);
        return beatMap;
    }

}
