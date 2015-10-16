# QuickStart

To build a cabal package you can either use stack or cabal as below.  These
should be executed from directories containing Haskell packages (see Ingest,
Trint).

```
$ cabal install
OR
$ stack install
```

After building trint the executable, `trint`, can parse, lint and graph properly
formatted prov-n files.  What is a properly formated Prov-N file?  See
(the language markdown)[Language.md] for a specification that is surely not
going to remain up-to-date but hopefully be in the vein.
