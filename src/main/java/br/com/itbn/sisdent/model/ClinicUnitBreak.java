package br.com.itbn.sisdent.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "clinic_unit_breaks")
public class ClinicUnitBreak {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_unit_id")
    private ClinicUnit clinicUnit;

    private int dayOfWeek;
    private int startMinute;
    private int endMinute;

    protected ClinicUnitBreak() {
    }

    public ClinicUnitBreak(ClinicUnit clinicUnit, int dayOfWeek, int startMinute, int endMinute) {
        this.clinicUnit = clinicUnit;
        this.dayOfWeek = dayOfWeek;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public int getStartMinute() {
        return startMinute;
    }

    public int getEndMinute() {
        return endMinute;
    }
}
