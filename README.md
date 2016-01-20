# adapt
ADAPT software for Transparent Computing

This integration repo holds references to individual component repos.
Use a command like this to learn how current the component files are:

    git ls-remote https://github.com/GaloisInc/Adapt-classify.git

Update adapt files from a subtree repo with a command like:

    git subtree pull --prefix=classifier https://github.com/GaloisInc/Adapt-classify.git master --squash

or push adapt files to a subtree repo with a command like:

    git subtree push --prefix=classifier https://github.com/GaloisInc/Adapt-classify.git master
