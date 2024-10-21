@file:JvmName("AssertkMigratorMain")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import java.io.File
import kotlin.io.path.ExperimentalPathApi

fun main(vararg args: String) {
	AssertkMigratorCommand().main(args)
}

@OptIn(ExperimentalPathApi::class)
private class AssertkMigratorCommand : CliktCommand(name = "assertk-migrator") {
	override fun help(context: Context) =
		"Migrate your repo from kotlin.test and Truth assertions to AssertK automatically"

	private val projectDir by argument().file()

	private val truth by option()
		.default("truth")
		.help("Version catalog path to the Truth dependency (default: truth)")

	private val assertk by option()
		.default("assertk")
		.help("Version catalog path to the AssertK dependency (default: assertk)")

	private val debug by option(hidden = true).flag()

	override fun run() {
		// TODO git ls-files
		projectDir.walk()
			.filter { ".git/" !in it.path }
			.filter { "/build/" !in it.path }
			.filter(File::isFile)
			.forEach {
				migrateBuild(it)
				migrateTest(it)
			}
	}

	private fun migrateBuild(file: File) {
		if (file.name !in listOf("build.gradle", "build.gradle.kts")) {
			return
		}
		println("BUILD $file")

		val original = file.readText()

		// TODO What about modules with only kotlin.test dependency and no Truth? Dup + replace.

		val newLines = original.lines()
			.map {
				if ("libs.$truth" in it) {
					it.replace("libs.$truth", "libs.$assertk")
				} else {
					it
				}
			}

		file.writeText(newLines.joinToString("\n"))
	}

	private fun migrateTest(file: File) {
		val original = file.readText()
		if ("org.junit.Assert" !in original &&
			"kotlin.test" !in original &&
			"com.google.common.truth" !in original
		) {
			return
		}
		println("SOURCE $file")

		// Get rid of static assertThat import from Truth which will conflict with AssertK
		val assertThatImport = "import com.google.common.truth.Truth.assertThat\n"
		val assertThatImportIndex = original.indexOf(assertThatImport)
		val withoutAssertThatImport = if (assertThatImportIndex != -1) {
			original.substring(0, assertThatImportIndex) +
				original.substring(assertThatImportIndex + assertThatImport.length)
		} else {
			original
		}

		// Add star imports for AssertK, Spotless will clean them up to individual imports later.
		val firstImportIndex = withoutAssertThatImport.indexOf("\nimport ")
		val withImports = buildString {
			append(withoutAssertThatImport.substring(0, firstImportIndex))
			assertkImports.joinTo(this, separator = "") { "\nimport $it" }
			append(withoutAssertThatImport.substring(firstImportIndex))
		}

		val migrated = withImports.replace(".isOfType<", ".isInstanceOf<")

		// TODO fix-up callsites
		//  Truth.assertThat --> assertThat(actual).isEqualTo(expected)
		//  assertEquals --> assertThat(actual).isEqualTo(expected)
		//  assertTrue --> assertThat(actual).isTrue()
		//  etc..
		//  assertThat(actual).apply { .. } --> assertThat(actual).all { .. }

		file.writeText(migrated)
	}

	private fun debugLog(log: () -> Any) {
		if (debug) {
			println("[DEBUG] ${log()}")
		}
	}

	private val assertkImports = listOf(
		"assertk.assertThat",
		"assertk.assertions.containsExactly",
		"assertk.assertions.hasSize",
		"assertk.assertions.isEqualTo",
		"assertk.assertions.isFalse",
		"assertk.assertions.isInstanceOf",
		"assertk.assertions.isNotNull",
		"assertk.assertions.isNull",
		"assertk.assertions.isTrue",
	)
}
