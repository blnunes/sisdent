package br.com.itbn.sisdent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.itbn.sisdent.model.IdentificationType;
import br.com.itbn.sisdent.model.Role;
import br.com.itbn.sisdent.model.User;
import br.com.itbn.sisdent.repository.UserRepository;
import br.com.itbn.sisdent.service.IdentificationNumbers;

@Component
public class AdminDataLoader implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdentificationType identificationType;
    private final String identificationNumber;
    private final String password;

    public AdminDataLoader(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${sisdent.bootstrap-admin.identification-type}") IdentificationType identificationType,
            @Value("${sisdent.bootstrap-admin.identification-number}") String identificationNumber,
            @Value("${sisdent.bootstrap-admin.password}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.identificationType = identificationType;
        this.identificationNumber = identificationNumber;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String normalizedNumber = IdentificationNumbers.normalize(identificationNumber);
        userRepository.findByIdentificationTypeAndIdentificationNumber(
                identificationType,
                normalizedNumber)
                .ifPresentOrElse(existingUser -> {
                    if (existingUser.getRole() == Role.ADMIN) {
                        existingUser.setPermissions(Role.ADMIN.defaultPermissions());
                        userRepository.save(existingUser);
                    }
                }, () -> userRepository.save(new User(
                        identificationType,
                        normalizedNumber,
                        passwordEncoder.encode(password),
                        Role.ADMIN,
                        Role.ADMIN.defaultPermissions())));
    }
}
