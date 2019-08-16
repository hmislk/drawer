/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean;

import com.divudi.data.BillType;
import com.divudi.data.dataStructure.ReportKeyWord;
import com.divudi.entity.BillItem;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Summery;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.util.JsfUtil;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 *
 * @author archmage
 */
@Named(value = "drawerReportController")
@SessionScoped
public class DrawerReportController implements Serializable {

    @EJB
    BillItemFacade billItemFacade;

    @Inject
    private SessionController sessionController;

    List<BillItem> billItems;

    ReportKeyWord reportKeyWord;

    public DrawerReportController() {
    }

    public void createSummeryTable() {
        String s = " ";
        if (getReportKeyWord().getString().equals("1")) {
            s += " and bi.authorized=true ";
        }
        if (getReportKeyWord().getString().equals("2")) {
            s += " and bi.authorized=false ";
        }
        billItems = fetchBillItems(new BillType[]{BillType.CashIn}, getReportKeyWord().getFromDate(), getReportKeyWord().getToDate(),
                getReportKeyWord().getFromUser(), getReportKeyWord().getToUser(),
                getReportKeyWord().getDepartment(), getReportKeyWord().getInstitution(),getReportKeyWord().getItem(), s);
    }

    public void summeryAuthorized(BillItem bi) {
        if (bi.getAuthorizedComment() == null || bi.getAuthorizedComment().equals("") || bi.getAuthorizedComment().equals(" ")) {
            JsfUtil.addErrorMessage("Please Enter a Comment");
            return;
        }
        bi.setAuthorized(true);
        bi.setAuthorizedAt(new Date());
        bi.setAuthorizedBy(getSessionController().getLoggedUser());
        getBillItemFacade().edit(bi);
        JsfUtil.addSuccessMessage("Summery Authorized");

    }

    public void summeryUnAuthorized(BillItem bi) {
        bi.setAuthorized(false);
        bi.setAuthorizedComment("");
        bi.setAuthorizedAt(new Date());
        bi.setAuthorizedBy(getSessionController().getLoggedUser());
        getBillItemFacade().edit(bi);
        JsfUtil.addSuccessMessage("Summery Authorization Removed");

    }

    private List<BillItem> fetchBillItems(BillType[] bts, Date fd, Date td, WebUser fUser, WebUser tUser,
            Department d, Institution ins, Item i, String s) {
        List<BillItem> items;
        String sql;
        Map m = new HashMap();

        sql = "Select bi From BillItem bi "
                + " join bi.bill b where "
                + " b.retired=false "
                + " and b.billType in :bts "
                //                + " and b.createdAt between :fd and :td "
                + " and type(bi.item)=:class "
                + " and bi.item.department is not null ";

        if (s != null) {
            sql += s;
        }

        if (getReportKeyWord().getString1().equals("0")) {
            sql += " and b.createdAt between :fd and :td ";
        }
        if (getReportKeyWord().getString1().equals("1")) {
            sql += " and bi.fromTime between :fd and :td ";
        }
        if (getReportKeyWord().getString1().equals("2")) {
            sql += " and bi.toTime between :fd and :td ";
        }

        if (i != null) {
            sql += " and bi.item=:i ";
            m.put("i", i);
        }

        if (d != null) {
            sql += " and bi.item.department=:dep ";
            m.put("dep", d);
        }

        if (ins != null) {
            sql += " and bi.item.department.institution=:ins ";
            m.put("ins", ins);
        }

        if (fUser != null) {
            sql += " and b.toWebUser=:user ";
            m.put("user", fUser);
        }

        if (tUser != null) {
            sql += " and b.fromWebUser=:user ";
            m.put("user", tUser);
        }

        m.put("bts", Arrays.asList(bts));
        m.put("fd", fd);
        m.put("td", td);
        m.put("class", Summery.class);

        items = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
        System.out.println("billItems = " + items.size());

        return items;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public List<BillItem> getBillItems() {
        if (billItems == null) {
            billItems = new ArrayList<>();
        }
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

}
