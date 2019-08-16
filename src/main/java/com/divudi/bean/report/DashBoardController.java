/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.report;

import com.divudi.data.Sex;
import com.divudi.entity.Consultant;
import org.json.JSONArray;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.TemporalType;

/**
 *
 * @author archmage
 */
@Named(value = "dashBoardController")
@SessionScoped
public class DashBoardController implements Serializable {

    /**
     * Creates a new instance of DashBoardController
     */
    public DashBoardController() {
    }
    
    public String drawPiechartStaffCount() {
        Date startTime = new Date();
        JSONArray mainJSONArray = new JSONArray();
        JSONArray subArray = new JSONArray();
        subArray.put(0, "Gender");
        subArray.put(1, "Count");
        mainJSONArray.put(subArray);

        String sql;
        Map m = new HashMap();

//        sql = "select s.person.sex,count(s.person.sex) "
//                + " from Staff s "
//                + " where s.retired=false "
//                + " and type(s)!=:class "
//                + " and s.person.sex is not null "
//                + " and (s.dateLeft is null or s.dateLeft>:d) "
//                + " group by s.person.sex "
//                + " order by s.person.sex ";
//
//        m.put("d", new Date());
//        m.put("class", Consultant.class);
//
//        List<Object[]> objects = getStaffFacade().findAggregates(sql, m, TemporalType.DATE);
//        for (Object[] ob : objects) {
//            Sex s = (Sex) ob[0];
////            System.out.println("s = " + s);
//            long d = (long) ob[1];
////            System.out.println("d = " + d);
//            subArray = new JSONArray();
//            subArray.put(0, s);
//            subArray.put(1, d);
//            mainJSONArray.put(subArray);
//        }

        return mainJSONArray.toString();

    }
    
}
