package com.example.products;

import com.example.products.entity.Product;
import com.example.products.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import java.util.Arrays;

@SpringBootApplication
@EnableDiscoveryClient
public class ProductsApplication {

	@Autowired
	private ProductRepository productRepository;

	public static void main(String[] args) {
		SpringApplication.run(ProductsApplication.class, args);
	}

	@PostConstruct
	public void setUpData(){
		productRepository.saveAll(Arrays.asList(Product.builder().productName("Soap").quantity(100).price(50).build(),
				Product.builder().productName("Shampoo").quantity(200).price(175).build(),
				Product.builder().productName("Toothpaste").quantity(300).price(60).build()));	}
}
