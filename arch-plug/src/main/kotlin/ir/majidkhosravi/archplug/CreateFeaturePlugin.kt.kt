package ir.majidkhosravi.archplug

import java
import java.io.File
import java.util.Locale

open class ArchPluginExtension {
    var packageName: String = "com.example.app"
}

class CreateFeaturePlugin : org.gradle.api.Plugin<org.gradle.api.Project> {

    override fun apply(project: org.gradle.api.Project) {
        val extension = org.gradle.api.plugins.ExtensionContainer.create(
            "archConfig",
            ArchPluginExtension::class.java
        )

        org.gradle.api.tasks.TaskContainer.register("createFeature") {
            org.gradle.api.Task.setGroup = "architecture"
            org.gradle.api.Task.setDescription =
                "Generates clean architecture layers with dynamic package name"

            org.gradle.api.Task.doLast {
                val basePackage = extension.packageName

                val feature = org.gradle.api.Project.findProperty("feature") as String?
                    ?: throw java.lang.IllegalArgumentException("❌ Error: Please provide a feature name. Usage: ./gradlew createFeature -Pfeature=User")

                val featureLower = feature.lowercase(java.util.Locale.US)
                val basePackagePath = basePackage.replace(".", "/")

                val modulesPathMap = mapOf(
                    "app" to "app/src/main/java/$basePackagePath/app",
                    "presentation" to "presentation/src/main/java/$basePackagePath/presentation",
                    "domain" to "domain/src/main/java/$basePackagePath/domain",
                    "data" to "data/src/main/java/$basePackagePath/data"
                )

                val filesByModule = mapOf(
                    "app" to listOf(
                        "composables/${feature}Screen.kt",
                        "viewmodels/${feature}ViewModel.kt",
                        "navigation/${feature}Nav.kt"
                    ),
                    "presentation" to listOf(
                        "features/$featureLower/${feature}Content.kt",
                        "state/${feature}UiState.kt"
                    ),
                    "domain" to listOf(
                        "model/${feature}.kt",
                        "repository/${feature}Repository.kt",
                        "usecase/Get${feature}UseCase.kt"
                    ),
                    "data" to listOf(
                        "repository/${feature}RepositoryImpl.kt",
                        "datasource/remote/${feature}RemoteDataSource.kt",
                        "datasource/local/${feature}LocalDataSource.kt",
                        "di/${feature}Module.kt"
                    )
                )

                filesByModule.forEach { (moduleName, paths) ->
                    val moduleRootPath = modulesPathMap[moduleName] ?: return@forEach
                    val baseDir = java.io.File(org.gradle.api.Project.getRootDir, moduleRootPath)

                    paths.forEach { relativePath ->
                        val file = baseDir.resolve(relativePath)
                        val subFolder = relativePath.substringBeforeLast("/", "").replace("/", ".")

                        val finalPackage =
                            if (subFolder.isNotEmpty()) "$basePackage.$moduleName.$subFolder" else "$basePackage.$moduleName"
                        val className = file.nameWithoutExtension

                        if (!file.exists()) {
                            file.parentFile.mkdirs()
                            val content = generateContent(className, finalPackage, basePackage)
                            file.writeText(content)
                            println("✔ Created: ${file.path}")
                        } else {
                            println("⚠ Skipped (Already exists): ${file.path}")
                        }
                    }
                }
            }
        }
    }

    private fun generateContent(className: String, packageName: String, basePackage: String): String {
        return when {
            className.endsWith("Screen") || className.endsWith("Content") -> """
                package $packageName

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier

                @Composable
                fun $className(modifier: Modifier = Modifier) {
                    // TODO: Implement $className
                }
            """.trimIndent()

            className.endsWith("ViewModel") -> """
                package $packageName

                import androidx.lifecycle.ViewModel
                import dagger.hilt.android.lifecycle.HiltViewModel
                import javax.inject.Inject

                @HiltViewModel
                class $className @Inject constructor() : ViewModel() {
                }
            """.trimIndent()

            className.endsWith("UiState") -> """
                package $packageName

                data class $className(
                    val isLoading: Boolean = false,
                    val error: String? = null
                )
            """.trimIndent()

            className.endsWith("Repository") -> """
                package $packageName

                interface $className {
                }
            """.trimIndent()

            className.endsWith("UseCase") -> """
                package $packageName

                import javax.inject.Inject

                class $className @Inject constructor() {
                    operator fun invoke() { }
                }
            """.trimIndent()

            className.endsWith("Impl") -> {
                val interfaceName = className.removeSuffix("Impl")
                """
                package $packageName

                import javax.inject.Inject
                import $basePackage.domain.repository.$interfaceName

                class $className @Inject constructor() : $interfaceName {
                }
                """.trimIndent()
            }

            className.endsWith("Module") -> """
                package $packageName

                import dagger.Module
                import dagger.hilt.InstallIn
                import dagger.hilt.components.SingletonComponent

                @Module
                @InstallIn(SingletonComponent::class)
                object $className {
                }
            """.trimIndent()

            else -> """
                package $packageName

                class $className {
                }
            """.trimIndent()
        }
    }
}
