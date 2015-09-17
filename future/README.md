# QuickStart

To build a cabal package you can either use stack or cabal to:

```
$ cabal install
OR
$ stack install
```

From here, the REPL allows simple, manual, interaction with the ingestor:

```
$ stack exec ghci
> import Ingest
> import Graph
> ts <- readTriplesFromFile "example.raw"
> writeFile "example2.dot" (graph ts)
> import Types
> Data.Text.IO.putStrLn $ render ts
```
