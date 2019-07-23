/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.ejb;

import com.divudi.bean.SessionController;
import com.divudi.data.BillType;
import com.divudi.data.InOutType;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.Department;
import com.divudi.entity.cashTransaction.CashTransaction;
import com.divudi.entity.cashTransaction.CashTransactionHistory;
import com.divudi.entity.cashTransaction.Drawer;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.CashTransactionFacade;
import com.divudi.facade.CashTransactionHistoryFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.DrawerFacade;
import com.divudi.facade.WebUserFacade;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 *
 * @author safrin
 */
@Stateless
public class CashTransactionBean {

    @EJB
    CashTransactionHistoryFacade cashTransactionHistoryFacade;
    @EJB
    private DrawerFacade drawerFacade;
    @EJB
    private CashTransactionFacade cashTransactionFacade;
    @EJB
    private WebUserFacade webUserFacade;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private BillItemFacade billItemFacade;

    @Inject
    private SessionController sessionController;

    public void updateDrawers() {
        //   //System.err.println("1");
        List<Drawer> items = getDrawerFacade().findBySQL("Select d from Drawer d where d.retired=false ");
        //   //System.err.println("2");
        for (Drawer dr : items) {
            dr.setWebUsers(getUser(dr));
            getDrawerFacade().edit(dr);
        }
        //   //System.err.println("3");
    }

    public List<WebUser> getUser(Drawer drawer) {
        String sql = "select wu from WebUser wu "
                + " where wu.retired=false "
                + " and wu.drawer=:d";
        HashMap hm = new HashMap();
        hm.put("d", drawer);
        List<WebUser> wb = getWebUserFacade().findBySQL(sql, hm);
        if (wb == null) {
            wb = new ArrayList<>();
        }

        return wb;
    }

    public CashTransaction setCashTransactionValue(CashTransaction cashTransaction, Bill bill) {
        if (bill.getPaymentMethod() == null) {
            return cashTransaction;
        }
        switch (bill.getPaymentMethod()) {
            case Cash:
                cashTransaction.setCashValue(bill.getNetTotal());
                break;
            case Card:
                cashTransaction.setCreditCardValue(bill.getNetTotal());
                break;
            case Cheque:
                cashTransaction.setChequeValue(bill.getNetTotal());
                break;
            case Slip:
                cashTransaction.setSlipValue(bill.getNetTotal());
                break;
            case Credit:
                cashTransaction.setCreditValue(bill.getNetTotal());
                break;
            case IOU:
                cashTransaction.setIouValue(bill.getNetTotal());
                break;
        }

        return cashTransaction;
    }

    public CashTransaction saveCashInTransaction(CashTransaction ct, Bill bill, WebUser webUser) {
        if (ct == null) {
            return null;
        }
        if (webUser == null) {
            return null;
        }

        if (ct.getCashValue() != null) {
            ct.setCashValue(Math.abs(ct.getCashValue()));
        }

        if (ct.getSlipValue() != null) {
            ct.setSlipValue(Math.abs(ct.getSlipValue()));
        }

        if (ct.getCreditCardValue() != null) {
            ct.setCreditCardValue(Math.abs(ct.getCreditCardValue()));
        }

        if (ct.getChequeValue() != null) {
            ct.setChequeValue(Math.abs(ct.getChequeValue()));
        }

        if (ct.getCreditValue() != null) {
            ct.setCreditValue(Math.abs(ct.getCreditValue()));
        }

        if (ct.getIouValue() != null) {
            ct.setIouValue(Math.abs(ct.getIouValue()));
        }

        if (ct.getShortValue() != null) {
            ct.setShortValue(ct.getShortValue());
        }

        if (bill.getBillType() == BillType.SummeryOut) {
            ct.setInOutType(InOutType.summeryin);
        } else {
            ct.setInOutType(InOutType.in);
        }
        ct.setCreatedAt(new Date());
        ct.setDrawer(getDrawer(webUser));
        ct.setCreater(webUser);
        ct.setBill(bill);
        getCashTransactionFacade().create(ct);

        return ct;
    }

