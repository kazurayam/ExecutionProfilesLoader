package com.kazurayam.ks.globalvariable

import static java.lang.reflect.Modifier.isPublic
import static java.lang.reflect.Modifier.isStatic
import static java.lang.reflect.Modifier.isTransient

import java.lang.reflect.Field
import java.util.stream.Collectors

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

import com.kms.katalon.core.annotation.Keyword

import internal.GlobalVariable

import java.util.regex.Pattern
import java.util.regex.Matcher

/**
 * 
 * The GlobalVariableAnnex modifies the behavior of the GlobalVariable class dynamically using Groovy's Meta-programming technique.
 *
 * The GlobalVariableAnnex object maintain a object of type Map<String, Object> 
 * which looks just similar to static instance of "internal.GlobalVariable".
 * 
 * A caller script can add a new GlobalVariableEntity with any name and value into the ExpandoGlobalVariable object.
 * 
 * For example, let's assume that `GlobalVariable.FOO` has been added by
 * ExecutionProfilesLoader class. When `GlobalVaraible.FOO` is accessed, the GlobalVariable object will
 * - first, try to look up "FOO" in the Map inside the GlobalVariableAnnex object
 * - secondly, try to lookup "FOO" in the original internal.GlobalVariable object.
 * 
 * GlobalVariableAnnex implements methods to add/modify/delete GVEntries.
 * GlobalVariableAnnex provides method to retrieve all GVEntries' name and their current values runtime.
 * 
 * @author kazurayam
 */
public final class GlobalVariableAnnex {

	// Singleton design pattern of GOF
	private static GlobalVariableAnnex INSTANCE

	// https://docs.groovy-lang.org/latest/html/documentation/core-metaprogramming.html#_properties
	private static final Map<String, Object> additionalGVEntities = Collections.synchronizedMap([:])

	private GlobalVariableAnnex() {}

