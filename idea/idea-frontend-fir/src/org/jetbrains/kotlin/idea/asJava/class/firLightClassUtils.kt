/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava.`class`

import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.shouldNotBeVisibleAsLightClass
import org.jetbrains.kotlin.idea.asJava.FirLightClassForSymbol
import org.jetbrains.kotlin.idea.asJava.fields.FirLightFieldForEnumEntry
import org.jetbrains.kotlin.idea.frontend.api.analyze
import org.jetbrains.kotlin.idea.util.ifTrue
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.containingClass

private fun lightClassForEnumEntry(ktEnumEntry: KtEnumEntry): KtLightClass? {
    if (ktEnumEntry.body == null) return null

    val firClass = ktEnumEntry
        .containingClass()
        ?.tryCreateLightClass()
        as? FirLightClassForSymbol
        ?: return null

    val targetField = firClass.ownFields
        .firstOrNull { it is FirLightFieldForEnumEntry && it.kotlinOrigin == ktEnumEntry }
        ?: return null

    return (targetField as? FirLightFieldForEnumEntry)?.initializingClass as? KtLightClass
}

private fun KtClassOrObject.isSupportedByFirLightClasses() =
    containingFile.let { it is KtFile && !it.isCompiled } &&
            !isLocal /*TODO*/ &&
            !shouldNotBeVisibleAsLightClass()

fun KtClassOrObject.tryCreateLightClass(): KtLightClass? = isSupportedByFirLightClasses().ifTrue {
    when (this) {
        is KtEnumEntry -> lightClassForEnumEntry(this)
        else -> analyze(this) { FirLightClassForSymbol(getClassOrObjectSymbol(), manager) }
    }
}
