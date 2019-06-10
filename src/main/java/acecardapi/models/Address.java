/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.models;


import io.reactiverse.pgclient.Tuple;

import java.util.UUID;

public class Address {

  private UUID id;
  private String address;
  private int address_number;
  private String address_annex;
  private String city;
  private String postalcode;
  private String country;


  public Address(String address, int address_number, String address_annex, String city, String postalcode) {
    this.address = address;
    this.address_number = address_number;
    this.address_annex = address_annex;
    this.city = city;
    this.postalcode = postalcode;
    this.country = "NLD";
    this.id = UUID.randomUUID();
  }

  public Address(UUID id, String address, int address_number, String address_annex, String city, String postalcode, String country) {
    this.id = id;
    this.address = address;
    this.address_number = address_number;
    this.address_annex = address_annex;
    this.city = city;
    this.postalcode = postalcode;
    this.country = country;
  }

  public Tuple toTupleNoId() {
    return Tuple.of(address, address_number, address_annex, city, postalcode, country);
  }

  public Tuple toTupleWithId() {
    return Tuple.of(id, address, address_number, address_annex, city, postalcode, country);
  }

  public UUID getId() {
    return id;
  }

  public Address() {
    this.id = UUID.randomUUID();
  }

  public String getAddress() {
    return address;
  }

  public int getAddress_number() {
    return address_number;
  }

  public String getAddress_annex() {
    return address_annex;
  }

  public String getCity() {
    return city;
  }

  public String getPostalcode() {
    return postalcode;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public void setAddress_number(int address_number) {
    this.address_number = address_number;
  }

  public void setAddress_annex(String address_annex) {
    this.address_annex = address_annex;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public void setPostalcode(String postalcode) {
    this.postalcode = postalcode;
  }

}

