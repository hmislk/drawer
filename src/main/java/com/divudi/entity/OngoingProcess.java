package com.divudi.entity;

import java.io.Serializable;
import javax.persistence.*;

/**
 * Ensures unique process names.
 * 
 * @author Dr M H B Ariyaratne
 */
@Entity
public class OngoingProcess implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true, nullable = false)
    private String processName;

    private boolean currentlyOngoing;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public boolean isCurrentlyOngoing() {
        return currentlyOngoing;
    }

    public void setCurrentlyOngoing(boolean currentlyOngoing) {
        this.currentlyOngoing = currentlyOngoing;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof OngoingProcess)) {
            return false;
        }
        OngoingProcess other = (OngoingProcess) object;
        return !((this.id == null && other.id != null)
                || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "com.divudi.entity.OngoingProcess[ id=" + id + " ]";
    }
}
