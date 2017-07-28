
Scenario Fragment  - Bad 'ls'

This scenario is not designed to illustrate all of the phases of an APT campaign; it is a more of a "scenario fragment" that Trevor designed to inform our early experiments with processing PGs (feature extraction, mainly)

---

1. Assume that the machine has been compromised through means that aren't really relevant.
2. Using the compromised user, install a script called `ls` into `/tmp`. This script will copy out the ssh server's private key to an attacker managed host when run.
3. Using the root user, make sure that their path includes the current directory `.`, and change to `/tmp` and run `ls`.

This gives us some good options for establishing normal use, as well as a few hints on good features to define. Normal use could be just using ssh and running programs like `ls`.

From this, we can define two features to start with:

1. A user, and the full path to a program that they have run
2. A user, a file, and a program that's using the file

Each feature can be used to establish anomalies in the example defined above, as the root user should not be running programs out of /tmp, and the ssh server's private key should not be used with anything other than the ssh daemon. I think that both features establish a sort of a learned policy, as we had talked about yesterday.
