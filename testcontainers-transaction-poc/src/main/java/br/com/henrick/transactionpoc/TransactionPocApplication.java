package br.com.henrick.transactionpoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class TransactionPocApplication {

	public static void main(String[] args) {
		SpringApplication.run(TransactionPocApplication.class, args);
	}

}
