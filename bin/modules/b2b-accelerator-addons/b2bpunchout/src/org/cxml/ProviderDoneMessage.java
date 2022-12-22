/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.05.12 at 07:19:30 PM EDT 
//



package org.cxml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "originatorCookie",
    "returnData"
})
@XmlRootElement(name = "ProviderDoneMessage")
public class ProviderDoneMessage {

    @XmlElement(name = "OriginatorCookie", required = true)
    protected String originatorCookie;
    @XmlElement(name = "ReturnData")
    protected List<ReturnData> returnData;

    /**
     * Gets the value of the originatorCookie property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOriginatorCookie() {
        return originatorCookie;
    }

    /**
     * Sets the value of the originatorCookie property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOriginatorCookie(String value) {
        this.originatorCookie = value;
    }

    /**
     * Gets the value of the returnData property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the returnData property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getReturnData().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ReturnData }
     * 
     * 
     */
    public List<ReturnData> getReturnData() {
        if (returnData == null) {
            returnData = new ArrayList<ReturnData>();
        }
        return this.returnData;
    }

}
