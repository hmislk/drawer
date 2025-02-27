package com.divudi.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
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
public class CashBookRowBundle implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<CashBookRow> cashBookRows;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ColumnModel> columnModels;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date fromDate;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date toDate;
    private boolean onlyRealized;

    //Created Properties
    @ManyToOne
    private WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;
    //Edited Properties
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date completedAt;
    //Retairing properties
    private boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;
    private String retireComments;

    public CashBookRowBundle() {
        createdAt = new Date();
    }

    
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public boolean isOnlyRealized() {
        return onlyRealized;
    }

    public void setOnlyRealized(boolean onlyRealized) {
        this.onlyRealized = onlyRealized;
    }

    public WebUser getCreater() {
        return creater;
    }

    public void setCreater(WebUser creater) {
        this.creater = creater;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public WebUser getRetirer() {
        return retirer;
    }

    public void setRetirer(WebUser retirer) {
        this.retirer = retirer;
    }

    public Date getRetiredAt() {
        return retiredAt;
    }

    public void setRetiredAt(Date retiredAt) {
        this.retiredAt = retiredAt;
    }

    public String getRetireComments() {
        return retireComments;
    }

    public void setRetireComments(String retireComments) {
        this.retireComments = retireComments;
    }

    public Date getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
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
        if (!(object instanceof CashBookRowBundle)) {
            return false;
        }
        CashBookRowBundle other = (CashBookRowBundle) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.CashBookRowBundle[ id=" + id + " ]";
    }

    public List<CashBookRow> getCashBookRows() {
        if (cashBookRows == null) {
            cashBookRows = new ArrayList<>();
        }
        java.util.Collections.sort(cashBookRows, new Comparator<CashBookRow>() {
            @Override
            public int compare(CashBookRow r1, CashBookRow r2) {
                return Double.compare(r1.getOrderNumber(), r2.getOrderNumber());
            }
        });
        return cashBookRows;
    }

    public void setCashBookRows(List<CashBookRow> cashBookRows) {
        this.cashBookRows = cashBookRows;
    }

    public List<ColumnModel> getColumnModels() {
        if (columnModels == null) {
            columnModels = new ArrayList<>();
        }
        java.util.Collections.sort(columnModels, new Comparator<ColumnModel>() {
            @Override
            public int compare(ColumnModel c1, ColumnModel c2) {
                return Double.compare(c1.getOrderNumber(), c2.getOrderNumber());
            }
        });
        return columnModels;
    }

    public void setColumnModels(List<ColumnModel> columnModels) {
        this.columnModels = columnModels;
    }

}
