/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.cashTransaction;

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
import com.divudi.entity.Settle;
import com.divudi.entity.Summery;
import com.divudi.entity.cashTransaction.CashTransaction;
import com.divudi.entity.cashTransaction.Drawer;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class CashOutController implements Serializable {

    private boolean printPreview;
    Bill bill;
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
    private Drawer drawer;

    private List<BillItem> billItems;
    private List<BillItem> selectedBillItems;

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
        if (getBill().getToWebUser() == null) {
            JsfUtil.addErrorMessage("Please Select a User");
            return true;
        }
        if (getBill().getToWebUser().equals(getSessionController().getLoggedUser())) {
            JsfUtil.addErrorMessage("Please Select a Deffernt User");
            return true;
        }

        return false;
    }

    private boolean errorCheckValues() {
//        if (getSessionController().getLoggedUser().getDrawer().getRunningBallance()<getBill().getCashTransaction().getCashValue()) {
//            JsfUtil.addErrorMessage("Wrong Cash Amount");
//            return true;
//        }
//        if (getSessionController().getLoggedUser().getDrawer().getChequeBallance()<getBill().getCashTransaction().getChequeValue()) {
//            JsfUtil.addErrorMessage("Wrong Cheque Amount");
//            return true;
//        }
//        if (getSessionController().getLoggedUser().getDrawer().getCreditBallance()<getBill().getCashTransaction().getCreditValue()) {
//            JsfUtil.addErrorMessage("Wrong Credit Amount");
//            return true;
//        }
//        if (getSessionController().getLoggedUser().getDrawer().getCreditCardBallance()<getBill().getCashTransaction().getCreditCardValue()) {
//            JsfUtil.addErrorMessage("Wrong Credit Card Amount");
//            return true;
//        }
//        if (getSessionController().getLoggedUser().getDrawer().getSlipBallance()<getBill().getCashTransaction().getSlipValue()) {
//            JsfUtil.addErrorMessage("Wrong Slip Amount");
//            return true;
//        }
//        if (getSessionController().getLoggedUser().getDrawer().getIouBallance()<getBill().getCashTransaction().getIouValue()) {
//            JsfUtil.addErrorMessage("Wrong IOU Amount");
//            return true;
//        }
        if (getBill().getCashTransaction().getCashValue() == 0.0 && getBill().getCashTransaction().getChequeValue() == 0.0
                && getBill().getCashTransaction().getCreditValue() == 0.0 && getBill().getCashTransaction().getCreditCardValue() == 0.0
                && getBill().getCashTransaction().getSlipValue() == 0.0 && getBill().getCashTransaction().getIouValue() == 0.0) {
            JsfUtil.addErrorMessage("Wrong Amount");
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

        getBill().setNetTotal(0 - netTotal);

        getBill().setBillType(BillType.CashOut);
        getBill().setCreatedAt(new Date());
        getBill().setCreater(getSessionController().getLoggedUser());
        getBill().setDepartment(getSessionController().getDepartment());
        getBill().setInstitution(getSessionController().getInstitution());
        getBill().setFromWebUser(getSessionController().getLoggedUser());

        getBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getBill(), getBill().getBillType(), BillNumberSuffix.CSOUT));
        getBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), getBill(), getBill().getBillType(), BillNumberSuffix.CSOUT));

        getBillFacade().create(getBill());

    }

    private void saveBill(CashTransaction cashTransaction, BillType bt) {
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

        getBill().setNetTotal(0 - netTotal);

        getBill().setBillType(bt);
        getBill().setCreatedAt(new Date());
        getBill().setCreater(getSessionController().getLoggedUser());
        getBill().setDepartment(getSessionController().getDepartment());
        getBill().setInstitution(getSessionController().getInstitution());
        getBill().setFromWebUser(getSessionController().getLoggedUser());

        getBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getBill(), getBill().getBillType(), BillNumberSuffix.CSOUT));
        getBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), getBill(), getBill().getBillType(), BillNumberSuffix.CSOUT));

        getBillFacade().create(getBill());

    }

    @EJB
    private WebUserFacade webUserFacade;

    public void settle() {
        if (errorCheck()) {
            return;
        }
        if (errorCheckValues()) {
            return;
        }

        if (getBill().getCashTransaction().getCashValue() == null) {
            calTotal();
        }

        CashTransaction ct = getBill().getCashTransaction();
        getBill().setCashTransaction(null);
        saveBill(ct);
        SaveBillItem(getBill(), "Cash Out");
        SaveBillItems(getBill(), selectedBillItems);

        getCashTransactionBean().saveCashOutTransaction(ct, getBill(), getSessionController().getLoggedUser());

        getBill().setCashTransaction(ct);
        getBillFacade().edit(getBill());

        getCashTransactionBean().deductFromBallance(getSessionController().getLoggedUser().getDrawer(), ct);

        WebUser wb = getWebUserFacade().find(getSessionController().getLoggedUser().getId());
        getSessionController().setLoggedUser(wb);

//        if (getBill().getToWebUser() != null) {
//            getCashTransactionBean().addToBallance(getBill().getToWebUser().getDrawer(), dbl, ct);
//        }
        UtilityController.addSuccessMessage("Succesfully Cash Out");
        printPreview = true;

    }

    public void makeNull() {
        printPreview = false;
        bill = null;
        drawer = null;
        createBillItemTable(new PaymentMethod[]{PaymentMethod.Card, PaymentMethod.Cheque, PaymentMethod.Credit, PaymentMethod.IOU, PaymentMethod.Slip});
    }

    public void calTotal() {
        double dbl = getCashTransactionBean().calTotal(getBill().getCashTransaction());
        getBill().getCashTransaction().setCashValue(dbl);
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

    public void pendingTransationsHandOver() {
        calTotal(selectedBillItems, getBill().getCashTransaction());
    }

    private void calTotal(List<BillItem> items, CashTransaction ct) {
        ct.setCreditCardValue(0.0);
        ct.setChequeValue(0.0);
        ct.setSlipValue(0.0);
        ct.setCreditValue(0.0);
        ct.setIouValue(0.0);
        for (BillItem bi : items) {
            switch (bi.getItem().getPaymentMethod()) {
                case Card:
                    ct.setCreditCardValue(getBill().getCashTransaction().getCreditCardValue() + bi.getNetValue());
                    break;
                case Cheque:
                    ct.setChequeValue(getBill().getCashTransaction().getChequeValue() + bi.getNetValue());
                    break;
                case Slip:
                    ct.setSlipValue(getBill().getCashTransaction().getSlipValue() + bi.getNetValue());
                    break;
                case Credit:
                    ct.setCreditValue(getBill().getCashTransaction().getCreditValue() + bi.getNetValue());
                    break;
                case IOU:
                    ct.setIouValue(getBill().getCashTransaction().getIouValue() + bi.getNetValue());
                    break;
            }
        }
    }
    
    private void SaveBillItems(Bill bill, List<BillItem> billItems) {
        for (BillItem bi : billItems) {
            bi.setHandOvered(true);
            bi.setHandOverAt(new Date());
            bi.setSettled(true);
            bi.setSettler(getSessionController().getLoggedUser());
            bi.setSettleAt(new Date());
            getBillItemFacade().edit(bi);
            
            BillItem item=new BillItem();
            item.copy(bi);
            
            item.setBill(bill);
            item.setReferanceBillItem(bi);
            item.setHandOvered(true);
            item.setHandOverAt(new Date());
            getBillItemFacade().create(item);
            getBill().getBillItems().add(item);
        }
        getBillFacade().edit(getBill());
    }

    /**
     * Creates a new instance of BulkCashierController
     */
    public CashOutController() {
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

    public Drawer getDrawer() {
        if (drawer == null) {
            drawer = getCashTransactionBean().getDrawer(getSessionController().getLoggedUser());
        }
        return drawer;
    }

    public void setDrawer(Drawer drawer) {
        this.drawer = drawer;
    }

    public WebUserFacade getWebUserFacade() {
        return webUserFacade;
    }

    public void setWebUserFacade(WebUserFacade webUserFacade) {
        this.webUserFacade = webUserFacade;
    }

    private void SaveBillItem(Bill bill, String type) {
//        Item i=getItemFacade().findByField("name", type, false);
        Summery s = getSummeryFacade().findByField("name", type, false);
        System.out.println("s = " + s);
        if (s == null) {
            s = new Summery();
            s.setName(type);
            getSummeryFacade().create(s);
        }
        BillItem bi = new BillItem();
        bi.setBill(bill);
        bi.setItem(s);
        bi.setHandOvered(true);
        bi.setNetValue(0 - getBill().getNetTotal());
        bi.setCreatedAt(new Date());
        bi.setCreater(getSessionController().getLoggedUser());
        getBillItemFacade().create(bi);

        getBill().setSingleBillItem(bi);
        getBillFacade().edit(getBill());
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

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public List<BillItem> getSelectedBillItems() {
        return selectedBillItems;
    }

    public void setSelectedBillItems(List<BillItem> selectedBillItems) {
        this.selectedBillItems = selectedBillItems;
    }

}
