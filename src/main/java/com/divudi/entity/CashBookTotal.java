package com.divudi.entity;

import java.io.Serializable;
import java.util.Comparator;
import javax.persistence.*;

/**
 *
 * @author Dr M H B Ariyaratne
 */
@Entity
public class CashBookTotal implements Serializable, Comparable<CashBookTotal> {

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    private Double totalValue;
    
    @Transient
    private Double value;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private CashBookRow cashBookRow;
    
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private CashBookRowBundle cashBookRowBundle;

    private double orderNumber;
    
    

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(Double totalValue) {
        this.totalValue = totalValue;
    }

    public CashBookRow getCashBookRow() {
        return cashBookRow;
    }

    public void setCashBookRow(CashBookRow cashBookRow) {
        this.cashBookRow = cashBookRow;
    }

    public double getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(double orderNumber) {
        this.orderNumber = orderNumber;
    }

    @Override
    public int compareTo(CashBookTotal other) {
        return Double.compare(this.orderNumber, other.orderNumber);
    }

    @Override
    public int hashCode() {
        return (id != null ? id.hashCode() : 0);
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
        return "com.divudi.entity.CashBookTotal[ id=" + id + ", value=" + totalValue + ", orderNumber=" + orderNumber + " ]";
    }

    public Double getValue() {
        return totalValue;
    }

    public void setValue(Double value) {
        this.totalValue = value;
        this.value = value;
    }

    public CashBookRowBundle getCashBookRowBundle() {
        return cashBookRowBundle;
    }

    public void setCashBookRowBundle(CashBookRowBundle cashBookRowBundle) {
        this.cashBookRowBundle = cashBookRowBundle;
    }
}
