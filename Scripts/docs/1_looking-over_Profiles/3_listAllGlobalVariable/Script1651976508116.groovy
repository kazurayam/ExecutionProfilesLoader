// Test Cases/main/1_listing/3_listAllGlobalVariable

List<String> gvipStrList = 
    CustomKeywords.'com.kazurayam.ks.globalvariable.LookoverExecutionProfilesKeyword.listAllGlobalVariable'()

for (String gvipStr in gvipStrList) {
	println gvipStr
}