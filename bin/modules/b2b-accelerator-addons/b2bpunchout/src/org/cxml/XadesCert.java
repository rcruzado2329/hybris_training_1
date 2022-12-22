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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "xadesCertDigest",
    "xadesIssuerSerial"
})
@XmlRootElement(name = "xades:Cert")
public class XadesCert {

    @XmlAttribute(name = "URI")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected String uri;
    @XmlElement(name = "xades:CertDigest", required = true)
    protected XadesCertDigest xadesCertDigest;
    @XmlElement(name = "xades:IssuerSerial", required = true)
    protected XadesIssuerSerial xadesIssuerSerial;

    /**
     * Gets the value of the uri property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getURI() {
        return uri;
    }

    /**
     * Sets the value of the uri property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setURI(String value) {
        this.uri = value;
    }

    /**
     * Gets the value of the xadesCertDigest property.
     * 
     * @return
     *     possible object is
     *     {@link XadesCertDigest }
     *     
     */
    public XadesCertDigest getXadesCertDigest() {
        return xadesCertDigest;
    }

    /**
     * Sets the value of the xadesCertDigest property.
     * 
     * @param value
     *     allowed object is
     *     {@link XadesCertDigest }
     *     
     */
    public void setXadesCertDigest(XadesCertDigest value) {
        this.xadesCertDigest = value;
    }

    /**
     * Gets the value of the xadesIssuerSerial property.
     * 
     * @return
     *     possible object is
     *     {@link XadesIssuerSerial }
     *     
     */
    public XadesIssuerSerial getXadesIssuerSerial() {
        return xadesIssuerSerial;
    }

    /**
     * Sets the value of the xadesIssuerSerial property.
     * 
     * @param value
     *     allowed object is
     *     {@link XadesIssuerSerial }
     *     
     */
    public void setXadesIssuerSerial(XadesIssuerSerial value) {
        this.xadesIssuerSerial = value;
    }

}
