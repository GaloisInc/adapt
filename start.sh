#!/bin/sh

#gnome-terminal --title=UI -e "python ui/fake-ui.py"
#gnome-terminal --title="Diagnostic Engine" -e "python dx/fake-diagnose.py"
#gnome-terminal --title=Prioritizer -e "python dx/fake-prioritizer.py"
#gnome-terminal --title=Classifiers -e "python classifier/fake-classifier.py"
#gnome-terminal --title="Anomoly Detector" -e "python ad/fake-ad.py"
#gnome-terminal --title="Feature Extractor" -e "python featureExtractor/fake-fe.py"
#gnome-terminal --title=Segmenter -e "python segment/fake-segment.py"
#gnome-terminal --title=Ingester -e "python ingest/fake-ingest.py"

tmux new-session -s adapt -n "ADAPT" -d 'python ui/fake-ui.py'
tmux split-window -p 87 -t adapt 'python dx/fake-diagnose.py'
tmux split-window -p 86 -t adapt "python dx/fake-prioritizer.py"
tmux split-window -p 83 -t adapt "python classifier/fake-classifier.py"
tmux split-window -p 80 -t adapt "python ad/fake-ad.py"
tmux split-window -p 75 -t adapt "python featureExtractor/fake-fe.py"
tmux split-window -p 66 -t adapt "python segment/fake-segment.py"
tmux split-window -p 50 -t adapt "python ingest/fake-ingest.py"
tmux attach
