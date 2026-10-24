package com.example.eventRegisteration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class EventRegisterationApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventRegisterationApplication.class, args);
	}

}
