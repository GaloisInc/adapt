Trevor: I've included a first cut at 6 or so features, as well as examples of how we might encode them as JSON. The JSON includes type tags called "feature", which I'm not sure is actually necessary.

----

1. User executes file

This feature is present when a user executes a given file. It might be useful to break the "file" part of this feature out into a directory and a filename.

{ "feature" : "user-executed-file", "user" : "root", "file" : "/bin/ls" }
{ "feature" : "user-executed-file", "user" : "trevor", "file" : "/home/trevor/bin/irssi" }

2. Program reads file

Present when an executing program reads a file from the file system.

{ "feature" : "program-reads-file", "user" : "root", "program" : "chpasswd", "file" : "/etc/shadow" }
{ "feature" : "program-reads-file", "user" : "trevor", "program" : "/usr/bin/vim", "file" : "/home/trevor/.vim/vimrc" }

3. Program listens on port

Present when a program creates a socket, and then uses the `listen` syscall on it.

{ "feature" : "program-listens-on-port", "user" : "httpd", "program" : "/usr/bin/apache2", "port" : 80 }
{ "feature" : "program-listens-on-port", "user" : "root", "program" : "/usr/bin/sshd", "port" : 22 }

4. Program makes tcp connection

Present when a program uses the connect syscall to connect to another remote system.

{ "feature" : "program-makes-tcp-connection", "user" : "trevor", "program" : "/usr/bin/chrome", "host" : "127.0.0.1", "port" : 8000 }

5. Program receives signal

When a program receives a signal. For example, the "test" program in my Scratch directory got a segfault:

{ "feature" : "program-signal", "user" : "trevor", "program" : "/home/trevor/Scratch/test", "signal" : 11 }

6. Program creates file

When a program creates a file. For example, the user "malice" is creating a file in "/tmp" called "ls":

{ "feature" : "program-creates-file", "user" : "malice", "program" : "touch", "file" : "/tmp/ls" }
