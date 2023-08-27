package com.example.issuerms.controller;

import com.example.issuerms.model.Book;
import com.example.issuerms.model.User;
import com.example.issuerms.repo.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriTemplate;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@RestController
public class UserResource {
    @Autowired
    private WebClient webClient;
    @Autowired
    private ReactiveCircuitBreakerFactory circuitBreakerFactory;

    @Autowired
    private UserRepo userRepo;

    Logger logger= LoggerFactory.getLogger(UserResource.class);

    @GetMapping("/book-details")
    public Mono<ResponseEntity<List<Book>>> getBooksFromBookms() {

        Mono<ResponseEntity<List<Book>>> responseEntityMono = webClient.get().uri("/books").retrieve().toEntity(
                new ParameterizedTypeReference<List<Book>>() {
                }).transform(it -> circuitBreakerFactory.create("backendB")
                .run(it, throwable -> {
                    Book book = new Book();
                    book.setTitle("Book Not Found");
                    return Mono.just(ResponseEntity.ok((List.of(book))));
                }));


        return responseEntityMono;
    }


    public Mono<ResponseEntity<Book>> getBooksFromBookms(int id) {
        logger.info("inside bookms call");
        String uri = UriComponentsBuilder.fromPath("/issue-book/{id}")
                .buildAndExpand(id)
                .toUriString();

        Mono<ResponseEntity<Book>> responseEntityMono = webClient.get().uri(uri).retrieve().toEntity(
                new ParameterizedTypeReference<Book>() {
                }).transform(it -> circuitBreakerFactory.create("backendB")
                .run(it, throwable -> {
                    Book book = new Book(); // or use a cache or call backupms
                    book.setTitle("Book Not Found");
                    logger.error("not found");
                    return Mono.just(ResponseEntity.ok(book));
                }));


        return responseEntityMono;
    }
    @GetMapping ("/getBook/{id}/{userId}")
    public ResponseEntity<User> issueBook(@PathVariable int id, @PathVariable int userId) throws URISyntaxException {
        Mono<ResponseEntity<Book>> responseEntityMono = getBooksFromBookms(id);
        if(responseEntityMono.block().getBody().getIssuedCopies() >0){
            User user= new User();
            user.setCustId(userId);
            user.setIsbn(id);
            user.setNoOfCopies(1);
            User issuedBook =  userRepo.save(user);
            logger.info("book issued");
            return ResponseEntity.created(new URI(Integer.toString(issuedBook.getCustId()))).body(issuedBook);

        }
        logger.error("book not found: {}", id);
        return ResponseEntity.notFound().build();

    }
}
