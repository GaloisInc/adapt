
# vendor hashes

Enumerating "known bad" doesn't work, despite the efforts of
the $106 B antivirus industry.

Enumerating "known good" *is* effective at exonerating programs that
might otherwise be suspect. A software vendor puts code through a
release process, and securely communicates hashes of binaries to
sites that choose to run the vendor's code. A signed hash answers the
question "did I mean to run this binary, or did an APT create the binary?"
It directly shows the provenance of a binary is a certain vendor.

Attackers tend to introduce files to an environment and quickly make
use of them. Timestamps can be adjusted. Daily filesystem reports
offer non-forgeable evidence that today's file contents have not
changed since some historic snapshot, and that auditors choose
to trust them.

## weaknesses

Such hashes provide no information about whether the vendor's binary
showed exploitable behavior due to

- poorly chosen local configuration, or
- zero day bug (e.g. buffer overrun), or
- ROP gadgets

For a site to assure its security it must perform a few tasks at a
bare minimum, including computing the integrity of installed binaries.
That will be just a small part of the necessary more comprehensive approach.

## dynamic files

Most files on disk today were there a week ago, and a month ago.
Vendor hashes are an appropriate means of making "stable" files tamper-evident.
They are *not* a good match for "recent" files created today.
TA1 might race with the attacker while trying to compute hashes,
or might spend many extra CPU cycles gathering evidence inline.

## availability

Some vendors routinely provide signed hashes along with their binaries.
For those that do not, a site with spare hosts can readily obtain hashes.

Do a fresh OS install on sandbox host1, record hashes, install a software application,
record hashes again, and put the newly introduced ones into a candidate set S1.
On host2 similarly record hashes, install, record hashes again, extract the new ones as S2.
The intersection of S1, S2 is the set of hashes we'd expect to see on other hosts
that are similarly running the app. Details peculiar to host1 were removed by
incorporating the host2 observations.

It is already routine for IT departments to perform such test installs of important apps,
to determine they function appropriately, before rolling them out to production.
