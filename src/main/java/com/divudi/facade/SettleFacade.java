/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.facade;

import com.divudi.entity.Settle;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author archmage
 */
@Stateless
public class SettleFacade extends AbstractFacade<Settle> {
    @PersistenceContext(unitName = "pu")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public SettleFacade() {
        super(Settle.class);
    }
    
}
