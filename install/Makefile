
# Ensures that prerequisites are available.

all: \
 as_root \
 /usr/bin/java \
 /usr/sbin/apachectl \

as_root:
	@if [ $(shell id -u) != 0 ]; then echo Please run as root: sudo make; exit 1; fi

# java8: http://www.webupd8.org/2012/09/install-oracle-java-8-in-ubuntu-via-ppa.html
/usr/bin/java:
	apt-get install -y openjdk-8-jdk openjdk-8-jre-headless

/usr/bin/oracle_java:
	add-apt-repository ppa:webupd8team/java
	apt-get update
	apt-get install oracle-java8-installer

/usr/sbin/apachectl:
	apt-get install -y apache2
	apache2ctl configtest