    public CashTransaction saveCashInTransactionNegtiveValue(CashTransaction ct, Bill bill, WebUser webUser) {
        if (ct == null) {
            return null;
        }
        if (webUser == null) {
            return null;
        }

        if (ct.getCashValue() != null) {
            ct.setCashValue(ct.getCashValue());
        }

        if (ct.getSlipValue() != null) {
            ct.setSlipValue(ct.getSlipValue());
        }

        if (ct.getCreditCardValue() != null) {
            ct.setCreditCardValue(ct.getCreditCardValue());
        }

        if (ct.getChequeValue() != null) {
            ct.setChequeValue(ct.getChequeValue());
        }

        if (ct.getCreditValue() != null) {
            ct.setCreditValue(ct.getCreditValue());
        }

        if (ct.getIouValue() != null) {
            ct.setIouValue(ct.getIouValue());
        }

        if (ct.getShortValue() != null) {
            ct.setShortValue(ct.getShortValue());
        }

        ct.setInOutType(InOutType.summery);
        ct.setCreatedAt(new Date());
        ct.setDrawer(getDrawer(webUser));
        ct.setCreater(webUser);
        ct.setBill(bill);
        getCashTransactionFacade().create(ct);

        return ct;
    }

    public CashTransaction saveCashOutTransaction(CashTransaction ct, Bill bill, WebUser webUser) {
        if (ct.getCashValue() != null) {
            ct.setCashValue(0 - Math.abs(ct.getCashValue()));
        }

        if (ct.getSlipValue() != null) {
            ct.setSlipValue(0 - Math.abs(ct.getSlipValue()));
        }

        if (ct.getCreditCardValue() != null) {
            ct.setCreditCardValue(0 - Math.abs(ct.getCreditCardValue()));
        }

        if (ct.getChequeValue() != null) {
            ct.setChequeValue(0 - Math.abs(ct.getChequeValue()));
        }

        if (ct.getCreditValue() != null) {
            ct.setCreditValue(0 - Math.abs(ct.getCreditValue()));
        }

        if (ct.getIouValue() != null) {
            ct.setIouValue(0 - Math.abs(ct.getIouValue()));
        }

        ct.setInOutType(InOutType.out);
        ct.setDrawer(getDrawer(webUser));
        ct.setCreatedAt(new Date());
        ct.setBill(bill);
        ct.setCreater(webUser);
        getCashTransactionFacade().create(ct);

        return ct;
    }

    public CashTransaction saveCashTransaction(CashTransaction ct, Bill bill, WebUser webUser, InOutType type) {
        if (ct.getCashValue() != null) {
            if (type == InOutType.in || type == InOutType.summery) {
                ct.setCashValue(Math.abs(ct.getCashValue()));
            }
            if (type == InOutType.out || type == InOutType.handover || type == InOutType.summeryout) {
                ct.setCashValue(0 - Math.abs(ct.getCashValue()));
            }
        }

        if (ct.getSlipValue() != null) {
            if (type == InOutType.in || type == InOutType.summery) {
                ct.setSlipValue(Math.abs(ct.getSlipValue()));
            }
            if (type == InOutType.out || type == InOutType.handover || type == InOutType.summeryout) {
                ct.setSlipValue(0 - Math.abs(ct.getSlipValue()));
            }
        }

        if (ct.getCreditCardValue() != null) {
            if (type == InOutType.in || type == InOutType.summery) {
                ct.setCreditCardValue(Math.abs(ct.getCreditCardValue()));
            }
            if (type == InOutType.out || type == InOutType.handover || type == InOutType.summeryout) {
                ct.setCreditCardValue(0 - Math.abs(ct.getCreditCardValue()));
            }
        }

        if (ct.getChequeValue() != null) {
            if (type == InOutType.in || type == InOutType.summery) {
                ct.setChequeValue(Math.abs(ct.getChequeValue()));
            }
            if (type == InOutType.out || type == InOutType.handover || type == InOutType.summeryout) {
                ct.setChequeValue(0 - Math.abs(ct.getChequeValue()));
            }
        }

        if (ct.getCreditValue() != null) {
            if (type == InOutType.in || type == InOutType.summery) {
                ct.setCreditValue(Math.abs(ct.getCreditValue()));
            }
            if (type == InOutType.out || type == InOutType.handover || type == InOutType.summeryout) {
                ct.setCreditValue(0 - Math.abs(ct.getCreditValue()));
            }
        }

        if (ct.getIouValue() != null) {
            if (type == InOutType.in || type == InOutType.summery) {
                ct.setIouValue(Math.abs(ct.getIouValue()));
            }
            if (type == InOutType.out || type == InOutType.handover || type == InOutType.summeryout) {
                ct.setIouValue(0 - Math.abs(ct.getIouValue()));
            }
        }

//        if (ct.getCashierShortValue() != null) {
////            if (type == InOutType.in || type == InOutType.summery) {
////                ct.setCashierExcessValue(Math.abs(ct.getCashierShortValue()));
////            }
//            if (type == InOutType.out || type == InOutType.handover || type == InOutType.summeryout) {
//                ct.setCashierShortValue(Math.abs(ct.getCashierShortValue()));
//            }
//        }
//        
//        if (ct.getCashierExcessValue() != null) {
////            if (type == InOutType.in || type == InOutType.summery) {
////                ct.setCashierExcessValue(Math.abs(ct.getCashierShortValue()));
////            }
//            if (type == InOutType.out || type == InOutType.handover || type == InOutType.summeryout) {
//                ct.setCashierExcessValue(0-Math.abs(ct.getCashierExcessValue()));
//            }
//        }
        ct.setInOutType(type);
        ct.setDrawer(getDrawer(webUser));
        ct.setCreatedAt(new Date());
        ct.setBill(bill);
        ct.setCreater(webUser);
        getCashTransactionFacade().create(ct);

        return ct;
    }

