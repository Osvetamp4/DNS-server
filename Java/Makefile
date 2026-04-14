#
#  This Makefile is designed to compile the DNS server code and create the executable
#  script "4700dns". The DNS server uses the dnsjava library for low-level DNS message
#  construction and SLF4J (api and simple) for minimal logging support.
#
#
#  Usage:
#       make deps       # Download the required JARs (dnsjava, slf4j-api, and slf4j-simple)
#       make build      # Compile the code, build the JAR file, and create the executable script "4700dns"
#       make clean      # Remove all compiled classes and generated files
#
#  Students must submit a separate Makefile with their submission that compiles 
#  their code. Whether the '4700dns' script is created by the Makefile is 
#  the developer's decision. 

JFLAGS = -g -cp .:dnsjava.jar:slf4j-api.jar:slf4j-simple.jar
JC = javac
JAR = jar
JAVA = java

CLASSES = Server.java
JAR_FILE = server.jar

# By uncommenting the line below, you acknowledge the starter code uses dnsjava and slf4j and that
# you can modify the starter code to use the dns and/or logging library of your choice.
#DNSJAVA_URL = https://repo1.maven.org/maven2/dnsjava/dnsjava/3.5.2/dnsjava-3.5.2.jar
#SLF4J_API_URL = https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.30/slf4j-api-1.7.30.jar
#SLF4J_SIMPLE_URL = https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.30/slf4j-simple-1.7.30.jar

default: build

deps:
	@echo "Downloading dependencies..."
	wget -O dnsjava.jar $(DNSJAVA_URL)
	wget -O slf4j-api.jar $(SLF4J_API_URL)
	wget -O slf4j-simple.jar $(SLF4J_SIMPLE_URL)

%.class: %.java
	$(JC) $(JFLAGS) $<

build: $(CLASSES:.java=.class)
	@echo "Creating manifest file for the DNS server"
	echo "Main-Class: Server" > manifest.txt
	echo "Class-Path: dnsjava.jar slf4j-api.jar slf4j-simple.jar" >> manifest.txt
	@echo "Building JAR file $(JAR_FILE)"
	$(JAR) cfm $(JAR_FILE) manifest.txt *.class
	@echo "Creating executable script '4700dns'"
	echo '#!/bin/bash\n$(JAVA) -cp $(JAR_FILE):dnsjava.jar:slf4j-api.jar:slf4j-simple.jar Server "$$@"' > 4700dns
	chmod +x 4700dns

clean:
	@echo "Cleaning compiled classes, manifest, executable script, and JAR file"
	rm -f *.class manifest.txt 4700dns $(JAR_FILE)
