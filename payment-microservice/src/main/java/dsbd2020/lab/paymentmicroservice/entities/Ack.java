package dsbd2020.lab.paymentmicroservice.entities;

/**
 * Classe che modella il concetto di ACK in relazione al
 * servizio di Heart-Beat.
 */
public class Ack {

    private String status;

    private String msg;

    public Ack() {
    }

    public Ack(String status, String msg) {
        this.status = status;
        this.msg = msg;
    }

    public String getStatus() {
        return status;
    }

    public Ack setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getMsg() {
        return msg;
    }

    public Ack setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    @Override
    public String toString() {
        return "Ack{" +
                "status='" + status + '\'' +
                ", msg='" + msg +
                '}';
    }

}
