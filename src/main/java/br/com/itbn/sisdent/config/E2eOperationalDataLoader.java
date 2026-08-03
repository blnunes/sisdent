package br.com.itbn.sisdent.config;

import br.com.itbn.sisdent.model.ClinicUnit;
import br.com.itbn.sisdent.model.Organization;
import br.com.itbn.sisdent.model.Patient;
import br.com.itbn.sisdent.model.PatientLinkBasis;
import br.com.itbn.sisdent.model.PatientOrganizationLink;
import br.com.itbn.sisdent.model.Practitioner;
import br.com.itbn.sisdent.model.Speciality;
import br.com.itbn.sisdent.repository.ClinicUnitRepository;
import br.com.itbn.sisdent.repository.OrganizationRepository;
import br.com.itbn.sisdent.repository.PatientOrganizationLinkRepository;
import br.com.itbn.sisdent.repository.PatientRepository;
import br.com.itbn.sisdent.repository.PractitionerRepository;
import br.com.itbn.sisdent.repository.SpecialityRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@Profile("e2e")
@Order(4)
public class E2eOperationalDataLoader implements ApplicationRunner {
    private final OrganizationRepository organizations;
    private final ClinicUnitRepository clinics;
    private final PatientRepository patients;
    private final PatientOrganizationLinkRepository links;
    private final PractitionerRepository practitioners;
    private final SpecialityRepository specialities;

    public E2eOperationalDataLoader(OrganizationRepository organizations, ClinicUnitRepository clinics,
            PatientRepository patients, PatientOrganizationLinkRepository links,
            PractitionerRepository practitioners, SpecialityRepository specialities) {
        this.organizations = organizations; this.clinics = clinics; this.patients = patients;
        this.links = links; this.practitioners = practitioners; this.specialities = specialities;
    }

    @Override @Transactional
    public void run(ApplicationArguments args) {
        Organization organization = organizations.findAll().stream().findFirst().orElseThrow();
        ClinicUnit clinic = clinics.findAll().stream().filter(unit -> unit.getOrganization().getId().equals(organization.getId()))
                .findFirst().orElseGet(() -> clinics.save(new ClinicUnit(organization, "Training Clinic")));
        Patient patient = patients.findAll().getFirst();
        if (links.findFirstByPatient_GlobalIdAndOrganization_GlobalId(patient.getGlobalId(), organization.getGlobalId()).isEmpty()) {
            links.save(new PatientOrganizationLink(patient, organization, clinic, PatientLinkBasis.ATTENDANCE));
        }
        if (practitioners.findAllByOrganization_GlobalIdOrderByDisplayName(organization.getGlobalId()).isEmpty()) {
            Speciality endodontics = specialities.findByName("Endodontics").orElseThrow();
            practitioners.save(new Practitioner(organization, null, "Dr. Avery Morgan", "TRAIN-001", Set.of(endodontics)));
        }
    }
}
