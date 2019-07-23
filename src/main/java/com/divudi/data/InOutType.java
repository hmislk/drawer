/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data;

/**
 *
 * @author safrin
 */
public enum InOutType {

    in, out,none,summery,summeryin,summeryout,handover;
    
    public String getLable(){
        
        switch(this){
            case in:
                return "Cash In";
            case out:
                return "Cash Out";
            case none:
                return "None";
            case summery:
                return "Summery";
            case summeryin:
                return "Summery In";
            case summeryout:
                return "Summery Out";
            case handover:
                return "Hand Over";
        }
        return "";
    }
}
