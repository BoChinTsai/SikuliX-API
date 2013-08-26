Sikuli API 1.0.1 <br />Service Update per August 12th, 2013
===========
**MANDATORY ;-) --->>>** [Read the Release Notes carefully to avoid problems from the beginning](https://github.com/RaiMan/SikuliX-API/wiki/Release-Notes-API)

[to download setup-sikuli.jar version 1.0.1 start here](http://www.sikuli.org/download.html)

Sikuli API is targeted at people who want to develop, run and debug programs in Java, other Java based languages or Java aware scripting languages (for Jython we have [Sikuli IDE](https://github.com/RaiMan/SikuliX-IDE) ).

Same goes for people who want to develop, run and debug scripts using Sikuli IDE supported scripting languages in other IDE's like Eclipse, Netbeans, 
<br /><br />... [some quickstart info ...](https://github.com/RaiMan/SikuliX-API/wiki/Usage-in-Java-programming)
<br /><br />
The downloadable packages of Sikuli API contain everything needed to develop, test, run and debug with any suitable IDE (e.g. Eclipse, Netbeans, ...) or however you like ;-).
<br /><br />
Hava a look at the Java docs: 
[click to view Online](https://dl.dropboxusercontent.com/u/42895525/SikuliX/SikuliX-API-JavaDocs/index.html)
 or [click to download as zipfile](https://dl.dropboxusercontent.com/u/42895525/SikuliX/SikuliX-API-JavaDocs.zip)
<br /><br />
This repo is **fully Maven**, so a fork of this repo can be directly used as project in NetBeans/Eclipse/... or with mvn on commandline.<br />
[A more detailed info on usage, contents and production of standalone runnable packages](https://github.com/RaiMan/SikuliX-API/wiki/Maven-support)

It produces a lightweight **sikulix-api.jar**, that only contains the Sikuli Java API and is intended for use in pure Java or in Java aware scripting and testing environments while developing in IDEs using Maven (depends on and needs [Sikuli Basics](https://github.com/RaiMan/SikuliX-Basics) )

At runtime in pure Java or in Java aware scripting and testing environments as well when using standalone Jython you need the **sikuli-java.jar** in class path (contains [Sikuli Basics](https://github.com/RaiMan/SikuliX-Basics)). <br />It can be produced using <br />*mvn -f pom_make_sikulix-java-jar.xml clean package assembly:single* <br />having a valid [Sikuli Basics](https://github.com/RaiMan/SikuliX-Basics) in your local repo.

To run Sikuli scripts from commandline in any of the supported scripting languages you need the **sikuli-script.jar** (currently only Jython supported).It contains [Sikuli Basics](https://github.com/RaiMan/SikuliX-Basics), [Sikuli Jython](https://github.com/RaiMan/SikuliX-Jython) and a complete Jython 2.5.4.<br />
It can be produced using <br />*mvn -f pom_make_sikuli-script-jar.xml clean package assembly:single* <br />having the mentioned dependencies in your local repo.<br />

**If new to Sikuli**, you might aternatively be interested in the pure Java implementation, which is to some extent feature compatible, but not API compatible: [Sikuli Java API](http://code.google.com/p/sikuli-api).
<br /><br />
**Roadmap**
 - **2013 August 12:** release of service update Sikuli API 1.0.1
  - bug fixes and smaller enhancements 
<br /><br />
 - **2013 August:** open a developement branch for Sikuli API 1.1
  - isolate the script running feature (already in 1.0.1) to allow more scripting languages (e.g. JRuby)
  - bug fixes and more enhancements tbd.
<br /><br />
 - **2013 November 29:** release of Sikuli API 1.1
  - merge branch develop into branch master
  - open a developement branch for Sikuli API 1.2
  - use existing Java wrappers for OpenCV (javacv) and Tesseract (Tes4J) alternatively
  - new features tbd.
<br /><br />
 - **2014:** new versions in May and November

**History**
 - this is based on the developement at MIT (Tsung-Hsiang Chang (Sean aka vgod) and Tom Yeh) which was discontinued end 2011 (https://github.com/sikuli/sikuli) with a latest version called Sikuli X-1.0r930.
 - and the [follow up repo](https://github.com/RaiMan/Sikuli12.11), where I prepared the creation of a final version 1.0
 - in April 2013 I decided, to divide Sikuli into the 2 packages [Sikuli IDE](https://github.com/RaiMan/SikuliX-IDE) and Sikuli API (this repo), to better support future contributions.

**Support**
 - until otherwise noted: [questions, requests and bugs can still be posted here](https://answers.launchpad.net/sikuli)
 - the wiki in this repo will be used extensively to document anything (taking over this roll from the webpage and lauchpad)
 - you might always post an issue with any content in this repo of course

**Contribution**
 - pull requests are always welcome
 - everyone is welcome to add interesting stuff, experiences, solutions to the wiki in this repo
