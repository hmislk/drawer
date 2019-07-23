/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.cashTransaction;

import com.divudi.bean.SessionController;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.ejb.BillNumberBean;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Department;
import com.divudi.entity.cashTransaction.CashTransaction;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.WebUserFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class DrawerAdjustmentController implements Serializable {

    @Inject
    SessionController sessionController;
////////////////////////
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    BillNumberBean billNumberBean;
    @EJB
    CashTransactionBean cashTransactionBean;
    /////////////////////////
    private Bill adjustmentBill;
    private WebUser webUser;
    Double value;
    Department department;
    String comment;
    String radioVal = "0";
    private boolean printPreview;

    /**
     * Creates a new instance of PharmacySaleController
     */
    public DrawerAdjustmentController() {
    }

    public Bill getAdjustmentBill() {
        if (adjustmentBill == null) {
            adjustmentBill = new BilledBill();
            adjustmentBill.setBillType(BillType.DrawerAdjustment);
        }
        return adjustmentBill;
    }

    public void setAdjustmentPreBill(Bill adjustmentPreBill) {
        this.adjustmentBill = adjustmentPreBill;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public void makeNull() {
        printPreview = false;
        comment = null;
        value = null;
        adjustmentBill = null;
        department = null;
        webUser=null;
    }

    private void saveAdjustmentBill() {
        getAdjustmentBill().setCreatedAt(Calendar.getInstance().getTime());
        getAdjustmentBill().setCreater(getWebUser());
        getAdjustmentBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getLoggedUser().getDepartment(), getAdjustmentBill(), getAdjustmentBill().getBillType(), BillNumberSuffix.DRADJ));
        getAdjustmentBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getLoggedUser().getDepartment().getInstitution(), getAdjustmentBill(), getAdjustmentBill().getBillType(), BillNumberSuffix.DRADJ));
        getAdjustmentBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getAdjustmentBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getAdjustmentBill().setToDepartment(null);
        getAdjustmentBill().setToInstitution(null);
        getAdjustmentBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getAdjustmentBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getAdjustmentBill().setComments(comment);
        //creater link in to history of drawer approved user use as a creater
        getAdjustmentBill().setApproveUser(getSessionController().getLoggedUser());
        getAdjustmentBill().setApproveAt(new Date());
        if (getAdjustmentBill().getId() == null) {
            getBillFacade().create(getAdjustmentBill());
        } else {
            getBillFacade().edit(getAdjustmentBill());
        }
    }

    private boolean errorCheck() {
        if (getWebUser() == null) {
            JsfUtil.addErrorMessage("Please Select a User");
            return true;
        }
        if (getWebUser().getDrawer() == null) {
            return true;
        }

        if (getValue() == null) {
            JsfUtil.addErrorMessage("Please Select a Value");
            return true;
        }
        if (getComment() == null || getComment().equals("")) {
            JsfUtil.addErrorMessage("Please Enter a comment");
            return true;
        }

        return false;
    }

    private boolean errorCheckCashBook() {
        if (getDepartment() == null) {
            JsfUtil.addErrorMessage("Please Select Cash Book");
            return true;
        }

        if (getValue() == null) {
            JsfUtil.addErrorMessage("Please Select Correct Value");
            return true;
        }
        if (comment == null || comment.equals("")) {
            JsfUtil.addErrorMessage("Please Enter a comment");
            return true;
        }

        return false;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    @EJB
    WebUserFacade webUserFacade;

    public WebUserFacade getWebUserFacade() {
        return webUserFacade;
    }

    public void setWebUserFacade(WebUserFacade webUserFacade) {
        this.webUserFacade = webUserFacade;
    }

    private void save(double ballance, PaymentMethod paymentMethod) {

        saveAdjustmentBill();

        double difference = ballance - getValue();

//        //System.err.println("Cash Ballance " + cashBallance);
//        //System.err.println("Difference  " + difference);
        CashTransaction cashTransaction = new CashTransaction();
        cashTransaction.setCreatedAt(new Date());
        cashTransaction.setCreater(getWebUser());

//        if (getSessionController().getLoggedUser().getDrawer() != null) {
//            cashTransaction.setCashValue(getSessionController().getLoggedUser().getDrawer().getRunningBallance());
//            cashTransaction.setCreditCardValue(getSessionController().getLoggedUser().getDrawer().getCreditCardBallance());
//            cashTransaction.setChequeValue(getSessionController().getLoggedUser().getDrawer().getChequeBallance());
//            cashTransaction.setSlipValue(getSessionController().getLoggedUser().getDrawer().getSlipBallance());
//            cashTransaction.setCreditValue(getSessionController().getLoggedUser().getDrawer().getCreditBallance());
//            cashTransaction.setIouValue(getSessionController().getLoggedUser().getDrawer().getIouBallance());
//            cashTransaction.setShortValue(getSessionController().getLoggedUser().getDrawer().getShortBallance());
//        }

        if (difference < 0) {
            //  //System.err.println("Adding");
            if (paymentMethod != null) {
                switch (paymentMethod) {
                    case Cash:
                        cashTransaction.setCashValue(0 - difference);
                        break;
                    case Card:
                        cashTransaction.setCreditCardValue(0 - difference);
                        break;
                    case Cheque:
                        cashTransaction.setChequeValue(0 - difference);
                        break;
                    case Slip:
                        cashTransaction.setSlipValue(0 - difference);
                        break;
                    case Credit:
                        cashTransaction.setCreditValue(0 - difference);
                        break;
                    case IOU:
                        cashTransaction.setIouValue(0 - difference);
                }
            } else {
                cashTransaction.setShortValue(0 - difference);
            }

            getCashTransactionBean().addCashTransactionHistory(cashTransaction, adjustmentBill, getWebUser().getDrawer());
            getCashTransactionBean().saveCashAdjustmentTransactionIn(cashTransaction, adjustmentBill, getWebUser().getDrawer(), getWebUser());

            getAdjustmentBill().setCashTransaction(cashTransaction);
            getAdjustmentBill().setNetTotal(0 - difference);
            getBillFacade().edit(getAdjustmentBill());
            getCashTransactionBean().addToBallance(getWebUser().getDrawer(), cashTransaction);

        } else {
            ////System.err.println("Diduct");
            if (paymentMethod != null) {
                switch (paymentMethod) {
                    case Cash:
                        cashTransaction.setCashValue(0 - difference);
                        break;
                    case Card:
                        cashTransaction.setCreditCardValue(0 - difference);
                        break;
                    case Cheque:
                        cashTransaction.setChequeValue(0 - difference);
                        break;
                    case Slip:
                        cashTransaction.setSlipValue(0 - difference);
                        break;
                    case Credit:
                        cashTransaction.setCreditValue(0 - difference);
                        break;
                    case IOU:
                        cashTransaction.setIouValue(0 - difference);
                }
            } else {
                cashTransaction.setShortValue(0 - difference);
            }

            getCashTransactionBean().addCashTransactionHistory(cashTransaction, adjustmentBill, getWebUser().getDrawer());
            getCashTransactionBean().saveCashAdjustmentTransactionOut(cashTransaction, adjustmentBill, getWebUser().getDrawer(), getWebUser());

            getAdjustmentBill().setCashTransaction(cashTransaction);
            getAdjustmentBill().setNetTotal(0 - difference);
            getBillFacade().edit(getAdjustmentBill());
            getCashTransactionBean().deductFromBallance(getWebUser().getDrawer(), cashTransaction);
        }

        WebUser wb = getWebUserFacade().find(getSessionController().getLoggedUser().getId());
        getSessionController().setLoggedUser(wb);

        printPreview = true;
    }

    private void saveCashBook(double ballance) {

        saveAdjustmentBill();
        getAdjustmentBill().setToDepartment(getDepartment());
        getAdjustmentBill().setToInstitution(getDepartment().getInstitution());
        getAdjustmentBill().setNetTotal(getValue());

        double difference = 0.0;
        if (getRadioVal().equals("0")) {
            difference = getValue() - ballance;
        } else {
            difference = getValue();
        }

//        //System.err.println("Cash Ballance " + cashBallance);
//        //System.err.println("Difference  " + difference);
        CashTransaction cashTransaction = new CashTransaction();
        cashTransaction.setCreatedAt(new Date());
        cashTransaction.setCreater(getSessionController().getLoggedUser());

        BillItem billItem = new BillItem();
        billItem.setBill(getAdjustmentBill());
        billItem.setNetValue(roundOff(difference));
        billItem.setDeptId(getDepartment().getName());
        getBillItemFacade().create(billItem);

        getAdjustmentBill().getBillItems().add(billItem);
        getBillFacade().edit(getAdjustmentBill());

        if (getRadioVal().equals("0")) {
            getCashTransactionBean().addDepartmentHistory(billItem, getDepartment(), getValue());
        } else {
            getCashTransactionBean().addDepartmentHistory(billItem, getDepartment(), getValue() + getDepartment().getBalance());
        }

        printPreview = true;
    }

    private double roundOff(double d) {
        DecimalFormat newFormat = new DecimalFormat("#.##");
        try {
            return Double.valueOf(newFormat.format(d));
        } catch (Exception e) {
            return 0;
        }
    }

    public void saveAdjustBillCash() {
        if (errorCheck()) {
            return;
        }
        save(getWebUser().getDrawer().getRunningBallance(), PaymentMethod.Cash);
    }

    public void saveAdjustBillCheque() {
        if (errorCheck()) {
            return;
        }
        save(getWebUser().getDrawer().getChequeBallance(), PaymentMethod.Cheque);
    }

    public void saveAdjustBillSlip() {
        if (errorCheck()) {
            return;
        }
        save(getWebUser().getDrawer().getSlipBallance(), PaymentMethod.Slip);
    }

    public void saveAdjustBillCreditCard() {
        if (errorCheck()) {
            return;
        }
        save(getWebUser().getDrawer().getCreditCardBallance(), PaymentMethod.Card);
    }

    public void saveAdjustBillCredit() {
        if (errorCheck()) {
            return;
        }
        save(getWebUser().getDrawer().getCreditBallance(), PaymentMethod.Credit);
    }

    public void saveAdjustBillIOU() {
        if (errorCheck()) {
            return;
        }
        save(getWebUser().getDrawer().getIouBallance(), PaymentMethod.IOU);
    }

    public void saveAdjustBillShort() {
        if (errorCheck()) {
            return;
        }
        save(getWebUser().getDrawer().getShortBallance(), null);
    }

    public void saveAdjustBillCashBook() {
        if (errorCheckCashBook()) {
            return;
        }
        saveCashBook(getDepartment().getBalance());
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public BillNumberBean getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberBean billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public String getRadioVal() {
        return radioVal;
    }

    public void setRadioVal(String radioVal) {
        this.radioVal = radioVal;
    }

    public WebUser getWebUser() {
        if (webUser==null) {
            webUser=new WebUser();
        }
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

}
