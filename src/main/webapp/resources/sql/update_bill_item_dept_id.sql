select b.`INSID`,cth.`ID`,b.`CREATEDAT`,bi.`DEPTID`,d.`NAME` from cashtransactionhistory cth 
join billitem bi on cth.`BILLITEM_ID`=bi.id  
join bill b on bi.`BILL_ID`=b.`ID`
join item i on bi.`ITEM_ID`=i.`ID`
join department d on i.`DEPARTMENT_ID`=d.`ID`
WHERE b.`BILLTYPE`='CashIn';

-- UPDATE cashtransactionhistory cth 
-- join billitem bi on cth.`BILLITEM_ID`=bi.id  
-- join bill b on bi.`BILL_ID`=b.`ID`
-- join item i on bi.`ITEM_ID`=i.`ID`
-- join department d on i.`DEPARTMENT_ID`=d.`ID`
-- set bi.`DEPTID`=d.`NAME`
-- WHERE b.`BILLTYPE`='CashIn';

