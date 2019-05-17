import java.nio.file.Path
import java.nio.file.Paths

import static javax.json.JsonValue.ValueType.*
import javax.json.Json
import javax.json.JsonArray
import javax.json.JsonNumber
import javax.json.JsonReader
import javax.json.JsonObject
import javax.json.JsonString
import javax.json.JsonStructure
import javax.json.JsonValue
import javax.json.JsonWriter
import javax.json.JsonWriterFactory
import javax.json.stream.JsonGenerator

import org.leadpony.justify.api.JsonSchema
import org.leadpony.justify.api.JsonValidationService
import org.leadpony.justify.api.ProblemHandler
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.configuration.RunConfiguration

// from http://kagamihoge.hatenablog.com/entry/2014/06/10/223338
class MyJsonNavigator {
	
	/**
	 * convert a JsonValue object into String myself using javax.json API
	 */
	public static String navigate(JsonValue tree) {
		StringBuilder sb = new StringBuilder()
		this.navigateTree(tree, null, sb)
		return sb.toString()
	}
	
	private static void navigateTree(JsonValue tree, String key, StringBuilder sb) {
		if (key != null) {
			sb.append("Key ${key}: ") 
		}
		switch(tree.getValueType()) {
			case OBJECT:
				sb.append("OBJECT\n")
				JsonObject object = (JsonObject)tree
				for (String name: object.keySet()) {
					navigateTree(object.get(name), name, sb)
				}
				break
			case ARRAY:
				sb.append("ARRAY")
				sb.append("\n")
				JsonArray array = (JsonArray)tree
				for (JsonValue val : array) {
					navigateTree(val, null, sb)
				}
				break
			case STRING:
				sb.append("STRING ")
				JsonString st = (JsonString)tree
				sb.append("${st.getString()}\n")
				break
			case NUMBER:
				JsonNumber num = (JsonNumber)tree
				sb.append("NUMBER ${num.toString()}\n")
				break
			case TRUE:
			case FALSE:
			case NULL:
				sb.append("${tree.getValueType().toString()}\n")
				break
		}
	}
}

// from https://stackoverflow.com/questions/23007567/java-json-pretty-print-javax-json
class MyJsonPrinter {

	/**
	 * pretty-print an JsonValue instance using javax.json.JsonWriter
	 */
	public static String prettyPrint(JsonStructure json) {
		return jsonFormat(json, JsonGenerator.PRETTY_PRINTING);
	}
	
	public static String jsonFormat(JsonStructure json, String... options) {
		StringWriter stringWriter = new StringWriter()
		Map<String, Boolean> config = buildConfig(options)
		JsonWriterFactory writerFactory = Json.createWriterFactory(config)
		JsonWriter jsonWriter = writerFactory.createWriter(stringWriter)
		jsonWriter.write(json)
		jsonWriter.close()
		return stringWriter.toString();
	}
	
	private static Map<String, Boolean> buildConfig(String... options) {
		Map<String, Boolean> config = new HashMap<String, Boolean>();
		if (options != null) {
			for (String option : options) {
				config.put(option, true);
			}
		}
		return config;
	}
}



Path projectDir = Paths.get(RunConfiguration.getProjectDir())
Path credSchemaPath = projectDir.resolve('Include').resolve('resources').resolve('credential-schema.json')
Path credDataPath   = projectDir.resolve('Include').resolve('resources').resolve('credential.json')
Path coordDataPath  = projectDir.resolve('Include').resolve('resources').resolve('geographical-coordinates.json')

JsonValidationService service = JsonValidationService.newInstance()

// Reads the JSON schema
JsonSchema schema = service.readSchema(credSchemaPath);

// Problem handler which will print problems found.
ProblemHandler handler = service.createProblemPrinter({ s ->
	println "*** ${s}"
})

WebUI.comment("Reads the credentail.json file, validate it to be VALID, print the contents")
service.createReader(credDataPath, schema, handler). with { reader ->
	try {
		JsonValue value = reader.readValue()
		println MyJsonNavigator.navigate(value)
		println MyJsonPrinter.prettyPrint(value)
	} finally {
	    reader.close()
	}
}

WebUI.comment("Reads the geographical-coordiation.json file validate it against credential-schema.json, print the error messages")
service.createReader(coordDataPath, schema, handler). with { reader ->
	try {
		JsonValue value = reader.readValue()
		println MyJsonNavigator.navigate(value)
		println MyJsonPrinter.prettyPrint(value)
	} finally {
		reader.close()
	}
}

