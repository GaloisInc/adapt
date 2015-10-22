This document specifies the Prov-N language dielect proposed by the Adapt team
for communication between technical areas one and two.  Below, we exhaustively
cover the set of legal statements and their syntax.

The tokens accepted by the parser include:

* Raw strings (IDENT): alphanumeric strings starting with a letter.
* Integers (NUMLIT): Base 10 numbers.
* Quoted Strings (STRINGLIT): Double-quoted strings with any embedded double-quotes escaped via a backslash.
* Time (TIME): ISO-8601 time format in Zulu (Z).
* Universal Resource Identifiers (URILIT): Commonly known as URIs, such as web addresses.

The keywords include:

> document endDocument prefix activity agent wasAssociatedWith entity used
> wasStartedBy wasGeneratedBy wasEndedBy wasInformedBy wasAttributedTo
> wasDerivedFrom actedOnBehalfOf wasInvalidatedBy description isPartOf atTime
> args returnVal operation read recv accept execute

The symbols include

> '%%' '=' ':' ',' '(' ')' '[' ']' '-' ';' '\''

Identifiers are any string starting with a latin character and including
characters, numbers, underscores, and optionally a single colon (for qualified
names).  These are sometimes called scoped names.

```
ident :: { Ident }
  : IDENT ':' IDENT
  | ':' IDENT
  | IDENT
```

Prov-N, which is a super-set of the language described here, start with a
'document' string, include a list of prefixes and expressions, and end with
either 'endDocument' or 'end document'.

```
prov :: { Prov }
  : 'document' list(prefix) list(expr) 'endDocument'
```

A prefix is a method to define a qualified name for an otherwise unweildy URI.
The URI should be valid and provide any visitor with an ontology.

```
prefix :: { Prefix }
  : 'prefix' ident URILIT
```

Valid expressions are a sub-set of Prov-N and Dublin Core.

```
expr :: { Expr }
  : provExpr            { $1 }
  | dcExpr              { $1 }

provExpr :: { Expr }
  : entity
  | activity
  | generation
  | usage
  | start
  | end
  | invalidation
  | communication
  | agent
  | association
  | attribution
  | delegation
  | derivation

dcExpr :: { Expr }
  : isPartOf
  | description
```

The entity predicate is valid for use with either the resource or artifact
types, where resources are an enumerated list of devices typical of mobile
phones and artifacts are common communication idioms including  file, packet,
and memory.  The attribute list included with an entity must include an
'prov:artifactType' field with value of 'adapt:artifact' or 'adapt:resource'.
Resources must include a 'devType' in their attributes as well.

TODO: adapt:resource is not in the model diagram.

```
entity          :: { Expr }
  : 'entity' '(' ident ',' attrs(attrVals) ')'

devType
  : 'gps'
  | 'camera'
  | 'keyboard'
  | 'accelerometer'
  | 'microphone'
```

Activity is used to represent units of execution, may include a start time and
end time and an attribute list.

```
activity        :: { Expr }
  : 'activity' '(' ident ',' may(time) ',' may(time) soattrVals ')'
  | 'activity' '(' ident soattrVals ')'
```

Artifact creation is communicated with the 'wasGeneratedBy' predicate. Unlike
in Prov-N, we require inclusion of the time field in 'wasGeneratedBy' as well as
in 'used', 'wasStartedBy', and 'wasEndedBy' predicates.

```
generation      :: { Expr }
  : 'wasGeneratedBy' '(' ident ',' may(ident) ',' time  optAttrs(generationAttr) ')'

generationAttr
 : 'genOp'    '=' genOp
 | 'modVec'   '=' NUM

genOp
 : 'write'
 | 'send'
 | 'connect'
 | 'truncate'
 | 'chmod'
 | 'touch'
```

The 'used' predicate is invalid without the 'useOp' attribute.

```
usage           :: { Expr }
  : 'used' '(' ident ',' may(ident) ',' time ',' attrs(usageAttr) ')'

usageAttr :: { (Key,Value) }
  : 'atTime'    '=' time
  | 'args'      '=' STRINGLIT
  | 'returnVal' '=' STRINGLIT
  | 'operation' '=' '\'' useOp '\''

useOp :: { Value }
  : 'read'                      { ValIdent adaptRead    }
  | 'recv'                      { ValIdent adaptRecv    }
  | 'accept'                    { ValIdent adaptAccept  }
  | 'execute'                   { ValIdent adaptExecute }
```

