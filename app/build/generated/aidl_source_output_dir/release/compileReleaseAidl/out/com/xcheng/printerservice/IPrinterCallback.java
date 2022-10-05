/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: H:\\SVN_FILES\\ANDROID_COMMIT\\ANDROID_CIELO_STONE\\TestFrameworkSrc\\app\\src\\main\\aidl\\com\\xcheng\\printerservice\\IPrinterCallback.aidl
 */
package com.xcheng.printerservice;
// Declare any non-default types here with import statements

public interface IPrinterCallback extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.xcheng.printerservice.IPrinterCallback
{
private static final java.lang.String DESCRIPTOR = "com.xcheng.printerservice.IPrinterCallback";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.xcheng.printerservice.IPrinterCallback interface,
 * generating a proxy if needed.
 */
public static com.xcheng.printerservice.IPrinterCallback asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.xcheng.printerservice.IPrinterCallback))) {
return ((com.xcheng.printerservice.IPrinterCallback)iin);
}
return new com.xcheng.printerservice.IPrinterCallback.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
java.lang.String descriptor = DESCRIPTOR;
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(descriptor);
return true;
}
case TRANSACTION_onException:
{
data.enforceInterface(descriptor);
int _arg0;
_arg0 = data.readInt();
java.lang.String _arg1;
_arg1 = data.readString();
this.onException(_arg0, _arg1);
return true;
}
default:
{
return super.onTransact(code, data, reply, flags);
}
}
}
private static class Proxy implements com.xcheng.printerservice.IPrinterCallback
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
/**
	 * 执行发生异常
	 * code： 异常代码
	 * msg:	 异常描述
	 */
@Override public void onException(int code, java.lang.String msg) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(code);
_data.writeString(msg);
mRemote.transact(Stub.TRANSACTION_onException, _data, null, android.os.IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
}
static final int TRANSACTION_onException = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
}
/**
	 * 执行发生异常
	 * code： 异常代码
	 * msg:	 异常描述
	 */
public void onException(int code, java.lang.String msg) throws android.os.RemoteException;
}
