## Install java 11 in mac

Use the following command to install java
```bash
brew tap AdoptOpenJDK/openjdk
brew cask install adoptopenjdk11
java -version
```
Configure your IDE to use the new java before using the project

## Recommended IDE

Intellij version: 2019.13.2+ 

## Configuring lombok configuration
- Using IDE built-in plugin system on MacOs:
  - <kbd>Preferences</kbd> > <kbd>Settings</kbd> > <kbd>Plugins</kbd> > <kbd>Browse repositories...</kbd> > <kbd>Search for "lombok"</kbd> > <kbd>Install Plugin</kbd>
- Using IDE built-in plugin system on Windows:
  - <kbd>Preferences<kbd> <kbd>Plugins</kbd> > <kbd>Browse repositories...</kbd> > <kbd>Search for "lombok"</kbd> > <kbd>Install Plugin</kbd>
- Manually:
  - Download the [latest release](https://github.com/mplushnikov/lombok-intellij-plugin/releases/latest) and install it manually using <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Install plugin from disk...</kbd>
- Restart IDE.
- In your project: Click Preferences -> "Build, Execution, Deployment" -> "Compiler, Annotation Processors". Click Enable Annotation Processing
- Afterwards you might need to do a complete rebuild of your project via Build -> "Rebuild Project".

## Install mongo 

####To set up site meta which is necessary for site creation ,run the following command
```bash
$ sh scripts/setup-site-meta.sh
```

###To setup pim workflow template
```bash
export datacenter='{"data":{"regions":[{"id":"us-gcp","name":"us","lat_long":"37.0902N,95.7129W","type":"GCP","skipperEndPoint":"https://console-g-nam.unbxd.io"},{"id":"us","name":"us","lat_long":"37.0902N,95.7129W","type":"AWS","skipperEndPoint":"https://console-nam.unbxd.io"},{"id":"sg","name":"sg","lat_long":"1.3521N,103.8198E","type":"AWS","skipperEndPoint":"https://console-apac.unbxd.io"},{"id":"uk","name":"uk","lat_long":"55.3781N,3.4360W","type":"AWS","skipperEndPoint":"https://console-uk.unbxd.io"},{"id":"au","name":"au","lat_long":"25.2744S,133.7751E","type":"AWS","skipperEndPoint":"https://console-anz.unbxd.io"}]}}'
export mongoHost=mongodb://localhost:27017
export skipperHost=localhost:8338
sh scripts/setup-pim-workflow.sh
```

 


