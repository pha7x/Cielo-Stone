// IPrinterCallback.aidl
package com.xcheng.printerservice;

// Declare any non-default types here with import statements

interface IPrinterCallback {
	/**
	 * 执行发生异常
	 * code： 异常代码
	 * msg:	 异常描述
	 */
	oneway void  onException(int code, String msg);
}
