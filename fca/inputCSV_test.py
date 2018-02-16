import tests

proc_event_cadets_bovia=input('Path to CSV file generated from Cadets Bovia data using neo4jspec_ProcessEvent.json\n')
r1=tests.test_inputfiles(proc_event_cadets_bovia,'./explore/contextSpecFiles/neo4jspec_ProcessEvent.csv')

if r1==0:
	print('\nFIRST TEST PASSED. Starting second test')
	proc_byIPPort_cadets_bovia=input('Path to CSV file generated from Cadets Bovia data using neo4jspec_ProcessByIPPort.json\n')
	r2=tests.test_inputfiles(proc_byIPPort_cadets_bovia,'./explore/contextSpecFiles/neo4jspec_ProcessByIPPort.csv')
	if r2==0:
		print('\nSECOND TEST PASSED')
