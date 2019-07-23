/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.cashTransaction;

import com.divudi.bean.BillSearch;
import com.divudi.bean.SessionController;
import com.divudi.bean.UtilityController;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.ejb.BillNumberBean;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Item;
import com.divudi.entity.Settle;
import com.divudi.entity.Summery;
import com.divudi.entity.cashTransaction.CashTransaction;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.CashTransactionFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.SummeryFacade;
import com.divudi.facade.WebUserFacade;
import com.divudi.facade.util.JsfUtil;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;
import javax.ejb.EJB;
import javax.inject.Inject;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class CashInController implements Serializable {

    private boolean printPreview;
    Bill bill;
    private Bill referenceBill;
    @EJB
    private CashTransactionBean cashTransactionBean;
    @EJB
    CashTransactionFacade cashTransactionFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillNumberBean billNumberBean;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private SummeryFacade summeryFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @Inject
    private SessionController sessionController;
    @Inject
    private BillSearch billSearch;

    public CashTransactionFacade getCashTransactionFacade() {
        return cashTransactionFacade;
    }

    public void setCashTransactionFacade(CashTransactionFacade cashTransactionFacade) {
        this.cashTransactionFacade = cashTransactionFacade;
    }

    private boolean errorCheck() {
        if (getBill().getCashTransaction() == null) {
            return true;
        }
        if ((getBill().getCashTransaction().getCashValue() == null || getBill().getCashTransaction().getCashValue() == 0.0)
                && (getBill().getCashTransaction().getCreditValue() == null || getBill().getCashTransaction().getCreditValue() == 0.0)
                && (getBill().getCashTransaction().getCreditCardValue() == null || getBill().getCashTransaction().getCreditCardValue() == 0.0)
                && (getBill().getCashTransaction().getSlipValue() == null || getBill().getCashTransaction().getSlipValue() == 0.0)
                && (getBill().getCashTransaction().getIouValue() == null || getBill().getCashTransaction().getIouValue() == 0.0)
                && (getBill().getCashTransaction().getChequeValue() == null || getBill().getCashTransaction().getChequeValue() == 0.0)) {
            JsfUtil.addErrorMessage("Please Enter correct Value");
            return true;
        }

        return false;
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

    private void saveBill(CashTransaction ct) {
        double netTotal = 0;
        if (ct.getCashValue() != null) {
            netTotal += Math.abs(ct.getCashValue());
        }
        if (ct.getChequeValue() != null) {
            netTotal += Math.abs(ct.getChequeValue());
        }
        if (ct.getCreditCardValue() != null) {
            netTotal += Math.abs(ct.getCreditCardValue());
        }
        if (ct.getSlipValue() != null) {
            netTotal += Math.abs(ct.getSlipValue());
        }
        if (ct.getCreditValue() != null) {
            netTotal += Math.abs(ct.getCreditValue());
        }
        if (ct.getIouValue() != null) {
            netTotal += Math.abs(ct.getIouValue());
        }
        if (ct.getShortValue() != null) {
            if (ct.getShortValue() < 0) {
                netTotal += Math.abs(ct.getShortValue());
            }
        }

        getBill().setNetTotal(netTotal);
        getBill().setBillType(BillType.CashIn);
        getBill().setCreatedAt(new Date());
        getBill().setCreater(getSessionController().getLoggedUser());
        getBill().setDepartment(getSessionController().getDepartment());
        getBill().setInstitution(getSessionController().getInstitution());
        getBill().setToWebUser(getSessionController().getLoggedUser());
        getBill().setBackwardReferenceBill(getReferenceBill());

        getBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getBill(), getBill().getBillType(), BillNumberSuffix.CSIN));
        getBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), getBill(), getBill().getBillType(), BillNumberSuffix.CSIN));

        getBillFacade().create(getBill());

    }

    @EJB
    WebUserFacade webUserFacade;

    public WebUserFacade getWebUserFacade() {
        return webUserFacade;
    }

    public void setWebUserFacade(WebUserFacade webUserFacade) {
        this.webUserFacade = webUserFacade;
    }

    public void settle() {
        if (errorCheck()) {
            return;
        }

        if (getBill().getCashTransaction().getCashValue() == null) {
            calTotal();
        }

        CashTransaction ct = getBill().getCashTransaction();
        getBill().setCashTransaction(null);
        saveBill(ct);
        SaveBillItem(getBill(), "Cash In");

        getCashTransactionBean().saveCashInTransaction(ct, getBill(), getSessionController().getLoggedUser());

        getBill().setCashTransaction(ct);
        getBillFacade().edit(getBill());
        System.out.println("getReferenceBill() = " + getReferenceBill());
        if (getReferenceBill() != null) {
            getReferenceBill().getForwardReferenceBills().add(getBill());
            getBillFacade().edit(getReferenceBill());
            System.out.println("getReferenceBill().getBillItems().size() = " + getReferenceBill().getBillItems().size());
            for (BillItem bi : getReferenceBill().getBillItems()) {
                System.out.println("bi.getItem().getName() = " + bi.getItem().getName());
                if (bi.getItem() instanceof Summery) {
                    BillItem billItem = new BillItem();
                    billItem.setBill(getBill());
                    billItem.setItem(bi.getItem());
                    billItem.setFromTime(bi.getFromTime());
                    billItem.setToTime(bi.getToTime());
                    billItem.setAgentRefNo(bi.getAgentRefNo());
                    billItem.setNetValue(bi.getNetValue());
                    getBillItemFacade().create(billItem);
                    if (bi.getItem().getDepartment() != null) {
                        billItem.setDeptId(bi.getItem().getDepartment().getName());
                        getBillItemFacade().edit(billItem);
                        getCashTransactionBean().addDepartmentHistory(billItem, billItem.getItem().getDepartment());
                    }
                }
                if (bi.getItem() instanceof Settle) {
                    System.out.println("---bi.isSettled() = " + bi.isSettled());
                    System.out.println("---bi.getItem().getName() = " + bi.getItem().getName());
                    System.out.println("referenceBill.getBillType() = " + referenceBill.getBillType());
                    bi.setHandOvered(true);
                    bi.setSettled(true);
                    getBillItemFacade().edit(bi);
                    BillItem billItem = new BillItem();
                    billItem.setBill(getBill());
                    billItem.setReferanceBillItem(bi);
                    billItem.setItem(bi.getItem());
                    billItem.setDescreption(bi.getDescreption());
                    billItem.setFromTime(bi.getFromTime());
                    billItem.setToTime(bi.getToTime());
                    billItem.setNetValue(bi.getNetValue());
                    billItem.setCreatedAt(new Date());
                    billItem.setCreater(getSessionController().getLoggedUser());
                    if (referenceBill.getBillType() == BillType.HandOver) {
                        billItem.setHandOverAt(new Date());
                        billItem.setHandOvered(true);
                    }
                    System.out.println("billItem.getId() = " + billItem.getId());
                    getBillItemFacade().create(billItem);
                    System.out.println("billItem.getId() = " + billItem.getId());
                    getBill().getBillItems().add(billItem);
                    getBillFacade().edit(getBill());
                }
            }

        }
        getBillSearch().setBill((BilledBill) getBill());

//        if (getBill().getFromWebUser() != null) {
//            getCashTransactionBean().deductFromBallance(getBill().getFromWebUser().getDrawer(), dbl, ct);
//        }
        getCashTransactionBean().addToBallance(getSessionController().getLoggedUser().getDrawer(), ct);

        WebUser wb = getWebUserFacade().find(getSessionController().getLoggedUser().getId());
        getSessionController().setLoggedUser(wb);

        UtilityController.addSuccessMessage("Succesfully Cash Inned ");
        printPreview = true;

    }

    public void makeNull() {
        printPreview = false;
        bill = null;
        referenceBill = null;
    }

    public void calTotal() {
        double dbl = getCashTransactionBean().calTotal(getBill().getCashTransaction());
        //System.err.println("Cal Total " + dbl);
        getBill().getCashTransaction().setCashValue(dbl);
    }

    private void SaveBillItem(Bill bill, String type) {
//        Item i=getItemFacade().findByField("name", type, false);
        Summery s = getSummeryFacade().findByField("name", type, false);
        System.out.println("i = " + s);
        if (s == null) {
            s = new Summery();
            s.setName(type);
            getSummeryFacade().create(s);
        }
        BillItem bi = new BillItem();
        bi.setBill(bill);
        bi.setItem(s);
        bi.setHandOvered(true);
        bi.setNetValue(getBill().getNetTotal());
        bi.setCreatedAt(new Date());
        bi.setCreater(getSessionController().getLoggedUser());
        getBillItemFacade().create(bi);

        getBill().setSingleBillItem(bi);
        getBillFacade().edit(getBill());
    }

    /**
     * Creates a new instance of BulkCashierController
     */
    public CashInController() {
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public boolean getPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillNumberBean getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberBean billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public Bill getReferenceBill() {
        return referenceBill;
    }

    public void setReferenceBill(Bill referenceBill) {
        makeNull();
        this.referenceBill = referenceBill;
        getBill().getCashTransaction().copyQty(referenceBill.getCashTransaction());
        getBill().setFromWebUser(referenceBill.getFromWebUser());
        getBill().setNetTotal(referenceBill.getNetTotal());
        System.out.println("referenceBill.getPaymentMethod() = " + referenceBill.getPaymentMethod());
        System.out.println("referenceBill.getBillType() = " + referenceBill.getBillType());
        System.out.println("referenceBill.getCashTransaction().getIouValue() = " + referenceBill.getCashTransaction().getIouValue());
        System.out.println("referenceBill.getCashTransaction().getCashValue() = " + referenceBill.getCashTransaction().getCashValue());
        if (referenceBill != null && referenceBill.getBillType() == BillType.HandOver && referenceBill.getPaymentMethod() == PaymentMethod.IOU) {
            getBill().getCashTransaction().setCashValue(referenceBill.getCashTransaction().getIouValue());
            getBill().getCashTransaction().setIouValue(0.0);
        }
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public SummeryFacade getSummeryFacade() {
        return summeryFacade;
    }

    public void setSummeryFacade(SummeryFacade summeryFacade) {
        this.summeryFacade = summeryFacade;
    }

    public BillSearch getBillSearch() {
        return billSearch;
    }

    public void setBillSearch(BillSearch billSearch) {
        this.billSearch = billSearch;
    }

}
