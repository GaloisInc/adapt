
The `bad_ls` provenance graph is comprised of three parts:

- an SRI SPADE trace
- AIDE / tripwire hashes gathered by TA1 on the Monitored Host
- hashes from a vendor's software release process (ubuntu, here)


A `make clean all` completes in less than twenty seconds.
Before that you should have already started `supervisord`,
perhaps with `(cd ../titan && make big)`.