    public CashTransaction saveCashTransactionSummery(CashTransaction ct, Bill bill, WebUser webUser, InOutType type) {
        if (ct.getCashValue() != null) {
            if (type == InOutType.summery) {
                ct.setCashValue(-getSessionController().getLoggedUser().getDrawer().getRunningBallance() + Math.abs(ct.getCashValue()));
            }
        }

        if (ct.getSlipValue() != null) {
            if (type == InOutType.summery) {
                ct.setSlipValue(-getSessionController().getLoggedUser().getDrawer().getSlipBallance() + Math.abs(ct.getSlipValue()));
            }
        }

        if (ct.getCreditCardValue() != null) {
            if (type == InOutType.summery) {
                ct.setCreditCardValue(-getSessionController().getLoggedUser().getDrawer().getCreditCardBallance() + Math.abs(ct.getCreditCardValue()));
            }
        }

        if (ct.getChequeValue() != null) {
            if (type == InOutType.summery) {
                ct.setChequeValue(-getSessionController().getLoggedUser().getDrawer().getChequeBallance() + Math.abs(ct.getChequeValue()));
            }
        }

        if (ct.getCreditValue() != null) {
            if (type == InOutType.summery) {
                ct.setCreditValue(-getSessionController().getLoggedUser().getDrawer().getCreditBallance() + Math.abs(ct.getCreditValue()));
            }
        }

        if (ct.getIouValue() != null) {
            if (type == InOutType.summery) {
                ct.setIouValue(-getSessionController().getLoggedUser().getDrawer().getIouBallance() + Math.abs(ct.getIouValue()));
            }
        }

        if (ct.getShortValue() != null) {
            if (type == InOutType.summery) {
                ct.setShortValue(-getSessionController().getLoggedUser().getDrawer().getShortBallance() + ct.getShortValue());
            }
        }

        ct.setInOutType(type);
        ct.setDrawer(getDrawer(webUser));
        ct.setCreatedAt(new Date());
        ct.setReferanceBill(bill);
        ct.setCreater(webUser);
        getCashTransactionFacade().create(ct);

        return ct;
    }

    public CashTransaction saveCashAdjustmentTransactionOut(CashTransaction ct, Bill bill, Drawer drawer, WebUser webUser) {

        ct.setInOutType(InOutType.out);
        ct.setDrawer(drawer);
        ct.setCreatedAt(new Date());
        ct.setBill(bill);
        ct.setCreater(webUser);
        getCashTransactionFacade().create(ct);

        return ct;
    }

