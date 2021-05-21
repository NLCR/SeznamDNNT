/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.indexer.models;

import java.util.Date;
import java.util.List;
import org.apache.solr.client.solrj.beans.Field;

/**
 *
 * @author alberto
 */
public class Zadost {
  
  @Field
  String id;
  
  @Field
  List<String> identifiers;
  
  @Field
  String typ;
  
  @Field
  String user;
  
  @Field
  String state;
  
  @Field
  String new_stav;
  
  @Field
  String poznamka;
  
  @Field
  String pozadavek;
  
  @Field
  Date datum_zadani;
  
  @Field
  Date datum_vyrizeni;
  
  @Field
  String formular;
}
