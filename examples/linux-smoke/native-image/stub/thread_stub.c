/* 只为链接期补符号：AWT 在 Linux 上要 FindClass("java/lang/Thread")，
 * 把 java.lang.Thread 注册进 jni-config 后 native-image 会为它的 native 方法
 * 生成 Java_java_lang_Thread_* 引用，而这两个方法由 VM 自己实现、libjava.a 里没有。
 * 运行期不会走到这里（AWT 只拿 Class 对象，不调这两个方法）。 */
#include <jni.h>

JNIEXPORT jlong JNICALL Java_java_lang_Thread_getNextThreadIdOffset(JNIEnv *env, jclass cls) {
    return 16;
}

JNIEXPORT jobjectArray JNICALL Java_java_lang_Thread_getStackTrace0(JNIEnv *env, jobject self) {
    return NULL;
}
