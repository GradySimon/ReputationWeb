package com.gradysimon.reputationweb;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Entity()
@Table(name = "rw_trusts")
public class TrustRecord {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
	private int id;
    
    
	private String nameOfTruster;
	private String nameOfTrustee;
	
}
