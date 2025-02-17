package com.divudi.entity;

import java.io.Serializable;
import javax.persistence.*;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com>
 */
@Entity
public class CashBookTotal implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    private Double value;

    @ManyToOne
    private CashBookRow cashBookRow;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public CashBookRow getCashBookRow() {
        return cashBookRow;
    }

    public void setCashBookRow(CashBookRow cashBookRow) {
        this.cashBookRow = cashBookRow;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof CashBookTotal)) {
            return false;
        }
        CashBookTotal other = (CashBookTotal) object;
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "com.divudi.entity.CashBookTotal[ id=" + id + ", value=" + value + " ]";
    }
}
