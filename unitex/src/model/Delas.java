/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

/**
 *
 * @author rojo
 */
public class Delas{
    private  String pOS;
    private  String lemma;
    private  String fSTCode;
    private  String simSem;
    private  String comment;
    private  String lemmaInv;
    private  String wn_sinSet;
    private  int lemmaId;
    private  String dicFile;
    private  int dicId;

    public Delas(String pOS, String lemma, String fSTCode, String simSem, String comment, String lemmaInv, String wn_sinSet, int lemmaId, String dicFile, int dicId) {
        
        this.pOS = pOS;
        this.lemma = lemma;
        this.fSTCode = fSTCode;
        this.simSem = simSem;
        this.comment = comment;
        this.lemmaInv = lemmaInv;
        this.wn_sinSet = wn_sinSet;
        this.lemmaId = lemmaId;
        this.dicFile = dicFile;
        this.dicId = dicId;
    }

    public Delas() {

    }

    /**
     * @return the pOS
     */
    public String getpOS() {
        return pOS;
    }

    /**
     * @return the lemma
     */
    public String getLemma() {
        return lemma;
    }

    /**
     * @return the fSTCode
     */
    public String getfSTCode() {
        return fSTCode;
    }

    /**
     * @return the simSem
     */
    public String getSimSem() {
        return simSem;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @return the lemmaInv
     */
    public String getLemmaInv() {
        return lemmaInv;
    }

    /**
     * @return the wn_sinSet
     */
    public String getWn_sinSet() {
        return wn_sinSet;
    }

    /**
     * @return the lemmaId
     */
    public int getLemmaId() {
        return lemmaId;
    }

    /**
     * @return the dicFile
     */
    public String getDicFile() {
        return dicFile;
    }

    /**
     * @return the dicId
     */
    public int getDicId() {
        return dicId;
    }
    
}