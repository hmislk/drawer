/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean;

import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.InstitutionType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.ReportKeyWord;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.ejb.BillBean;
import com.divudi.ejb.CommonFunctions;
import com.divudi.ejb.PharmacyBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.PreBill;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.Summery;
import com.divudi.entity.WebUser;
import com.divudi.entity.cashTransaction.CashTransactionHistory;
import com.divudi.facade.CashTransactionHistoryFacade;
import com.divudi.facade.util.JsfUtil;
import java.text.SimpleDateFormat;
import java.util.Arrays;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class SearchController implements Serializable {

    private SearchKeyword searchKeyword;
    Date fromDate;
    Date toDate;
    private int maxResult = 50;
    private BillType billType;
    ////////////
    private List<Bill> bills;
    private List<Bill> selectedBills;
    private List<BillFee> billFees;
    private List<BillItem> billItems;
    private List<CashTransactionHistory> cashTransactionHistories;
    private List<String> headers;
    private List<ColumnModel> columnModels;
    private List<CashBookRow> cashBookRows;
    ////////////
    @EJB
    private CommonFunctions commonFunctions;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private CashTransactionHistoryFacade cashTransactionHistoryFacade;

    @EJB
    private BillBean billBean;
    @EJB
    private PharmacyBean pharmacyBean;
    //////////
    @Inject
    private SessionController sessionController;
    @Inject
    EnumController enumController;
    @Inject
    TransferController transferController;
    Institution institution;
    Institution bank;
    Department department;
    WebUser webUser;
    ReportKeyWord reportKeyWord;

    Bill realizingBill;

    double cashInOutVal;
    double cashTranVal;

    boolean withoutCancell = false;
    boolean onlyRealized = false;

    public void realizeBill() {
        if (realizingBill == null) {
            JsfUtil.addErrorMessage("Please select a  bill");
            return;
        }
        realizingBill.setReactivated(true);
        realizingBill.setApproveAt(new Date());
        getBillFacade().edit(realizingBill);
//        createGrnPaymentTable();
    }

    public Bill getRealizingBill() {
        return realizingBill;
    }

    public void setRealizingBill(Bill realizingBill) {
        this.realizingBill = realizingBill;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Institution getBank() {
        return bank;
    }

    public void setBank(Institution bank) {
        this.bank = bank;
    }

    public void makeListNull() {
        maxResult = 50;
        bills = null;
        selectedBills = null;
        billFees = null;
        billItems = null;
    }

    public void createPreRefundTable() {

        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from RefundBill b where b.billType = :billType "
                + " and b.institution=:ins and "
                + " (b.billedBill is null  or type(b.billedBill)=:billedClass ) "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false and b.deptId is not null ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billedClass", PreBill.class);
        temMap.put("billType", BillType.PharmacyPre);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        ////System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createReturnSaleBills() {

        Map m = new HashMap();
        m.put("bt", BillType.PharmacyPre);
        m.put("billedClass", PreBill.class);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        String sql;

        sql = "Select b from RefundBill b where  b.retired=false "
                + " and b.institution=:ins and "
                + " (b.billedBill is null  or type(b.billedBill)=:billedClass ) "
                + " and b.createdAt between :fd and :td"
                + " and b.billType=:bt ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getRefBillNo() != null && !getSearchKeyword().getRefBillNo().trim().equals("")) {
            sql += " and  (upper(b.billedBill.deptId) like :rNo )";
            m.put("rNo", "%" + getSearchKeyword().getRefBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            m.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        bills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createReturnBhtBills() {

        Map m = new HashMap();
        m.put("bt", BillType.PharmacyBhtPre);
        m.put("billedClass", PreBill.class);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        String sql;

        sql = "Select b from RefundBill b where  b.retired=false "
                + " and b.institution=:ins and "
                + " (b.billedBill is null  or type(b.billedBill)=:billedClass ) "
                + " and b.createdAt between :fd and :td"
                + " and b.billType=:bt ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.name) like :patientName )";
            m.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getRefBillNo() != null && !getSearchKeyword().getRefBillNo().trim().equals("")) {
            sql += " and  (upper(b.billedBill.deptId) like :rNo )";
            m.put("rNo", "%" + getSearchKeyword().getRefBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            m.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.bhtNo) like :bht )";
            m.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        bills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createVariantReportSearch() {
        String sql = "";
        HashMap tmp = new HashMap();

        sql = "Select b From PreBill b where b.cancelledBill is null  "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false and b.billType= :bTp ";

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  (upper(b.department.name) like :dep )";
            tmp.put("dep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCategory() != null && !getSearchKeyword().getCategory().trim().equals("")) {
            sql += " and  (upper(b.category.name) like :cat )";
            tmp.put("cat", "%" + getSearchKeyword().getCategory().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("bTp", BillType.PharmacyMajorAdjustment);
        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP);
    }

    List<Bill> prescreptionBills;

    public List<Bill> getPrescreptionBills() {
        return prescreptionBills;
    }

    public void createPrescreptionBills() {
        Map m = new HashMap();
        m.put("bt", BillType.PharmacyPre);
        m.put("rBt", BillType.PharmacySale);
        m.put("class", PreBill.class);
        m.put("rClass", BilledBill.class);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        String sql;
        sql = "Select b from Bill b "
                + " where b.retired=false and b.createdAt between :fd and :td and b.billType=:bt "
                + " and b.referredBy is not null "
                + " and b.institution=:ins "
                + " and b.referenceBill.billType=:rBt "
                + " and type(b)=:class "
                + " and type(b.referenceBill)=:rClass ";

        if (department != null) {
            sql += " and b.department=:dept ";
            m.put("dept", department);
        }

        sql += " order by b.createdAt ";

        prescreptionBills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

    }

    public void createPharmacyTable() {

        Map m = new HashMap();
        m.put("bt", BillType.PharmacyPre);
        m.put("rBt", BillType.PharmacySale);
        m.put("class", PreBill.class);
        m.put("rClass", BilledBill.class);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        String sql;

        sql = "Select b from Bill b where b.retired=false and b.createdAt  "
                + " between :fd and :td and b.billType=:bt and b.institution=:ins and"
                + " b.referenceBill.billType=:rBt and type(b)=:class and type(b.referenceBill)=:rClass ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            m.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  (upper(b.department.name) like :dep )";
            m.put("dep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            m.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        //     //////System.out.println("sql = " + sql);
        bills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createPharmacyTableBht() {
        createTableBht(BillType.PharmacyBhtPre);
    }

    public void createStoreTableBht() {
        createTableBht(BillType.StoreBhtPre);
    }

    public void createTableBht(BillType btp) {

        Map m = new HashMap();
        m.put("bt", btp);
        m.put("class", PreBill.class);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        String sql;

        sql = "Select b from Bill b where b.retired=false "
                + "  and b.billedBill is null and b.createdAt "
                + " between :fd and :td and b.billType=:bt"
                + " and b.institution=:ins "
                + " and type(b)=:class ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.name) like :patientName )";
            m.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.bhtNo) like :bht )";
            m.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  (upper(b.department.name) like :dep )";
            m.put("dep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            m.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        //     //////System.out.println("sql = " + sql);
        bills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createIssueTable() {
        String sql;
        HashMap tmp = new HashMap();
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("dep", getSessionController().getDepartment());
        tmp.put("bTp", BillType.PharmacyTransferIssue);
        sql = "Select b From BilledBill b where b.retired=false and "
                + " b.toDepartment=:dep and b.billType= :bTp "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  (upper(b.toStaff.person.name) like :stf )";
            tmp.put("stf", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  (upper(b.department.name) like :fDep )";
            tmp.put("fDep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, 50);

        for (Bill b : bills) {
            b.setTmpRefBill(getRefBill(b));

        }

    }

    private Bill getRefBill(Bill b) {
        String sql = "Select b From Bill b where b.retired=false "
                + " and b.cancelled=false and b.billType=:btp and "
                + " b.referenceBill=:ref";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", BillType.PharmacyTransferReceive);
        return getBillFacade().findFirstBySQL(sql, hm);
    }

    public void makeNull() {
        searchKeyword = null;
        bills = null;

    }

    public void createTableByBillType() {

        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where b.retired=false and "
                + " (type(b)=:class1 or type(b)=:class2) "
                + " and b.department=:dep and b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFromInstitution() != null && !getSearchKeyword().getFromInstitution().trim().equals("")) {
            sql += " and  (upper(b.fromInstitution.name) like :frmIns )";
            temMap.put("frmIns", "%" + getSearchKeyword().getFromInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFromDepartment() != null && !getSearchKeyword().getFromDepartment().trim().equals("")) {
            sql += " and  (upper(b.fromDepartment.name) like :frmDept )";
            temMap.put("frmDept", "%" + getSearchKeyword().getFromDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getToInstitution() != null && !getSearchKeyword().getToInstitution().trim().equals("")) {
            sql += " and  (upper(b.toInstitution.name) like :toIns )";
            temMap.put("toIns", "%" + getSearchKeyword().getToInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getToDepartment() != null && !getSearchKeyword().getToDepartment().trim().equals("")) {
            sql += " and  (upper(b.toDepartment.name) like :toDept )";
            temMap.put("toDept", "%" + getSearchKeyword().getToDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getRefBillNo() != null && !getSearchKeyword().getRefBillNo().trim().equals("")) {
            sql += " and  (upper(b.referenceBill.deptId) like :refId )";
            temMap.put("refId", "%" + getSearchKeyword().getRefBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  (upper(b.invoiceNumber) like :inv )";
            temMap.put("inv", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and b.id in (select bItem.bill.id  "
                    + " from BillItem bItem where bItem.retired=false and  "
                    + " (upper(bItem.item.name) like :itm ))";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and b.id in (select bItem.bill.id  "
                    + " from BillItem bItem where bItem.retired=false and  "
                    + " (upper(bItem.item.code) like :cde ))";
            temMap.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        temMap.put("class1", BilledBill.class);
        temMap.put("class2", PreBill.class);
        temMap.put("billType", billType);
        temMap.put("dep", getSessionController().getDepartment());
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        //temMap.put("dep", getSessionController().getDepartment());
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, maxResult);
        //     ////System.err.println("SIZE : " + lst.size());

    }

    public void createTableByBillTypeAllDepartment() {

        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where b.retired=false and "
                + " (type(b)=:class1 or type(b)=:class2) "
                + " and  b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFromInstitution() != null && !getSearchKeyword().getFromInstitution().trim().equals("")) {
            sql += " and  (upper(b.fromInstitution.name) like :frmIns )";
            temMap.put("frmIns", "%" + getSearchKeyword().getFromInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getToInstitution() != null && !getSearchKeyword().getToInstitution().trim().equals("")) {
            sql += " and  (upper(b.toInstitution.name) like :toIns )";
            temMap.put("toIns", "%" + getSearchKeyword().getToInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getRefBillNo() != null && !getSearchKeyword().getRefBillNo().trim().equals("")) {
            sql += " and  (upper(b.referenceBill.deptId) like :refId )";
            temMap.put("refId", "%" + getSearchKeyword().getRefBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  (upper(b.invoiceNumber) like :inv )";
            temMap.put("inv", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and b.id in (select bItem.bill.id  "
                    + " from BillItem bItem where bItem.retired=false and  "
                    + " (upper(bItem.item.name) like :itm ))";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and b.id in (select bItem.bill.id  "
                    + " from BillItem bItem where bItem.retired=false and  "
                    + " (upper(bItem.item.code) like :cde ))";
            temMap.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        temMap.put("class1", BilledBill.class);
        temMap.put("class2", PreBill.class);
        temMap.put("billType", billType);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        //temMap.put("dep", getSessionController().getDepartment());
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, maxResult);
        //     ////System.err.println("SIZE : " + lst.size());

    }

    public void createRequestTable() {
        String sql;

        HashMap tmp = new HashMap();
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("toDep", getSessionController().getDepartment());
        tmp.put("bTp", BillType.PharmacyTransferRequest);

        sql = "Select b From Bill b where "
                + " b.retired=false and  b.toDepartment=:toDep"
                + " and b.billType= :bTp and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  (upper(b.department.name) like :dep )";
            tmp.put("dep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, 50);

        for (Bill b : bills) {
            b.setListOfBill(getIssudBills(b));
        }

    }

    public void viewRequestTable() {
        String sql;

        HashMap tmp = new HashMap();
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("dep", getSessionController().getDepartment());
        tmp.put("bTp", BillType.PharmacyTransferRequest);

        sql = "Select b From Bill b where "
                + " b.retired=false and  b.department=:dep"
                + " and b.billType= :bTp and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  (upper(b.toDepartment.name) like :toDep )";
            tmp.put("toDep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP);

        for (Bill b : bills) {
            b.setListOfBill(getIssudBills(b));
        }

    }

    public void createListToCashRecieve() {
        String sql;

        HashMap tmp = new HashMap();
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("toWeb", getSessionController().getLoggedUser());
        tmp.put("bts", Arrays.asList(new BillType[]{BillType.CashOut, BillType.SummeryOut, BillType.HandOver}));
//        tmp.put("bTp", BillType.CashOut);
//        tmp.put("bTp2", BillType.SummeryOut);

        sql = "Select b From Bill b where "
                + " b.retired=false "
                + " and b.toWebUser=:toWeb "
                + " and b.billType in :bts "
                //                + " and (b.billType= :bTp or b.billType= :bTp2) "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :net )";
            tmp.put("net", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPersonName() != null && !getSearchKeyword().getPersonName().trim().equals("")) {
            sql += " and  (upper(b.fromWebUser.webUserPerson.name) like :dep )";
            tmp.put("dep", "%" + getSearchKeyword().getPersonName().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, 50);
        List<Bill> removeBills = new ArrayList<>();
        for (Bill b : bills) {
            if (b.getBillType() == BillType.HandOver && b.getPaymentMethod() != PaymentMethod.IOU) {
                removeBills.add(b);
            }
        }
        bills.removeAll(removeBills);
    }

    public void createListToCashNotRecieve() {
        String sql;

        HashMap tmp = new HashMap();
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("bts", Arrays.asList(new BillType[]{BillType.CashOut, BillType.SummeryOut, BillType.HandOver}));
//        tmp.put("bTp", BillType.CashOut);
//        tmp.put("bTp1", BillType.SummeryOut);
        tmp.put("class", BilledBill.class);

        sql = "Select b From Bill b where "
                + " b.retired=false "
                + " and b.billType in :bts "
                //                + " and (b.billType= :bTp or b.billType= :bTp1) "
                + " and type(b)=:class "
                + " and b.cancelled=false "
                + " and b.createdAt between :fromDate and :toDate "
                + " order by b.createdAt desc  ";

        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP);
        System.out.println("bills.size() = " + bills.size());

        List<Bill> removeBills = new ArrayList<>();
        for (Bill bill : bills) {
            if (bill.getBillType() == BillType.HandOver && bill.getPaymentMethod() != PaymentMethod.IOU) {
                removeBills.add(bill);
                continue;
            }
            boolean status = false;
            System.out.println("bill.getBackwardReferenceBills().size() = " + bill.getForwardReferenceBills().size());
            for (Bill b : bill.getForwardReferenceBills()) {
                System.out.println("b.isCancelled() = " + b.isCancelled());
                System.out.println("b.isRetired() = " + b.isRetired());
                if (b.isCancelled() || b.isRetired()) {
                    status = true;
                } else {
                    status = false;
                }
            }
            System.out.println("status = " + status);
            if (!status && !bill.getForwardReferenceBills().isEmpty()) {
                removeBills.add(bill);
            }
        }
        System.out.println("bills.size() = " + bills.size());
        bills.removeAll(removeBills);
        System.out.println("bills.size() = " + bills.size());
    }

    public void createCashBook() {
        String sql;

        HashMap m = new HashMap();
//        tmp.put("class", BilledBill.class);

        sql = "Select bi From BillItem bi join bi.bill b where "
                + " b.retired=false "
                + " and b.billType in :bts "
                //                + " and type(b)=:class "
                //                + " and b.cancelled=false "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getReportKeyWord().getInstitution() != null) {
            sql += " and b.institution=:ins ";
            m.put("ins", getReportKeyWord().getInstitution());
        }

        if (getDepartment() != null) {
            sql += " and b.department=:dep ";
            m.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getUser() != null) {
            sql += " and b.creater=:u ";
            m.put("u", getReportKeyWord().getUser());
        }

        sql += " order by b.id ";

        m.put("toDate", getReportKeyWord().getToDate());
        m.put("fromDate", getReportKeyWord().getFromDate());
        m.put("bts", Arrays.asList(new BillType[]{BillType.SummeryOut, BillType.CashSettle,
            BillType.CreditCardSettle,
            BillType.ChequeSettle,
            BillType.SlipSettle,
            BillType.CreditSettle,
            BillType.IOUSettle,}));

        billItems = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
        System.out.println("billItems.size() = " + billItems.size());
        double d = 0.0;
        for (BillItem bi : billItems) {
            d += bi.getNetValue();
            bi.setTotalGrnQty(d);
        }

//        List<Bill> removeBills = new ArrayList<>();
//        for (Bill bill : bills) {
//            boolean status = false;
//            System.out.println("bill.getBackwardReferenceBills().size() = " + bill.getBackwardReferenceBills().size());
//            for (Bill b : bill.getBackwardReferenceBills()) {
//                System.out.println("b.isCancelled() = " + b.isCancelled());
//                System.out.println("b.isRetired() = " + b.isRetired());
//                if (b.isCancelled() || b.isRetired()) {
//                    status = true;
//                } else {
//                    status = false;
//                }
//            }
//            if (!status && !bill.getBackwardReferenceBills().isEmpty()) {
//                removeBills.add(bill);
//            }
//        }
//        System.out.println("bills.size() = " + bills.size());
//        bills.removeAll(removeBills);
        System.out.println("billItems.size() = " + billItems.size());
    }

    public void createCashBook3D() {
        columnModels = new ArrayList<>();
        fetchHeaders();
        fetchCashBook3D();

        CashBookRow row = new CashBookRow();
        row.setString1("Closing Balance");
        int i = cashBookRows.size();
        int j = headers.size();
        System.out.println("j = " + j);
//        row.setCategoryName("Total");
        List<Double> list = new ArrayList<>();
//        System.out.println("Time 1 = " + new Date());
        for (int k = 0; k < j; k++) {
            double total = 0.0;
            for (int l = 0; l < i; l++) {
                total += cashBookRows.get(l).getTotals().get(k);
            }
            list.add((double) total);
        }
        row.setTotals(list);
//        System.out.println("Time 2 = " + new Date());
        cashBookRows.add(row);

        Long l = 0l;
        for (String h : headers) {
            ColumnModel c = new ColumnModel();
            c.setHeader(h);
            c.setProperty(l.toString());
            columnModels.add(c);
            l++;
        }
//        commonController.printReportDetails(fromDate, toDate, startTime, "OPD Billitem with bill");

    }

    public void createCashBook3DAccountant() {
        columnModels = new ArrayList<>();
        fetchHeadersAccountant();
        fetchCashBook3D();

        CashBookRow row = new CashBookRow();
        row.setString1("Closing Balance");
        int i = cashBookRows.size();
        int j = headers.size();
        System.out.println("j = " + j);
//        row.setCategoryName("Total");
        List<Double> list = new ArrayList<>();
//        System.out.println("Time 1 = " + new Date());
        for (int k = 0; k < j; k++) {
            double total = 0.0;
            for (int l = 0; l < i; l++) {
                total += cashBookRows.get(l).getTotals().get(k);
            }
            list.add((double) total);
        }
        row.setTotals(list);
//        System.out.println("Time 2 = " + new Date());
        cashBookRows.add(row);

        Long l = 0l;
        for (String h : headers) {
            ColumnModel c = new ColumnModel();
            c.setHeader(h);
            c.setProperty(l.toString());
            columnModels.add(c);
            l++;
        }
//        commonController.printReportDetails(fromDate, toDate, startTime, "OPD Billitem with bill");

    }

    public void createPharmacyBillItemTable() {
        //  searchBillItems = null;
        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", BillType.PharmacyPre);
        m.put("rBType", BillType.PharmacySale);
        m.put("ins", getSessionController().getInstitution());
        m.put("class", PreBill.class);
        m.put("rClass", BilledBill.class);

        sql = "select bi from BillItem bi"
                + " where  type(bi.bill)=:class and type(bi.bill.referenceBill)=:rClass"
                + " and bi.bill.institution=:ins"
                + " and bi.bill.billType=:bType and "
                + " bi.bill.referenceBill.billType=:rBType "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(bi.bill.patient.person.name) like :patientName )";
            m.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(bi.bill.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(bi.netValue) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(bi.item.name) like :itm )";
            m.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and  (upper(bi.item.code) like :cde )";
            m.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc  ";

        billItems = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createPharmacyAdjustmentBillItemTable() {
        //  searchBillItems = null;
        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", BillType.PharmacyAdjustment);
        m.put("ins", getSessionController().getInstitution());
        m.put("class", PreBill.class);

        sql = "select bi from BillItem bi"
                + " where  type(bi.bill)=:class "
                + " and bi.bill.institution=:ins"
                + " and bi.bill.billType=:bType  "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(bi.bill.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(bi.item.name) like :itm )";
            m.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and  (upper(bi.item.code) like :cde )";
            m.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc  ";

        billItems = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createDrawerAdjustmentTable() {
        //  searchBillItems = null;
        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", BillType.DrawerAdjustment);
        m.put("ins", getSessionController().getInstitution());
        m.put("class", BilledBill.class);

        sql = "select bi from Bill bi"
                + " where  type(bi)=:class "
                + " and bi.institution=:ins"
                + " and bi.billType=:bType  "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(bi.insId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc  ";

        bills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createPharmacyBillItemTableBht() {
        createBillItemTableBht(BillType.StoreBhtPre);
    }

    public void createStoreBillItemTableBht() {
        createBillItemTableBht(BillType.StoreBhtPre);
    }

    public void createBillItemTableBht(BillType btp) {
        //  searchBillItems = null;
        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", btp);
        m.put("ins", getSessionController().getInstitution());
        m.put("class", PreBill.class);

        sql = "select bi from BillItem bi"
                + " where  type(bi.bill)=:class "
                + " and bi.bill.institution=:ins"
                + " and bi.bill.billType=:bType and "
                + " bi.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(bi.bill.patient.person.name) like :patientName )";
            m.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(bi.bill.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(bi.netValue) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(bi.item.name) like :itm )";
            m.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and  (upper(bi.item.code) like :cde )";
            m.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(bi.bill.patientEncounter.bhtNo) like :bht )";
            m.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc  ";

        billItems = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createPoRequestedAndApproved() {
        bills = null;
        String sql = "";
        HashMap tmp = new HashMap();

        sql = "Select b From BilledBill b where "
                + " b.createdAt between :fromDate and :toDate "
                + "and b.retired=false and b.billType= :bTp  ";

        sql += createKeySql(tmp);

        sql += " order by b.createdAt desc  ";

        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("bTp", BillType.PharmacyOrder);
        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, maxResult);

    }

    public void createApproved() {
        bills = null;
        String sql = "";
        HashMap tmp = new HashMap();

        sql = "Select b From BilledBill b where"
                + " b.referenceBill.creater is not null and b.referenceBill.cancelled=false "
                + " and b.createdAt between :fromDate and :toDate "
                + "and b.retired=false and b.billType= :bTp  ";

        sql += createKeySql(tmp);

        sql += " order by b.createdAt desc  ";

        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("bTp", BillType.PharmacyOrder);
        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, maxResult);

    }

    public void createNotApproved() {
        bills = null;
        String sql = "";
        HashMap tmp = new HashMap();

        sql = "Select b From BilledBill b where  b.referenceBill is null  "
                + " and b.createdAt between :fromDate and :toDate "
                + "and b.retired=false and b.billType= :bTp ";

        sql += createKeySql(tmp);
        sql += " order by b.createdAt desc  ";

        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("bTp", BillType.PharmacyOrder);
        List<Bill> lst1 = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, maxResult);

        sql = "Select b From BilledBill b where "
                + " b.referenceBill.creater is null "
                + " and b.createdAt between :fromDate and :toDate "
                + "and b.retired=false and b.billType= :bTp  ";

        sql += createKeySql(tmp);
        sql += " order by b.createdAt desc  ";

        List<Bill> lst2 = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, maxResult);

        sql = "Select b From BilledBill b where "
                + "  b.referenceBill.creater is not null and b.referenceBill.cancelled=true "
                + " and b.createdAt between :fromDate and :toDate "
                + "and b.retired=false and b.billType= :bTp ";

        sql += createKeySql(tmp);
        sql += " order by b.createdAt desc  ";

        List<Bill> lst3 = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, maxResult);

        lst1.addAll(lst2);
        lst1.addAll(lst3);

        bills = lst1;

    }

    private String createKeySql(HashMap tmp) {
        String sql = "";
        if (getSearchKeyword().getToInstitution() != null && !getSearchKeyword().getToInstitution().trim().equals("")) {
            sql += " and  (upper(b.toInstitution.name) like :toIns )";
            tmp.put("toIns", "%" + getSearchKeyword().getToInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCreator() != null && !getSearchKeyword().getCreator().trim().equals("")) {
            sql += " and  (upper(b.creater.webUserPerson.name) like :crt )";
            tmp.put("crt", "%" + getSearchKeyword().getCreator().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  (upper(b.department.name) like :crt )";
            tmp.put("crt", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getRefBillNo() != null && !getSearchKeyword().getRefBillNo().trim().equals("")) {
            sql += " and  (upper(b.referenceBill.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getRefBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :reqTotal )";
            tmp.put("reqTotal", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.referenceBill.netTotal) like :appTotal )";
            tmp.put("appTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        return sql;

    }

    private String keysForGrnReturn(HashMap tmp) {
        String sql = "";
        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  (upper(b.invoiceNumber) like :invoice )";
            tmp.put("invoice", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getRefBillNo() != null && !getSearchKeyword().getRefBillNo().trim().equals("")) {
            sql += " and  (upper(b.referenceBill.deptId) like :refNo )";
            tmp.put("refNo", "%" + getSearchKeyword().getRefBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFromInstitution() != null && !getSearchKeyword().getFromInstitution().trim().equals("")) {
            sql += " and  (upper(b.fromInstitution.name) like :frmIns )";
            tmp.put("frmIns", "%" + getSearchKeyword().getFromInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.referenceBill.netTotal) like :total )";
            tmp.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            tmp.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        return sql;
    }

    public void createGrnTable() {
        bills = null;
        String sql;
        HashMap tmp = new HashMap();

        sql = "Select b From BilledBill b where  b.retired=false and b.billType= :bTp "
                + " and b.institution=:ins and"
                + " b.createdAt between :fromDate and :toDate ";

        sql += keysForGrnReturn(tmp);

        sql += " order by b.createdAt desc  ";

        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("ins", getSessionController().getInstitution());
        tmp.put("bTp", BillType.PharmacyGrnBill);
        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, 50);

        for (Bill b : bills) {
            b.setListOfBill(getReturnBill(b));
        }

    }

    public void createGrnTableAllIns() {
        bills = null;
        String sql;
        HashMap tmp = new HashMap();

        sql = "Select b From BilledBill b where  b.retired=false and b.billType= :bTp "
                + " and b.createdAt between :fromDate and :toDate ";

        sql += keysForGrnReturn(tmp);

        sql += " order by b.createdAt desc  ";

        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        // tmp.put("ins", getSessionController().getInstitution());
        tmp.put("bTp", BillType.PharmacyGrnBill);
        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, 50);

        for (Bill b : bills) {
            b.setListOfBill(getReturnBill(b));
        }

    }

    private List<Bill> getReturnBill(Bill b) {
        String sql = "Select b From BilledBill b where b.retired=false and b.creater is not null"
                + " and  b.billType=:btp and "
                + " b.referenceBill=:ref";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", BillType.PharmacyGrnReturn);
        return getBillFacade().findBySQL(sql, hm);
    }

    public void createPoTable() {
        bills = null;
        String sql;
        HashMap tmp = new HashMap();

        sql = "Select b From BilledBill b where  b.retired=false and b.billType= :bTp"
                + " and  b.referenceBill.institution=:ins and "
                + " b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getToInstitution() != null && !getSearchKeyword().getToInstitution().trim().equals("")) {
            sql += " and  (upper(b.toInstitution.name) like :toIns )";
            tmp.put("toIns", "%" + getSearchKeyword().getToInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            tmp.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("ins", getSessionController().getInstitution());
        tmp.put("bTp", BillType.PharmacyOrderApprove);
        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, 50);

        for (Bill b : bills) {
            b.setListOfBill(getGrns(b));
        }

    }

    private List<Bill> getGrns(Bill b) {
        String sql = "Select b From Bill b where b.retired=false and b.creater is not null"
                + " and b.billType=:btp and "
                + " b.referenceBill=:ref";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", BillType.PharmacyGrnBill);
        return getBillFacade().findBySQL(sql, hm);
    }

    private List<Bill> getIssudBills(Bill b) {
        String sql = "Select b From Bill b where b.retired=false and b.creater is not null"
                + " and b.billType=:btp and "
                + " b.referenceBill=:ref or b.backwardReferenceBill=:ref";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", BillType.PharmacyTransferIssue);
        return getBillFacade().findBySQL(sql, hm);
    }

    private List<Bill> getIssuedBills(Bill b) {
        String sql = "Select b From Bill b where b.retired=false and b.creater is not null"
                + " and b.billType=:btp and "
                + " b.referenceBill=:ref and b.backwardReferenceBill=:ref";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", BillType.PharmacyTransferIssue);
        return getBillFacade().findBySQL(sql, hm);
    }

    public void createDueFeeTable() {

        String sql;
        Map temMap = new HashMap();

        sql = "select b from BillFee b where b.retired=false and "
                + " b.bill.billType=:btp "
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0 and"
                + "  b.bill.billDate between :fromDate"
                + " and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.bill.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.feeValue) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  (upper(b.staff.speciality.name) like :special )";
            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  (upper(b.staff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(b.billItem.item.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += "  order by b.staff.id    ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", BillType.OpdBill);

        billFees = getBillFeeFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createDueFeeTableInward() {

        String sql;
        Map temMap = new HashMap();

        sql = "select b from BillFee b where "
                + " b.retired=false "
                + " and (b.bill.billType=:btp or b.bill.billType=:btp2 )"
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0"
                + " and  b.bill.billDate between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.bill.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.bill.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.feeValue) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  (upper(b.staff.speciality.name) like :special )";
            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  (upper(b.staff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(b.billItem.item.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += "  order by b.staff.id    ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", BillType.InwardBill);
        temMap.put("btp2", BillType.InwardProfessional);

        billFees = getBillFeeFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createPaymentTable() {
        billItems = null;
        HashMap temMap = new HashMap();
        String sql = "Select b FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.referenceBill.billType=:refType "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.bill.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.bill.deptId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.staff.speciality.name) like :special )";
            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.staff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.billItem.item.name) like :item )";
            temMap.put("item", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.feeValue) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
        temMap.put("refType", BillType.OpdBill);

        billItems = getBillItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createProfessionalPaymentTableInward() {
        billItems = null;
        HashMap temMap = new HashMap();
        String sql = "Select b FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and (b.referenceBill.billType=:refType or b.referenceBill.billType=:refType2) "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.bill.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.bill.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.bill.deptId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.staff.speciality.name) like :special )";
            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.staff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.billItem.item.name) like :item )";
            temMap.put("item", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.feeValue) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
        temMap.put("refType", BillType.InwardBill);
        temMap.put("refType2", BillType.InwardProfessional);

        billItems = getBillItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createBillItemTableByKeyword() {

        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", BillType.OpdBill);
        m.put("ins", getSessionController().getInstitution());

        sql = "select bi from BillItem bi where bi.bill.institution=:ins "
                + " and bi.bill.billType=:bType "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (searchKeyword.getPatientName() != null && !searchKeyword.getPatientName().trim().equals("")) {
            sql += " and  (upper(bi.bill.patient.person.name) like :patientName )";
            m.put("patientName", "%" + searchKeyword.getPatientName().trim().toUpperCase() + "%");
        }

        if (searchKeyword.getPatientPhone() != null && !searchKeyword.getPatientPhone().trim().equals("")) {
            sql += " and  (upper(bi.bill.patient.person.phone) like :patientPhone )";
            m.put("patientPhone", "%" + searchKeyword.getPatientPhone().trim().toUpperCase() + "%");
        }

        if (searchKeyword.getBillNo() != null && !searchKeyword.getBillNo().trim().equals("")) {
            sql += " and  (upper(bi.bill.insId) like :billNo )";
            m.put("billNo", "%" + searchKeyword.getBillNo().trim().toUpperCase() + "%");
        }

        if (searchKeyword.getItemName() != null && !searchKeyword.getItemName().trim().equals("")) {
            sql += " and  (upper(bi.item.name) like :itemName )";
            m.put("itemName", "%" + searchKeyword.getItemName().trim().toUpperCase() + "%");
        }

        if (searchKeyword.getToInstitution() != null && !searchKeyword.getToInstitution().trim().equals("")) {
            sql += " and  (upper(bi.bill.toInstitution.name) like :toIns )";
            m.put("toIns", "%" + searchKeyword.getToInstitution().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc  ";
        ////System.err.println("Sql " + sql);

        billItems = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 50);

        //   searchBillItems = new LazyBillItem(tmp);
    }

    public void createPatientInvestigationsTable() {

        String sql = "select pi from PatientInvestigation pi join pi.investigation  "
                + " i join pi.billItem.bill b join b.patient.person p where "
                + " b.createdAt between :fromDate and :toDate  ";

        Map temMap = new HashMap();

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(p.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(p.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(i.name) like :itm )";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += " order by pi.id desc  ";
//    

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        ////System.err.println("Sql " + sql);
    }

    public void createPreBillsForReturn() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from PreBill b where b.billType = :billType and "
                + " b.institution=:ins and (b.billedBill is null) and "
                + " b.referenceBill.billType=:refBillType "
                + " and b.createdAt between :fromDate and :toDate and b.retired=false ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        temMap.put("billType", BillType.PharmacyPre);
        temMap.put("refBillType", BillType.PharmacySale);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", getSessionController().getInstitution());

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public BillBean getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBean billBean) {
        this.billBean = billBean;
    }

    public void createPreBillsNotPaid() {

        bills = getBillBean().billsForTheDayNotPaid(BillType.PharmacyPre, getSessionController().getDepartment());

    }

    public void addToStock() {
        for (Bill b : getSelectedBills()) {
            if (b.checkActiveCashPreBill()) {
                continue;
            }

            Bill prebill = getPharmacyBean().reAddToStock(b, getSessionController().getLoggedUser(),
                    getSessionController().getDepartment(), BillNumberSuffix.PRECAN);

            b.setCancelled(true);
            b.setCancelledBill(prebill);
            getBillFacade().edit(b);
        }

        createPreBillsNotPaid();

    }

    public void removeStockItems() {
        for (Bill bb : getSelectedBills()) {

            bb.setRetired(true);
            bb.setRetireComments("Bulk Bill Remove");
            getBillFacade().edit(bb);
        }

        createPreBillsNotPaid();
    }

    public void createPreTable() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from PreBill b where b.billType = :billType "
                + " and b.institution=:ins and b.billedBill is null "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false and b.deptId is not null "
                + " and b.department=:dep ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.PharmacyPre);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("dep", getSessionController().getDepartment());

        ////System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createGrnPaymentTable() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b"
                + " where b.billType = :billType "
                + " and b.institution=:ins "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false ";

        if (onlyRealized) {
            sql += " and (b.paymentMethod in :pms or (b.paymentMethod=:pm and b.reactivated=true)) ";

            List<PaymentMethod> pms = Arrays.asList(PaymentMethod.Cash, PaymentMethod.Slip, PaymentMethod.Agent, PaymentMethod.Card, PaymentMethod.Credit);

            temMap.put("pms", pms);
            temMap.put("pm", PaymentMethod.Cheque);
        }

        if (!withoutCancell) {
            System.err.println("innnn");
            //for hide cancell bill
            sql += " and b.cancelled=false ";
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (institution != null) {
            sql += " and b.toInstitution=:toInstitution ";
            temMap.put("toInstitution", institution);
        }

        if (bank != null) {
            sql += " and b.bank=:bankName ";
            temMap.put("bankName", bank);
        }

        if (getSearchKeyword().getToInstitution() != null && !getSearchKeyword().getToInstitution().trim().equals("")) {
            sql += " and  (upper(b.toInstitution.name) like :toInstitution )";
            temMap.put("toInstitution", "%" + getSearchKeyword().getToInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBank() != null && !getSearchKeyword().getBank().trim().equals("")) {
            sql += " and  (upper(b.bank.name) like :bankName )";
            temMap.put("bankName", "%" + getSearchKeyword().getBank().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  (upper(b.chequeRefNo) like :chequeNo )";
            temMap.put("chequeNo", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.GrnPayment);
        temMap.put("insTp", InstitutionType.Dealer);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        System.err.println("Sql " + sql);
        //    //System.out.println("temMap = " + temMap);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);
        getNetTotal(bills);

        //    //System.out.println("bills.size() = " + bills.size());
    }

    public void getNetTotal(List<Bill> bills) {
        netTotal = 0.0;
        for (Bill b : bills) {
            netTotal += b.getNetTotal();
        }
    }

    public void createGrnPaymentTableChequeDate() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b"
                + " where b.billType = :billType "
                + " and b.institution=:ins "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.chequeDate between :fromDate and :toDate"
                + " and b.retired=false ";

        if (onlyRealized) {
            sql += " and (b.paymentMethod in :pms or (b.paymentMethod=:pm and b.reactivated=true)) ";

            List<PaymentMethod> pms = Arrays.asList(PaymentMethod.Cash, PaymentMethod.Slip, PaymentMethod.Agent, PaymentMethod.Card, PaymentMethod.Credit);

            temMap.put("pms", pms);
            temMap.put("pm", PaymentMethod.Cheque);
        }

        if (!withoutCancell) {
            System.err.println("innnn");
            //for hide cancell bill
            sql += " and b.cancelled=false  ";
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (institution != null) {
            sql += " and b.toInstitution=:toInstitution ";
            temMap.put("toInstitution", institution);
        }

        if (bank != null) {
            sql += " and b.bank=:bankName ";
            temMap.put("bankName", bank);
        }

        if (getSearchKeyword().getToInstitution() != null && !getSearchKeyword().getToInstitution().trim().equals("")) {
            sql += " and  (upper(b.toInstitution.name) like :toInstitution )";
            temMap.put("toInstitution", "%" + getSearchKeyword().getToInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBank() != null && !getSearchKeyword().getBank().trim().equals("")) {
            sql += " and  (upper(b.bank.name) like :bankName )";
            temMap.put("bankName", "%" + getSearchKeyword().getBank().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  (upper(b.chequeRefNo) like :chequeNo )";
            temMap.put("chequeNo", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        sql += " order by b.chequeDate ";
//      sql += " order by b.chequeDate desc  ";
        temMap.put("billType", BillType.GrnPayment);
        temMap.put("insTp", InstitutionType.Dealer);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        System.err.println("Sql " + sql);
        //    //System.out.println("temMap = " + temMap);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);
        getNetTotal(bills);

        //    //System.out.println("bills.size() = " + bills.size());
    }

    double netTotal;

    public void createGrnChequePayment() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b"
                + " where b.billType = :billType "
                + " and b.paymentMethod=:pm "
                + " and b.chequeDate between :fromDate and :toDate"
                + " and b.retired=false ";
        temMap.put("billType", BillType.GrnPayment);
        temMap.put("pm", PaymentMethod.Cheque);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        if (institution != null) {
            sql = sql + " and b.toInstitution=:ins ";
            temMap.put("ins", institution);
        }

        if (department != null) {
            sql = sql + " and b.department=:dep ";
            temMap.put("dep", department);
        }
        sql = sql + " order by b.chequeDate ";
        //System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
        netTotal = 0.0;
//        netTotal = sumFrom(bills).getNetTotal();

        for (Bill b : bills) {
            netTotal += b.getNetTotal();
        }
    }

//    public void createGrnChequePaymentAll() {
//        bills = null;
//        String sql;
//        Map temMap = new HashMap();
//
//        sql = "select b from Bill b"
//                + " where b.billType = :billType "
//                //                + " and b.institution=:ins "
//                + " and b.paymentMethod=:pm "
//                + " and b.toInstitution.institutionType=:insTp "
//                + " and b.createdAt between :fromDate and :toDate"
//                + " and b.retired=false "
//                + " order by b.createdAt ";
////    
//        temMap.put("billType", BillType.GrnPayment);
//        temMap.put("insTp", InstitutionType.Dealer);
//        temMap.put("pm", PaymentMethod.Cheque);
//        temMap.put("toDate", getToDate());
//        temMap.put("fromDate", getFromDate());
////        temMap.put("ins", getSessionController().getInstitution());
//
//        ////System.err.println("Sql " + sql);
//        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
//
//        netTotal = 0.0;
//
//        for (Bill b : bills) {
//            netTotal += b.getNetTotal();
//        }
//
//    }
    public void createGrnPaymentTableStore() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b"
                + " where b.billType = :billType "
                + " and b.institution=:ins "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.GrnPayment);
        temMap.put("insTp", InstitutionType.StoreDealor);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        ////System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createGrnPaymentTableAll() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b "
                + " where b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.retired=false ";

        if (onlyRealized) {
            sql += " and (b.paymentMethod in :pms or (b.paymentMethod=:pm and b.reactivated=true)) ";

            List<PaymentMethod> pms = Arrays.asList(PaymentMethod.Cash, PaymentMethod.Slip, PaymentMethod.Agent, PaymentMethod.Card, PaymentMethod.Credit);

            temMap.put("pms", pms);
            temMap.put("pm", PaymentMethod.Cheque);
        }

        if (!withoutCancell) {
            System.err.println("innnn");
            //for hide cancell bill
            sql += " and b.cancelled=false  ";
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (institution != null) {
            sql += " and b.toInstitution=:toInstitution ";
            temMap.put("toInstitution", institution);
        }

        if (bank != null) {
            sql += " and b.bank=:bankName ";
            temMap.put("bankName", bank);
        }

        if (getSearchKeyword().getToInstitution() != null && !getSearchKeyword().getToInstitution().trim().equals("")) {
            sql += " and  (upper(b.toInstitution.name) like :toInstitution )";
            temMap.put("toInstitution", "%" + getSearchKeyword().getToInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBank() != null && !getSearchKeyword().getBank().trim().equals("")) {
            sql += " and  (upper(b.bank.name) like :bankName )";
            temMap.put("bankName", "%" + getSearchKeyword().getBank().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  (upper(b.chequeRefNo) like :chequeNo )";
            temMap.put("chequeNo", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.GrnPayment);
        temMap.put("insTp", InstitutionType.Dealer);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
      //  temMap.put("ins", getSessionController().getInstitution());

        ////System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
        getNetTotal(bills);

    }

    public void createGrnPaymentTableAllChequeDate() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b "
                + " where b.billType = :billType "
                + " and b.chequeDate between :fromDate and :toDate "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.retired=false ";

        if (onlyRealized) {
            sql += " and (b.paymentMethod in :pms or (b.paymentMethod=:pm and b.reactivated=true)) ";

            List<PaymentMethod> pms = Arrays.asList(PaymentMethod.Cash, PaymentMethod.Slip, PaymentMethod.Agent, PaymentMethod.Card, PaymentMethod.Credit);

            temMap.put("pms", pms);
            temMap.put("pm", PaymentMethod.Cheque);
        }

        if (!withoutCancell) {
            System.err.println("innnn");
            //for hide cancell bill
            sql += " and b.cancelled=false  ";
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (institution != null) {
            sql += " and b.toInstitution=:toInstitution";
            temMap.put("toInstitution", institution);
        }

        if (bank != null) {
            sql += " and b.bank=:bankName ";
            temMap.put("bankName", bank);
        }

        if (getSearchKeyword().getToInstitution() != null && !getSearchKeyword().getToInstitution().trim().equals("")) {
            sql += " and  (upper(b.toInstitution.name) like :toInstitution )";
            temMap.put("toInstitution", "%" + getSearchKeyword().getToInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBank() != null && !getSearchKeyword().getBank().trim().equals("")) {
            sql += " and  (upper(b.bank.name) like :bankName )";
            temMap.put("bankName", "%" + getSearchKeyword().getBank().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  (upper(b.chequeRefNo) like :chequeNo )";
            temMap.put("chequeNo", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        sql += " order by b.chequeDate ";
//      sql += " order by b.chequeDate desc  ";
        temMap.put("billType", BillType.GrnPayment);
        temMap.put("insTp", InstitutionType.Dealer);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
      //  temMap.put("ins", getSessionController().getInstitution());

        ////System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
        getNetTotal(bills);

    }

    public void cancelChequesss() {
        bills = new ArrayList<>();
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b"
                + " where b.billType = :billType "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.retired=false "
                + " and type(b)=:class "
                //                + " and b.bank is null "
                //                + " and b.chequeRefNo is null "
                //                + " and b.chequeDate is null "
                + " and (b.bank is null or b.chequeRefNo is null or b.chequeDate is null) ";

        sql += " order by b.createdAt ";
//    
        temMap.put("billType", BillType.GrnPayment);
        temMap.put("insTp", InstitutionType.Dealer);
        temMap.put("class", CancelledBill.class);

        System.err.println("Sql " + sql);
        //    //System.out.println("temMap = " + temMap);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

        //System.out.println("bills.size() = " + bills.size());
    }

    public void createGrnPaymentTableAllStore() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b "
                + " where b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.GrnPayment);
        temMap.put("insTp", InstitutionType.StoreDealor);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
      //  temMap.put("ins", getSessionController().getInstitution());

        ////System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createTableByKeyword() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.billType = :billType and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.OpdBill);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        ////System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createTableCashIn() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.billType = :billType "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false"
                + " and b.creater=:w ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPersonName() != null && !getSearchKeyword().getPersonName().trim().equals("")) {
            sql += " and  (upper(b.fromWebUser.webUserPerson.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPersonName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.CashIn);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("w", getSessionController().getLoggedUser());

        ////System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    List<Bill> billlist;

    public List<Bill> getBilllist() {
        return billlist;
    }

    public void setBilllist(List<Bill> billlist) {
        this.billlist = billlist;
    }

    public void createTableCashInOut() {

        if (department == null) {
            UtilityController.addErrorMessage("Select a Department");
            return;
        }
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where (b.billType = :billType1 or b.billType = :billType2) "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false"
                + " and b.creater=:w "
                + " and b.department=:dep ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPersonName() != null && !getSearchKeyword().getPersonName().trim().equals("")) {
            sql += " and  (upper(b.fromWebUser.webUserPerson.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPersonName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.billTime desc  ";
//    
        temMap.put("billType1", BillType.CashIn);
        temMap.put("billType2", BillType.CashOut);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("w", getSessionController().getLoggedUser());
        temMap.put("dep", department);

        ////System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

        cashInOutVal = 0.0;
        cashTranVal = 0.0;

        Bill bill = new Bill();
        bill.setComments("Sale Bill Total " + department.getName() + " Department");
        bill.setNetTotal(getDepartmentSale(department));
        bill.setBillType(BillType.CashIn);
        bills.add(bill);

        for (Bill b : bills) {
            cashInOutVal = cashInOutVal + (b.getNetTotal());
            cashTranVal = cashTranVal + (b.getNetTotal());
            b.setTmp(cashTranVal);
        }

    }

    public void createTableCashTransactionHistory() {

        if (getWebUser() == null) {
            JsfUtil.addErrorMessage("Please Select a User");
            return;
        }

        String sql;
        Map temMap = new HashMap();

        sql = "select c from CashTransactionHistory c "
                + " where c.retired=false "
                + " and c.createdAt between :fd and :td "
                + " and c.cashTransaction.creater=:user "
                + " order by c.id ";

        temMap.put("user", getWebUser());
        temMap.put("fd", getFromDate());
        temMap.put("td", getToDate());

        cashTransactionHistories = getCashTransactionHistoryFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

        System.out.println("cashTransactionHistories.size() = " + cashTransactionHistories.size());
    }

    private double getDepartmentSale(Department d) {
        String sql = "Select sum(b.netTotal) from Bill b "
                + " where b.retired=false"
                + " and  b.billType=:bType"
                + " and b.referenceBill.department=:dep "
                + " and b.createdAt between :fromDate and :toDate "
                + " and (b.paymentMethod = :pm1 "
                + " or  b.paymentMethod = :pm2 "
                + " or  b.paymentMethod = :pm3 "
                + " or  b.paymentMethod = :pm4)";
        HashMap hm = new HashMap();
        hm.put("bType", BillType.PharmacySale);
        hm.put("dep", d);
        hm.put("fromDate", getFromDate());
        hm.put("toDate", getToDate());
        hm.put("pm1", PaymentMethod.Cash);
        hm.put("pm2", PaymentMethod.Card);
        hm.put("pm3", PaymentMethod.Cheque);
        hm.put("pm4", PaymentMethod.Slip);
        double netTotal = getBillFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

        return netTotal;
    }

    public void createTableCashInAll() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.billType = :billType "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPersonName() != null && !getSearchKeyword().getPersonName().trim().equals("")) {
            sql += " and  (upper(b.fromWebUser.webUserPerson.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPersonName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.CashIn);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        ////System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createTableCashInOutAll() {
        if (department == null) {
            UtilityController.addErrorMessage("Select a Department");
            return;
        }
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where (b.billType = :billType1 or b.billType = :billType2) "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false "
                + " and b.department=:dep ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPersonName() != null && !getSearchKeyword().getPersonName().trim().equals("")) {
            sql += " and  (upper(b.fromWebUser.webUserPerson.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPersonName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.billTime desc ";
//    
        temMap.put("billType1", BillType.CashIn);
        temMap.put("billType2", BillType.CashOut);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("dep", department);

        ////System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

        cashInOutVal = 0.0;
        cashTranVal = 0.0;

        Bill bill = new Bill();
        bill.setComments("Sale Bill Total " + department.getName() + " Department");
        bill.setNetTotal(getDepartmentSale(department));
        bill.setBillType(BillType.CashIn);
        bills.add(bill);

        for (Bill b : bills) {
            cashInOutVal = cashInOutVal + (b.getNetTotal());
            cashTranVal = cashTranVal + (b.getNetTotal());
            b.setTmp(cashTranVal);
        }

    }

    public void createTableCashInOutNew() {

        if (department == null) {
            UtilityController.addErrorMessage("Select a Department");
            return;
        }
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where (b.billType = :billType1 or b.billType = :billType2) "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false"
                + " and b.creater=:w "
                + " and b.department=:dep ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPersonName() != null && !getSearchKeyword().getPersonName().trim().equals("")) {
            sql += " and  (upper(b.fromWebUser.webUserPerson.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPersonName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt ";
//    
        temMap.put("billType1", BillType.CashIn);
        temMap.put("billType2", BillType.CashOut);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("w", getSessionController().getLoggedUser());
        temMap.put("dep", department);

        ////System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

        cashInOutVal = 0.0;
        cashTranVal = 0.0;

        Date dateFrist = fromDate;
        Date dateSecond = toDate;
        Date dateTemp;
        billlist = new ArrayList<>();
        for (Bill b : bills) {
            dateTemp = b.getCreatedAt();
            dateSecond = dateTemp;
            billlist.add(b);
            Bill bill = new Bill();
            bill.setComments("Sale Bill Total " + department.getName() + " Department");
            bill.setCreatedAt(dateSecond);
            bill.setNetTotal(getDepartmentSale(department, dateFrist, dateSecond));
            bill.setBillType(BillType.CashIn);
            billlist.add(bill);

            dateSecond = dateFrist;
            dateFrist = dateTemp;

        }

        for (Bill b : billlist) {
            cashInOutVal = cashInOutVal + (b.getNetTotal());
            cashTranVal = cashTranVal + (b.getNetTotal());
            b.setTmp(cashTranVal);
        }

    }

    public void createTableCashInOutAllNew() {
        if (department == null) {
            UtilityController.addErrorMessage("Select a Department");
            return;
        }
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where (b.billType = :billType1 or b.billType = :billType2) "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false "
                + " and b.department=:dep ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPersonName() != null && !getSearchKeyword().getPersonName().trim().equals("")) {
            sql += " and  (upper(b.fromWebUser.webUserPerson.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPersonName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt ";
//    
        temMap.put("billType1", BillType.CashIn);
        temMap.put("billType2", BillType.CashOut);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("dep", department);

        ////System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

        cashInOutVal = 0.0;
        cashTranVal = 0.0;

        Date dateFrist = fromDate;
        Date dateSecond;
        Date dateTemp;
        billlist = new ArrayList<>();
        for (Bill b : bills) {
            dateTemp = b.getCreatedAt();
            dateSecond = dateTemp;
            billlist.add(b);
            Bill bill = new Bill();
            bill.setComments("Sale Bill Total " + department.getName() + " Department");
            bill.setCreatedAt(dateSecond);
            bill.setNetTotal(getDepartmentSale(department, dateFrist, dateSecond));
            bill.setBillType(BillType.CashIn);
            billlist.add(bill);

            dateSecond = dateFrist;
            dateFrist = dateTemp;

        }

        for (Bill b : billlist) {
            cashInOutVal = cashInOutVal + (b.getNetTotal());
            cashTranVal = cashTranVal + (b.getNetTotal());
            b.setTmp(cashTranVal);
        }

    }

    private double getDepartmentSale(Department d, Date df, Date ds) {
        String sql = "Select sum(b.netTotal) from Bill b "
                + " where b.retired=false"
                + " and  b.billType=:bType"
                + " and b.referenceBill.department=:dep "
                + " and b.createdAt between :fromDate and :toDate "
                + " and (b.paymentMethod = :pm1 "
                + " or  b.paymentMethod = :pm2 "
                + " or  b.paymentMethod = :pm3 "
                + " or  b.paymentMethod = :pm4)";
        HashMap hm = new HashMap();
        hm.put("bType", BillType.PharmacySale);
        hm.put("dep", d);
        hm.put("fromDate", df);
        hm.put("toDate", ds);
        hm.put("pm1", PaymentMethod.Cash);
        hm.put("pm2", PaymentMethod.Card);
        hm.put("pm3", PaymentMethod.Cheque);
        hm.put("pm4", PaymentMethod.Slip);
        double netTotal = getBillFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

        return netTotal;
    }

    public void createTableCashOut() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.retired=false "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and (b.billType = :billType or b.billType = :billType1) "
                + " and b.creater=:w ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPersonName() != null && !getSearchKeyword().getPersonName().trim().equals("")) {
            sql += " and  (upper(b.toWebUser.webUserPerson.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPersonName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.CashOut);
        temMap.put("billType1", BillType.SummeryOut);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("w", getSessionController().getLoggedUser());

        ////System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createTableCashOutAll() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.retired=false "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and (b.billType = :billType or b.billType = :billType1) ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPersonName() != null && !getSearchKeyword().getPersonName().trim().equals("")) {
            sql += " and  (upper(b.toWebUser.webUserPerson.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPersonName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.CashOut);
        temMap.put("billType1", BillType.SummeryOut);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        ////System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createTableHandOver() {

        bills = createBillTable(getSessionController().getInstitution(), new BillType[]{BillType.HandOver}, getSessionController().getLoggedUser(), "");
    }

    public void createTableHandOverApprove() {
        String s = "";
        if (getSearchKeyword().getString().equals("1")) {
            s = " and b.approveUser is not null "
                    + " and b.cancelled=false ";
        }
        if (getSearchKeyword().getString().equals("2")) {
            s = " and b.approveUser is null "
                    + " and b.cancelled=false ";
        }
//        bills = createBillTable(getSessionController().getInstitution(), enumController.getBulkSettleTypes(), null, s);
        bills = createBillTable(getSessionController().getInstitution(), new BillType[]{BillType.HandOver}, null, s);
    }

    public void createSearchAll() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where "
                + " b.createdAt between :fromDate and :toDate "
                + " and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getInstitution() != null && !getSearchKeyword().getInstitution().trim().equals("")) {
            sql += " and  (upper(b.institution.name) like :ins )";
            temMap.put("ins", "%" + getSearchKeyword().getInstitution().trim().toUpperCase() + "%");
        }
        if (getSearchKeyword().getToInstitution() != null && !getSearchKeyword().getToInstitution().trim().equals("")) {
            sql += " and  (upper(b.toInstitution.name) like :toIns )";
            temMap.put("toIns", "%" + getSearchKeyword().getToInstitution().trim().toUpperCase() + "%");
        }
        if (getSearchKeyword().getToDepartment() != null && !getSearchKeyword().getToDepartment().trim().equals("")) {
            sql += " and  (upper(b.toDepartment.name) like :toDept )";
            temMap.put("toDept", "%" + getSearchKeyword().getToDepartment().trim().toUpperCase() + "%");
        }
        if (getSearchKeyword().getFromInstitution() != null && !getSearchKeyword().getFromInstitution().trim().equals("")) {
            sql += " and  (upper(b.fromInstitution.name) like :frmIns )";
            temMap.put("frmIns", "%" + getSearchKeyword().getFromInstitution().trim().toUpperCase() + "%");
        }
        if (getSearchKeyword().getFromDepartment() != null && !getSearchKeyword().getFromDepartment().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getFromDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPaymentScheme() != null && !getSearchKeyword().getPaymentScheme().trim().equals("")) {
            sql += " and  (upper(b.paymentScheme.name) like :pScheme )";
            temMap.put("pScheme", "%" + getSearchKeyword().getPaymentScheme().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPaymentmethod() != null && !getSearchKeyword().getPaymentmethod().trim().equals("")) {
            sql += " and  (upper(b.paymentmethod) like :pm )";
            temMap.put("pm", "%" + getSearchKeyword().getPaymentmethod().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getInsId() != null && !getSearchKeyword().getInsId().trim().equals("")) {
            sql += " and  (upper(b.insId) like :insId )";
            temMap.put("insId", "%" + getSearchKeyword().getInsId().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDeptId() != null && !getSearchKeyword().getDeptId().trim().equals("")) {
            sql += " and  (upper(b.insId) like :deptId )";
            temMap.put("deptId", "%" + getSearchKeyword().getDeptId().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        ////System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createCollectingTable() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.billType = :billType and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.LabBill);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
//        temMap.put("ins", getSessionController().getInstitution());

        ////System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createCollectingTableAll() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.billType = :billType and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.LabBill);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
//        temMap.put("ins", getSessionController().getInstitution());

        ////System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createCreditTable() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.billType = :billType and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate and b.retired=false ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFromInstitution() != null && !getSearchKeyword().getFromInstitution().trim().equals("")) {
            sql += " and  (upper(b.fromInstiution.name) like :frmIns )";
            temMap.put("frmIns", "%" + getSearchKeyword().getFromInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBank() != null && !getSearchKeyword().getBank().trim().equals("")) {
            sql += " and  (upper(b.bank.name) like :bank )";
            temMap.put("bank", "%" + getSearchKeyword().getBank().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  (upper(b.chequeRefNo) like :num )";
            temMap.put("num", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.CashRecieveBill);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        ////System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public List<Bill> getChannelPaymentBills() {
        if (bills == null) {
            String sql;
            Map temMap = new HashMap();
            if (bills == null) {
                sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.id in"
                        + "(Select bt.bill.id From BillItem bt Where bt.referenceBill.billType=:btp"
                        + " or bt.referenceBill.billType=:btp2) and b.billType=:type and b.createdAt "
                        + "between :fromDate and :toDate order by b.id";

                temMap.put("toDate", getToDate());
                temMap.put("fromDate", getFromDate());
                temMap.put("type", BillType.PaymentBill);
                temMap.put("btp", BillType.ChannelPaid);
                temMap.put("btp2", BillType.ChannelCredit);
                bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 100);

                if (bills == null) {
                    bills = new ArrayList<>();
                }
            }
        }
        return bills;

    }

    public void createChannelDueBillFee() {

        String sql;
        Map temMap = new HashMap();

        sql = "select b from BillFee b where b.retired=false and (b.bill.billType=:btp or b.bill.billType=:btp2) "
                + " and b.bill.id in(Select bs.bill.id From BillSession bs where bs.retired=false ) "
                + "and b.bill.cancelled=false and (b.feeValue - b.paidValue) > 0 and  "
                + "b.bill.createdAt between :fromDate and :toDate order by b.staff.id  ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", BillType.ChannelPaid);
        temMap.put("btp2", BillType.ChannelCredit);

        billFees = getBillFeeFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createAgentPaymentTable() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.billType = :billType "
                + " and b.institution=:ins and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFromInstitution() != null && !getSearchKeyword().getFromInstitution().trim().equals("")) {
            sql += " and  (upper(b.fromInstitution.name) like :frmIns )";
            temMap.put("frmIns", "%" + getSearchKeyword().getFromInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  (upper(b.fromInstitution.institutionCode) like :num )";
            temMap.put("num", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        temMap.put("billType", BillType.AgentPaymentReceiveBill);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        ////System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createInwardServiceTable() {

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where "
                + " b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false order by b.insId desc  ";

        temMap.put("billType", BillType.InwardBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createInwardServiceTableDischarged() {

        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.createdAt is not null "
                + " and b.patientEncounter.discharged=true and"
                + " b.billType = :billType and b.createdAt between :fromDate and :toDate "
                + "and b.retired=false  ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += "order by b.insId desc";

        temMap.put("billType", BillType.InwardBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createInwardPaymentFinalBills() {

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where"
                + " b.billType = :billType and "
                + " b.createdAt between :fromDate and :toDate "
                + "and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.InwardFinalBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createInwardPaymentBills() {

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where"
                + " b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createInwardRefundBills() {

        String sql;
        Map temMap = new HashMap();
        sql = "select b from RefundBill b where "
                + " b.billType = :billType "
                + " and b.billedBill is null "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createInwardSurgeryBills() {

        String sql;
        Map temMap = new HashMap();
        sql = "select b from Bill b where "
                + " b.billType = :billType and "
                + " b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  ";

        if (getSearchKeyword().getPatientName() != null
                && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null
                && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null
                && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null
                && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null
                && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(b.procedure.item.name) like :itm )";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null
                && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.SurgeryBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createInwardPaymentBillsDischarged() {

        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where  "
                + " and b.patientEncounter.paymentFinalized=true"
                + " and b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += "order by b.insId desc";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createInwardProBills() {

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where "
                + " b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.insId desc";

        temMap.put("billType", BillType.InwardProfessional);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createInwardProBillsDischarged() {

        String sql;
        Map temMap = new HashMap();
//        sql = "select b from BilledBill b where b.createdAt is not null and b.billType = :billType and b.patientEncounter.discharged=true and "
//                + " b.id in(Select bf.bill.id From BillFee bf where bf.retired=false and bf.createdAt between :fromDate and :toDate and bf.billItem is null)"
//                + " and b.createdAt between :fromDate and :toDate and b.retired=false";

        sql = "select b from BilledBill b where b.createdAt is not null "
                + " and b.patientEncounter.discharged=true and"
                + " b.billType = :billType and b.createdAt between :fromDate and :toDate "
                + "and b.retired=false  ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += "order by b.insId desc";

        temMap.put("billType", BillType.InwardBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createPettyTable() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.billType = :billType and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate and b.retired=false ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  (upper(b.staff.person.name) like :stf )";
            temMap.put("stf", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPersonName() != null && !getSearchKeyword().getPersonName().trim().equals("")) {
            sql += " and  (upper(b.person.name) like :per )";
            temMap.put("per", "%" + getSearchKeyword().getPersonName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  (upper(b.invoiceNumber) like :num )";
            temMap.put("num", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.PettyCash);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        ////System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    private void fetchHeaders() {
        String sql;
        Map m = new HashMap();

        sql = "Select distinct(i.department.name) From Item i where "
                + " i.retired=false "
                + " and type(i)=:class ";

        sql += " order by i.department.name ";

        m.put("class", Summery.class);
        headers = getBillItemFacade().findString(sql, m, TemporalType.TIMESTAMP);
        System.out.println("headers = " + headers.size());
        headers.add("Bulk");
        System.out.println("headers = " + headers);

    }

    private void fetchHeadersAccountant() {
        String sql;
        Map m = new HashMap();

        sql = "Select distinct(i.department.name) From Item i where "
                + " i.retired=false "
                + " and type(i)=:class ";

        if (getReportKeyWord().getString().equals("0")) {
            sql += " and (i.department.id=" + 13551 + " or i.department.id=" + 13460957 + " )";
//            sql += " and (i.department.id=" + 13551 +" or i.department.id=" + 13460957+ " or i.department.id=" + 13458602 + " )";
        }
        if (getReportKeyWord().getString().equals("1")) {
            sql += " and i.department.id=" + 13551;
        }
        if (getReportKeyWord().getString().equals("2")) {
            sql += " and i.department.id=" + 13460957;
        }
//        if (getReportKeyWord().getString().equals("3")) {
//            sql += " and i.department.id=" + 13458602;
//        }

        sql += " order by i.department.name ";

        m.put("class", Summery.class);
        headers = getBillItemFacade().findString(sql, m, TemporalType.TIMESTAMP);
        System.out.println("headers = " + headers.size());
        headers.add("Bulk");
        System.out.println("headers = " + headers);

    }

    private void fetchCashBook3D() {
        cashBookRows = new ArrayList<>();
        CashBookRow rowOpen = new CashBookRow();
        rowOpen.setString1("Opening Balance");
        rowOpen.setTotals(openningBalanceRow());
        cashBookRows.add(rowOpen);
        for (Object[] obs : fetchBillDetais()) {
            long id = (long) obs[0];
            String dep = (String) obs[1];
            CashBookRow row = new CashBookRow();
            Bill b = getBillFacade().find(id);
            SimpleDateFormat format = new SimpleDateFormat("YYYY MM dd hh:mm:ss a");
            if (onlyRealized) {
                for (BillItem bi : billItemsForBill(id)) {
                    if (bi.getDeptId() == null) {
                        continue;
                    }
                    row = new CashBookRow();
                    row.setString1(b.getInsId());
                    if (b.getFromWebUser() != null) {
                        row.setString2(b.getFromWebUser().getWebUserPerson().getName());
                    }
                    if (b.getToWebUser() != null) {
                        row.setString3(b.getToWebUser().getWebUserPerson().getName());
                    }
                    if (b.getToInstitution() != null) {
                        row.setString3(b.getToInstitution().getName());
                    }
                    if (b.getCreatedAt() != null) {
                        row.setString4(format.format(b.getCreatedAt()));
                    }
                    if (bi.getFromTime() != null) {
                        row.setString5(format.format(bi.getFromTime()));
                    }
                    if (bi.getToTime() != null) {
                        row.setString6(format.format(bi.getToTime()));
                    }
                    if (bi.getAgentRefNo() != null) {
                        row.setString7(bi.getAgentRefNo());
                    }
                    row.setTotals(fetchTotalsDetail(bi));
                    cashBookRows.add(row);
                }
            } else {
                System.err.println("**********else**********");
                row.setString1(b.getInsId());
                if (b.getFromWebUser() != null) {
                    row.setString2(b.getFromWebUser().getWebUserPerson().getName());
                }
                if (b.getToWebUser() != null) {
                    row.setString3(b.getToWebUser().getWebUserPerson().getName());
                }
//                if (b.getToInstitution() != null) {
//                    row.setString3(b.getToInstitution().getName());
//                }
                if (b.getCreatedAt() != null) {
                    row.setString4(format.format(b.getCreatedAt()));
                }
                row.setTotals(fetchTotals(id));
                cashBookRows.add(row);
            }
        }
        System.out.println("cashBookRows.size() = " + cashBookRows.size());
    }

    private List<Object[]> fetchBillDetais() {
        List<Object[]> objects = new ArrayList<>();
        String sql;
        Map m = new HashMap();

        sql = "Select distinct(bi.bill.id),bi.bill.insId From BillItem bi "
                + " join bi.bill b where "
                + " b.retired=false "
                + " and b.billType in :bts"
                + " and bi.deptId is not null "
                //                + " and type(b)=:class "
                //                + " and b.cancelled=false "
                + " and b.createdAt between :fromDate and :toDate ";

//        if (getReportKeyWord().getInstitution() != null) {
//            sql += " and bi.item..institution=:ins ";
//            m.put("ins", getReportKeyWord().getInstitution());
//        }
//
//        if (getDepartment() != null) {
//            sql += " and bi.item..department=:dep ";
//            m.put("dep", getReportKeyWord().getDepartment());
//        }
        if (getReportKeyWord().getUser() != null) {
            sql += " and b.creater=:u ";
            m.put("u", getReportKeyWord().getUser());
        }

        sql += " order by bi.bill.id ";

        m.put("toDate", getReportKeyWord().getToDate());
        m.put("fromDate", getReportKeyWord().getFromDate());
        m.put("bts", Arrays.asList(new BillType[]{BillType.CashIn, BillType.HandOver,BillType.DrawerAdjustment,}));
//        m.put("bts", Arrays.asList(new BillType[]{BillType.CashIn, BillType.CashSettle,
//            BillType.CreditCardSettle,
//            BillType.ChequeSettle,
//            BillType.SlipSettle,
//            BillType.CreditSettle,
//            BillType.IOUSettle,}));
        objects = getBillItemFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);
        System.out.println("objects = " + objects.size());

        return objects;
    }

    private List<Double> fetchTotals(long id) {
        List<Double> ls = new ArrayList<>();
        double tot = 0l;
        for (String s : headers) {
            if (s.equals("Bulk")) {
                continue;
            }
            double d;
            d = calTotal(id, s);
            tot += d;
            ls.add(d);
        }
        ls.add(tot);
        System.out.println("ls = " + ls);
        return ls;
    }

    private List<Double> fetchTotalsDetail(BillItem bi) {
        List<Double> ls = new ArrayList<>();
        double tot = 0l;
        for (String s : headers) {
            if (s.equals("Bulk")) {
                continue;
            }
            double d = 0.0;
            System.out.println("s = " + s);
            System.out.println("bi.getDeptId() = " + bi.getDeptId());
            System.out.println("bi.getBill().getInsId() = " + bi.getBill().getInsId());
//            if (bi.getDeptId() == null) {
//                if (bi.getItem() != null && bi.getItem().getDepartment() != null) {
//                    System.out.println("bi.getItem().getDepartment().getName() = " + bi.getItem().getDepartment().getName());
//                }
//                tot += d;
//                ls.add(d);
//                continue;
//            }
            if (bi.getDeptId().equals(s)) {
                d = bi.getNetValue();
            }
            tot += d;
            ls.add(d);
        }
        ls.add(tot);
        System.out.println("ls = " + ls);
        return ls;
    }

    private double calTotal(long id, String dep) {
        double d = 0.0;

        String sql;
        Map m = new HashMap();

        sql = "Select sum(bi.netValue) From BillItem bi "
                + " join bi.bill b where "
                + " b.retired=false "
                + " and b.billType in :bts "
                + " and bi.bill.id=:id "
                + " and (bi.item.department.name=:dep or bi.deptId=:dep) ";
//                + " and b.createdAt between :fromDate and :toDate ";

//        if (getReportKeyWord().getInstitution() != null) {
//            sql += " and b.item.institution=:ins ";
//            m.put("ins", getReportKeyWord().getInstitution());
//        }
//
//        if (getDepartment() != null) {
//            sql += " and b.item.department=:dep ";
//            m.put("dep", getReportKeyWord().getDepartment());
//        }
        if (getReportKeyWord().getUser() != null) {
            sql += " and b.creater=:u ";
            m.put("u", getReportKeyWord().getUser());
        }

        sql += " order by bi.bill.id ";

        m.put("id", id);
        m.put("dep", dep);
//        m.put("toDate", getReportKeyWord().getToDate());
//        m.put("fromDate", getReportKeyWord().getFromDate());
        m.put("bts", Arrays.asList(new BillType[]{BillType.CashIn, BillType.HandOver,BillType.DrawerAdjustment,}));
//        m.put("bts", Arrays.asList(new BillType[]{BillType.CashIn, BillType.CashSettle,
//            BillType.CreditCardSettle,
//            BillType.ChequeSettle,
//            BillType.SlipSettle,
//            BillType.CreditSettle,
//            BillType.IOUSettle,}));
//        Object[]  ob= getBillItemFacade().findSingleAggregate(sql, m, TemporalType.TIMESTAMP);
//        System.out.println("ob = " + ob);
//        if (ob[0]!=null) {
//            d=(double) ob[0];
//        }
        try {
            d = getBillItemFacade().findAggregateDblNew(sql, m);
            System.out.println("--d(try) = " + d);
        } catch (Exception e) {
            d = calTotalModified(id, dep);
            System.out.println("--d(catch) = " + d);
        }
//        d = getBillItemFacade().findAggregateDbl(sql, m, TemporalType.TIMESTAMP);

        return d;
    }

    private double calTotalModified(long id, String dep) {
        double d = 0.0;

        String sql;
        Map m = new HashMap();

        sql = "Select sum(bi.netValue) From BillItem bi "
                + " join bi.bill b where "
                + " bi.bill.id=:id "
                + " and bi.deptId=:dep ";
        if (getReportKeyWord().getUser() != null) {
            sql += " and b.creater=:u ";
            m.put("u", getReportKeyWord().getUser());
        }

        m.put("id", id);
        m.put("dep", dep);
        try {
            System.out.println("m = " + m);
            System.out.println("sql = " + sql);
            d = getBillItemFacade().findAggregateDblNew(sql, m);
            System.out.println("--d(try) = " + d);
        } catch (Exception e) {
            d = 0;
            System.out.println("--d(catch) = " + d);
        }
//        d = getBillItemFacade().findAggregateDbl(sql, m, TemporalType.TIMESTAMP);

        return d;
    }

    private double fetchOpenningBalnce(String dep) {
        double d = 0.0;

        String sql;
        Map m = new HashMap();

        sql = "Select bi From BillItem bi "
                + " join bi.bill b where "
                + " b.retired=false "
                + " and b.billType in :bts "
                + " and bi.deptId=:dep"
                + " and type(bi.item)=:type "
                //                + " and (bi.item.department.name=:dep or bi.deptId=:dep) "
                + " and b.createdAt between :fromDate and :toDate ";

//        if (getReportKeyWord().getInstitution() != null) {
//            sql += " and bi.item.institution=:ins ";
//            m.put("ins", getReportKeyWord().getInstitution());
//        }
//
//        if (getDepartment() != null) {
//            sql += " and bi.item.department=:dep ";
//            m.put("dep", getReportKeyWord().getDepartment());
//        }
//        if (getReportKeyWord().getUser() != null) {
//            sql += " and b.creater=:u ";
//            m.put("u", getReportKeyWord().getUser());
//        }
        sql += " order by bi.bill.id ";

        m.put("toDate", getReportKeyWord().getToDate());
        m.put("fromDate", getReportKeyWord().getFromDate());
        m.put("type", Summery.class);
        m.put("dep", dep);
        m.put("bts", Arrays.asList(new BillType[]{BillType.CashIn, BillType.HandOver,}));
//        m.put("bts", Arrays.asList(new BillType[]{BillType.CashIn, BillType.CashSettle,
//            BillType.CreditCardSettle,
//            BillType.ChequeSettle,
//            BillType.SlipSettle,
//            BillType.CreditSettle,
//            BillType.IOUSettle,}));
        BillItem bi = getBillItemFacade().findFirstBySQL(sql, m, TemporalType.TIMESTAMP);
        System.out.println("---bi = " + bi);
        if (bi == null) {
            m = new HashMap();
            sql = "Select bi From BillItem bi "
                    + " join bi.bill b where "
                    + " b.retired=false "
                    + " and b.billType in :bts "
                    + " and bi.deptId=:dep "
                    //                    + " and (bi.item.department.name=:dep or bi.deptId=:dep) "
                    + " and b.createdAt <= :fromDate ";

//            if (getReportKeyWord().getInstitution() != null) {
//                sql += " and bi.item.institution=:ins ";
//                m.put("ins", getReportKeyWord().getInstitution());
//            }
//
//            if (getDepartment() != null) {
//                sql += " and bi.item.department=:dep ";
//                m.put("dep", getReportKeyWord().getDepartment());
//            }
            if (getReportKeyWord().getUser() != null) {
                sql += " and b.creater=:u ";
                m.put("u", getReportKeyWord().getUser());
            }

            sql += " order by bi.bill.id desc ";
            m.put("dep", dep);
            m.put("bts", Arrays.asList(new BillType[]{BillType.CashIn, BillType.HandOver,}));
            m.put("fromDate", getReportKeyWord().getFromDate());
            bi = getBillItemFacade().findFirstBySQL(sql, m, TemporalType.TIMESTAMP);
            System.out.println("+++bi = " + bi);
            if (bi == null) {
                d = 0;
            } else {
                System.out.println("+++bi.getCashTransactionHistory() = " + bi.getCashTransactionHistory());
                if (bi.getCashTransactionHistory() != null) {
                    d = bi.getCashTransactionHistory().getBeforeBalance() + bi.getCashTransactionHistory().getTransactionValue();
                }
            }
        } else {
            System.out.println("******bi.getCashTransactionHistory() = " + bi.getCashTransactionHistory());
            if (bi.getCashTransactionHistory() != null) {
                System.out.println("******bi.getCashTransactionHistory().getBeforeBalance() = " + bi.getCashTransactionHistory().getBeforeBalance());
                d = bi.getCashTransactionHistory().getBeforeBalance();
            }
        }
        System.out.println("d = " + d);

        return d;
    }

    private double fetchOpenningBalnceModified(String dep) {
        double d = 0.0;

        String sql;
        Map m = new HashMap();

        sql = " Select cth From BillItem bi "
                + " join bi.bill b "
                + " join bi.cashTransactionHistory cth where "
                + " b.retired=false "
                + " and b.billType in :bts "
                + " and bi.deptId=:dep"
                //                + " and type(bi.item)=:type "
                //                + " and (bi.item.department.name=:dep or bi.deptId=:dep) "
                + " and b.createdAt between :fromDate and :toDate ";

        sql += " order by cth.id ";

        m.put("toDate", getReportKeyWord().getToDate());
        m.put("fromDate", getReportKeyWord().getFromDate());
//        m.put("type", Summery.class);
        m.put("dep", dep);
        m.put("bts", Arrays.asList(new BillType[]{BillType.CashIn, BillType.HandOver,BillType.DrawerAdjustment,}));
//        m.put("bts", Arrays.asList(new BillType[]{BillType.CashIn, BillType.CashSettle,
//            BillType.CreditCardSettle,
//            BillType.ChequeSettle,
//            BillType.SlipSettle,
//            BillType.CreditSettle,
//            BillType.IOUSettle,}));
        CashTransactionHistory cth = getCashTransactionHistoryFacade().findFirstBySQL(sql, m, TemporalType.TIMESTAMP);
//        System.out.println("---bi = " + cth);
        if (cth == null) {
            m = new HashMap();
            sql = " Select cth From BillItem bi "
                    + " join bi.bill b "
                    + " join bi.cashTransactionHistory cth where "
                    + " b.retired=false "
                    + " and b.billType in :bts "
                    + " and bi.deptId=:dep "
                    //                    + " and type(bi.item)=:type "
                    //                    + " and (bi.item.department.name=:dep or bi.deptId=:dep) "
                    + " and b.createdAt <= :fromDate ";

//            if (getReportKeyWord().getInstitution() != null) {
//                sql += " and bi.item.institution=:ins ";
//                m.put("ins", getReportKeyWord().getInstitution());
//            }
//
//            if (getDepartment() != null) {
//                sql += " and bi.item.department=:dep ";
//                m.put("dep", getReportKeyWord().getDepartment());
//            }
            sql += " order by cth.id desc ";
            m.put("dep", dep);
//            m.put("type", Summery.class);
            m.put("bts", Arrays.asList(new BillType[]{BillType.CashIn, BillType.HandOver,BillType.DrawerAdjustment,}));
            m.put("fromDate", getReportKeyWord().getFromDate());
            cth = getCashTransactionHistoryFacade().findFirstBySQL(sql, m, TemporalType.TIMESTAMP);
//            System.out.println("+++cth = " + cth);
            if (cth == null) {
                d = 0;
            } else {
//                System.out.println("+++bi.getCashTransactionHistory().getBeforeBalance() = " + cth.getBeforeBalance());
//                System.out.println("+++bi.getCashTransactionHistory().getTransactionValue() = " + cth.getTransactionValue());
                if (cth != null) {
                    d = cth.getBeforeBalance() + cth.getTransactionValue();
                }
            }
        } else {
//            System.out.println("******cth = " + cth);
            if (cth != null) {
//                System.out.println("******cth.getBeforeBalance() = " + cth.getBeforeBalance());
                d = cth.getBeforeBalance();
            }
        }
        System.out.println("d = " + d);

        return d;
    }

    private List<Bill> createBillTable(Institution institution, BillType[] billTypes, WebUser user, String s) {
        List<Bill> list = new ArrayList<>();
        String sql;
        Map m = new HashMap();

        sql = "select b from BilledBill b where "
                + " b.retired=false "
                + " and b.createdAt between :fromDate and :toDate ";

        if (s != null && !s.equals("")) {
            sql += s;
        }

        if (institution != null) {
            sql += " and b.institution=:ins ";
            m.put("ins", institution);
        }

        if (billTypes != null) {
            sql += " and b.billType in :bts ";
            m.put("bts", Arrays.asList(billTypes));
        }

        if (user != null) {
            sql += " and b.creater=:w ";
            m.put("w", user);
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :total )";
            m.put("total", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        m.put("toDate", getToDate());
        m.put("fromDate", getFromDate());

//        System.err.println("Sql " + sql);
        list = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
        System.out.println("bill list.size() = " + list.size());
        return list;
    }

    private List<Double> openningBalanceRow() {
        List<Double> ls = new ArrayList<>();
        double tot = 0l;
        for (String s : headers) {
            if (s.equals("Bulk")) {
                continue;
            }
            double d;
            d = fetchOpenningBalnceModified(s);
//            d = fetchOpenningBalnce(s);
            tot += d;
            ls.add(d);
        }
        ls.add(tot);
        System.out.println("ls = " + ls);
        return ls;
    }

    private List<BillItem> billItemsForBill(long id) {
        List<BillItem> items;

        String sql;
        Map m = new HashMap();

        sql = "Select bi From BillItem bi "
                + " join bi.bill b where "
                + " b.retired=false "
                + " and b.billType in :bts "
                + " and bi.bill.id=:id ";

        m.put("id", id);
        m.put("bts", Arrays.asList(new BillType[]{BillType.CashIn, BillType.HandOver,BillType.DrawerAdjustment,}));
//        System.out.println("+++++++++m = " + m);
//        System.out.println("+++++++++sql = " + sql);
        items = getBillItemFacade().findBySQL(sql, m);
//        System.out.println("billItems = " + items.size());

        return items;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public List<ColumnModel> getColumnModels() {
        return columnModels;
    }

    public void setColumnModels(List<ColumnModel> columnModels) {
        this.columnModels = columnModels;
    }

    public List<CashBookRow> getCashBookRows() {
        return cashBookRows;
    }

    public void setCashBookRows(List<CashBookRow> cashBookRows) {
        this.cashBookRows = cashBookRows;
    }

//     public List<Bill> getInstitutionPaymentBills() {
//        if (bills == null) {
//            String sql;
//            Map temMap = new HashMap();
//            if (bills == null) {
//                if (txtSearch == null || txtSearch.trim().equals("")) {
//                    sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.billType=:type and b.createdAt between :fromDate and :toDate order by b.id";
//                    temMap.put("toDate", getToDate());
//                    temMap.put("fromDate", getFromDate());
//                    temMap.put("type", BillType.PaymentBill);
//                    bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 100);
//
//                } else {
//                    sql = "select b from BilledBill b where b.retired=false and b.billType=:type and b.createdAt between :fromDate and :toDate and (upper(b.staff.person.name) like '%" + txtSearch.toUpperCase() + "%'  or upper(b.staff.person.phone) like '%" + txtSearch.toUpperCase() + "%'  or upper(b.insId) like '%" + txtSearch.toUpperCase() + "%') order by b.createdAt desc  ";
//                    temMap.put("toDate", getToDate());
//                    temMap.put("fromDate", getFromDate());
//                    temMap.put("type", BillType.PaymentBill);
//                    bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 100);
//                }
//                if (bills == null) {
//                    bills = new ArrayList<Bill>();
//                }
//            }
//        }
//        return bills;
//
//    }
    public class ColumnModel {

        private String header;
        private String property;

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
    }

    public class CashBookRow {

        String string1;
        String string2;
        String string3;
        String string4;
        String string5;
        String string6;
        String string7;

        List<Double> totals;

        public String getString7() {
            return string7;
        }

        public void setString7(String string7) {
            this.string7 = string7;
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

        public List<Double> getTotals() {
            return totals;
        }

        public void setTotals(List<Double> totals) {
            this.totals = totals;
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

    }

    public SearchController() {
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public SearchKeyword getSearchKeyword() {
        if (searchKeyword == null) {
            searchKeyword = new SearchKeyword();
        }
        return searchKeyword;
    }

    public void setSearchKeyword(SearchKeyword searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public List<BillFee> getBillFees() {
        return billFees;
    }

    public void setBillFees(List<BillFee> billFees) {
        this.billFees = billFees;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public int getMaxResult() {

        return maxResult;
    }

    public void setMaxResult(int maxResult) {
        this.maxResult = maxResult;
    }

    public List<Bill> getSelectedBills() {
        return selectedBills;
    }

    public void setSelectedBills(List<Bill> selectedBills) {
        this.selectedBills = selectedBills;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public TransferController getTransferController() {
        return transferController;
    }

    public void setTransferController(TransferController transferController) {
        this.transferController = transferController;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }

    public double getCashInOutVal() {
        return cashInOutVal;
    }

    public void setCashInOutVal(double cashInOutVal) {
        this.cashInOutVal = cashInOutVal;
    }

    public double getCashTranVal() {
        return cashTranVal;
    }

    public void setCashTranVal(double cashTranVal) {
        this.cashTranVal = cashTranVal;
    }

    public boolean isWithoutCancell() {
        return withoutCancell;
    }

    public void setWithoutCancell(boolean withoutCancell) {
        this.withoutCancell = withoutCancell;
    }

    public boolean isOnlyRealized() {
        return onlyRealized;
    }

    public void setOnlyRealized(boolean onlyRealized) {
        this.onlyRealized = onlyRealized;
    }

    public List<CashTransactionHistory> getCashTransactionHistories() {
        return cashTransactionHistories;
    }

    public void setCashTransactionHistories(List<CashTransactionHistory> cashTransactionHistories) {
        this.cashTransactionHistories = cashTransactionHistories;
    }

    public CashTransactionHistoryFacade getCashTransactionHistoryFacade() {
        return cashTransactionHistoryFacade;
    }

    public void setCashTransactionHistoryFacade(CashTransactionHistoryFacade cashTransactionHistoryFacade) {
        this.cashTransactionHistoryFacade = cashTransactionHistoryFacade;
    }

    public WebUser getWebUser() {
        if (webUser == null) {
            webUser = getSessionController().getLoggedUser();
        }
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
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

}
