/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.8
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.neighbor.vstream;

public class vstreamJNI {
  public final static native long new_VStreamManager();
  public final static native void delete_VStreamManager(long jarg1);
  public final static native int VStreamManager_initSession(long jarg1, VStreamManager jarg1_, String jarg2, String jarg3, int jarg4);
  public final static native void VStreamManager_destroySession(long jarg1, VStreamManager jarg1_);
  public final static native boolean VStreamManager_isConnected(long jarg1, VStreamManager jarg1_);
  public final static native int VStreamManager_sendAudioData(long jarg1, VStreamManager jarg1_, int jarg2, int jarg3, java.math.BigInteger jarg4, byte[] jarg5, int jarg6);
  public final static native int VStreamManager_sendVideoData(long jarg1, VStreamManager jarg1_, boolean jarg2, int jarg3, int jarg4, java.math.BigInteger jarg5, byte[] jarg6, int jarg7);
}
