/*
 * CUCUMBER +
 * Copyright (C) 2023  Maxime HAMM - NIMBLY CONSULTING - Maxime.HAMM@nimbly-consulting.com
 *
 * This document is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This work is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package io.nimbly.tzatziki.pdf

import freemarker.cache.StringTemplateLoader
import freemarker.ext.beans.BooleanModel
import freemarker.ext.beans.DateModel
import freemarker.ext.beans.NumberModel
import freemarker.ext.beans.StringModel
import freemarker.template.*
import java.util.*
import java.util.regex.Pattern
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
fun initFreeMarker(
    vararg wrapper: KotlinWrapper<*>): Configuration {

    val version = Configuration.VERSION_2_3_23
    return Configuration(version).apply {

        defaultEncoding = "UTF-8"
        templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
        logTemplateExceptions = false

        val toMap: Map<KClass<*>, KotlinWrapper<Any>> = wrapper.associate { it.kclass to it as KotlinWrapper<Any> }
        objectWrapper = SmartKotlinWrapper(version, toMap, true).apply {
            isAPIBuiltinEnabled = true
        }

        setClassForTemplateLoading(javaClass, "/")
    }
}

fun Configuration.registerTemplates(vararg templates: Pair<String, String?>) {

    val loader = StringTemplateLoader()
    templates.forEach {

        val templateName = it.first
        val template = it.second
            ?: throw Exception("Template '$templateName' is null !")

        loader.putTemplate(templateName, template)
    }
    templateLoader = loader
}

private class SmartKotlinWrapper(
        incompatibleImprovements: Version,
        val wrappers: Map<KClass<*>, KotlinWrapper<Any>>,
        val allEscapeHtml: Boolean) : DefaultObjectWrapper(incompatibleImprovements) {

    override fun wrap(obj: Any?): TemplateModel? {
        if (obj!=null) {
            val block = wrappers[obj::class]
            if (block != null) {
                return SmartKotlinModel(obj, this, allEscapeHtml)
            }
            else if (allEscapeHtml && obj is String) {
                return SimpleScalar(obj.escape())
            }
            else if (allEscapeHtml
                    && obj !is Map<*, *> && obj !is List<*>
                    && obj !is Date && obj !is Number && obj !is Boolean) {
                return SmartKotlinModel(obj, this, allEscapeHtml)
            }
        }
        return super.wrap(obj)
    }

    private class SmartKotlinModel(
            objet: Any,
            wrapper: SmartKotlinWrapper,
            val allEscapeHtml: Boolean
    ): StringModel(objet, wrapper) {

        private val kotlinWrapper = wrapper
        override fun get(key: String?): TemplateModel? {
            if (`object`!=null && key!=null) {

                val wrapper = kotlinWrapper.wrappers[`object`::class]
                if (wrapper != null) {

                    val v = (wrapper.wrapper)(`object`, key)
                    if (v != null) {
                        if (v is String)
                            return SimpleScalar(if (wrapper.escapeForHtml) v.escape() else v)
                        else if (v is Boolean)
                            return BooleanModel(v, kotlinWrapper)
                        else if (v is Number)
                            return NumberModel(v, kotlinWrapper)
                        else if (v is Date)
                            return DateModel(v, kotlinWrapper)
                        else
                            return SmartKotlinModel(v, kotlinWrapper, allEscapeHtml)
                    }
                }
            }

            return super.get(key);
        }

        override fun getAsString(): String {
            val s = super.getAsString()
            return if (allEscapeHtml) s.escape() else s
        }
    }
}

infix fun <T: Any> KClass<*>.wrap(that: (T, String?) -> Any?): KotlinWrapper<T>
    = KotlinWrapper(this, that, true)

infix fun <T: Any> KClass<*>.wrapNoEscape(that: (T, String?) -> Any?): KotlinWrapper<T>
        = KotlinWrapper(this, that, false)

open class KotlinWrapper<T: Any>(
    val kclass: KClass<*>,
    val wrapper: (T, String?) -> Any?,
    val escapeForHtml: Boolean = true) {
}

fun String?.noblank(): String? {
    if (this == null)
        return null
    if (this.isBlank())
        return null
    return this
}

private val CONTENT_HEIGHT_REGEX = Regex("content[0-9]+")

object PictureWrapper : KotlinWrapper<Picture>(
    escapeForHtml = false,
    kclass = Picture::class,
    wrapper = { it: Picture, key: String? ->
        val picture: String?
        if (key?.matches(CONTENT_HEIGHT_REGEX)!!) {
            val height = key.substringAfter("content").toInt()
            picture = it.resized(height = height)
        } else {
            picture = null
        }
        picture
    })

class Picture(
    val name: String,
    val content: String,
    val type: String
)

fun Picture.resized(width: Int? = null, height: Int? = null)
    = scaleSVG(content, height = height)

private val SVG_WIDTH_PATTERN = Pattern.compile("(?s).*<svg[^>]*width\\s*=\\s*\"(\\d*)")
private val SVG_HEIGHT_PATTERN = Pattern.compile("(?s).*<svg[^>]*height\\s*=\\s*\"(\\d*)")
private val WIDTH_ATTR_REGEX = Regex("width\\s*=\\s*\"\\d*")
private val HEIGHT_ATTR_REGEX = Regex("height\\s*=\\s*\"\\d*")
private val SVG_TAG_REGEX = Regex("<svg")

fun scaleSVG(image: String, width: Int? = null, height: Int? = null): String {

    var svgImage = image
    if (width != null) {
        val widthMatcher = SVG_WIDTH_PATTERN.matcher(svgImage)
        if (widthMatcher.find()) {
            val originalWidth = widthMatcher.group(1).toInt()
            svgImage = svgImage.replaceFirst(WIDTH_ATTR_REGEX, "width=\"$width")
            if (height == null) {
                // Must also scale height manually relative to width if explicit height is not set.
                val heightMatcher = SVG_HEIGHT_PATTERN.matcher(svgImage)
                if (heightMatcher.find()) {
                    val originalHeight = heightMatcher.group(1).toInt()
                    val computedScaledHeight = ((originalHeight * width).toDouble() / originalWidth.toDouble()).toInt()
                    svgImage = svgImage.replaceFirst(HEIGHT_ATTR_REGEX, "height=\"$computedScaledHeight")
                }
            }
        } else {
            svgImage = svgImage.replaceFirst(SVG_TAG_REGEX, "<svg width=\"$width\"")
        }
    }
    if (height != null) {
        val heightMatcher = SVG_HEIGHT_PATTERN.matcher(svgImage)
        if (heightMatcher.find()) {
            val originalHeight = heightMatcher.group(1).toInt()
            svgImage = svgImage.replaceFirst(HEIGHT_ATTR_REGEX, "height=\"$height")
            if (width == null) {
                // Must also scale width manually relative to height if explicit width is not set.
                val widthMatcher = SVG_WIDTH_PATTERN.matcher(svgImage)
                if (widthMatcher.find()) {
                    val originalWidth = widthMatcher.group(1).toInt()
                    val computedScaledWidth = ((originalWidth * height).toDouble() / originalHeight.toDouble()).toInt()
                    svgImage = svgImage.replaceFirst(WIDTH_ATTR_REGEX, "width=\"$computedScaledWidth")
                }
            }
        } else {
            svgImage = svgImage.replaceFirst(SVG_TAG_REGEX, "<svg height=\"$height\"")
        }
    }
    return svgImage
}
