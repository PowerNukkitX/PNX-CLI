# PNX-CLI
[![English](https://img.shields.io/badge/English-100%25-green?style=flat-square)](https://github.com/PowerNukkitX/PNX-CLI/blob/master/README.md)
[![简体中文](https://img.shields.io/badge/简体中文-100%25-green?style=flat-square)](https://github.com/PowerNukkitX/PNX-CLI/blob/master/lang/ZH-README.md)


PNX-CLI is a command line tool for PNX. It can help you to install and start PNX quickly.
windows using . \pnx.exe to run.
linux use . /pnx to run.
Hereafter abbreviated to pnx.
## Some common commands
The sub-list is the parameter body, and the main list is the command body.
- pnx start
  - -g&emsp;Generate startup commands
  - -r&emsp;Start the server in automatic reboot mode
  - --stdin=xxx&emsp;Read console input from the specified file (xxx input the file address, from pnx-cli current path)
- pnx server
  - --latest&emsp;Install the latest version of pnx core
  - --dev&emsp;Install the development version of pnx core
  - install / update&emsp;Install or upgrade PNX server core (manual selection required)
- pnx libs
  - -u / update&emsp;Install or update dependent libraries
  - -c / check&emsp;Check if the dependency library is up to date
  - --latest&emsp;Install the latest version of the dependency library
  - --dev&emsp;Install the development version of the dependency library
- pnx jvm
  - check&emsp;View the installed JVMs
  - remote&emsp;Lists all available JVM in the PNX remote repository
  - install=name&emsp;Install the JVM according to the name.(The name is queried from the command above)
  - uninstall&emsp;Uninstall the installed JVM according to the entered serial number.
- pnx comp
  - -c&emsp;Check for available addons。
  - -i=name&emsp;Install or repair the addon according to the entered name. (The name is checked from the command above)
- pnx about&emsp;Information about the PowerNukkitX CLI
