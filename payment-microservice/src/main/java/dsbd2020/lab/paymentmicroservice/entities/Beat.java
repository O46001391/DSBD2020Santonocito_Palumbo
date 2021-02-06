package dsbd2020.lab.paymentmicroservice.entities;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe che modella il concetto di Beat in relazione al
 * servizio di Heart-Beat.
 */
public class Beat {

    private String serviceName;

    private String serviceStatus;

    private String dbStatus;

    public Beat(String serviceName, String serviceStatus, String dbStatus) {
        this.serviceName = serviceName;
        this.serviceStatus = serviceStatus;
        this.dbStatus = dbStatus;
    }

    public String getService() {
        return serviceName;
    }

    public Beat setService(String service) {
        this.serviceName = service;
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
        return dbStatus;
    }

    public Beat setDBStatus(String DBStatus) {
        this.dbStatus = DBStatus;
        return this;
    }

    @Override
    public String toString() {
        return "Beat{" +
                "serviceName='" + serviceName + '\'' +
                ", serviceStatus='" + serviceStatus + '\'' +
                ", dbStatus='" + dbStatus + '\'' +
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
        beatMap.put("serviceName", this.serviceName);
        beatMap.put("serviceStatus", this.serviceStatus);
        beatMap.put("dbStatus", this.dbStatus);
        return beatMap;
    }

}
