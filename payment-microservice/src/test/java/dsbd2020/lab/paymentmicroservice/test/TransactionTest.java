package dsbd2020.lab.paymentmicroservice.test;

import dsbd2020.lab.paymentmicroservice.model.Payment;
import dsbd2020.lab.paymentmicroservice.repository.PaymentRepository;
import dsbd2020.lab.paymentmicroservice.service.PaymentService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.test.StepVerifier;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TransactionTest {

    @Autowired
    PaymentService paymentService;

    @Autowired
    PaymentRepository paymentRepository;

    @Test
    public void savePayments() throws Exception {

        Payment p1 = new Payment("10", "1", 10.5, "orazio1997@outlook.it");
        Payment p2 = new Payment("11", "2", 10.5, "orazio1997@outlook.it");
        Payment p3 = new Payment("12", "3", 10.5, "orazio1997outlook.it");

        StepVerifier
                .create(this.paymentRepository.deleteAll())
                .verifyComplete();

        StepVerifier
                .create(this.paymentService.savePayments(p1, p2))
                .expectNextCount(2)
                .verifyComplete();

        StepVerifier
                .create(this.paymentRepository.findAll())
                .expectNextCount(2)
                .verifyComplete();

        StepVerifier
                .create(this.paymentRepository.deleteAll())
                .verifyComplete();

        StepVerifier
                .create(this.paymentService.savePayments(p1, p2, p3))
                .expectNextCount(2)
                .expectError()
                .verify();

        StepVerifier
                .create(this.paymentRepository.findAll())
                .expectNextCount(0)
                .verifyComplete();
    }

}
