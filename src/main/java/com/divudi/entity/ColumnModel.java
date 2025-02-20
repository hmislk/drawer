package com.divudi.entity;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com>
 */
@Entity
public class ColumnModel implements Serializable, Comparable<ColumnModel> {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String header;
    private String property;
    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.EAGER)
    private CashBookRowBundle cashBookRowBundle;
    private double orderNumber;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ColumnModel)) {
            return false;
        }
        ColumnModel other = (ColumnModel) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.ColumnModel[ id=" + id + " ]";
    }

    public CashBookRowBundle getCashBookRowBundle() {
        return cashBookRowBundle;
    }

    public void setCashBookRowBundle(CashBookRowBundle cashBookRowBundle) {
        this.cashBookRowBundle = cashBookRowBundle;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public double getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(double orderNumber) {
        this.orderNumber = orderNumber;
    }

    @Override
    public int compareTo(ColumnModel other) {
        return Double.compare(this.orderNumber, other.orderNumber);
    }

}
