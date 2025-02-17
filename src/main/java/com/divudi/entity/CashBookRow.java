package com.divudi.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com>
 */
@Entity
public class CashBookRow implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private CashBookRowBundle cashBookRowBundle;
    
    private String string1;
    private String string2;
    private String string3;
    private String string4;
    private String string5;
    private String string6;
    private String string7;

    

    @OneToMany(mappedBy = "cashBookRow", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CashBookTotal> totals;

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
        if (!(object instanceof CashBookRow)) {
            return false;
        }
        CashBookRow other = (CashBookRow) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.CashBookRow[ id=" + id + " ]";
    }

    public String getString1() {
        return string1;
    }

    public void setString1(String string1) {
        this.string1 = string1;
    }

    public String getString2() {
        return string2;
    }

    public void setString2(String string2) {
        this.string2 = string2;
    }

    public String getString3() {
        return string3;
    }

    public void setString3(String string3) {
        this.string3 = string3;
    }

    public String getString4() {
        return string4;
    }

    public void setString4(String string4) {
        this.string4 = string4;
    }

    public String getString5() {
        return string5;
    }

    public void setString5(String string5) {
        this.string5 = string5;
    }

    public String getString6() {
        return string6;
    }

    public void setString6(String string6) {
        this.string6 = string6;
    }

    public String getString7() {
        return string7;
    }

    public void setString7(String string7) {
        this.string7 = string7;
    }

    public List<CashBookTotal> getTotals() {
        if (totals == null) {
            totals = new ArrayList<>();
        }
        return totals;
    }

    public void setTotals(List<CashBookTotal> totals) {
        this.totals = totals;
    }

    public CashBookRowBundle getCashBookRowBundle() {
        return cashBookRowBundle;
    }

    public void setCashBookRowBundle(CashBookRowBundle cashBookRowBundle) {
        this.cashBookRowBundle = cashBookRowBundle;
    }

    

}
