package com.example.instagramblocker.accessibility

/** Identifica o único pacote que o serviço está autorizado a bloquear. */
object InstagramTarget {
    const val PACKAGE_NAME = "com.instagram.android"

    fun matches(packageName: CharSequence?): Boolean =
        packageName?.toString() == PACKAGE_NAME
}
