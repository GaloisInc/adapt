function install_ingest() {
    if [ ! \( -e $USER_BIN/Trint \) ]
    then
        cd ingest/Trint ; stack install
    fi
}
