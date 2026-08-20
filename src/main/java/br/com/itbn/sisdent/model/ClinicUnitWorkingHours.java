package br.com.itbn.sisdent.model;
import jakarta.persistence.*;
@Entity @Table(name="clinic_unit_working_hours") public class ClinicUnitWorkingHours {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="clinic_unit_id") private ClinicUnit clinicUnit;
 private int dayOfWeek; private int startMinute; private int endMinute;
 protected ClinicUnitWorkingHours(){} public ClinicUnitWorkingHours(ClinicUnit c,int d,int s,int e){clinicUnit=c;dayOfWeek=d;startMinute=s;endMinute=e;}
 public ClinicUnit getClinicUnit(){return clinicUnit;} public int getDayOfWeek(){return dayOfWeek;} public int getStartMinute(){return startMinute;} public int getEndMinute(){return endMinute;}
}