    public void addCashTransactionHistory(CashTransaction ct, Bill b, Drawer drawer) {

        CashTransactionHistory sh = new CashTransactionHistory();
        sh.setTransactionValue(b.getNetTotal());

        sh.setCashBallance(drawer.getRunningBallance());
        sh.setChequeBallance(drawer.getChequeBallance());
        sh.setCreditBallance(drawer.getCreditBallance());
        sh.setCreditCardBallance(drawer.getCreditCardBallance());
        sh.setIouBallance(drawer.getIouBallance());
        sh.setSlipBallance(drawer.getSlipBallance());
        sh.setShortBallance(drawer.getShortBallance());

        sh.setFromDate(new Date());
        sh.setHxDate(Calendar.getInstance().get(Calendar.DATE));
        sh.setHxMonth(Calendar.getInstance().get(Calendar.MONTH));
        sh.setHxWeek(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
        sh.setHxYear(Calendar.getInstance().get(Calendar.YEAR));

        sh.setCashAt(new Date());
        sh.setCreatedAt(new Date());
        getCashTransactionHistoryFacade().create(sh);

        ct.setCashTransactionHistory(sh);
    }

    public CashTransaction saveCashAdjustmentTransactionIn(CashTransaction ct, Bill bill, Drawer drawer, WebUser webUser) {

        ct.setInOutType(InOutType.in);
        ct.setDrawer(drawer);
        ct.setCreatedAt(new Date());
        ct.setBill(bill);
        ct.setCreater(webUser);
        getCashTransactionFacade().create(ct);

        return ct;
    }

    public CashTransactionHistoryFacade getCashTransactionHistoryFacade() {
        return cashTransactionHistoryFacade;
    }

    public void setCashTransactionHistoryFacade(CashTransactionHistoryFacade cashTransactionHistoryFacade) {
        this.cashTransactionHistoryFacade = cashTransactionHistoryFacade;
    }

    public double calTotal(CashTransaction cashTransaction) {
        double val = 0;
        if (cashTransaction.getQty1() != null) {
            val += cashTransaction.getQty1() * 1;
        }
        if (cashTransaction.getQty10() != null) {
            val += cashTransaction.getQty10() * 10;
        }
        if (cashTransaction.getQty100() != null) {
            val += cashTransaction.getQty100() * 100;
        }
        if (cashTransaction.getQty1000() != null) {
            val += cashTransaction.getQty1000() * 1000;
        }
        if (cashTransaction.getQty10000() != null) {
            val += cashTransaction.getQty10000() * 10000;
        }

        if (cashTransaction.getQty2() != null) {
            val += cashTransaction.getQty2() * 2;
        }
        if (cashTransaction.getQty20() != null) {
            val += cashTransaction.getQty20() * 20;
        }
        if (cashTransaction.getQty200() != null) {
            val += cashTransaction.getQty200() * 200;
        }
        if (cashTransaction.getQty2000() != null) {
            val += cashTransaction.getQty2000() * 2000;
        }

        if (cashTransaction.getQty5() != null) {
            val += cashTransaction.getQty5() * 5;
        }
        if (cashTransaction.getQty50() != null) {
            val += cashTransaction.getQty50() * 50;
        }
        if (cashTransaction.getQty500() != null) {
            val += cashTransaction.getQty500() * 500;
        }
        if (cashTransaction.getQty5000() != null) {
            val += cashTransaction.getQty5000() * 5000;
        }

        return val;
    }

    public Drawer getDrawer(WebUser webUser) {
        if (webUser == null) {
            return null;
        }

        Drawer drw = webUser.getDrawer();

        if (drw == null) {
            drw = new Drawer();
            drw.setCreatedAt(new Date());
            drw.setName(webUser.getWebUserPerson().getName() + " 's drawer");
            drw.getWebUsers().add(webUser);
            getDrawerFacade().create(drw);

            webUser.setDrawer(drw);
            getWebUserFacade().edit(webUser);
        }

        return drw;
    }

    public void addToTransactionHistory(CashTransaction cashTransaction, Drawer drawer) {
        if (cashTransaction == null) {
            return;
        }

        CashTransactionHistory sh;
        String sql;
        sql = "Select sh from CashTransactionHistory sh where sh.cashTransaction=:pbi";
        Map m = new HashMap();
        m.put("pbi", cashTransaction);
        sh = getCashTransactionHistoryFacade().findFirstBySQL(sql, m);
        if (sh == null) {
            sh = new CashTransactionHistory();
        } else {
            return;
        }

        sh.setFromDate(new Date());
        sh.setCashTransaction(cashTransaction);
        sh.setHxDate(Calendar.getInstance().get(Calendar.DATE));
        sh.setHxMonth(Calendar.getInstance().get(Calendar.MONTH));
        sh.setHxWeek(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
        sh.setHxYear(Calendar.getInstance().get(Calendar.YEAR));

        sh.setCashAt(new Date());
        sh.setCreatedAt(new Date());

        Drawer fetchedDrw = getDrawerFacade().find(drawer.getId());

        sh.setCashBallance(fetchedDrw.getRunningBallance());
        sh.setCreditCardBallance(fetchedDrw.getCreditCardBallance());
        sh.setChequeBallance(fetchedDrw.getChequeBallance());
        sh.setSlipBallance(fetchedDrw.getSlipBallance());
        sh.setCreditBallance(fetchedDrw.getCreditBallance());
        sh.setIouBallance(fetchedDrw.getIouBallance());
        sh.setShortBallance(fetchedDrw.getShortBallance());
        sh.setCashierShortBallance(fetchedDrw.getCashierShortBallance());
        sh.setCashierExcessBallance(fetchedDrw.getCashierExcessBallance());

        getCashTransactionHistoryFacade().create(sh);

        cashTransaction.setCashTransactionHistory(sh);
        getCashTransactionFacade().edit(cashTransaction);

    }

    public void deductFromTransactionHistory(CashTransaction cashTransaction, Drawer drawer) {
        if (cashTransaction == null) {
            return;
        }

        CashTransactionHistory sh;
        String sql;
        sql = "Select sh from CashTransactionHistory sh where sh.cashTransaction=:pbi";
        Map m = new HashMap();
        m.put("pbi", cashTransaction);
        sh = getCashTransactionHistoryFacade().findFirstBySQL(sql, m);
        if (sh == null) {
            sh = new CashTransactionHistory();
        } else {
            return;
        }

        sh.setFromDate(new Date());
        sh.setCashTransaction(cashTransaction);
        sh.setHxDate(Calendar.getInstance().get(Calendar.DATE));
        sh.setHxMonth(Calendar.getInstance().get(Calendar.MONTH));
        sh.setHxWeek(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
        sh.setHxYear(Calendar.getInstance().get(Calendar.YEAR));

        sh.setCashAt(new Date());
        sh.setCreatedAt(new Date());

        Drawer fetchedDrw = getDrawerFacade().find(drawer.getId());

        sh.setCashBallance(fetchedDrw.getRunningBallance());
        sh.setCreditCardBallance(fetchedDrw.getCreditCardBallance());
        sh.setChequeBallance(fetchedDrw.getChequeBallance());
        sh.setSlipBallance(fetchedDrw.getSlipBallance());
        sh.setCreditBallance(fetchedDrw.getCreditBallance());
        sh.setIouBallance(fetchedDrw.getIouBallance());
        sh.setShortBallance(fetchedDrw.getShortBallance());
        sh.setCashierShortBallance(0 - fetchedDrw.getCashierShortBallance());
        sh.setCashierExcessBallance(fetchedDrw.getCashierExcessBallance());

        getCashTransactionHistoryFacade().create(sh);

        cashTransaction.setCashTransactionHistory(sh);
        getCashTransactionFacade().edit(cashTransaction);

    }

    public void addDepartmentHistory(BillItem bi, Department d) {

        CashTransactionHistory sh = new CashTransactionHistory();
        sh.setBillItem(bi);
        sh.setTransactionValue(bi.getNetValue());
        sh.setBeforeBalance(d.getBalance());

        sh.setFromDate(new Date());
        sh.setHxDate(Calendar.getInstance().get(Calendar.DATE));
        sh.setHxMonth(Calendar.getInstance().get(Calendar.MONTH));
        sh.setHxWeek(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
        sh.setHxYear(Calendar.getInstance().get(Calendar.YEAR));

        sh.setCashAt(new Date());
        sh.setCreatedAt(new Date());
        getCashTransactionHistoryFacade().create(sh);

        System.out.println("bi.getItem().getDepartment().getBalance() = " + d.getBalance());
        d.setBalance(d.getBalance() + bi.getNetValue());
        System.out.println("bi.getItem().getDepartment().getBalance() = " + d.getBalance());
        getDepartmentFacade().edit(d);

    }

    public void addDepartmentHistory(BillItem bi, Department d, double transVal) {

        CashTransactionHistory sh = new CashTransactionHistory();
        sh.setBillItem(bi);
        sh.setTransactionValue(bi.getNetValue());
        sh.setBeforeBalance(d.getBalance());

        sh.setFromDate(new Date());
        sh.setHxDate(Calendar.getInstance().get(Calendar.DATE));
        sh.setHxMonth(Calendar.getInstance().get(Calendar.MONTH));
        sh.setHxWeek(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
        sh.setHxYear(Calendar.getInstance().get(Calendar.YEAR));

        sh.setCashAt(new Date());
        sh.setCreatedAt(new Date());
        getCashTransactionHistoryFacade().create(sh);

        bi.setCashTransactionHistory(sh);
        getBillItemFacade().edit(bi);

        System.out.println("bi.getItem().getDepartment().getBalance() = " + d.getBalance());
        d.setBalance(roundOff(transVal));
        System.out.println("bi.getItem().getDepartment().getBalance() = " + d.getBalance());
        getDepartmentFacade().edit(d);

    }

    public void deductDepartmentHistory(BillItem bi, Department d) {

        CashTransactionHistory sh = new CashTransactionHistory();
        sh.setBillItem(bi);
        sh.setTransactionValue(bi.getNetValue());
        sh.setBeforeBalance(d.getBalance());

        sh.setFromDate(new Date());
        sh.setHxDate(Calendar.getInstance().get(Calendar.DATE));
        sh.setHxMonth(Calendar.getInstance().get(Calendar.MONTH));
        sh.setHxWeek(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
        sh.setHxYear(Calendar.getInstance().get(Calendar.YEAR));

        sh.setCashAt(new Date());
        sh.setCreatedAt(new Date());
        getCashTransactionHistoryFacade().create(sh);

        System.out.println("bi.getItem().getDepartment().getBalance() = " + d.getBalance());
        d.setBalance(d.getBalance() - bi.getNetValue());
        System.out.println("bi.getItem().getDepartment().getBalance() = " + d.getBalance());
        getDepartmentFacade().edit(d);

    }

    @EJB
    BillFacade billFacade;

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public WebUser saveBillCashInTransaction(Bill bill, WebUser webUser) {
        if (bill == null) {
            return null;
        }

        if (webUser == null) {
            return null;
        }

        CashTransaction cashTransaction = setCashTransactionValue(new CashTransaction(), bill);
        saveCashInTransaction(cashTransaction, bill, webUser);

        bill.setCashTransaction(cashTransaction);
        getBillFacade().edit(bill);

        addToBallance(webUser.getDrawer(), cashTransaction);

        WebUser w = getWebUserFacade().find(webUser.getId());

        return w;
    }

    public WebUser saveBillCashOutTransaction(Bill bill, WebUser webUser) {
        CashTransaction cashTransaction = setCashTransactionValue(new CashTransaction(), bill);
        saveCashOutTransaction(cashTransaction, bill, webUser);

        bill.setCashTransaction(cashTransaction);
        getBillFacade().edit(bill);

        deductFromBallance(webUser.getDrawer(), cashTransaction);

        WebUser w = getWebUserFacade().find(webUser.getId());

        return w;
    }

    public boolean addToBallance(Drawer drawer, CashTransaction cashTransaction) {
        if (drawer == null) {
            return false;
        }

        if (drawer.getId() == null) {
            return false;
        }

        addToTransactionHistory(cashTransaction, drawer);

        Drawer fetchedDrw = getDrawerFacade().find(drawer.getId());

        if (cashTransaction.getCashValue() != null) {
            fetchedDrw.setRunningBallance(fetchedDrw.getRunningBallance() + Math.abs(cashTransaction.getCashValue()));
        }
        if (cashTransaction.getChequeValue() != null) {
            fetchedDrw.setChequeBallance(fetchedDrw.getChequeBallance() + Math.abs(cashTransaction.getChequeValue()));
        }
        if (cashTransaction.getCreditCardValue() != null) {
            fetchedDrw.setCreditCardBallance(fetchedDrw.getCreditCardBallance() + Math.abs(cashTransaction.getCreditCardValue()));
        }
        if (cashTransaction.getSlipValue() != null) {
            fetchedDrw.setSlipBallance(fetchedDrw.getSlipBallance() + Math.abs(cashTransaction.getSlipValue()));
        }
        if (cashTransaction.getCreditValue() != null) {
            fetchedDrw.setCreditBallance(fetchedDrw.getCreditBallance() + Math.abs(cashTransaction.getCreditValue()));
        }
        if (cashTransaction.getIouValue() != null) {
            fetchedDrw.setIouBallance(fetchedDrw.getIouBallance() + Math.abs(cashTransaction.getIouValue()));
        }
        if (cashTransaction.getShortValue() != null) {
            fetchedDrw.setShortBallance(fetchedDrw.getShortBallance() + cashTransaction.getShortValue());
        }
//        if (cashTransaction.getCashierShortValue() != null) {
//            fetchedDrw.setCashierShortBallance(fetchedDrw.getCashierShortBallance() +  Math.abs(cashTransaction.getCashierShortValue()));
//        }
//        if (cashTransaction.getCashierExcessValue() != null) {
//            fetchedDrw.setCashierExcessBallance(fetchedDrw.getCashierExcessBallance() +  Math.abs(cashTransaction.getCashierExcessValue()));
//        }

        getDrawerFacade().edit(fetchedDrw);

        return true;
    }

    public boolean addToBallanceModifid(Drawer drawer, CashTransaction cashTransaction) {
        if (drawer == null) {
            return false;
        }

        if (drawer.getId() == null) {
            return false;
        }

        addToTransactionHistory(cashTransaction, drawer);

        Drawer fetchedDrw = getDrawerFacade().find(drawer.getId());

        if (cashTransaction.getCashValue() != null) {
            fetchedDrw.setRunningBallance(fetchedDrw.getRunningBallance() + cashTransaction.getCashValue());
        }
        if (cashTransaction.getChequeValue() != null) {
            fetchedDrw.setChequeBallance(fetchedDrw.getChequeBallance() + cashTransaction.getChequeValue());
        }
        if (cashTransaction.getCreditCardValue() != null) {
            fetchedDrw.setCreditCardBallance(fetchedDrw.getCreditCardBallance() + cashTransaction.getCreditCardValue());
        }
        if (cashTransaction.getSlipValue() != null) {
            fetchedDrw.setSlipBallance(fetchedDrw.getSlipBallance() + cashTransaction.getSlipValue());
        }
        if (cashTransaction.getCreditValue() != null) {
            fetchedDrw.setCreditBallance(fetchedDrw.getCreditBallance() + cashTransaction.getCreditValue());
        }
        if (cashTransaction.getIouValue() != null) {
            fetchedDrw.setIouBallance(fetchedDrw.getIouBallance() + cashTransaction.getIouValue());
        }
        if (cashTransaction.getShortValue() != null) {
            fetchedDrw.setShortBallance(fetchedDrw.getShortBallance() + cashTransaction.getShortValue());
//            fetchedDrw.setShortBallance(fetchedDrw.getShortBallance() + cashTransaction.getShortValue());
        }
//        if (cashTransaction.getCashierShortValue() != null) {
//            fetchedDrw.setCashierShortBallance(fetchedDrw.getCashierShortBallance() +  Math.abs(cashTransaction.getCashierShortValue()));
//        }
//        if (cashTransaction.getCashierExcessValue() != null) {
//            fetchedDrw.setCashierExcessBallance(fetchedDrw.getCashierExcessBallance() +  Math.abs(cashTransaction.getCashierExcessValue()));
//        }

        getDrawerFacade().edit(fetchedDrw);

        return true;
    }

    public boolean deductFromBallance(Drawer drawer, CashTransaction cashTransaction) {
        if (drawer == null) {
            return false;
        }

        if (drawer.getId() == null) {
            return false;
        }

        deductFromTransactionHistory(cashTransaction, drawer);

        Drawer fetchedDrw = getDrawerFacade().find(drawer.getId());

        if (cashTransaction.getCashValue() != null) {
            fetchedDrw.setRunningBallance(fetchedDrw.getRunningBallance() - Math.abs(cashTransaction.getCashValue()));
        }
        if (cashTransaction.getChequeValue() != null) {
            fetchedDrw.setChequeBallance(fetchedDrw.getChequeBallance() - Math.abs(cashTransaction.getChequeValue()));
        }
        if (cashTransaction.getCreditCardValue() != null) {
            fetchedDrw.setCreditCardBallance(fetchedDrw.getCreditCardBallance() - Math.abs(cashTransaction.getCreditCardValue()));
        }
        if (cashTransaction.getSlipValue() != null) {
            fetchedDrw.setSlipBallance(fetchedDrw.getSlipBallance() - Math.abs(cashTransaction.getSlipValue()));
        }
        if (cashTransaction.getCreditValue() != null) {
            fetchedDrw.setCreditBallance(fetchedDrw.getCreditBallance() - Math.abs(cashTransaction.getCreditValue()));
        }
        if (cashTransaction.getIouValue() != null) {
            fetchedDrw.setIouBallance(fetchedDrw.getIouBallance() - Math.abs(cashTransaction.getIouValue()));
        }
        if (cashTransaction.getShortValue() != null) {
            fetchedDrw.setShortBallance(fetchedDrw.getShortBallance() + cashTransaction.getShortValue());
        }
//        if (cashTransaction.getCashierShortValue() != null) {
//            fetchedDrw.setCashierShortBallance(fetchedDrw.getCashierShortBallance() - Math.abs(cashTransaction.getCashierShortValue()));
//        }
//        if (cashTransaction.getCashierExcessValue() != null) {
//            fetchedDrw.setCashierExcessBallance(fetchedDrw.getCashierExcessBallance() - Math.abs(cashTransaction.getCashierExcessValue()));
//        }

        getDrawerFacade().edit(fetchedDrw);

        return true;
    }

    public boolean resetBallance(Drawer drawer, CashTransaction cashTransaction) {
        if (drawer == null) {
            return false;
        }

        if (drawer.getId() == null) {
            return false;
        }

        //System.err.println("11 " + drawer);
        //System.err.println("22 " + cashTransaction);
        addToTransactionHistory(cashTransaction, drawer);

        //System.err.println("13 " + drawer);
        //System.err.println("24 " + cashTransaction);
        Drawer fetchedDrw = getDrawerFacade().find(drawer.getId());

        fetchedDrw.setRunningBallance(cashTransaction.getCashValue());
        fetchedDrw.setChequeBallance(cashTransaction.getChequeValue());
        fetchedDrw.setCreditCardBallance(cashTransaction.getCreditCardValue());
        fetchedDrw.setSlipBallance(cashTransaction.getSlipValue());

        //System.err.println("15 " + drawer);
        //System.err.println("26 " + cashTransaction);
        getDrawerFacade().edit(fetchedDrw);

        return true;
    }

    private double roundOff(double d) {
        DecimalFormat newFormat = new DecimalFormat("#.##");
        try {
            return Double.valueOf(newFormat.format(d));
        } catch (Exception e) {
            return 0;
        }
    }

    public CashTransactionFacade getCashTransactionFacade() {
        return cashTransactionFacade;
    }

    public void setCashTransactionFacade(CashTransactionFacade cashTransactionFacade) {
        this.cashTransactionFacade = cashTransactionFacade;
    }

    public DrawerFacade getDrawerFacade() {
        return drawerFacade;
    }

    public void setDrawerFacade(DrawerFacade drawerFacade) {
        this.drawerFacade = drawerFacade;
    }

    public WebUserFacade getWebUserFacade() {
        return webUserFacade;
    }

    public void setWebUserFacade(WebUserFacade webUserFacade) {
        this.webUserFacade = webUserFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }
}
