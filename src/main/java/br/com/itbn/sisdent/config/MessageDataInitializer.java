package br.com.itbn.sisdent.config;

import br.com.itbn.sisdent.model.Message;
import br.com.itbn.sisdent.repository.MessageRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageDataInitializer {
    @Bean
    CommandLineRunner seedHelloWorld(MessageRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new Message("Hello World"));
            }
        };
    }
}
