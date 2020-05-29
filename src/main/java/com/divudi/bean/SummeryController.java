/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean;

import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.InOutType;
import com.divudi.data.PaymentMethod;
import com.divudi.ejb.BillNumberBean;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Item;
import com.divudi.entity.Settle;
import com.divudi.entity.Summery;
import com.divudi.entity.WebUser;
import com.divudi.entity.cashTransaction.CashTransaction;
import com.divudi.entity.cashTransaction.Drawer;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.DrawerFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.SettleFacade;
import com.divudi.facade.SummeryFacade;
import com.divudi.facade.WebUserFacade;
import com.divudi.facade.util.JsfUtil;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 *
 * @author archmage
 */
@Named(value = "summeryController")
@SessionScoped
public class SummeryController implements Serializable {

    @EJB
    private SummeryFacade ejbFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private WebUserFacade webUserFacade;
    @EJB
    private SettleFacade settleFacade;
    @EJB
    private DrawerFacade DrawerFacade;
    @EJB
    private ItemFacade itemFacade;

    @Inject
    SessionController sessionController;
    @Inject
    private BillSearch billSearch;

    @EJB
    CommonFunctions commonFunctions;
    @EJB
    BillNumberBean billNumberBean;
    @EJB
    CashTransactionBean cashTransactionBean;

    Summery current;
    Settle settleItem;
    BillItem currentBillItem;
    BillItem currentSetlleBillItem;
    Bill bill;

    double total;
    double totalCalculated;
    double totalDue;
//    double totalNotEfected;

    boolean printPreview;

    List<BillItem> billItems;
    List<BillItem> billItemsSettle;
    List<BillItem> selectedBillItems;
    List<BillItem> selectedBillItemsSettle;

    private List<Bill> ListBills;

    public SummeryController() {
    }

    public void prepareAdd() {
        current = new Summery();
        bill = new BilledBill();
        billItems = new ArrayList<>();
        selectedBillItems = new ArrayList<>();
        selectedBillItemsSettle = new ArrayList<>();
        totalDue = 0.0;
        totalCalculated = 0.0;
        total = 0.0;
        ListBills = new ArrayList<>();
        Date d = new Date();
        commonFunctions.timeDeffrenceCalculate(d, "(1)");//1
        createSummeryBillItemTable();
        commonFunctions.timeDeffrenceCalculate(d, "(2)");//2
        createSettleBillItemTable();
        commonFunctions.timeDeffrenceCalculate(d, "(3)");//3
        calTotal();
        commonFunctions.timeDeffrenceCalculate(d, "(4)");//4
        pendingTransationsCalculations();
        commonFunctions.timeDeffrenceCalculate(d, "(5)");//5
        fetshListToCashRecieve();
        commonFunctions.timeDeffrenceCalculate(d, "(6)");//6
//        fetshListToCashRecieveNew();
//        commonFunctions.timeDeffrenceCalculate(d, "(6-New)");//6
        fetchListToCashRecieveFrom();
        commonFunctions.timeDeffrenceCalculate(d, "(7)");//7
        fetchListBillsCashIn();
        commonFunctions.timeDeffrenceCalculate(d, "(8)");//8
        printPreview = false;
    }

    public void prepareAddhandOver() {
        current = new Summery();
        bill = new BilledBill();
        billItems = new ArrayList<>();
        selectedBillItems = new ArrayList<>();
        selectedBillItemsSettle = new ArrayList<>();
        totalDue = 0.0;
        totalCalculated = 0.0;
        total = 0.0;
        printPreview = false;
    }

