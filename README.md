Using Justify a JSON Schema validator in Katalon Studio
=================================

## What is this?

This is a [Katalon Studio](https://www.katalon.com/) project for demonstration purpose. You can download the zip of this project from [Releases](https://github.com/kazurayam/JustifyInKatalonStudio/releases) page, and open
it locally on your PC with your Katalon Studio.

This project was developed using Katalon Studio v6.1.5.

## Motivation

[JSON Schema](https://json-schema.org/) is a vocabulary that allows you to annotate and validate JSON documents. JSON Schema is a nice tool for Web Service testing in Katalon Studio.

As the post in Katalon Forum "[JSON Schema validation broken after 5.9.0](https://forum.katalon.com/t/json-schema-validation-broken-after-5-9-0/23457)" reports, [everit-org/json-schema](https://github.com/everit-org/json-schema) once worked prior to KS 5.9.0, but as of KS 5.9.0 it does not work. [Ibus reported the reason](https://forum.katalon.com/t/json-schema-validation-broken-after-5-9-0/23457/12). For the time being, everit-org/json-schema is not an option for users.

[JSON Schema project's Implementation page](https://json-schema.org/implementations.html#validator-java) list an alternative JSON Schema validator: [Justify](https://github.com/leadpony/justify)

[Justify](https://github.com/leadpony/justify) uses `javax.json` library for parsing JSON. It does NOT depend on the [**org.json** library](https://stleary.github.io/JSON-java/) which caused problem for `everit-org/json-schema` in Katalon Studio. Therefore, I suppose, `Justify` can be an alternative to `everit/json-schema` usable in Katalon Studio. So I make a demo project to verify Justify in Katalon Studio.

## How to run the demonstration

1. download the latest zip of this project from [Releases](https://github.com/kazurayam/JustifyInKatalonStudio/releases) page
2. unzip it
3. you need to download external dependencies (jar files) from Maven Central repository into the `Drivers` dir. Do the followin operation in command line (here I assume that *java* is installed in your PC):
```
$ cd <JustifyInKatalonStudio directory>
$ .\gradlew.bat katalonCopyDependencies
```
Once finished, you will find the `Drivers` directoy containing the following jars:
  - katalon_generated_icu4j-63.1.jar
  - katalon_generated_javax.json-1.1.4.jar
  - katalon_generated_javax.json-api-1.1.4.jar
  - katalon_generated_justify-0.16.0.jar
4. start Katalon Studio, open the project
5. open `Test Cases/TC1` and run it.

## What `TC1` does

[`TC1`](Scripts/TC1/Script1558066108685.groovy) is a bit lengthy code. But it is  essentially simple. It reads a JSON Schema from file, reads a JSON data from file, parse it and apply schema. It prints the data in 2 formats (custom format and pretty-print format). If any problem against schema found, report it. The core part is as follows:
```
Path projectDir = Paths.get(RunConfiguration.getProjectDir())
Path credSchemaPath = projectDir.resolve('Include').resolve('resources').resolve('credential-schema.json')
Path credDataPath   = projectDir.resolve('Include').resolve('resources').resolve('credential.json')
Path coordDataPath  = projectDir.resolve('Include').resolve('resources').resolve('geographical-coordinates.json')

JsonValidationService service = JsonValidationService.newInstance()

// Reads the JSON schema
JsonSchema schema = service.readSchema(credSchemaPath);

// Problem handler which will just print messages to stdout.
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
```

Json Schema file is:
- [credential-schema.json](Include/resources/credential-schema.json)

Sample Json Data files are:
- [credentail.json](Include/resources/credential.json)
- [geographical-coodinates.json](Include/resources/geographical-coordinates.json)

Both data files are found INVALID against the schema. When executed, `TC1` emits following output in the console view:
```
OBJECT
Key firstName: STRING John
Key lastName: STRING Doe
Key age: NUMBER 59

{
    "firstName": "John",
    "lastName": "Doe",
    "age": 59
}

<table>
<thead>
<tr><th>Line No.</th><th>Column No.</th><th>Message</th><th>Assertion Keyword</th></tr>
</thead>
<tbody>
<tr><td>4</td><td>11</td><td>数値は35以下でなければいけません。</td><td>maximum</td></tr>
</tbody>
</table>




OBJECT
Key coordinates: OBJECT
Key latitude: NUMBER 48.858093
Key longitude: NUMBER 2.294694

{
    "coordinates": {
        "latitude": 48.858093,
        "longitude": 2.294694
    }
}

<table>
<thead>
<tr><th>Line No.</th><th>Column No.</th><th>Message</th><th>Assertion Keyword</th></tr>
</thead>
<tbody>
<tr><td>6</td><td>1</td><td>オブジェクトはプロパティ"firstName"を持たなければいけません。</td><td>required</td></tr>
<tr><td>6</td><td>1</td><td>オブジェクトはプロパティ"lastName"を持たなければいけません。</td><td>required</td></tr>
</tbody>
</table>
```

Justify emits error messages in English or Japanese depending on the locale of you PC.

## Conclusion

[Justify](https://github.com/leadpony/justify) just works well in Katalon Studio. I think that `Justify` is a good alternative JSON Schema Validator to the `everit/json-schema` library in Katalon Studio
