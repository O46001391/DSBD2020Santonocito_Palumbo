package dsbd2020.lab.paymentmicroservice.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.Instant;

/**
 * Classe che modella il concetto di pagamento da rendere persistente
 * all'interno del database NoSQL MongoDB.
 */
@Document
public class Payment implements Serializable {

    @Id
    private ObjectId _id;

    @JsonProperty("invoice")
    private String orderId;

    @JsonProperty("payer_id")
    private String userId;

    @JsonProperty("mc_gross")
    private Double amountPaid;

    private long unixTimestamp;

    @JsonProperty("receiver_email")
    private String businessEmail;

    public Payment() {
    }

    @JsonCreator
    public Payment(String orderId, String userId, Double amountPaid, String businessEmail) {
        this.orderId = orderId;
        this.userId = userId;
        this.amountPaid = amountPaid;
        this.unixTimestamp = Instant.now().getEpochSecond();
        this.businessEmail = businessEmail;
    }

    @JsonGetter("_id")
    public String getIdString() {
        return this._id.toHexString();
    }

    public ObjectId get_id() {
        return _id;
    }

    public Payment set_id(ObjectId _id) {
        this._id = _id;
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public Payment setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public Payment setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public Double getAmountPaid() {
        return amountPaid;
    }

    public Payment setAmountPaid(Double amountPaid) {
        this.amountPaid = amountPaid;
        return this;
    }

    public long getUnixTimestamp() {
        return unixTimestamp;
    }

    public Payment setUnixTimestamp(long unixTimestamp) {
        this.unixTimestamp = unixTimestamp;
        return this;
    }

    public String getBusinessEmail() {
        return businessEmail;
    }

    public Payment setBusinessEmail(String businessEmail) {
        this.businessEmail = businessEmail;
        return this;
    }
}