    public void prepareAddSettle() {
        settleItem = new Settle();
        billItemsSettle = new ArrayList<>();
        totalDue = 0.0;
        totalCalculated = 0.0;
        total = 0.0;
        createSettleBillItemTable();
        calTotal();
//        pendingTransationsCalculations();
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getEjbFacade().edit(current);
            UtilityController.addSuccessMessage("Delete Successfull");
        } else {
            UtilityController.addSuccessMessage("Nothing To Delete");
        }
        current = new Summery();
    }

    public void deleteSettle() {

        if (settleItem != null) {
            settleItem.setRetired(true);
            settleItem.setRetiredAt(new Date());
            settleItem.setRetirer(getSessionController().getLoggedUser());
            getSettleFacade().edit(settleItem);
            UtilityController.addSuccessMessage("Delete Successfull");
        } else {
            UtilityController.addSuccessMessage("Nothing To Delete");
        }
        settleItem = new Settle();
    }

    public void saveSelected() {
        if (errorCheck()) {
            return;
        }

        if (getCurrent().getId() != null) {
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getEjbFacade().edit(getCurrent());
            UtilityController.addSuccessMessage("Update Successfull");
        } else {
            getCurrent().setCreatedAt(new Date());
            getCurrent().setCreater(getSessionController().getLoggedUser());
            getEjbFacade().create(getCurrent());
            UtilityController.addSuccessMessage("Save Successfull");
        }
    }

    public void saveSelectedSettle() {
        if (errorCheckSettleItem()) {
            return;
        }

        if (getSettleItem().getId() != null) {
            getSettleItem().setCreatedAt(new Date());
            getSettleItem().setCreater(getSessionController().getLoggedUser());
            getSettleFacade().edit(getSettleItem());
            UtilityController.addSuccessMessage("Update Successfull");
        } else {
            getSettleItem().setCreatedAt(new Date());
            getSettleItem().setCreater(getSessionController().getLoggedUser());
            getSettleFacade().create(getSettleItem());
            UtilityController.addSuccessMessage("Save Successfull");
        }
    }

    public void addSummery() {
        if (errorCheckBillItem()) {
            return;
        }

        if (errorCheckCashIn()) {
            return;
        }
        if (errorCheckCashInFrom()) {
            return;
        }

        if (getCurrentBillItem().getId() == null) {
//            getCurrentBillItem().setSearialNo(getBillNumberBean().summeryBillNumberGenerator(getSessionController().getLoggedUser()));
            getCurrentBillItem().setAgentRefNo(getBillNumberBean().summeryBillNumberGenerator(getSessionController().getLoggedUser(), (Summery) getCurrentBillItem().getItem()));
            getCurrentBillItem().setCreatedAt(new Date());
            getCurrentBillItem().setCreater(getSessionController().getLoggedUser());
            getCurrentBillItem().setHandOvered(false);
            System.out.println("getCurrentBillItem().getNetValue() = " + getCurrentBillItem().getNetValue());
            getCurrentBillItem().setNetValue(roundOff(getCurrentBillItem().getNetValue()));
            System.out.println("getCurrentBillItem().getNetValue() = " + getCurrentBillItem().getNetValue());
            getBillItemFacade().create(getCurrentBillItem());
            createSummeryBillItemTable();
            currentBillItem = null;
            UtilityController.addSuccessMessage("Summery Added Successfull");
        } else {
            getCurrentBillItem().setEditedAt(new Date());
            getCurrentBillItem().setEditor(getSessionController().getLoggedUser());
            getCurrentBillItem().setHandOvered(false);
            System.out.println("getCurrentBillItem().getNetValue() = " + getCurrentBillItem().getNetValue());
            getCurrentBillItem().setNetValue(roundOff(getCurrentBillItem().getNetValue()));
            System.out.println("getCurrentBillItem().getNetValue() = " + getCurrentBillItem().getNetValue());
            getBillItemFacade().edit(getCurrentBillItem());
            createSummeryBillItemTable();
            currentBillItem = null;
            UtilityController.addSuccessMessage("Summery Updated Successfull");
        }
        calFinalTotal();
    }

    public void addSettleItem() {
        if (errorCheckSettleBillItem()) {
            return;
        }

        if (getCurrentSetlleBillItem().getId() == null) {
            getCurrentSetlleBillItem().setCreatedAt(new Date());
            getCurrentSetlleBillItem().setCreater(getSessionController().getLoggedUser());
            getCurrentSetlleBillItem().setHandOvered(false);

            System.out.println("getCurrentSetlleBillItem().getDescreption() = " + getCurrentSetlleBillItem().getDescreption());
            if (getSessionController().getLoggedUser() != null) {
                getCurrentSetlleBillItem().setDescreption(getCurrentSetlleBillItem().getDescreption() + "-(" + getSessionController().getLoggedUser().getCode() + ")");
            }
            System.out.println("getCurrentSetlleBillItem().getDescreption() = " + getCurrentSetlleBillItem().getDescreption());

            System.out.println("getCurrentSetlleBillItem().getNetValue() = " + getCurrentSetlleBillItem().getNetValue());
            getCurrentSetlleBillItem().setNetValue(roundOff(getCurrentSetlleBillItem().getNetValue()));
            System.out.println("getCurrentSetlleBillItem().getNetValue() = " + getCurrentSetlleBillItem().getNetValue());
            getBillItemFacade().create(getCurrentSetlleBillItem());
            createSettleBillItemTable();
            Item tempItem = getCurrentSetlleBillItem().getItem();
            currentSetlleBillItem = new BillItem();
            getCurrentSetlleBillItem().setItem(tempItem);
            getCurrentSetlleBillItem().setFromTime(getCommonFunctions().getStartOfDay(new Date()));
            UtilityController.addSuccessMessage("Summery Added Successfull");
        } else {
            getCurrentSetlleBillItem().setEditedAt(new Date());
            getCurrentSetlleBillItem().setEditor(getSessionController().getLoggedUser());
            getCurrentSetlleBillItem().setHandOvered(false);
            System.out.println("getCurrentBillItem().getNetValue() = " + getCurrentBillItem().getNetValue());
            getCurrentSetlleBillItem().setNetValue(roundOff(getCurrentSetlleBillItem().getNetValue()));
            System.out.println("getCurrentBillItem().getNetValue() = " + getCurrentBillItem().getNetValue());
            getBillItemFacade().edit(getCurrentSetlleBillItem());
            createSummeryBillItemTable();
            Item tempItem = getCurrentSetlleBillItem().getItem();
            currentSetlleBillItem = new BillItem();
            getCurrentSetlleBillItem().setItem(tempItem);
            getCurrentSetlleBillItem().setFromTime(getCommonFunctions().getStartOfDay(new Date()));
            UtilityController.addSuccessMessage("Summery Updated Successfull");
        }
//        calFinalTotal();
    }

    public void removeSummery(BillItem bi) {
        if (bi == null) {
            UtilityController.addErrorMessage("Nothing to Remove");
            return;
        }
        bi.setRetired(true);
        bi.setRetiredAt(new Date());
        bi.setRetirer(getSessionController().getLoggedUser());
        getBillItemFacade().edit(bi);
        createSummeryBillItemTable();
        createSettleBillItemTable();
        UtilityController.addSuccessMessage("Summery Removed Successfully");
    }

    public void editSummery(BillItem bi) {
        if (bi == null) {
            UtilityController.addErrorMessage("Nothing to edit");
            return;
        }
        setCurrentBillItem(bi);
    }

    public void calTotal() {
        double dbl = getCashTransactionBean().calTotal(getBill().getCashTransaction());
        getBill().getCashTransaction().setCashValue(dbl);
        calFinalTotal();
    }

    public void calFinalTotal() {
        if (getSessionController().getLoggedUser().getDrawer() == null) {
            Drawer drw = new Drawer();
            drw.setCreatedAt(new Date());
            drw.setName(getSessionController().getLoggedUser().getWebUserPerson().getName() + " 's drawer");
            drw.getWebUsers().add(getSessionController().getLoggedUser());
            getDrawerFacade().create(drw);

            getSessionController().getLoggedUser().setDrawer(drw);
            getWebUserFacade().edit(getSessionController().getLoggedUser());
        }
        totalCalculated = 0 - (getSessionController().getLoggedUser().getDrawer().getRunningBallance()
                + getSessionController().getLoggedUser().getDrawer().getChequeBallance()
                + getSessionController().getLoggedUser().getDrawer().getCreditBallance()
                + getSessionController().getLoggedUser().getDrawer().getCreditCardBallance()
                + getSessionController().getLoggedUser().getDrawer().getIouBallance()
                + getSessionController().getLoggedUser().getDrawer().getSlipBallance())
                //need to settle short every time
                + getSessionController().getLoggedUser().getDrawer().getShortBallance();
        if (getBill().getCashTransaction().getCashValue() != null) {
            totalCalculated += getBill().getCashTransaction().getCashValue();
        }
        if (getBill().getCashTransaction().getChequeValue() != null) {
            totalCalculated += getBill().getCashTransaction().getChequeValue();
        }
        if (getBill().getCashTransaction().getCreditCardValue() != null) {
            totalCalculated += getBill().getCashTransaction().getCreditCardValue();
        }
        if (getBill().getCashTransaction().getCreditValue() != null) {
            totalCalculated += getBill().getCashTransaction().getCreditValue();
        }
        if (getBill().getCashTransaction().getSlipValue() != null) {
            totalCalculated += getBill().getCashTransaction().getSlipValue();
        }
        if (getBill().getCashTransaction().getIouValue() != null) {
            totalCalculated += getBill().getCashTransaction().getIouValue();
        }
        totalDue = totalCalculated - total;
    }

    public void settle() {
        calTotal();

        if (errorCheckCashIn()) {
            return;
        }
        if (errorCheckCashInFrom()) {
            return;
        }

        if (errorCheckSettle()) {
            return;
        }

        if (getBill().getCashTransaction().getCashValue() == null) {
            calTotal();
        }

        CashTransaction ct = getBill().getCashTransaction();
        getBill().setCashTransaction(null);
        ct.setShortValue(totalDue);
//        if (totalDue > 0) {
//            ct.setCashierExcessValue(totalDue);
//        }
//        if (totalDue < 0) {
//            ct.setCashierShortValue(totalDue);
//        }
        saveBill(ct);
        SaveBillItems(getBill(), selectedBillItems);
        SaveBillItems(getBill(), selectedBillItemsSettle);

        getCashTransactionBean().saveCashTransactionSummery(ct, getBill(), getSessionController().getLoggedUser(), InOutType.summery);
        getCashTransactionBean().addToBallanceModifid(getSessionController().getLoggedUser().getDrawer(), ct);

        getBill().setReferanceCashTransaction(ct);
        getBillFacade().edit(getBill());

        WebUser loggedUser = getWebUserFacade().find(getSessionController().getLoggedUser().getId());

        ct = new CashTransaction();
        ct.copyQty(getBill().getReferanceCashTransaction());
        ct.setDrawerTransactions(loggedUser);
        getCashTransactionBean().saveCashTransaction(ct, getBill(), getSessionController().getLoggedUser(), InOutType.summeryout);
        getCashTransactionBean().deductFromBallance(getSessionController().getLoggedUser().getDrawer(), ct);

        getBill().setCashTransaction(ct);
        getBillFacade().edit(getBill());

//        getCashTransactionBean().addToBallance(getSessionController().getLoggedUser().getDrawer(), ct);
        WebUser wb = getWebUserFacade().find(getSessionController().getLoggedUser().getId());
        getSessionController().setLoggedUser(wb);
        getBillSearch().setBill((BilledBill) getBill());
//        prepareAdd();

//        if (getBill().getToWebUser() != null) {
//            getCashTransactionBean().addToBallance(getBill().getToWebUser().getDrawer(), dbl, ct);
//        }
        UtilityController.addSuccessMessage("Succesfully Cash Out");
//        printPreview = true;
        prepareAdd();

    }

    public void settleBulkCashier() {
        calTotal();

        if (errorCheckSettleBulkCashier()) {
            return;
        }

        CashTransaction ct = getBill().getCashTransaction();
        getBill().setCashTransaction(null);
        saveBillHandOver(ct);
        SaveHandOverBillItems(getBill(), selectedBillItems);

        getCashTransactionBean().saveCashTransaction(ct, getBill(), getSessionController().getLoggedUser(), InOutType.handover);

        getBill().setCashTransaction(ct);
        getBillFacade().edit(getBill());

        getCashTransactionBean().deductFromBallance(getSessionController().getLoggedUser().getDrawer(), ct);

        WebUser wb = getWebUserFacade().find(getSessionController().getLoggedUser().getId());
        getSessionController().setLoggedUser(wb);
//        prepareAddhandOver();
//        if (getBill().getToWebUser() != null) {
//            getCashTransactionBean().addToBallance(getBill().getToWebUser().getDrawer(), dbl, ct);
//        }
        UtilityController.addSuccessMessage("Succesfully Settled");
        printPreview = true;

    }

    private void saveBill(CashTransaction cashTransaction) {
        double netTotal = 0;
        if (cashTransaction.getCashValue() != null) {
            netTotal += Math.abs(cashTransaction.getCashValue());
        }

        if (cashTransaction.getCreditCardValue() != null) {
            netTotal += Math.abs(cashTransaction.getCreditCardValue());
        }

        if (cashTransaction.getChequeValue() != null) {
            netTotal += Math.abs(cashTransaction.getChequeValue());
        }

        if (cashTransaction.getSlipValue() != null) {
            netTotal += Math.abs(cashTransaction.getSlipValue());
        }

        if (cashTransaction.getCreditValue() != null) {
            netTotal += Math.abs(cashTransaction.getCreditValue());
        }

        if (cashTransaction.getIouValue() != null) {
            netTotal += Math.abs(cashTransaction.getIouValue());
        }

        if (cashTransaction.getShortValue() != null) {
            if (cashTransaction.getShortValue() < 0) {
                netTotal += Math.abs(cashTransaction.getShortValue());
            }
        }
        getBill().setNetTotal(netTotal);

        getBill().setBillType(BillType.SummeryOut);
        getBill().setCreatedAt(new Date());
        getBill().setCreater(getSessionController().getLoggedUser());
        getBill().setDepartment(getSessionController().getDepartment());
        getBill().setInstitution(getSessionController().getInstitution());
        getBill().setFromWebUser(getSessionController().getLoggedUser());

        getBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getBill(), getBill().getBillType(), BillNumberSuffix.SUMMARYOUT));
        getBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), getBill(), getBill().getBillType(), BillNumberSuffix.SUMMARYOUT));

        getBillFacade().create(getBill());

    }

    private void saveBillHandOver(CashTransaction cashTransaction) {
        double netTotal = 0;
        if (cashTransaction.getCashValue() != null) {
            netTotal += Math.abs(cashTransaction.getCashValue());
        }

        if (cashTransaction.getCreditCardValue() != null) {
            netTotal += Math.abs(cashTransaction.getCreditCardValue());
        }

        if (cashTransaction.getChequeValue() != null) {
            netTotal += Math.abs(cashTransaction.getChequeValue());
        }

        if (cashTransaction.getSlipValue() != null) {
            netTotal += Math.abs(cashTransaction.getSlipValue());
        }

        if (cashTransaction.getCreditValue() != null) {
            netTotal += Math.abs(cashTransaction.getCreditValue());
        }

        if (cashTransaction.getIouValue() != null) {
            netTotal += Math.abs(cashTransaction.getIouValue());
        }

        if (cashTransaction.getShortValue() != null) {
            if (cashTransaction.getShortValue() < 0) {
                netTotal += Math.abs(cashTransaction.getShortValue());
            }
        }
        getBill().setNetTotal(0 - netTotal);

        getBill().setCreatedAt(new Date());
        getBill().setBillType(BillType.HandOver);
        getBill().setCreater(getSessionController().getLoggedUser());
        getBill().setDepartment(getSessionController().getDepartment());
        getBill().setInstitution(getSessionController().getInstitution());
        getBill().setFromWebUser(getSessionController().getLoggedUser());

        getBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getBill(), getBill().getBillType(), BillNumberSuffix.HANDOVER));
        getBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), getBill(), getBill().getBillType(), BillNumberSuffix.HANDOVER));

        getBillFacade().create(getBill());

    }

    private void SaveBillItems(Bill bill, List<BillItem> billItems) {
        for (BillItem bi : billItems) {
            getBill().getBillItems().add(bi);
//            Mr. Lahiru need to update books after receive the cash bulk cashier.
//            if (bi.getItem() instanceof Summery) {
//                getCashTransactionBean().addDepartmentHistory(bi, bi.getItem().getDepartment());
//            }
            bi.setBill(bill);
            bi.setHandOvered(true);
            bi.setHandOverAt(new Date());
            getBillItemFacade().edit(bi);
            System.out.println("bi.getDescreption() = " + bi.getDescreption());
        }
        getBillFacade().edit(getBill());
    }

    private void SaveHandOverBillItems(Bill bill, List<BillItem> billItems) {
        for (BillItem bi : billItems) {
            BillItem item = new BillItem();
            item.setReferanceBillItem(bi);
            item.copy(bi);
            item.invertValue(bi);
            item.setBill(bill);
            item.setHandOvered(true);
            item.setHandOverAt(new Date());
            item.setSettled(true);
            item.setSettleAt(new Date());
            item.setSettler(getSessionController().getLoggedUser());
            if (bill.getFromDepartment() != null) {
                item.setDeptId(bill.getFromDepartment().getName());
            }
            getBillItemFacade().create(item);
            getBill().getBillItems().add(item);

            if (getBill().getPaymentMethod() != PaymentMethod.IOU) {
                getCashTransactionBean().addDepartmentHistory(item, bill.getFromDepartment());
            }

            bi.setSettled(true);
            bi.setSettleAt(new Date());
            bi.setSettler(getSessionController().getLoggedUser());
            getBillItemFacade().edit(bi);
        }
        if (billItems == null || billItems.isEmpty()) {
//            String sql = " select i from Item i where i.retierd=false and i.name=:name ";
//            Map m = new HashMap();
//            m.put("name", "Cash Settle Item");
//            Item i = getItemFacade().findFirstBySQL(sql, m);
//            if (i != null) {
            BillItem item = new BillItem();
            item.setBill(bill);
            item.setHandOvered(true);
            item.setHandOverAt(new Date());
            item.setSettled(true);
            item.setSettleAt(new Date());
            item.setSettler(getSessionController().getLoggedUser());
            item.setDeptId(bill.getFromDepartment().getName());
            item.setNetValue(bill.getNetTotal());
            item.setCreatedAt(new Date());
            getBillItemFacade().create(item);
            getBill().getBillItems().add(item);

            System.out.println("bill.getFromDepartment() = " + bill.getFromDepartment());
            getCashTransactionBean().addDepartmentHistory(item, bill.getFromDepartment());

        }
        getBillFacade().edit(getBill());
    }

    public void pendingTransationsCalculations() {
//        System.err.println("in");
        total = calTotal(selectedBillItems);
        calFinalTotal();
//        System.out.println("total = " + total);
//        System.out.println("totalCalculated = " + totalCalculated);
//        System.err.println("out");
    }

    public void pendingTransationsCalculationsSettle() {
//        System.err.println("in");
        double d = calTotal(selectedBillItemsSettle, getBill().getCashTransaction());
        calFinalTotal();
//        System.out.println("total = " + total);
//        System.out.println("totalCalculated = " + totalCalculated);
//        System.err.println("out");
    }

    public void calculateTotals() {
        calTotal(selectedBillItemsSettle, getBill().getCashTransaction());
    }

    public void pendingTransationsHandOver() {
        total = calTotal(selectedBillItems);
        if (getBill().getPaymentMethod() != null) {
            switch (getBill().getPaymentMethod()) {
                case Card:
                    getBill().getCashTransaction().setCreditCardValue(total);
                    break;
                case Cheque:
                    getBill().getCashTransaction().setChequeValue(total);
                    break;
                case Slip:
                    getBill().getCashTransaction().setSlipValue(total);
                    break;
                case Credit:
                    getBill().getCashTransaction().setCreditValue(total);
                    break;
                case IOU:
                    getBill().getCashTransaction().setIouValue(total);
                    break;
            }
        }
    }

    public List<Summery> completeSummery(String qry) {
        List<Summery> a = null;
        Map m = new HashMap();
        String sql;

        if (qry == null) {
            System.err.println("**Null Qry**");
            return new ArrayList<>();
        }

        sql = "select s from Summery s where s.retired=false "
                + " and (upper(s.name) like :n or upper(s.code) like :n)"
                + " and s.institution is not null "
                + " order by s.name";
        m.put("n", "%" + qry.toUpperCase() + "%");

        a = getEjbFacade().findBySQL(sql, m, 30);
        System.out.println("a.size() = " + a.size());

        return a;
    }

    public List<Summery> completeSettle(String qry) {
        List<Summery> a = null;
        Map m = new HashMap();
        String sql;

        if (qry == null) {
            System.err.println("**Null Qry**");
            return new ArrayList<>();
        }

        sql = "select s from Settle s where s.retired=false "
                + " and (upper(s.name) like :n or upper(s.code) like :n) "
                + " order by s.name ";
        m.put("n", "%" + qry.toUpperCase() + "%");

        a = getEjbFacade().findBySQL(sql, m, 30);
        System.out.println("a.size() = " + a.size());

        return a;
    }

    public List<Settle> getSettleItems() {
        List<Settle> a = null;

        a = getSettleFacade().findBySQL("select s from Settle s where s.retired=false order by s.name");
        System.out.println("a.size() = " + a.size());

        return a;
    }

    private double roundOff(double d) {
        DecimalFormat newFormat = new DecimalFormat("#.##");
        try {
            return Double.valueOf(newFormat.format(d));
        } catch (Exception e) {
            return 0;
        }
    }

