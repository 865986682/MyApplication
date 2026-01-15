pluginManagement {
    repositories {

        // 使用阿里云 Google 镜像（注意顺序！）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // 使用阿里云 Maven Central 镜像
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        // 可选：阿里云公共仓库（包含部分第三方库）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // 阿里云JCenter镜像（如果项目中有使用JCenter的依赖）
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }

        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "My Application"
include(":app")
