package br.com.itbn.sisdent.model;
import jakarta.persistence.*;
@Entity @Table(name="clinic_unit_breaks") public class ClinicUnitBreak {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="clinic_unit_id") private ClinicUnit clinicUnit;
 private int dayOfWeek; private int startMinute; private int endMinute;
 protected ClinicUnitBreak(){} public ClinicUnitBreak(ClinicUnit c,int d,int s,int e){clinicUnit=c;dayOfWeek=d;startMinute=s;endMinute=e;}
 public int getDayOfWeek(){return dayOfWeek;} public int getStartMinute(){return startMinute;} public int getEndMinute(){return endMinute;}
}