//    public List<Settle> getSettleitems() {
//        List<Settle> a = null;
//        a = getSettleFacade().findAll(Boolean.FALSE);
//        System.out.println("a.size() = " + a.size());
//        return a;
//    }
    public SummeryFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(SummeryFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public Summery getCurrent() {
        if (current == null) {
            current = new Summery();
        }
        return current;
    }

    public void setCurrent(Summery current) {
        this.current = current;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    private boolean errorCheck() {
        if (getCurrent() == null) {
            JsfUtil.addErrorMessage("Nothing to Save");
            return true;
        }
        if (getCurrent().getName() == null || getCurrent().getName().equals("")) {
            JsfUtil.addErrorMessage("Please Select Summery Name");
            return true;
        }
        if (getCurrent().getDepartment() == null) {
            JsfUtil.addErrorMessage("Please Select Deparment");
            return true;
        }
        if (getCurrent().getInstitution() == null) {
            JsfUtil.addErrorMessage("Please Select Institution");
            return true;
        }
        return false;
    }

    private boolean errorCheckSettleItem() {
        if (getSettleItem() == null) {
            JsfUtil.addErrorMessage("Nothing to Save");
            return true;
        }
        if (getSettleItem().getName() == null || getSettleItem().getName().equals("")) {
            JsfUtil.addErrorMessage("Please Select Summery Name");
            return true;
        }
        if (getSettleItem().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please Select Type");
            return true;
        }
        return false;
    }

    private boolean errorCheckSettle() {
        if (getBill().getCashTransaction() == null) {
            JsfUtil.addErrorMessage("Something went to wrong");
            return true;
        }
        if (getBill().getToWebUser() == null) {
            JsfUtil.addErrorMessage("Please Select a User");
            return true;
        }
        if (getBill().getToWebUser().equals(getSessionController().getLoggedUser())) {
            JsfUtil.addErrorMessage("Please Select a Correct User");
            return true;
        }

        if (selectedBillItems == null || selectedBillItems.isEmpty()) {
            JsfUtil.addErrorMessage("Please Select Summries to Settle");
            return true;
        }

        if (totalDue > 100) {
            JsfUtil.addErrorMessage("Please Settle Exess Cash as Excess Cash");
            return true;
        }

        return false;
    }

    private boolean errorCheckSettleBulkCashier() {
        if (getBill().getCashTransaction() == null) {
            JsfUtil.addErrorMessage("Something went to wrong");
            return true;
        }

        if (getBill().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please Select Payment Method");
            return true;
        }

        if (getBill().getFromDepartment() == null && getBill().getPaymentMethod() != PaymentMethod.IOU) {
            JsfUtil.addErrorMessage("Please Select Department");
            return true;
        }

        if (getBill().getPaymentMethod() == PaymentMethod.Cash || getBill().getPaymentMethod() == PaymentMethod.Cheque) {
            if (getBill().getToInstitution() == null) {
                JsfUtil.addErrorMessage("Please Select Bank");
                return true;
            }
        } else {
            if (getBill().getToWebUser() == null) {
                JsfUtil.addErrorMessage("Please Select User");
                return true;
            }
            if (getBill().getToWebUser().equals(getSessionController().getLoggedUser())) {
                JsfUtil.addErrorMessage("Please Select Defferent User to Hand Over");
                return true;
            }
            if (getSelectedBillItems().isEmpty()) {
                JsfUtil.addErrorMessage("Please Select Items");
                return true;
            }
        }

        if (getBill().getPaymentMethod() == PaymentMethod.Cash && getBill().getCashTransaction().getCashValue() <= 0) {
            JsfUtil.addErrorMessage("Please Select Correct Cash Amount");
            return true;
        }

        if (getBill().getPaymentMethod() == PaymentMethod.Card && getBill().getCashTransaction().getCreditCardValue() <= 0) {
            JsfUtil.addErrorMessage("Please Select Correct Credit Card Amount");
            return true;
        }
        if (getBill().getPaymentMethod() == PaymentMethod.Cheque && getBill().getCashTransaction().getChequeValue() <= 0) {
            JsfUtil.addErrorMessage("Please Select Correct Cheque Amount");
            return true;
        }
        if (getBill().getPaymentMethod() == PaymentMethod.Slip && getBill().getCashTransaction().getSlipValue() <= 0) {
            JsfUtil.addErrorMessage("Please Select Correct Slip Amount");
            return true;
        }
        if (getBill().getPaymentMethod() == PaymentMethod.Credit && getBill().getCashTransaction().getCreditValue() <= 0) {
            JsfUtil.addErrorMessage("Please Select Correct Credit Amount");
            return true;
        }
        if (getBill().getPaymentMethod() == PaymentMethod.IOU && getBill().getCashTransaction().getIouValue() <= 0) {
            JsfUtil.addErrorMessage("Please Select Correct IOU Amount");
            return true;
        }
        if (getBill().getComments() == null || getBill().getComments().equals("")) {
            JsfUtil.addErrorMessage("Please Enter a Comments");
            return true;
        }

        return false;
    }

    public void listnerBillTypeChange() {
        getBill().setCashTransaction(new CashTransaction());
        getBill().getCashTransaction().setCashValue(0.0);
        getBill().getCashTransaction().setCreditCardValue(0.0);
        getBill().getCashTransaction().setCreditValue(0.0);
        getBill().getCashTransaction().setSlipValue(0.0);
        getBill().getCashTransaction().setIouValue(0.0);
        getBill().getCashTransaction().setChequeValue(0.0);
        if (getBill().getBillType() != null) {
            switch (getBill().getBillType()) {
                case CreditCardSettle:
                    createBillItemTable(new PaymentMethod[]{PaymentMethod.Card});
                    break;
                case ChequeSettle:
                    createBillItemTable(new PaymentMethod[]{PaymentMethod.Cheque});
                    break;
                case SlipSettle:
                    createBillItemTable(new PaymentMethod[]{PaymentMethod.Slip});
                    break;
                case CreditSettle:
                    createBillItemTable(new PaymentMethod[]{PaymentMethod.Card});
                    break;
                case IOUSettle:
                    createBillItemTable(new PaymentMethod[]{PaymentMethod.IOU});
                    break;
            }
        }

    }

    public void listnerPaymentMethordChange() {
        getBill().setCashTransaction(new CashTransaction());
        getBill().getCashTransaction().setCashValue(0.0);
        getBill().getCashTransaction().setCreditCardValue(0.0);
        getBill().getCashTransaction().setCreditValue(0.0);
        getBill().getCashTransaction().setSlipValue(0.0);
        getBill().getCashTransaction().setIouValue(0.0);
        getBill().getCashTransaction().setChequeValue(0.0);
        selectedBillItems=new ArrayList<>();
        if (getBill().getPaymentMethod() != null) {
            switch (getBill().getPaymentMethod()) {
                case Card:
                    createBillItemTable(new PaymentMethod[]{PaymentMethod.Card});
                    break;
                case Cheque:
                    createBillItemTable(new PaymentMethod[]{PaymentMethod.Cheque});
                    break;
                case Slip:
                    createBillItemTable(new PaymentMethod[]{PaymentMethod.Slip});
                    break;
                case Credit:
                    createBillItemTable(new PaymentMethod[]{PaymentMethod.Credit});
                    break;
                case IOU:
                    createBillItemTable(new PaymentMethod[]{PaymentMethod.IOU});
                    break;
            }
        }

    }

    public BillItem getCurrentBillItem() {
        if (currentBillItem == null) {
            currentBillItem = new BillItem();
            currentBillItem.setFromTime(getCommonFunctions().getStartOfDay(new Date()));
            currentBillItem.setToTime(getCommonFunctions().getEndOfDay(new Date()));
        }
        return currentBillItem;
    }

    public void setCurrentBillItem(BillItem currentBillItem) {
        this.currentBillItem = currentBillItem;
    }

    private boolean errorCheckBillItem() {
        if (getCurrentBillItem() == null) {
            JsfUtil.addErrorMessage("Nothing To Add");
            return true;
        }
        if (getCurrentBillItem().getItem() == null) {
            JsfUtil.addErrorMessage("Please Select  Summery");
            return true;
        }
        if (getCurrentBillItem().getItem().getInstitution() == null || getCurrentBillItem().getItem().getDepartment() == null) {
            JsfUtil.addErrorMessage("Please Check  Summery Item Institution & Department");
            return true;
        }
        if (getCurrentBillItem().getItem().getInstitution().getInstitutionCode().isEmpty()
                || getCurrentBillItem().getItem().getInstitution().getInstitutionCode() == null
                || getCurrentBillItem().getItem().getDepartment().getDepartmentCode().isEmpty()
                || getCurrentBillItem().getItem().getDepartment().getDepartmentCode() == null) {
            JsfUtil.addErrorMessage("Please Check  Summery Item Institution Code & Department Code");
            return true;
        }
        if (getCurrentBillItem().getFromTime() == null) {
            JsfUtil.addErrorMessage("Please Select  From Time");
            return true;
        }
        if (getCurrentBillItem().getToTime() == null) {
            JsfUtil.addErrorMessage("Please Select  To Time");
            return true;
        }
        if (getCurrentBillItem().getToTime().before(getCurrentBillItem().getFromTime())) {
            JsfUtil.addErrorMessage("Please Select  correct From & To Times");
            return true;
        }
//        if (getCurrentBillItem().getNetValue() == 0.0) {
//            JsfUtil.addErrorMessage("Please Select Correct Amount");
//            return true;
//        }
        return false;
    }

    private boolean errorCheckSettleBillItem() {
        if (getCurrentSetlleBillItem() == null) {
            JsfUtil.addErrorMessage("Nothing To Add");
            return true;
        }
        if (getCurrentSetlleBillItem().getItem() == null) {
            JsfUtil.addErrorMessage("Please Select  Settle");
            return true;
        }
        if (getCurrentSetlleBillItem().getDescreption() == null || getCurrentSetlleBillItem().getDescreption().equals("")) {
            JsfUtil.addErrorMessage("Please Enter Memo");
            return true;
        }
        if (getCurrentSetlleBillItem().getFromTime() == null) {
            JsfUtil.addErrorMessage("Please Select  From Time");
            return true;
        }
        if (getCurrentSetlleBillItem().getNetValue() == 0.0) {
            JsfUtil.addErrorMessage("Please Select Correct Amount");
            return true;
        }
        return false;
    }

    private boolean errorCheckCashIn() {
        if (checkListToCashRecieve()) {
            return true;
        }
        return false;
    }

    private boolean errorCheckCashInFrom() {
        if (checkListToCashRecieveFrom()) {
            return true;
        }
        return false;
    }

    private boolean checkListToCashRecieve() {
        String sql;

        HashMap tmp = new HashMap();
        tmp.put("fromDate", getCommonFunctions().getDayBeforeOrAfterMonths(-1, true));
        tmp.put("toDate", getCommonFunctions().getEndOfDay(new Date()));
        tmp.put("toWeb", getSessionController().getLoggedUser());
        tmp.put("bTp", BillType.CashOut);
        tmp.put("bTp2", BillType.SummeryOut);

        sql = "Select b From Bill b where "
                + " b.retired=false"
                + " and b.cancelled=false "
                + " and b.toWebUser=:toWeb "
                //                + " and b.forwardReferenceBills is null "
                + " and (b.billType= :bTp or b.billType= :bTp2) "
                + " and b.createdAt between :fromDate and :toDate ";

        List<Bill> bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP);

        for (Bill b : bills) {
            if (b.getForwardReferenceBills() != null && !b.getForwardReferenceBills().isEmpty()) {
                System.out.println("b.getForwardReferenceBills().size() = " + b.getForwardReferenceBills().size());
            } else {
                System.out.println("b.getInsId() = " + b.getInsId());
                JsfUtil.addErrorMessage("Please Settle Cash In Transactions 1");
                return true;
            }
        }

        sql = "Select b From Bill b where "
                + " b.retired=false"
                + " and b.cancelled=false "
                + " and b.toWebUser=:toWeb "
                //                + " and b.forwardReferenceBills is not null "
                + " and (b.billType= :bTp or b.billType= :bTp2) "
                + " and b.createdAt between :fromDate and :toDate ";

        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP);

        for (Bill b : bills) {
            if (!b.checkActiveForwardReference()) {
                JsfUtil.addErrorMessage("Please Settle Cash In Transactions 2");
                return true;
            }
        }

        return false;

    }

    private boolean checkListToCashRecieveFrom() {
        String sql;

        HashMap tmp = new HashMap();
        tmp.put("fromDate", getCommonFunctions().getDayBeforeOrAfterMonths(-1, true));
        tmp.put("toDate", getCommonFunctions().getEndOfDay(new Date()));
        tmp.put("toWeb", getSessionController().getLoggedUser());
        tmp.put("bTp", BillType.CashOut);
        tmp.put("bTp2", BillType.SummeryOut);

        sql = "Select b From Bill b where "
                + " b.retired=false"
                + " and b.cancelled=false "
                + " and b.fromWebUser=:toWeb "
                //                + " and b.forwardReferenceBills is null "
                + " and (b.billType= :bTp or b.billType= :bTp2) "
                + " and b.createdAt between :fromDate and :toDate ";

        List<Bill> bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP);

        for (Bill b : bills) {
            if (b.getForwardReferenceBills() != null && !b.getForwardReferenceBills().isEmpty()) {
                System.out.println("b.getForwardReferenceBills().size() = " + b.getForwardReferenceBills().size());
            } else {
                System.out.println("b.getInsId() = " + b.getInsId());
                JsfUtil.addErrorMessage("Please Settle Cash  Out Pending Tranactions 1");
                return true;
            }
        }

        sql = "Select b From Bill b where "
                + " b.retired=false"
                + " and b.cancelled=false "
                + " and b.toWebUser=:toWeb "
                //                + " and b.forwardReferenceBills is not null "
                + " and (b.billType= :bTp or b.billType= :bTp2) "
                + " and b.createdAt between :fromDate and :toDate ";

        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP);

        for (Bill b : bills) {
            if (!b.checkActiveForwardReference()) {
                JsfUtil.addErrorMessage("Please Settle Cash  Out Pending Tranactions 2");
                return true;
            }
        }

        return false;

    }

    private List<Bill> fetshListToCashRecieve() {
//        List<Bill> list=new ArrayList<>();
        String sql;

        HashMap tmp = new HashMap();
        tmp.put("fromDate", getCommonFunctions().getDayBeforeOrAfterMonths(-1, true));
        tmp.put("toDate", getCommonFunctions().getEndOfDay(new Date()));
        tmp.put("toWeb", getSessionController().getLoggedUser());
        tmp.put("bTp", BillType.CashOut);
        tmp.put("bTp2", BillType.SummeryOut);

        sql = "Select b From Bill b where "
                + " b.retired=false"
                + " and b.cancelled=false "
                + " and b.toWebUser=:toWeb "
                //                + " and b.forwardReferenceBills is null "
                + " and (b.billType= :bTp or b.billType= :bTp2) "
                + " and b.createdAt between :fromDate and :toDate ";

        List<Bill> bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP);

        for (Bill b : bills) {
            if (b.getForwardReferenceBills() != null && !b.getForwardReferenceBills().isEmpty()) {
//                System.out.println("b.getForwardReferenceBills().size() = " + b.getForwardReferenceBills().size());
            } else {
//                System.out.println("b.getInsId() = " + b.getInsId());
//                JsfUtil.addErrorMessage("Please Settle Cash In Transactions 1");
                b.setOk(true);
                ListBills.add(b);
//                boolean  flag=false;
//                for (Bill bb : ListBills) {
//                    if (b.equals(bb)) {
//                       flag=true; 
//                    }
//                }
//                if (!flag) {
//                    ListBills.add(b);
//                }
            }
        }
        System.out.println("ListBills.size(1) = " + ListBills.size());

        sql = "Select b From Bill b where "
                + " b.retired=false"
                + " and b.cancelled=false "
                + " and b.toWebUser=:toWeb "
                //                + " and b.forwardReferenceBills is not null "
                + " and (b.billType= :bTp or b.billType= :bTp2) "
                + " and b.createdAt between :fromDate and :toDate ";

        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP);

        for (Bill b : bills) {
            if (!b.checkActiveForwardReference()) {
                System.out.println("b.getInsId() = " + b.getInsId());
//                JsfUtil.addErrorMessage("Please Settle Cash In Transactions 2");
                boolean flag = false;
                for (Bill bb : ListBills) {
                    if (b.equals(bb)) {
                        flag = true;
                    }
                }
                if (!flag) {
                    b.setOk(true);
                    ListBills.add(b);
                }
            }
        }
        System.out.println("ListBills.size(2) = " + ListBills.size());
        return ListBills;

    }

