package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.VoidOdontogramFindingRequest;
import br.com.itbn.sisdent.model.ClinicUnit;
import br.com.itbn.sisdent.model.OdontogramFinding;
import br.com.itbn.sisdent.repository.OdontogramFindingRepository;
import br.com.itbn.sisdent.repository.OrganizationRepository;
import br.com.itbn.sisdent.repository.PatientOrganizationLinkRepository;
import br.com.itbn.sisdent.repository.PractitionerRepository;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OdontogramServiceTest {
    @Mock
    private OdontogramFindingRepository findings;
    @Mock
    private PatientOrganizationLinkRepository links;
    @Mock
    private OrganizationRepository organizations;
    @Mock
    private PractitionerRepository practitioners;
    @Mock
    private ScopeAuthorizationService authorization;
    @Mock
    private CurrentAccountService current;
    @Mock
    private OdontogramFinding finding;
    @Mock
    private ClinicUnit clinicUnit;

    private OdontogramService service;
    private UUID organizationId;
    private UUID clinicUnitId;
    private UUID findingId;

    @BeforeEach
    void setUp() {
        service = new OdontogramService(findings, links, organizations, practitioners, authorization, current);
        organizationId = UUID.randomUUID();
        clinicUnitId = UUID.randomUUID();
        findingId = UUID.randomUUID();

        when(findings.findByGlobalIdAndOrganization_GlobalId(findingId, organizationId))
                .thenReturn(Optional.of(finding));
        when(finding.getClinicUnit()).thenReturn(clinicUnit);
        when(clinicUnit.getGlobalId()).thenReturn(clinicUnitId);
        when(finding.getVersion()).thenReturn(4L);
    }

    @Test
    void rejectsAnAbsentVersionWhenTheServiceIsCalledDirectly() {
        VoidOdontogramFindingRequest request = new VoidOdontogramFindingRequest("Correction", null);

        assertThatThrownBy(() -> service.voidRecord(organizationId, clinicUnitId, findingId, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(finding, never()).voidRecord(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsAStaleVersion() {
        VoidOdontogramFindingRequest request = new VoidOdontogramFindingRequest("Correction", 3L);

        assertThatThrownBy(() -> service.voidRecord(organizationId, clinicUnitId, findingId, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(finding, never()).voidRecord(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }
}