Start and end predicates require the time field, but are otherwise identical to
their Prov-N counterparts.

N.B. Most predicates in Prov-N can be assigned an optional identifier,
syntactically 'predicate( [ident1 ;]? .. other args ...)'. In the following
these predicates are shown without the identifier because the presented
conceptual model does not allow additional references, such as 'description's,
to refer to predicates - thus rendering any predicate identification moot.

```
start           :: { Expr }
  : 'wasStartedBy' '(' ident ',' may(ident) ',' may(ident) ',' time soattrVals ')'

end             :: { Expr }
  : 'wasEndedBy' '(' ident ',' may(ident) ',' may(ident) ',' time soattrVals ')'
```

All inter-activity communication (conceptual, communication between two units of
execution) is described by one of a set of enumerated identifiers including
fork, clone, execve, kill, and setuid.

```
communication   :: { Expr }
  : 'wasInformedBy' '(' ident ',' ident optAttrs(informedAttr) ')'
  | 'wasInformedBy' '(' ident soattrVals ')'

informedAttr :: { (Key,Value) }
  : 'atTime'    '=' time
  | 'useOp' '=' '\'' execOp '\''

execOp :: { Value }
  : 'fork'
  | 'clone'
  | 'execve'
  | 'kill'
  | 'setuid'
```

Agents remain an abstract concept and are legitimate for describing anything
from a user to a process or even a threat.

```
agent           :: { Expr }
  : 'agent' '(' ident attrs(agentAttr) ')'

agentAttr     :: { (Key,Value) }
  : 'uniqueID' '=' uuid
  | 'foaf:name' '=' STRINGLIT
  | 'foaf:accountName' '=' STRINGLIT
  | 'machineID'  '=' uuid

uuid
 : STRINGLIT
```

Units of execution can be associated with agents using the prov
'wasAssociatedWith' marking.  The three identifiers are, in order, activity
(unit of execution), agent, and as-yet unspecified plan identifier.

```
association     :: { Expr }
  : 'wasAssociatedWith' '(' ident ',' ident ',' may(ident) ')'
```

Attribution of artifacts to agents or activities follows naturally from Prov-N.

```
attribution     :: { Expr }
  : 'wasAttributedTo' '(' ident ',' ident ')'
```

Invalidation of an artifact by an activity requires a time.

```
invalidation    :: { Expr }
  : 'wasInvalidatedBy' '(' ident ',' ident ',' time ')'
```

As in Prov, either through analysis or direct observation, activities can be
said to act on behalf of agents.

```
delegation      :: { Expr }
  : 'actedOnBehalfOf' '(' ident ',' ident ')'
```

Similarly, artifacts can be derived from other artifacts.   Both the subject and
the object must be artifact, this should not be used for entities of prov:type
resource.

```
derivation      :: { Expr }
  | 'wasDerivedFrom' '(' ident ',' ident optAttrs(derivationAttr) ')'

deriviationAttr
  : 'prov:atTime' '=' time
  | 'deriveOp'    '=' deriveOp

deriveOp
 : 'rename'
 | 'link'
```

The is-part-of and description fields remain sketches but, as with the entirety
of the language including all attributes and enumerations, are expected to grow
to meet the needs of the project.


```
isPartOf        :: { Expr}
  : 'isPartOf'  '(' ident ',' ident ')'

description     :: { Expr }
  : 'description' '(' ident soattrVals ')'
```

Some optional fields can use a marker (hyphen) instead of an identifier or other
literal.  Notice not all fields optional in Prov-N remain optional in this
language.

```
may(p)
  : '-'
  | p
```

Optional attribute fields are fields that might be entirely non-existant.

```
optAttrs(avParse)
  : ',' attrs(avParse)
  | {- empty -}

attrs(avParse)
  : '[' sep(',', avParse) ']'        { $2 }
```

Comma-separated option attribute values, 'soattrVals' in the above descriptions,
are a sign of a portion of the syntax that remains under-specified and are
represented by a catch-all that parses generic key-value pairs.

```
soattrVals :: { [(Key,Value)] }
  : ',' oattrVals
  | {- empty -}

oattrVals :: { [(Key,Value)] }
  : '['  attrVals ']'

attrVals :: { [(Key,Value)] }
  : sep(',', attrVal)

attrVal :: { (Key,Value) }
  : ident '=' literal
```
