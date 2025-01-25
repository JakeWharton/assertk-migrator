@file:JvmName("AssertkMigratorMain")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.walk
import kotlin.io.path.writeText

fun main(vararg args: String) {
	AssertkMigratorCommand().main(args)
}

@OptIn(ExperimentalPathApi::class)
private class AssertkMigratorCommand : CliktCommand(name = "assertk-migrator") {
	override fun help(context: Context) =
		"Migrate your repo from kotlin.test and Truth assertions to AssertK automatically"

	private val projectDirs by argument().path().multiple(required = true)

	private val truth by option()
		.default("truth")
		.help("Version catalog path to the Truth dependency (default: truth)")

	private val assertk by option()
		.default("assertk")
		.help("Version catalog path to the AssertK dependency (default: assertk)")

	private val debug by option(hidden = true).flag()

	override fun run() {
		for (projectDir in projectDirs) {
			projectDir.walk()
				.filter { ".git/" !in it.toString() }
				.filter { "/build/" !in it.toString() }
				.filter(Path::isRegularFile)
				.forEach { file ->
					if (file.extension == "kt") {
						migrateTest(file)
					} else if (file.name !in listOf("build.gradle", "build.gradle.kts")) {
						migrateBuild(file)
					}
				}
		}
	}

	private fun migrateBuild(file: Path) {
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

	private fun migrateTest(file: Path) {
		val original = file.readText()
		if ("org.junit.Assert" !in original &&
			"kotlin.test." !in original &&
			"com.google.common.truth." !in original
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

		val migrated = withImports
			.replace(".isAssignableTo(", ".isInstanceOf(")
			.replace(".hasMessageThat()", ".message()")
			// AssertK defaults to ordered comparison.
			.replace(".inOrder()", "")
			// Note: Custom Cash App extension on Truth Subject.
			.replace(".isOfType<", ".isInstanceOf<")
			// .isInstanceOf(Home::class.java) --> .isInstanceOf<Home>()
			.replace(isInstanceOfRegex, ".isInstanceOf<$1>()")
			.replace("Truth.assertThat", "assertThat")
			.replace(".containsExactlyInOrder(", ".containsExactly(")
			.replace(".containsEntry(", ".contains(")

		// TODO fix-up remaining callsites
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
		"assertk.assertions.contains",
		"assertk.assertions.containsExactly",
		"assertk.assertions.doesNotContain",
		"assertk.assertions.hasSize",
		"assertk.assertions.isEmpty",
		"assertk.assertions.isEqualTo",
		"assertk.assertions.isFalse",
		"assertk.assertions.isInstanceOf",
		"assertk.assertions.isNotEmpty",
		"assertk.assertions.isNotNull",
		"assertk.assertions.isNull",
		"assertk.assertions.isTrue",
		"assertk.assertions.isSameInstanceAs",
		"assertk.assertions.isNotSameInstanceAs",
		"assertk.assertions.message",
	)
}

private val isInstanceOfRegex = """\.isInstanceOf\(([A-Za-z0-9_.]+)::class\.java\)""".toRegex()
