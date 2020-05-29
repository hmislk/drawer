-- select * from bill WHERE `INSID`='RHSUMMARYOUT/4373' or `INSID`='RHSUMMARYOUT/4374';


-- SELECT * FROM webuser w join person p on w.`WEBUSERPERSON_ID`=p.`ID` WHERE w.id=13633379;
-- SELECT * FROM staff s join person p on s.`PERSON_ID`=p.`ID` WHERE s.id=13633378;

SELECT * FROM 
webuser w 
join person p on w.`WEBUSERPERSON_ID`=p.`ID` 
join staff s on w.`STAFF_ID`=s.`ID` 
WHERE s.`CODE`='100587';

select * from bill WHERE `INSID`='RHSUMMARYOUT/4373' or `INSID`='RHSUMMARYOUT/4374';
-- 13650520
