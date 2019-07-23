/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data;

/**
 *
 * @author buddhika
 */
public enum PaymentMethod {

    Cash,
    Credit,
    Agent,
    Card,
    Cheque,
    Slip,
    IOU;

    public String getLabel() {
        switch (this) {
            case Cash:
                return "Cash";
            case Credit:
                return "Credit";
            case Card:
                return "Credit Card";
            case Agent:
                return "Agent";
            case Cheque:
                return "Cheque";
            case Slip:
                return "Slip";
            case IOU:
                return "IOU";
        }
        return "";
    }

}
