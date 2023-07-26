package com.cvereterra.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class ClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClientApplication.class, args);
	}

	@Bean
	ApplicationListener<ApplicationReadyEvent> ready(CustomerClient cc){
		return event -> cc.getAll().subscribe(System.out::println);
	}

	@Bean
	CustomerClient cc (WebClient.Builder  httpBuilder){
		var wc = httpBuilder
				.baseUrl("http://localhost:8080")
				.build();
		var htsp = HttpServiceProxyFactory
				.builder()
				.clientAdapter(WebClientAdapter.forClient(wc))
				.build()
				.createClient(CustomerClient.class);
		return htsp;

	}

}

@Controller
class GraphqlController {
	private final CustomerClient cc;

	GraphqlController(CustomerClient cc) {
		this.cc = cc;
	}

	@QueryMapping
	Flux<Customer> customers(){
		return this.cc.getAll();
	}

	@BatchMapping
	Map<Customer, Profile> profile(List<Customer> customerList){
		// Using batching to avoid N+1
		var m = new HashMap<Customer, Profile>();
		for (var customer : customerList){
			m.put(customer, new Profile(customer.id()));
		}
		return m;
	}

//	@SchemaMapping(typeName = "Customer")
//	Profile profile (Customer c ){
//		// If we add an API call here, it will be called once for each customer	
// 		// This is very inneficent, we will solve this issue with @BatchMapping
//		return new Profile(c.id());
//	}
}


record Profile(Integer id){

}

// copy pasted, shame
record Customer(Integer id, String name){

}

interface CustomerClient {
	@GetExchange("/customers")
	Flux<Customer> getAll();
}
