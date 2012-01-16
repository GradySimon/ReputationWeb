package com.gradysimon.reputationweb;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import com.avaje.ebean.validation.NotNull;

@Entity()
@Table(name = "rw_trusts")
public class Trust {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
	private int id;
    
    @NotNull 
	private String trusterName;
    
    @NotNull
	private String trusteeName;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTrusterName() {
		return trusterName;
	}

	public void setTrusterName(String trusterName) {
		this.trusterName = trusterName;
	}

	public String getTrusteeName() {
		return trusteeName;
	}

	public void setTrusteeName(String trusteeName) {
		this.trusteeName = trusteeName;
	}

    
    
	
}
