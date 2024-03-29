package com.kazurayam.ks.globalvariable

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.kms.katalon.core.annotation.Keyword

import internal.GlobalVariable

/**
 * This looks at the GlobalVariables (name=value pairs) in action;
 * provides a view in JSON text.
 */
public class LookAtGlobalVariablesKeyword {

	private final GlobalVariableAnnex XGV

	LookAtGlobalVariablesKeyword() {
		XGV = GlobalVariableAnnex.newInstance()
	}

	@Keyword
	String additionalGVEntitiesAsString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create()
		gson.toJson(XGV.additionalGVEntitiesAsMap())
	}


	/**
	 * pretty-printed JSON text of Map returned by mapOfGlobalVariables()
	 *
	 * @return
	 */
	@Keyword
	String allGVEntitiesAsString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create()
		gson.toJson(XGV.allGVEntitiesAsMap())
	}


	@Keyword
	String staticGVEntitiesAsString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create()
		gson.toJson(XGV.staticGVEntitiesAsMap())
	}


	@Keyword
	String toJson() {
		return XGV.toJson()
	}

	@Keyword
	String toJson(boolean prettyPrint) {
		return XGV.toJson(prettyPrint)
	}
}
