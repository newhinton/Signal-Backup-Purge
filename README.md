# Signal Backup Purge - Sensible cleanup for signal backups
[![license: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://github.com/newhinton/Round-Sync/blob/master/LICENSE)
[![Documentation](https://img.shields.io/badge/Documentation-NotYet-4aad4e)](https://felixnuesse.de/donate) [![supportive flags](https://img.shields.io/badge/support-🇺🇦_🏳️‍⚧_🏳️‍🌈-4aad4e)](https://roundsync.com)


A cli-tool to clean your signal backup folder. This is only useful if you move your backups off your android device and keep multiple copies.

## Features

- **Dynamic Retention Control** You choose how many months are kept fully or partial
- **Statistics** Show you how much storage you save by purging old backups!
- **Easy to use** Simple rules make it easy to predict what is kept!

## Installation

Grab the latest release and run it like this:

```sh
# java needs to be configured in the PATH-variable
java -jar Signal-Backup-Purge.jar -h
```


## Usage
[See the documentation](https://felixnuesse.de/donate). Which i haven't written yet.


Donations
------------

If you like my work, either this app or in general, you are more than welcome to leave a donation.
It helps me to dedicate time to further improve my apps!

[Paypal](https://www.paypal.com/paypalme/felixnuesse) | [Liberapay](https://liberapay.com/newhinton) | [Github Sponsor](https://github.com/sponsors/newhinton)


## Libraries
- [CLICKT](https://ajalt.github.io/clikt/) - Command Line Parser for kotlin
- [ascii-table](https://github.com/freva/ascii-table) by [freva](https://github.com/freva) - Command Line Table Formatter
- [Janis](http://fusesource.github.io/jansi/) - Command Line Colorizer

## Contributing

Anyone is welcome to contribute and help out. However, hate, discrimination and racism are decidedly unwelcome here. If you feel offended by this, you might belong to the group of people who are not welcome. I will not tolerate hate in any way.

## Developing


You can then build the app:

```sh
# build jar
./gradlew jar

```


For development you can also run the tool directly:

```sh
# build jar
./gradlew execute --args="-k 3 -c 4 -s testfiles/"

```

See the help for an explanation for the flags.


## License
This app is released under the terms of the [GPLv3 license](https://github.com/newhinton/signal-backup-purge/blob/master/LICENSE).



