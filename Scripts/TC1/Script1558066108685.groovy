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
import javax.json.stream.JsonLocation
import java.util.function.Consumer

import org.leadpony.justify.api.JsonSchema
import org.leadpony.justify.api.JsonValidationService
import org.leadpony.justify.api.Problem
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

/**
 * A custom problem handler which will format the found problems in a HTML table.
 *
 * For the sake of simplicity, this handler ignores the branches of the problem
 * even if there were multiple solutions for the problem.
 *
 * @see ProblemHandler
 */
public class MyProblemHandler implements ProblemHandler {

	private final StringBuilder sb
	private final PrintStream out

	public MyProblemHandler() {
		this.sb = new StringBuilder()
		this.out = System.out
		// Outputs opening elements.
		sb.append("<table>\n")
		sb.append("<thead>\n")
		sb.append("<tr>")
		sb.append("<th>Line No.</th>")
		sb.append("<th>Column No.</th>")
		sb.append("<th>Message</th>")
		sb.append("<th>Assertion Keyword</th>")
		sb.append("</tr>\n")
		sb.append("</thead>\n");
		sb.append("<tbody>\n");
	}

	public void flush() {
		// Outputs closing elements.
		sb.append("</tbody>\n")
		sb.append("</table>\n")
	}

	public String getOutput() {
		return sb.toString()
	}
	
	/**
	 * Handles the found problems.
	 * We will output a row of the table for each problem.
	 *
	 * @param problems the found problems.
	 */
	@Override
	public void handleProblems(List<Problem> problems) {
		for (Problem problem : problems) {
			JsonLocation loc = problem.getLocation()
			sb.append("<tr>")
			sb.append("<td>")
			sb.append(loc.getLineNumber())
			sb.append("</td>")
			sb.append("<td>")
			sb.append(loc.getColumnNumber())
			sb.append("</td>")
			sb.append("<td>")
			sb.append(problem.getMessage())
			sb.append("</td>")
			sb.append("<td>")
			sb.append(problem.getKeyword())
			sb.append("</td>")
			sb.append("</tr>")
			sb.append("\n")
		}
	}
}

Path projectDir = Paths.get(RunConfiguration.getProjectDir())
Path credSchemaPath = projectDir.resolve('Include').resolve('resources').resolve('credential-schema.json')
Path credDataPath   = projectDir.resolve('Include').resolve('resources').resolve('credential.json')
Path coordDataPath  = projectDir.resolve('Include').resolve('resources').resolve('geographical-coordinates.json')

JsonValidationService service = JsonValidationService.newInstance()

// Reads the JSON schema
JsonSchema schema = service.readSchema(credSchemaPath);

// Problem handler which will just print messages to stdout.
/*
ProblemHandler handler = service.createProblemPrinter(new Consumer<String>() {
	@Override
	public void accept (String s) {
		System.err.println("*** " + s)
	}
})
 */
MyProblemHandler handler1 = new MyProblemHandler()

WebUI.comment("Reads the credentail.json file, validate it to be VALID, print the contents")
service.createReader(credDataPath, schema, handler1).with { reader ->
	try {
		JsonValue value = reader.readValue()
		println MyJsonNavigator.navigate(value)
		println MyJsonPrinter.prettyPrint(value)
	} finally {
	    reader.close()
	}
}
handler1.flush()
println handler1.getOutput()




WebUI.comment("Reads the geographical-coordiation.json file validate it against credential-schema.json; this will fail")
MyProblemHandler handler2 = new MyProblemHandler()
service.createReader(coordDataPath, schema, handler2).with { reader ->
	try {
		JsonValue value = reader.readValue()
		println MyJsonNavigator.navigate(value)
		println MyJsonPrinter.prettyPrint(value)
	} finally {
		reader.close()
	}
}
handler2.flush()
println handler2.getOutput()
