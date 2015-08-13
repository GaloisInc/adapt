/*
 * Log.hpp
 *
 *  Created on: Jul 17, 2015
 *      Author: tadeze
 */
#include "utility.hpp"
#ifndef LOG_HPP_
#define LOG_HPP_

class Log {
public:
	static string fileName;
	static void write(string log);
	static void close();
	Log(string filename="logfile.log");

	virtual ~Log();
};

#endif /* LOG_HPP_ */
