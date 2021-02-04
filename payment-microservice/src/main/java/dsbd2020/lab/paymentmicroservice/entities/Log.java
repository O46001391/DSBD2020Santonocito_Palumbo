package dsbd2020.lab.paymentmicroservice.entities;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.Serializable;
import java.time.Instant;

/**
 * Classe che modella il concetto di Log che viene utilizzato
 * sistematicamente per la pubblicazione sul topic Kafka
 * "logging".
 */
public class Log implements Serializable {

    private long unixTimestamp;

    private String sourceIpAddress;

    private String serviceName;

    private String request;

    private String error;

    private Log() {

    }

    @JsonCreator
    public Log(String sourceIpAddress, String serviceName, String request, String error) {
        this.unixTimestamp = Instant.now().getEpochSecond();
        this.sourceIpAddress = sourceIpAddress;
        this.serviceName = serviceName;
        this.request = request;
        this.error = error;
    }

    public long getUnixTimestamp() {
        return unixTimestamp;
    }

    public Log setUnixTimestamp(long unixTimestamp) {
        this.unixTimestamp = unixTimestamp;
        return this;
    }

    public String getSourceIpAddress() {
        return sourceIpAddress;
    }

    public Log setSourceIpAddress(String sourceIpAddress) {
        this.sourceIpAddress = sourceIpAddress;
        return this;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Log setServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public String getRequest() {
        return request;
    }

    public Log setRequest(String request) {
        this.request = request;
        return this;
    }

    public String getError() {
        return error;
    }

    public Log setError(String error) {
        this.error = error;
        return this;
    }

    @Override
    public String toString() {
        return "Log{" +
                "unixTimestamp=" + unixTimestamp +
                ", sourceIpAddress=" + sourceIpAddress +
                ", serviceName='" + serviceName + '\'' +
                ", request='" + request + '\'' +
                ", error='" + error + '\'' +
                '}';
    }

}
