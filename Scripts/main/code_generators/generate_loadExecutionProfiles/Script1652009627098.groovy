import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import com.kazurayam.ks.globalvariable.LookoverExecutionProfilesKeyword
import com.kms.katalon.core.configuration.RunConfiguration

assert RunConfiguration.getProjectDir() != null

/**
 * This code generates the source code of 'Test Cases/main/loadExecutionProfiles' script
 * based on the current contents of the Profiles directory.
 */
Path projectDir = Paths.get(RunConfiguration.getProjectDir())
Path profilesDir = projectDir.resolve('Profiles')

LookoverExecutionProfilesKeyword KW = new LookoverExecutionProfilesKeyword(profilesDir)

StringBuilder sb = new StringBuilder()
sb.append("import com.kazurayam.ks.globalvariable.ExecutionProfilesLoader\n")
sb.append("\n")
sb.append("ExecutionProfilesLoader loader = new ExecutionProfilesLoader()\n")
sb.append("\n")

int count = 0
KW.listProfiles('main_env').each { environment ->
	KW.listProfiles('main_category').each { category ->
		KW.listProfiles('main_includeSheets').each { sheets ->
			KW.listProfiles('main_includeURLs').each { urls ->
				if (count != 14) {
					sb.append("""//""")
				}
				sb.append("""loader.loadProfiles(""")
				sb.append("""'${environment}', """)
				sb.append("""'${category}', """)
				sb.append("""'${sheets}', """)
				sb.append("""'${urls}')""")
				sb.append("\n")
				count += 1
			}
		}
	}
}
sb.append("""loader.loadEntries(["LOADED_ENTRY1":"VALUE", "loaded_entry2":999])""")

println sb.toString()

println "-----------------------------"
println "${count} lines were generated\n"

Path gensrcDir = projectDir.resolve('generated-src')
Files.createDirectories(gensrcDir)
Path generated = gensrcDir.resolve("loadExecutionProfiles.groovy")
generated.toFile().text = sb.toString()
println "written ${generated}"

