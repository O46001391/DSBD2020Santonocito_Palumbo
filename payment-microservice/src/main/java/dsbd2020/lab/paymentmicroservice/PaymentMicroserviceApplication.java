package dsbd2020.lab.paymentmicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@SpringBootApplication(
		exclude = {
				MongoAutoConfiguration.class,
				MongoDataAutoConfiguration.class,
				EmbeddedMongoAutoConfiguration.class
		}
)
@EnableTransactionManagement
public class PaymentMicroserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentMicroserviceApplication.class, args);
		//HeartBeatService heartBeatService = new HeartBeatService();
	}

}
