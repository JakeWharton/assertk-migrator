@file:JvmName("AssertkMigratorMain")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import java.io.File
import kotlin.io.path.ExperimentalPathApi

fun main(vararg args: String) {
	AssertkMigratorCommand().main(args)
}

@OptIn(ExperimentalPathApi::class)
private class AssertkMigratorCommand : CliktCommand(name = "assertk-migrator") {
	private val projectDir by argument().file()

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
		if (file.name != "build.gradle") {
			return
		}

		val original = file.readText()

		val newLines = original.lines()
			.map {
				if ("libs.truth" in it) {
					it.replace("libs.truth", "libs.assertk")
				} else {
					it
				}
			}

		println("BUILD $file")
		file.writeText(newLines.joinToString("\n"))
	}

	private fun migrateTest(file: File) {
		val original = file.readText()
		if ("kotlin.test" !in original || "com.google.common.truth" !in original) {
			return
		}

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
		val withImports = withoutAssertThatImport.substring(0, firstImportIndex) +
			"\nimport assertk.*\nimport assertk.assertions.*" +
			withoutAssertThatImport.substring(firstImportIndex)

		// TODO fix-up callsites
		//  Truth.assertThat --> assertThat(actual).isEqualTo(expected)
		//  assertEquals --> assertThat(actual).isEqualTo(expected)
		//  assertTrue --> assertThat(actual).isTrue()
		//  etc..
		//  assertThat(actual).apply { .. } --> assertThat(actual).all { .. }

		file.writeText(withImports)
		println("SOURCE $file")
	}
}
