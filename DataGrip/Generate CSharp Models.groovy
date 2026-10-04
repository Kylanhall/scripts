import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

/*
 * Generates simple C# model classes from selected database tables.
 * Intended to be used with JetBrains DataGrip.
 */

namespaceName = ".Models"

typeMapping = [
  (~/(?i)tinyint\(1\)|boolean|bool/)        : "bool",
  (~/(?i)bigint/)                           : "long",
  (~/(?i)smallint|mediumint|int|integer/)   : "int",
  (~/(?i)decimal|numeric/)                  : "decimal",
  (~/(?i)float/)                            : "float",
  (~/(?i)double|real/)                      : "double",
  (~/(?i)datetime|timestamp/)               : "DateTime",
  (~/(?i)date/)                             : "DateOnly",
  (~/(?i)time/)                             : "TimeOnly",
  (~/(?i)char|varchar|text|tinytext|mediumtext|longtext/) : "string",
  (~/(?i)/)                                 : "string"
]

FILES.chooseDirectoryAndSave(
    "Choose directory",
    "Choose where to store generated files"
) { dir ->
  SELECTION
    .filter { it instanceof DasTable }
    .each { generate(it, dir) }
}

def generate(table, dir) {
  def className = csharpName(table.getName(), true)
  def fields = calcFields(table)

  new File(dir, className + ".cs").withPrintWriter { out ->
    generate(out, className, fields)
  }
}

def generate(out, className, fields) {
  out.println "namespace ${namespaceName};"
  out.println ""
  out.println "public class ${className}"
  out.println "{"

  fields.each { field ->
    def defaultValue = ""

    if (field.type == "string" && !field.nullable) {
      defaultValue = " = string.Empty;"
    }

    out.println "    public ${field.type}${field.nullable ? "?" : ""} ${field.name} { get; set; }${defaultValue}"
  }

  out.println "}"
}

def calcFields(table) {
  DasUtil.getColumns(table).reduce([]) { fields, col ->

    def spec = Case.LOWER.apply(
        col.getDasType().getSpecification()
    )

    def mapping = typeMapping.find { pattern, type ->
      pattern.matcher(spec).find()
    }

    def typeStr = mapping != null ? mapping.value : "string"

    def nullable = !col.isNotNull()

    fields += [[
      name    : csharpName(col.getName(), true),
      type    : typeStr,
      nullable: nullable
    ]]
  }
}

def csharpName(str, capitalize) {
  def words = com.intellij.psi.codeStyle.NameUtil
    .splitNameIntoWordList(str)

  def s = words
    .collect { Case.LOWER.apply(it).capitalize() }
    .join("")
    .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")

  if (capitalize || s.length() == 1) {
    return s
  }

  return Case.LOWER.apply(s[0]) + s[1..-1]
}
