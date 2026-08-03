package br.com.itbn.sisdent.model;
import jakarta.persistence.*; import java.util.*;
@Entity @Table(name="practitioners") public class Practitioner extends AuditableEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="global_id", nullable=false, unique=true, updatable=false) private UUID globalId=UUID.randomUUID();
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="organization_id") private Organization organization;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="account_id") private Account account;
 @Column(nullable=false) private String displayName; private String registrationNumber; @Column(nullable=false) private boolean active=true;
 @ManyToMany(fetch=FetchType.LAZY) @JoinTable(name="practitioner_specialities",joinColumns=@JoinColumn(name="practitioner_id"),inverseJoinColumns=@JoinColumn(name="speciality_id")) private Set<Speciality> specialities=new LinkedHashSet<>();
 protected Practitioner(){} public Practitioner(Organization o, Account a,String n,String r,Set<Speciality>s){organization=o;account=a;displayName=n.strip();registrationNumber=r==null?null:r.strip();specialities.addAll(s);}
 public Long getId(){return id;} public UUID getGlobalId(){return globalId;} public Organization getOrganization(){return organization;} public Account getAccount(){return account;} public String getDisplayName(){return displayName;} public String getRegistrationNumber(){return registrationNumber;} public boolean isActive(){return active;} public Set<Speciality> getSpecialities(){return specialities;}
 public void update(Account a,String n,String r,Set<Speciality>s){account=a;displayName=n.strip();registrationNumber=r==null?null:r.strip();specialities.clear();specialities.addAll(s);} public void deactivate(){active=false;}
}
