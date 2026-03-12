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

package io.nimbly.tzatziki.preferences

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import io.nimbly.tzatziki.TOGGLE_CUCUMBER_PL
import javax.swing.JComponent
import javax.swing.JPanel

private const val ENABLED_KEY = "io.nimbly.tzatziki.enabled"
private const val PROGRESSION_KEY = "io.nimbly.tzatziki.execution.ShowProgressionGuides"

class CucumberPlusOptionsConfigurable : SearchableConfigurable, Configurable.NoScroll {

    private var enableCucumberPlus: JBCheckBox? = null
    private var showProgressionGuides: JBCheckBox? = null
    private var mainPanel: JPanel? = null

    override fun getId(): String = "io.nimbly.tzatziki.preferences"

    override fun getDisplayName(): String = "Cucumber+"

    override fun createComponent(): JComponent {
        if (mainPanel != null)
            return mainPanel!!

        enableCucumberPlus = JBCheckBox("Enable Cucumber+ table editing")
        showProgressionGuides = JBCheckBox("Show execution progression guides")

        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(enableCucumberPlus!!)
            .addComponent(showProgressionGuides!!)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        reset()
        return mainPanel!!
    }

    override fun isModified(): Boolean {
        val props = PropertiesComponent.getInstance()
        return enableCucumberPlus?.isSelected != props.getBoolean(ENABLED_KEY, true) ||
                showProgressionGuides?.isSelected != props.getBoolean(PROGRESSION_KEY, true)
    }

    override fun apply() {
        val props = PropertiesComponent.getInstance()
        val enabled = enableCucumberPlus?.isSelected ?: true
        val progression = showProgressionGuides?.isSelected ?: true

        props.setValue(ENABLED_KEY, enabled, true)
        props.setValue(PROGRESSION_KEY, progression, true)
        TOGGLE_CUCUMBER_PL = enabled
    }

    override fun reset() {
        val props = PropertiesComponent.getInstance()
        enableCucumberPlus?.isSelected = props.getBoolean(ENABLED_KEY, true)
        showProgressionGuides?.isSelected = props.getBoolean(PROGRESSION_KEY, true)
    }
}
