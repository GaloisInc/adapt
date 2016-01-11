#!/bin/sh

# Assume the adapt repo is cwd!
ADAPT=$HOME/adapt
supercfg=$ADAPT/config/supervisord.conf


# run supervisord (zookeeper kafka, gremlin)
tmux new-session -s adapt -n "ADAPT" -d "supervisord -c $supercfg --nodaemon"

# kafka and zookeeper are frustratingly slow and some of the helper
# scripts do not fail or retry well.
sleep 5

# Now start Adapt stuff
tmux split-window -p 87 -t adapt "$HOME/.local/bin/Trint -u $ADAPT/example/infoleak-small.provn"
tmux split-window -p 20 -t adapt "while true ; do sleep 5 ; curl -s -X POST -d \"{ \\\"gremlin\\\" : \\\"g.V().count()\\\" }\" \"http://localhost:8182\" | jq '.result.data' ; done "

# tmux new-session -s adapt -n "ADAPT" -d 'python ui/fake-ui.py'
# tmux split-window -p 87 -t adapt 'python dx/fake-diagnose.py'
# tmux split-window -p 86 -t adapt "python dx/fake-prioritizer.py"
# tmux split-window -p 83 -t adapt "python classifier/fake-classifier.py"
# tmux split-window -p 80 -t adapt "python ad/fake-ad.py"
# tmux split-window -p 75 -t adapt "python featureExtractor/fake-fe.py"
# tmux split-window -p 66 -t adapt "python segment/fake-segment.py"
# tmux split-window -p 50 -t adapt "python ingest/fake-ingest.py"
tmux attach
