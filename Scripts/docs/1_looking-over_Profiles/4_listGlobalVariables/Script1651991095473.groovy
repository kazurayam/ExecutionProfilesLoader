// Test Cases/main/1_listing/4_listGlobalVariable

List<String> gvipList = CustomKeywords.'com.kazurayam.ks.globalvariable.LookOverExecutionProfilesKeyword.listGlobalVariables'('ENV.*')

for (String gvip in gvipList) {
	println gvip
}