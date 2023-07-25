package com.cvereterra.service;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.function.Supplier;

@SpringBootApplication
public class ServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceApplication.class, args);
	}

	@Bean
	ApplicationListener<ApplicationReadyEvent> readyEventApplicationListener(CustomerRepository repository){
		return event -> repository.findAll().forEach(System.out::println);
	}

}

@Controller
@ResponseBody
class CustomerHttpController{
	private final CustomerRepository repository;
	private final ObservationRegistry registry;

	CustomerHttpController(CustomerRepository repository, ObservationRegistry registry) {
		this.repository = repository;
		this.registry = registry;
	}

	@GetMapping("/customers")
	Iterable<Customer> all(){
	return this.repository.findAll();
}

@GetMapping("/customers/{name}")
	Iterable<Customer> byName(@PathVariable String name){
		Assert.state(Character.isUpperCase(name.charAt(0)), "the name must be uppercase");
		return Observation.createNotStarted("by-name", this.registry).observe(
				() -> repository.findByName(name)
		);
}
}

interface CustomerRepository extends CrudRepository<Customer, Integer> {

	Iterable<Customer> findByName(String name);
}

record Customer(@Id Integer id, String name){

}

@ControllerAdvice
class ErrorHandlingControllerAdvice {

	@ExceptionHandler
	public ProblemDetail handle(IllegalStateException isa, HttpServletRequest request){
		request.getHeaderNames().asIterator().forEachRemaining(h -> System.out.println(h + " : " + request.getHeader(h)));
		var problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST.value());
		problemDetail.setDetail(isa.getMessage());
		return problemDetail;
	}
}