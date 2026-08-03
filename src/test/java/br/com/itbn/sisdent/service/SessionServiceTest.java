package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.model.Person;
import br.com.itbn.sisdent.repository.MembershipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {
    @Mock CurrentAccountService current;
    @Mock MembershipRepository memberships;
    @InjectMocks SessionService service;

    @Test
    void returnsCurrentAccountAndItsActiveMemberships() {
        Account account = new Account(new Person("Ana"), "ana@example.com", "encoded", false);
        when(current.require()).thenReturn(account);
        when(memberships.findAllByAccount_IdAndActiveTrue(any())).thenReturn(List.of());

        var response = service.current();

        assertThat(response.email()).isEqualTo("ana@example.com");
        assertThat(response.memberships()).isEmpty();
        assertThat(response.accountManagementOrganizationId()).isNull();
    }
}
