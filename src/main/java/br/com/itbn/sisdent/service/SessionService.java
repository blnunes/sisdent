package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.SessionResponse;
import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.repository.MembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {
    private final CurrentAccountService currentAccountService;
    private final MembershipRepository membershipRepository;

    public SessionService(CurrentAccountService currentAccountService, MembershipRepository membershipRepository) {
        this.currentAccountService = currentAccountService;
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public SessionResponse current() {
        Account account = currentAccountService.require();
        return new SessionResponse(account.getGlobalId(), account.getEmail(), account.getPerson().getDisplayName(),
                account.isPlatformAdministrator(),
                account.getAccountManagementOrganization() == null ? null : account.getAccountManagementOrganization().getGlobalId(),
                membershipRepository.findAllByAccount_IdAndActiveTrue(account.getId()).stream()
                        .map(OrganizationService::toResponse).toList());
    }
}