//    private List<Bill> fetshListToCashRecieveNew() {
//        ListBills = new ArrayList<>();
//
//        String sql;
//        HashMap m = new HashMap();
//
//        sql = "Select b From Bill b where "
//                + " b.retired=false "
//                + " and b.cancelled=false "
//                + " and b.toWebUser=:toWeb "
//                + " and b.id in (select rb.forwardReferenceBill.id From Bill rb where rb.retired=false and rb.forwardReferenceBill.id=b.id) "
//                + " and (b.billType= :bTp or b.billType= :bTp2) "
//                + " and b.createdAt between :fromDate and :toDate ";
//
//        m.put("fromDate", getCommonFunctions().getDayBeforeOrAfterMonths(-1, true));
//        m.put("toDate", getCommonFunctions().getEndOfDay(new Date()));
//        m.put("toWeb", getSessionController().getLoggedUser());
//        m.put("bTp", BillType.CashOut);
//        m.put("bTp2", BillType.SummeryOut);
//
//        List<Bill> bills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
//
//        for (Bill b : bills) {
//                System.out.println("b.getInsId() = " + b.getInsId());
//                b.setOk(true);
//                ListBills.add(b);
//        }
//        System.out.println("ListBills.size(1) = " + ListBills.size());
//
//        sql = "Select b From Bill b where "
//                + " b.retired=false"
//                + " and b.cancelled=false "
//                + " and b.toWebUser=:toWeb "
//                //                + " and b.forwardReferenceBills is not null "
//                + " and (b.billType= :bTp or b.billType= :bTp2) "
//                + " and b.createdAt between :fromDate and :toDate ";
//
//        bills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
//
//        for (Bill b : bills) {
//            if (!b.checkActiveForwardReference()) {
//                System.out.println("b.getInsId() = " + b.getInsId());
////                JsfUtil.addErrorMessage("Please Settle Cash In Transactions 2");
//                boolean flag = false;
//                for (Bill bb : ListBills) {
//                    if (b.equals(bb)) {
//                        flag = true;
//                    }
//                }
//                if (!flag) {
//                    b.setOk(true);
//                    ListBills.add(b);
//                }
//            }
//        }
//        System.out.println("ListBills.size(2) = " + ListBills.size());
//        return ListBills;
//
//    }

    private List<Bill> fetchListToCashRecieveFrom() {
//        List<Bill> list=new ArrayList<>();
        String sql;

        HashMap tmp = new HashMap();
        tmp.put("fromDate", getCommonFunctions().getDayBeforeOrAfterMonths(-1, true));
        tmp.put("toDate", getCommonFunctions().getEndOfDay(new Date()));
        tmp.put("toWeb", getSessionController().getLoggedUser());
        tmp.put("bTp", BillType.CashOut);
        tmp.put("bTp2", BillType.SummeryOut);

        sql = "Select b From Bill b where "
                + " b.retired=false"
                + " and b.cancelled=false "
                + " and b.fromWebUser=:toWeb "
                //                + " and b.forwardReferenceBills is null "
                + " and (b.billType= :bTp or b.billType= :bTp2) "
                + " and b.createdAt between :fromDate and :toDate ";

        List<Bill> bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP);

        for (Bill b : bills) {
            if (b.getForwardReferenceBills() != null && !b.getForwardReferenceBills().isEmpty()) {
                System.out.println("b.getForwardReferenceBills().size() = " + b.getForwardReferenceBills().size());
            } else {
                System.out.println("b.getInsId() = " + b.getInsId());
//                JsfUtil.addErrorMessage("Please Settle Cash  Out Pending Tranactions 1");
//                list.add(b);
                boolean flag = false;
                for (Bill bb : ListBills) {
                    if (b.equals(bb)) {
                        System.out.println("bb = " + bb.getInsId());
                        System.out.println("b = " + b);
                        flag = true;
                    }
                }
                if (!flag) {
                    b.setOk(true);
                    ListBills.add(b);
                }
            }
        }

        sql = "Select b From Bill b where "
                + " b.retired=false"
                + " and b.cancelled=false "
                + " and b.toWebUser=:toWeb "
                //                + " and b.forwardReferenceBills is not null "
                + " and (b.billType= :bTp or b.billType= :bTp2) "
                + " and b.createdAt between :fromDate and :toDate ";

        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP);

        for (Bill b : bills) {
            if (!b.checkActiveForwardReference()) {
                System.out.println("b.getInsId() = " + b.getInsId());
//                JsfUtil.addErrorMessage("Please Settle Cash  Out Pending Tranactions 2");
//                list.add(b);
                boolean flag = false;
                for (Bill bb : ListBills) {
                    if (b.equals(bb)) {
                        System.out.println("bb = " + bb);
                        System.out.println("b = " + b);
                        flag = true;
                    }
                }
                if (!flag) {
                    b.setOk(true);
                    ListBills.add(b);
                }
            }
        }
        System.out.println("ListBills.size(2) = " + ListBills.size());
        return ListBills;

    }

    public List<Bill> fetchListBillsCashIn() {
        List<Bill> items = new ArrayList<>();
        String sql;
        Map m = new HashMap();
        sql = "select b from BilledBill b where b.billType = :billType "
                + " and b.createdAt <= :date "
                + " and b.retired=false "
                + " and b.cancelled=false "
                + " and b.creater=:w order by b.createdAt desc  ";
        m.put("billType", BillType.SummeryOut);
        m.put("date", new Date());
        m.put("w", getSessionController().getLoggedUser());

        Bill b = getBillFacade().findFirstBySQL(sql, m, TemporalType.TIMESTAMP);
        System.out.println("b = " + b);
        m = new HashMap();
        sql = "select b from BilledBill b where (b.billType = :billType or b.billType = :billType2) "
                + " and b.creater=:w "
                + " and b.retired=false "
                + " and b.cancelled=false ";

        if (b != null) {
            System.out.println("b.getCreatedAt() = " + b.getCreatedAt());
            sql += " and b.createdAt between :fd and :td ";
            m.put("fd", b.getCreatedAt());
            m.put("td", new Date());
//            m.put("td", commonFunctions.getEndOfDay(new Date()));
        } else {
            sql += " and b.createdAt <= :date ";
            m.put("date", new Date());
//            m.put("date", commonFunctions.getEndOfDay(new Date()));
        }
        sql += " order by b.createdAt  ";

        m.put("billType", BillType.CashIn);
        m.put("billType2", BillType.CashOut);
        m.put("w", getSessionController().getLoggedUser());

        items = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
        System.out.println("items.size(1) = " + items.size());
        total = 0.0;
        if (items != null && !items.isEmpty()) {
            for (Bill bb : items) {
                bb.setOk(false);
            }
        }
        List<Bill> removeItems = new ArrayList<>();
        if (ListBills.size() > 0) {
            for (Bill bill : ListBills) {
                for (Bill bn : items) {
                    if (bill.equals(bn)) {
                        removeItems.add(bn);
                    }
                }
            }
            System.out.println("removeItems.size() = " + removeItems.size());
            items.removeAll(removeItems);
        }
        System.out.println("items.size(2) = " + items.size());
        ListBills.addAll(items);

        System.out.println("ListBills.size(3) = " + ListBills.size());
        return ListBills;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
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

    public void createSummeryBillItemTable() {
        String sql;
        Map m = new HashMap();
        billItems = new ArrayList<>();

        sql = " select bi from BillItem bi where "
                + " bi.retired=false "
                + " and bi.creater=:user "
                + " and bi.handOvered=false "
                + " and bi.item.name!=:name"
                //                + " and bi.referenceBill is null "
                //                + " and (bi.bill is null or bi.bill.cancelled=true) "
                + " and type(bi.item)=:class "
                + " order by bi.createdAt ";

        m.put("name", "Cash Out");
        m.put("user", getSessionController().getLoggedUser());
        m.put("class", Summery.class);

        billItems = getBillItemFacade().findBySQL(sql, m);
        List<BillItem> items = new ArrayList<>();
        for (BillItem bi : billItems) {
//            System.out.println("bi = " + bi.getId());
//            System.out.println("bi.getBill() = " + bi.getBill());
            if (bi.getBill() != null) {
//                System.out.println("bi.getBill().isCancelled() = " + bi.getBill().isCancelled());
//                System.out.println("bi.getBill().getBillClass() = " + bi.getBill().getBillClass());
//                System.out.println("bi.getBill().getBilledBill() = " + bi.getBill().getBilledBill());
                if (bi.getBill() instanceof CancelledBill) {
//                    System.out.println("++++bi.getBill().getBillClass() = " + bi.getBill().getBillClass());
                    items.add(bi);
                }
            }
        }
//        System.out.println("items.size() = " + items.size());
        billItems.removeAll(items);
//        if (billItems != null) {
//            calTotals(billItems);
//        }
//        System.out.println("billItems.size() = " + billItems.size());
    }

    public void createBillItemTable(PaymentMethod[] pms) {
        String sql;
        Map m = new HashMap();
        billItems = new ArrayList<>();
        selectedBillItems = new ArrayList<>();

        sql = " select bi from BillItem bi where "
                + " bi.retired=false "
                + " and bi.creater=:user "
                + " and bi.handOvered=false "
                + " and bi.referanceBillItem is not null "
                + " and bi.settled=false "
                + " and bi.item.paymentMethod in :pm "
                + " and type(bi.item)=:class "
                + " order by bi.createdAt ";

        m.put("pm", Arrays.asList(pms));
        m.put("user", getSessionController().getLoggedUser());
        m.put("class", Settle.class);

        billItems = getBillItemFacade().findBySQL(sql, m);

        System.out.println("billItems.size() = " + billItems.size());
    }

    public void createSettleBillItemTable() {
        String sql;
        Map m = new HashMap();
        billItemsSettle = new ArrayList<>();

        sql = " select bi from BillItem bi where "
                + " bi.retired=false "
                + " and bi.creater=:user "
                + " and bi.handOvered=false "
                //                + " and bi.referenceBill is null "
                //                + " and (bi.bill is null or bi.bill.cancelled=true) "
                + " and type(bi.item)=:class "
                + " order by bi.createdAt ";

        m.put("user", getSessionController().getLoggedUser());
        m.put("class", Settle.class);

        billItemsSettle = getBillItemFacade().findBySQL(sql, m);
        System.out.println("billItemsSettle.size() = " + billItemsSettle.size());
        List<BillItem> items = new ArrayList<>();
        for (BillItem bi : billItemsSettle) {
//            System.out.println("bi = " + bi.getId());
//            System.out.println("bi.getBill() = " + bi.getBill());
            if (bi.getBill() != null) {
//                System.out.println("bi.getBill().isCancelled() = " + bi.getBill().isCancelled());
//                System.out.println("bi.getBill().getBillClass() = " + bi.getBill().getBillClass());
//                System.out.println("bi.getBill().getBilledBill() = " + bi.getBill().getBilledBill());
                if (bi.getBill() instanceof CancelledBill) {
//                    System.out.println("++++bi.getBill().getBillClass() = " + bi.getBill().getBillClass());
                    items.add(bi);
                }
            }
        }
//        System.out.println("items.size() = " + items.size());
        billItemsSettle.removeAll(items);
//        System.out.println("billItemsSettle.size() = " + billItemsSettle.size());
    }

    public void createBillItemTable() {
        createSummeryBillItemTable();
        createSettleBillItemTable();
    }

    private double calTotal(List<BillItem> items) {
        double d = 0.0;
        for (BillItem bi : items) {
            d += bi.getNetValue();
        }
        return d;
    }

    private double calTotal(List<BillItem> items, CashTransaction ct) {
        double d = 0.0;
        ct.setChequeValue(0.0);
        ct.setCreditCardValue(0.0);
        ct.setCreditValue(0.0);
        ct.setIouValue(0.0);
        ct.setSlipValue(0.0);
        for (BillItem bi : items) {
            if (bi.getItem().getPaymentMethod() == PaymentMethod.Cheque) {
                ct.setChequeValue(ct.getChequeValue() + bi.getNetValue());
            }
            if (bi.getItem().getPaymentMethod() == PaymentMethod.Card) {
                ct.setCreditCardValue(ct.getCreditCardValue() + bi.getNetValue());
            }
            if (bi.getItem().getPaymentMethod() == PaymentMethod.Credit) {
                ct.setCreditValue(ct.getCreditValue() + bi.getNetValue());
            }
            if (bi.getItem().getPaymentMethod() == PaymentMethod.IOU) {
                ct.setIouValue(ct.getIouValue() + bi.getNetValue());
            }
            if (bi.getItem().getPaymentMethod() == PaymentMethod.Slip) {
                ct.setSlipValue(ct.getSlipValue() + bi.getNetValue());
            }
            d += bi.getNetValue();
        }
        return d;
    }

//    private void calTotals(List<BillItem> items) {
//        total = 0.0;
//        totalEfected = 0.0;
//        totalNotEfected = 0.0;
//        for (BillItem bi : items) {
//            total += bi.getNetValue();
//            if (bi.getBill() != null) {
//                totalNotEfected += bi.getNetValue();
//            } else {
//                totalEfected += bi.getNetValue();
//            }
//        }
//    }
    public BillNumberBean getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberBean billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public Bill getBill() {
        if (bill == null) {
            bill = new BilledBill();
            bill.setCashTransaction(new CashTransaction());
        }
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public List<BillItem> getSelectedBillItems() {
        if (selectedBillItems == null) {
            selectedBillItems = new ArrayList<>();
        }
        return selectedBillItems;
    }

    public void setSelectedBillItems(List<BillItem> selectedBillItems) {
        this.selectedBillItems = selectedBillItems;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public WebUserFacade getWebUserFacade() {
        return webUserFacade;
    }

    public void setWebUserFacade(WebUserFacade webUserFacade) {
        this.webUserFacade = webUserFacade;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public double getTotalCalculated() {
        return totalCalculated;
    }

    public void setTotalCalculated(double totalCalculated) {
        this.totalCalculated = totalCalculated;
    }

    public double getTotalDue() {
        return totalDue;
    }

    public void setTotalDue(double totalDue) {
        this.totalDue = totalDue;
    }

    public BillItem getCurrentSetlleBillItem() {
        if (currentSetlleBillItem == null) {
            currentSetlleBillItem = new BillItem();
            currentSetlleBillItem.setFromTime(getCommonFunctions().getStartOfDay(new Date()));
            currentSetlleBillItem.setToTime(getCommonFunctions().getStartOfDay(new Date()));
        }
        return currentSetlleBillItem;
    }

    public void setCurrentSetlleBillItem(BillItem currentSetlleBillItem) {
        this.currentSetlleBillItem = currentSetlleBillItem;
    }

    public SettleFacade getSettleFacade() {
        return settleFacade;
    }

    public void setSettleFacade(SettleFacade settleFacade) {
        this.settleFacade = settleFacade;
    }

    public List<BillItem> getBillItemsSettle() {
        return billItemsSettle;
    }

    public void setBillItemsSettle(List<BillItem> billItemsSettle) {
        this.billItemsSettle = billItemsSettle;
    }

    public Settle getSettleItem() {
        if (settleItem == null) {
            settleItem = new Settle();
        }
        return settleItem;
    }

    public void setSettleItem(Settle settle) {
        this.settleItem = settle;
    }

    public List<BillItem> getSelectedBillItemsSettle() {
        return selectedBillItemsSettle;
    }

    public void setSelectedBillItemsSettle(List<BillItem> selectedBillItemsSettle) {
        this.selectedBillItemsSettle = selectedBillItemsSettle;
    }

    public DrawerFacade getDrawerFacade() {
        return DrawerFacade;
    }

    public void setDrawerFacade(DrawerFacade DrawerFacade) {
        this.DrawerFacade = DrawerFacade;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public BillSearch getBillSearch() {
        return billSearch;
    }

    public void setBillSearch(BillSearch billSearch) {
        this.billSearch = billSearch;
    }

    public List<Bill> getBills() {
        if (ListBills == null) {
            ListBills = new ArrayList<>();
        }
        return ListBills;
    }

    public void setBills(List<Bill> bills) {
        this.ListBills = bills;
    }

    @FacesConverter("summery")
    public static class SummeryControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SummeryController controller = (SummeryController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "summeryController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Summery) {
                Summery o = (Summery) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + SummeryController.class.getName());
            }
        }
    }

}
