/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Settle;
import com.divudi.entity.Summery;
import com.divudi.entity.WebUser;
import java.util.Date;
import javax.ejb.EJB;

/**
 *
 * @author archmage
 */
public class ReportKeyWord {

    @EJB
    CommonFunctions commonFunctions;

    Date fromDate;
    Date toDate;
    Institution institution;
    Department department;
    WebUser user;
    Item item;
    Summery summery;
    Settle settle;
    boolean bool;
    String string = "0";

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public WebUser getUser() {
        return user;
    }

    public void setUser(WebUser user) {
        this.user = user;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Summery getSummery() {
        return summery;
    }

    public void setSummery(Summery summery) {
        this.summery = summery;
    }

    public Settle getSettle() {
        return settle;
    }

    public void setSettle(Settle settle) {
        this.settle = settle;
    }

    public boolean isBool() {
        return bool;
    }

    public void setBool(boolean bool) {
        this.bool = bool;
    }

    public CommonFunctions getCommonFunctions() {
        if (commonFunctions == null) {
            commonFunctions = new CommonFunctions();
        }
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }
}
