package glbl.sampledomain

import com.kazurayam.ks.globalvariable.xml.GlobalVariableEntity as GVE
import com.kazurayam.ks.globalvariable.xml.GlobalVariableEntity
import glbl.sampledomainconstruct.SerializableToGlobalVariableEntity

import groovy.json.JsonOutput

enum IncludeURLs implements SerializableToGlobalVariableEntity {

	AllURLs([]),
	Login(["login.html"]),
	Top(["top.html"]);

	private final List<String> urls
	List<String> getUrls() {
		return urls
	}
	String getUrlsAsJson() {
		return JsonOutput.toJson(getUrls())
	}
	IncludeURLs(List<String> urls) {
		this.urls = urls
	}
	GVE toGlobalVariableEntity() {
		GVE gve = new GVE()
		gve.description("")
		gve.initValue(this.getUrlsAsJson())
		gve.name("INCLUDE_URLS")
		return gve
	}
}
