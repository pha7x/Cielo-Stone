/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: A:\\dumps\\android\\ANDROID_CIELO_STONE\\TestFrameworkSrc\\app\\src\\main\\aidl\\com\\xcheng\\printerservice\\IPrinterService.aidl
 */
package com.xcheng.printerservice;
// Declare any non-default types here with import statements

public interface IPrinterService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.xcheng.printerservice.IPrinterService
{
private static final java.lang.String DESCRIPTOR = "com.xcheng.printerservice.IPrinterService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.xcheng.printerservice.IPrinterService interface,
 * generating a proxy if needed.
 */
public static com.xcheng.printerservice.IPrinterService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.xcheng.printerservice.IPrinterService))) {
return ((com.xcheng.printerservice.IPrinterService)iin);
}
return new com.xcheng.printerservice.IPrinterService.Stub.Proxy(obj);
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
case TRANSACTION_upgradePrinter:
{
data.enforceInterface(descriptor);
this.upgradePrinter();
reply.writeNoException();
return true;
}
case TRANSACTION_getFirmwareVersion:
{
data.enforceInterface(descriptor);
java.lang.String _result = this.getFirmwareVersion();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getBootloaderVersion:
{
data.enforceInterface(descriptor);
java.lang.String _result = this.getBootloaderVersion();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_printerInit:
{
data.enforceInterface(descriptor);
com.xcheng.printerservice.IPrinterCallback _arg0;
_arg0 = com.xcheng.printerservice.IPrinterCallback.Stub.asInterface(data.readStrongBinder());
this.printerInit(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_printerReset:
{
data.enforceInterface(descriptor);
com.xcheng.printerservice.IPrinterCallback _arg0;
_arg0 = com.xcheng.printerservice.IPrinterCallback.Stub.asInterface(data.readStrongBinder());
this.printerReset(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_printWrapPaper:
{
data.enforceInterface(descriptor);
int _arg0;
_arg0 = data.readInt();
com.xcheng.printerservice.IPrinterCallback _arg1;
_arg1 = com.xcheng.printerservice.IPrinterCallback.Stub.asInterface(data.readStrongBinder());
this.printWrapPaper(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_printText:
{
data.enforceInterface(descriptor);
java.lang.String _arg0;
_arg0 = data.readString();
com.xcheng.printerservice.IPrinterCallback _arg1;
_arg1 = com.xcheng.printerservice.IPrinterCallback.Stub.asInterface(data.readStrongBinder());
this.printText(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_printTextWithAttributes:
{
data.enforceInterface(descriptor);
java.lang.String _arg0;
_arg0 = data.readString();
java.util.Map _arg1;
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_arg1 = data.readHashMap(cl);
com.xcheng.printerservice.IPrinterCallback _arg2;
_arg2 = com.xcheng.printerservice.IPrinterCallback.Stub.asInterface(data.readStrongBinder());
this.printTextWithAttributes(_arg0, _arg1, _arg2);
reply.writeNoException();
return true;
}
case TRANSACTION_printColumnsTextWithAttributes:
{
data.enforceInterface(descriptor);
java.lang.String[] _arg0;
_arg0 = data.createStringArray();
java.util.List _arg1;
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_arg1 = data.readArrayList(cl);
com.xcheng.printerservice.IPrinterCallback _arg2;
_arg2 = com.xcheng.printerservice.IPrinterCallback.Stub.asInterface(data.readStrongBinder());
this.printColumnsTextWithAttributes(_arg0, _arg1, _arg2);
reply.writeNoException();
return true;
}
case TRANSACTION_printBitmap:
{
data.enforceInterface(descriptor);
android.graphics.Bitmap _arg0;
if ((0!=data.readInt())) {
_arg0 = android.graphics.Bitmap.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
com.xcheng.printerservice.IPrinterCallback _arg1;
_arg1 = com.xcheng.printerservice.IPrinterCallback.Stub.asInterface(data.readStrongBinder());
this.printBitmap(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_printBitmapWithAttributes:
{
data.enforceInterface(descriptor);
android.graphics.Bitmap _arg0;
if ((0!=data.readInt())) {
_arg0 = android.graphics.Bitmap.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
java.util.Map _arg1;
java.lang.ClassLoader cl = (java.lang.ClassLoader)this.getClass().getClassLoader();
_arg1 = data.readHashMap(cl);
com.xcheng.printerservice.IPrinterCallback _arg2;
_arg2 = com.xcheng.printerservice.IPrinterCallback.Stub.asInterface(data.readStrongBinder());
this.printBitmapWithAttributes(_arg0, _arg1, _arg2);
reply.writeNoException();
return true;
}
case TRANSACTION_printBarCode:
{
data.enforceInterface(descriptor);
java.lang.String _arg0;
_arg0 = data.readString();
int _arg1;
_arg1 = data.readInt();
int _arg2;
_arg2 = data.readInt();
int _arg3;
_arg3 = data.readInt();
boolean _arg4;
_arg4 = (0!=data.readInt());
com.xcheng.printerservice.IPrinterCallback _arg5;
_arg5 = com.xcheng.printerservice.IPrinterCallback.Stub.asInterface(data.readStrongBinder());
this.printBarCode(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5);
reply.writeNoException();
return true;
}
case TRANSACTION_printQRCode:
{
data.enforceInterface(descriptor);
java.lang.String _arg0;
_arg0 = data.readString();
int _arg1;
_arg1 = data.readInt();
int _arg2;
_arg2 = data.readInt();
com.xcheng.printerservice.IPrinterCallback _arg3;
_arg3 = com.xcheng.printerservice.IPrinterCallback.Stub.asInterface(data.readStrongBinder());
this.printQRCode(_arg0, _arg1, _arg2, _arg3);
reply.writeNoException();
return true;
}
case TRANSACTION_setPrinterSpeed:
{
data.enforceInterface(descriptor);
int _arg0;
_arg0 = data.readInt();
com.xcheng.printerservice.IPrinterCallback _arg1;
_arg1 = com.xcheng.printerservice.IPrinterCallback.Stub.asInterface(data.readStrongBinder());
this.setPrinterSpeed(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_sendRAWData:
{
data.enforceInterface(descriptor);
byte[] _arg0;
_arg0 = data.createByteArray();
com.xcheng.printerservice.IPrinterCallback _arg1;
_arg1 = com.xcheng.printerservice.IPrinterCallback.Stub.asInterface(data.readStrongBinder());
this.sendRAWData(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_printerTemperature:
{
data.enforceInterface(descriptor);
com.xcheng.printerservice.IPrinterCallback _arg0;
_arg0 = com.xcheng.printerservice.IPrinterCallback.Stub.asInterface(data.readStrongBinder());
int _result = this.printerTemperature(_arg0);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_printerPaper:
{
data.enforceInterface(descriptor);
com.xcheng.printerservice.IPrinterCallback _arg0;
_arg0 = com.xcheng.printerservice.IPrinterCallback.Stub.asInterface(data.readStrongBinder());
boolean _result = this.printerPaper(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
default:
{
return super.onTransact(code, data, reply, flags);
}
}
}
private static class Proxy implements com.xcheng.printerservice.IPrinterService
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
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
@Override public void upgradePrinter() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_upgradePrinter, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public java.lang.String getFirmwareVersion() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getFirmwareVersion, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.lang.String getBootloaderVersion() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getBootloaderVersion, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void printerInit(com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_printerInit, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void printerReset(com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_printerReset, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void printWrapPaper(int n, com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(n);
_data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_printWrapPaper, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void printText(java.lang.String text, com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(text);
_data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_printText, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void printTextWithAttributes(java.lang.String text, java.util.Map attributes, com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(text);
_data.writeMap(attributes);
_data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_printTextWithAttributes, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void printColumnsTextWithAttributes(java.lang.String[] text, java.util.List attributes, com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStringArray(text);
_data.writeList(attributes);
_data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_printColumnsTextWithAttributes, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void printBitmap(android.graphics.Bitmap bitmap, com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((bitmap!=null)) {
_data.writeInt(1);
bitmap.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
_data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_printBitmap, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void printBitmapWithAttributes(android.graphics.Bitmap bitmap, java.util.Map attributes, com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((bitmap!=null)) {
_data.writeInt(1);
bitmap.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
_data.writeMap(attributes);
_data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_printBitmapWithAttributes, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void printBarCode(java.lang.String content, int align, int width, int height, boolean showContent, com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(content);
_data.writeInt(align);
_data.writeInt(width);
_data.writeInt(height);
_data.writeInt(((showContent)?(1):(0)));
_data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_printBarCode, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void printQRCode(java.lang.String text, int align, int size, com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(text);
_data.writeInt(align);
_data.writeInt(size);
_data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_printQRCode, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void setPrinterSpeed(int level, com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(level);
_data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_setPrinterSpeed, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void sendRAWData(byte[] data, com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeByteArray(data);
_data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_sendRAWData, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public int printerTemperature(com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_printerTemperature, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean printerPaper(com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_printerPaper, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_upgradePrinter = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_getFirmwareVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_getBootloaderVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_printerInit = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_printerReset = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_printWrapPaper = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_printText = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_printTextWithAttributes = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_printColumnsTextWithAttributes = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_printBitmap = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
static final int TRANSACTION_printBitmapWithAttributes = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
static final int TRANSACTION_printBarCode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
static final int TRANSACTION_printQRCode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
static final int TRANSACTION_setPrinterSpeed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
static final int TRANSACTION_sendRAWData = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
static final int TRANSACTION_printerTemperature = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
static final int TRANSACTION_printerPaper = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
}
/**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
public void upgradePrinter() throws android.os.RemoteException;
public java.lang.String getFirmwareVersion() throws android.os.RemoteException;
public java.lang.String getBootloaderVersion() throws android.os.RemoteException;
public void printerInit(com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException;
public void printerReset(com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException;
public void printWrapPaper(int n, com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException;
public void printText(java.lang.String text, com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException;
public void printTextWithAttributes(java.lang.String text, java.util.Map attributes, com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException;
public void printColumnsTextWithAttributes(java.lang.String[] text, java.util.List attributes, com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException;
public void printBitmap(android.graphics.Bitmap bitmap, com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException;
public void printBitmapWithAttributes(android.graphics.Bitmap bitmap, java.util.Map attributes, com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException;
public void printBarCode(java.lang.String content, int align, int width, int height, boolean showContent, com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException;
public void printQRCode(java.lang.String text, int align, int size, com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException;
public void setPrinterSpeed(int level, com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException;
public void sendRAWData(byte[] data, com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException;
public int printerTemperature(com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException;
public boolean printerPaper(com.xcheng.printerservice.IPrinterCallback callback) throws android.os.RemoteException;
}
