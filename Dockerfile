FROM debian:jessie
MAINTAINER Thomas M. DuBuisson <tommd@galois.com>

# Expose zookeeper and kafka ports
EXPOSE 2181
EXPOSE 9092

# Setup some system tooling
RUN apt-get update
RUN apt-get -y install supervisor

# Setup application tools
RUN apt-get -y install python python-pip
RUN pip install kafka-python

RUN mkdir -p /usr/local/bin
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf
COPY deps/ta2 /

CMD ["/usr/bin/supervisord"]