	public static GlobalVariableAnnex getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new GlobalVariableAnnex()
		}
		return INSTANCE
	}

	/**
	 * "GVEntity" means "an entry of the GlobalVariable object" for short.
	 * 
	 * insert a public static property of type java.lang.Object
	 * into the internal.GlobalVarialbe runtime.
	 *
	 * e.g, GVH.addGVEntity('my_new_variable', 'foo') makes
	 * 1) internal.GlobalVariable.my_new_variable to be present and to have value 'foo'
	 * 2) internal.GlobalVariable.getMy_new_variale() to return 'foo'
	 * 3) internal.GlobalVariable.setMy_new_variable('bar') to set 'bar' as the value
	 *
	 * @param name
	 * @param value
	 */
	int addGVEntity(String name, Object value) {

		// check if the "name" is declared as a property of internal.GlobalVariable object statically or not
		if (staticGVEntitiesKeySet().contains(name)) {
			// Yes, the internal.GlobalVariable object has "name" already.
			// No need to add the name. Just update the value
			GlobalVariable[name] = value

			return 0

		} else {
			// No, the "name" is not present. Let's add a new property using Groovy's ExpandoMetaClass

			// the characters in the name must be valid as a Groovy variable name
			validateGVEntityName(name)

			// obtain the ExpandoMetaClass of the internal.GlobalVariable class
			MetaClass mc = GlobalVariable.metaClass

			// register the Getter method for the name
			String getterName = getGetterName(name)
			mc.'static'."${getterName}" = { -> return additionalGVEntities[name] }

			// register the Setter method for the name
			String setterName = getSetterName(name)
			mc.'static'."${setterName}" = { newValue ->
				additionalGVEntities[name] = newValue
			}

			// store the value into the storage
			additionalGVEntities.put(name, value)

			return 1
		}
	}


	private String getGetterName(String name) {
		return 'get' + getAccessorName(name)
	}

	private String getSetterName(String name) {
		return 'set' + getAccessorName(name)
	}


	private String getAccessorName(String name) {
		return ((CharSequence)name).capitalize()
	}


	/**
	 * check if the given string is valid as a name of GlobalVaraible.
	 * 1) should not starts with digits [0-9]
	 * 2) should not starts with punctuations [$_]
	 * 3) if the 1st character is upper case, then the second character MUST NOT be lower case
	 *
	 * @param name
	 * @throws IllegalArgumentException
	 */
	private static Pattern PTTN_LEADING_DIGITS = Pattern.compile('^[0-9]')
	private static Pattern PTTN_LEADING_PUNCTUATIONS = Pattern.compile('^[$_]')
	private static Pattern PTTN_UPPER_LOWER    = Pattern.compile('^[A-Z][a-z]')

	void validateGVEntityName(String name) {
		Objects.requireNonNull(name)
		if (PTTN_LEADING_DIGITS.matcher(name).find()) {
			throw new IllegalArgumentException("name=${name} must not start with digits")
		}
		if (PTTN_LEADING_PUNCTUATIONS.matcher(name).find()) {
			throw new IllegalArgumentException("name=${name} must not start with punctuations")
		}
		if (PTTN_UPPER_LOWER.matcher(name).find()) {
			throw new IllegalArgumentException("name=${name} must not start with a uppercase letter followed by a lower case letter")
		}
	}

	/**
	 *
	 * @param entries
	 * @return the number of entries which have been dynamically added as GlobalVariable
	 */
	int addGVEntities(Map<String, Object> entities) {
		int count = 0
		entities.each { entity ->
			count += addGVEntity(entity.key, entity.value)
		}
		return count
	}

	/**
	 * If GlobaleVariable.name is present, set the value into it.
	 * Otherwise create GlobalVariable.name dynamically and set the value into it.
	 *
	 * @param name
	 * @param value
	 */
	void ensureGVEntity(String name, Object value) {
		if (isGVEntityPresent(name)) {
			addGVEntity(name, value)   // will overwrite the previous value
		} else {
			addGVEntity(name, value)
		}
	}

	/**
	 * remove name=value pairs added by ExpandoGlobalVariable#addGVEntity(String name, Object value) method calls.
	 * The names that are NOT present in the original internal.GlobalVariable object will be removed.
	 * The names that are present in the original internal.GlobalVariable object will be left untouched; the values will be also unchanged.
	 */
	// the "clear" method does not workis not implemented.
	// we can add methods to a Metaclass, but we can not removed the added methods.
	// see https://stackoverflow.com/questions/2745615/remove-single-metaclass-method
	void clear() {
		additionalGVEntities.clear()
	}


	/**
	 * @return true if GlobalVarialbe.name is defined either in 2 places
	 * 1. statically predefined in the Execution Profile
	 * 2. dynamically added by ExpandoGlobalVariable.addGlobalVariable(name, value) call
	 */
	boolean isGVEntityPresent(String name) {
		return allGVEntitiesKeySet().contains(name)
	}

	Object getGVEntityValue(String name) {
		if (additionalGVEntitiesKeySet().contains(name)) {
			return additionalGVEntities[name]
		} else if (staticGVEntitiesKeySet().contains(name)) {
			return GlobalVariable[name]
		} else {
			return null
		}
	}




	// ------------------------------------------------------------------------


	/**
	 * inspect the 'internal.GlobalVariable' object to find the GlobalVariables contained,
	 * and return the list of the names.
	 *
	 * The list will include only the fields declared with modifiers `public static` in the internal.GlobalVariable class source.
	 * You can find the source at
	 * 
	 *     projectDir/Libs/internal/GlobalVariable.groovy
	 *  
	 * The list will NOT include the GVentities properties dynamically added by calling
	 * 
	 *    ExpandoGlobalVariable XGV = ExpandoGlobalVariable.newInstance()
	 *    XGV.addGVEntity(String name, Object value)
	 *
	 */
	SortedSet<String> staticGVEntitiesKeySet() {
		// getDeclaredFields() return fields both of static and additional
		Set<Field> fields = GlobalVariable.class.getDeclaredFields() as Set<Field>
		Set<String> result = fields.stream()
				.filter { f ->
					isPublic(f.modifiers) &&
							isStatic(f.modifiers) &&
							! isTransient(f.modifiers)
				}
				.map { f -> f.getName() }
				.collect(Collectors.toSet())
		SortedSet<String> sorted = new TreeSet()
		sorted.addAll(result)
		return sorted
	}


	Map<String, Object> staticGVEntitiesAsMap() {
		SortedSet<String> names = this.staticGVEntitiesKeySet()
		Map<String, Object> map = new HashMap<String, Object>()
		for (name in names) {
			map.put(name, GlobalVariable[name])
		}
		return map
	}



	/**
	 * inspect the 'internal.GlobalVariable' object to find the GlobalVariables contained,
	 * return the list of their names.
	 *
	 * The list will include only GlobalVariables that are added into the ExpandoGlobalVariable by calling
	 *    ExpandoGlobalVariable.addProperty(name,value) or equivalently
	 *    ExecutionProfilesLoader.loadProfile(name).
	 *
	 * The list will NOT include the GlobalVariables statically defined in the Execution Profile which was applied
	 *    to the Test Case run.
	 */
	SortedSet<String> additionalGVEntitiesKeySet() {
		SortedSet<String> sorted = new TreeSet<String>()
		sorted.addAll(additionalGVEntities.keySet())
		return sorted
	}

	Map<String, Object> additionalGVEntitiesAsMap() {
		SortedSet<String> names = this.additionalGVEntitiesKeySet()
		Map<String, Object> map = new HashMap<String, Object>()
		for (name in names) {
			map.put(name, GlobalVariable[name])
		}
		return map
	}


	/**
	 * inspect the 'internal.GlobalVariable' object to find the GlobalVariables contained,
	 * return the list of their names.
	 *
	 * The list will include 2 types of GlobalVariables.
	 *
	 * 1. GlobalVariables statically defined in the Execution Profile which was applied
	 *    to the Test Case run. In this category there are 2 types of GlobalVariable:
	 *
	 *    a) GlobalVariables defined in the Profile actually applied to the test case execution.
	 *    b) GlobalVariables defined in the "default" Profile.
	 *    c) GlobalVariables defined in any of Profiles which are NOT applied to the test case execution.
	 *
	 *    The a) and b) type of GlobalVariable will have some meaningful value.
	 *    But the c) type will always have 'null' value.
	 *
	 * 2. GlobalVariables dynamically added into the ExpandoGlobalVariable by calling
	 *    ExpandoGlobalVariable.addProperty(name,value) or equivalently
	 *    ExecutionProfilesLoader.loadProfile(name)
	 */
	SortedSet<String> allGVEntitiesKeySet() {
		SortedSet<String> sorted = new TreeSet()
		sorted.addAll(staticGVEntitiesKeySet())
		sorted.addAll(additionalGVEntitiesKeySet())
		return sorted
	}

	/**
	 * transform the GlobalVariable <name, vale> pairs as a Map.
	 */
	Map<String, Object> allGVEntitiesAsMap() {
		SortedSet<String> names = this.allGVEntitiesKeySet()
		Map<String, Object> map = new HashMap<String, Object>()
		for (name in names) {
			map.put(name, GlobalVariable[name])
		}
		return map
	}


	/**
	 * make String representation of the ExpandGlobalVariable instance in JSON format 
	 */
	String toJson() {
		Set<String> nameList = this.allGVEntitiesKeySet()
		SortedMap buffer = new TreeMap<String, Object>()
		for (name in nameList) {
			if (isGVEntityPresent(name)) {
				buffer.put(name, getGVEntityValue(name))
			}
		}
		GsonBuilder gb = new GsonBuilder()
		gb.disableHtmlEscaping()
		Gson gson = gb.create()
		StringWriter sw = new StringWriter()
		sw.write(gson.toJson(buffer))
		sw.flush()
		return sw.toString()
	}

	/**
	 * @param prettyPrint true to make pretty-print with NEWLINES and indents
	 */
	String toJson(boolean prettyPrint) {
		if (prettyPrint) {
			return JsonUtil.prettyPrint(toJson());
		} else {
			return toJson();
		}
	}

}