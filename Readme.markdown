This is a template (or skeleton or boilerplate) repository for Minecraft Forge modding projects.

## Requirements
* [Gradle installation with gradle binary in PATH](http://www.gradle.org/installation)

	Unlike the source package provided by Forge, this template does not include a gradle wrapper or distribution.

* NetBeans or any other IDE which can use a gradle build as a project

	Unlike the source package provided by Forge, this template does not include any eclipse files or project skeleton. You may try to generate one via gradle, but YMMV.

## Usage
Simply execute `gradle setupDevWorkspace` in the root directory of this repository. Then open it as a `Gradle project` using your IDE of choice.

An example mod is included in `.\src\main\java\com\example\examplemod`. To edit the metadata of your mod, edit `.\src\main\resources\mcmod.info`.

## Updating
To update to a later version of forge, open `build.gradle` in a text editor and update the `version` line under `minecraft` to the latest Forge version string. Alternatively, pull in and merge any changes from this template repository.

Finally, execute `gradle setupDevWorkspace --refresh-dependencies`.