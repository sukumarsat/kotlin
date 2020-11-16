/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.caching

import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.isAdvanced
import java.lang.reflect.Proxy
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

typealias CachedCompilerArgumentBySourceSet = Map<String, CachedArgsInfo>
typealias FlatCompilerArgumentBySourceSet = Map<String, FlatArgsInfo>

const val ARGUMENT_ANNOTATION_CLASS = "org.jetbrains.kotlin.cli.common.arguments.Argument"
const val COMMON_ARGUMENTS_CLASS = "org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments"
const val JVM_ARGUMENTS_CLASS = "org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments"
const val METADATA_ARGUMENTS_CLASS = "org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments"

fun obtainArgumentAnnotation(classLoader: ClassLoader?, className: String, propertyName: String): Argument? {
    val (kClazz, argumentKClazz) = try {
        Class.forName(className, false, classLoader).kotlin to Class.forName(ARGUMENT_ANNOTATION_CLASS, false, classLoader).kotlin
    } catch (e: ClassNotFoundException) {
        //It can be old version mpp gradle plugin. Supported only 1.4+
        return null
    }

    val requiredProperty = kClazz.declaredMemberProperties.find { it.name == propertyName }
    val argumentAnnotation = requiredProperty?.annotations?.filter { Proxy.isProxyClass(it.javaClass) }
    return argumentAnnotation as? Argument
}

/**
 * Creates deep copy in order to avoid holding links to Proxy objects created by gradle tooling api
 */
fun CachedCompilerArgumentBySourceSet.deepCopy(): CachedCompilerArgumentBySourceSet =
    entries.associate { it.key to CachedArgsInfoImpl(it.value) }

fun FlatCompilerArgumentsBucket.extractSingleGeneralArgumentOrNull(
    classLoader: ClassLoader, className: String, methodName: String
): String? {
    val propertyArgument = obtainArgumentAnnotation(classLoader, className, methodName) ?: return null
    val propertyName = propertyArgument.value
    return if (propertyArgument.isAdvanced) {
        generalArguments.firstOrNull { it.startsWith(propertyName) }?.removePrefix("${propertyName}=")
    } else {
        generalArguments.indexOf(propertyName).takeIf { it != -1 }?.let { generalArguments.getOrNull(it + 1) }
    }
}

fun FlatCompilerArgumentsBucket.setSingleGeneralArgument(
    classLoader: ClassLoader,
    className: String,
    propertyName: String,
    newValue: String?
) {
    val propertyArgument = obtainArgumentAnnotation(classLoader, className, propertyName) ?: return
    val argumentName = propertyArgument.value
    if (propertyArgument.isAdvanced) {
        generalArguments.removeAll { it.startsWith(argumentName) }
        newValue?.let { "${argumentName}=$it" }?.also { generalArguments.add(it) }
    } else {
        generalArguments.indexOfFirst { it == argumentName }.takeIf { it != -1 }
            ?.let { listOf(generalArguments[it], generalArguments[it + 1]) }
            ?.also { generalArguments.removeAll(it) }
        newValue?.also {
            generalArguments.apply {
                add(argumentName)
                add(it)
            }
        }
    }
}

fun FlatCompilerArgumentsBucket.extractGeneralFlag(
    classLoader: ClassLoader,
    className: String,
    propertyName: String
): Boolean = obtainArgumentAnnotation(classLoader, className, propertyName)?.value in generalArguments

fun <T : CommonToolArguments> FlatCompilerArgumentsBucket.setGeneralFlag(
    classLoader: ClassLoader,
    className: String,
    propertyName: String,
    newValue: Boolean
) {
    val propertyArgument = obtainArgumentAnnotation(classLoader, className, propertyName) ?: return
    val propertyArgumentName = propertyArgument.value
    if (newValue && propertyArgumentName !in generalArguments) generalArguments.add(propertyName)
    if (!newValue) generalArguments.remove(propertyArgumentName)
}

fun FlatCompilerArgumentsBucket.extractArrayGeneralArgument(
    classLoader: ClassLoader,
    className: String,
    propertyName: String
): Array<String>? {
    val delimeter = obtainArgumentAnnotation(classLoader, className, propertyName)?.delimiter ?: return null
    return extractSingleGeneralArgumentOrNull(classLoader, className, propertyName)
        ?.split(delimeter)
        ?.toTypedArray()
}


fun <T : CommonToolArguments> FlatCompilerArgumentsBucket.setArrayGeneralArgument(
    classLoader: ClassLoader,
    className: String,
    propertyName: String,
    newValue: Array<String>?
) {
    val delimeter = obtainArgumentAnnotation(classLoader, className, propertyName)?.delimiter ?: return
    val joined = newValue?.joinToString(delimeter)
    setSingleGeneralArgument(classLoader, className, propertyName, joined)
}
