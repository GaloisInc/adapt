#!/bin/bash

ssh -L8080:localhost:8081 gw.tc.bbn.com 'ssh -L8081:localhost:8180 ta2-adapt-1.tc.bbn.com -l vagrant'
