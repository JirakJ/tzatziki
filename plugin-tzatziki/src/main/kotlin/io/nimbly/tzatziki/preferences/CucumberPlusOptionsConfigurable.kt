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
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import io.nimbly.tzatziki.TOGGLE_CUCUMBER_PL
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JPanel

private const val PREFIX = "io.nimbly.tzatziki."
private const val ENABLED_KEY = PREFIX + "enabled"
private const val PROGRESSION_KEY = PREFIX + "execution.ShowProgressionGuides"

// PDF Export keys
private const val EXPORT_PREFIX = PREFIX + "export."
private const val FRONTPAGE_TITLE_KEY = EXPORT_PREFIX + "frontpage.title"
private const val FRONTPAGE_DESC_KEY = EXPORT_PREFIX + "frontpage.description"
private const val SUMMARY_DEPTH_KEY = EXPORT_PREFIX + "summary.depth"
private const val SUMMARY_LEADER_KEY = EXPORT_PREFIX + "summary.leader"
private const val DATE_FORMAT_KEY = EXPORT_PREFIX + "dateFormat"
private const val TOP_LEFT_KEY = EXPORT_PREFIX + "topLeft"
private const val TOP_CENTER_KEY = EXPORT_PREFIX + "topCenter"
private const val TOP_RIGHT_KEY = EXPORT_PREFIX + "topRight"
private const val BOTTOM_LEFT_KEY = EXPORT_PREFIX + "bottomLeft"
private const val BOTTOM_CENTER_KEY = EXPORT_PREFIX + "bottomCenter"
private const val BOTTOM_RIGHT_KEY = EXPORT_PREFIX + "bottomRight"

// Defaults from cucumber+.default.properties
private const val DEF_TITLE = "Cucumber+"
private const val DEF_DESC = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor " +
    "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation " +
    "ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit " +
    "in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat " +
    "non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.\""
private const val DEF_DEPTH = "Scenario"
private const val DEF_LEADER = "Solid"
private const val DEF_DATE_FORMAT = "dd-MM-yyyy"
private const val DEF_TOP_LEFT = "Nimbly"
private const val DEF_TOP_CENTER = ""
private const val DEF_TOP_RIGHT = "now()"
private const val DEF_BOTTOM_LEFT = "Maxime.HAMM@nimbly-consulting.com"
private const val DEF_BOTTOM_CENTER = ""
private const val DEF_BOTTOM_RIGHT = "'Page ' counter(page) ' / ' counter(pages);"

class CucumberPlusOptionsConfigurable : SearchableConfigurable, Configurable.NoScroll {

    // General
    private var enableCucumberPlus: JBCheckBox? = null
    private var showProgressionGuides: JBCheckBox? = null

    // PDF Export — Frontpage
    private var frontpageTitle: JBTextField? = null
    private var frontpageDesc: JBTextField? = null

    // PDF Export — Summary
    private var summaryDepth: ComboBox<String>? = null
    private var summaryLeader: ComboBox<String>? = null

    // PDF Export — Header/Footer
    private var topLeft: JBTextField? = null
    private var topCenter: JBTextField? = null
    private var topRight: JBTextField? = null
    private var bottomLeft: JBTextField? = null
    private var bottomCenter: JBTextField? = null
    private var bottomRight: JBTextField? = null
    private var dateFormat: JBTextField? = null

    private var mainPanel: JPanel? = null

    override fun getId(): String = "io.nimbly.tzatziki.preferences"

    override fun getDisplayName(): String = "Cucumber+"

    override fun createComponent(): JComponent {
        if (mainPanel != null)
            return mainPanel!!

        // General settings
        enableCucumberPlus = JBCheckBox("Enable Cucumber+ table editing")
        showProgressionGuides = JBCheckBox("Show execution progression guides")

        // PDF Export — Frontpage
        frontpageTitle = JBTextField()
        frontpageDesc = JBTextField()

        // PDF Export — Summary
        summaryDepth = ComboBox(DefaultComboBoxModel(arrayOf("Feature", "Rule", "Scenario")))
        summaryLeader = ComboBox(DefaultComboBoxModel(arrayOf("Dotted", "Solid", "Space")))

        // PDF Export — Header/Footer
        topLeft = JBTextField()
        topCenter = JBTextField()
        topRight = JBTextField()
        bottomLeft = JBTextField()
        bottomCenter = JBTextField()
        bottomRight = JBTextField()
        dateFormat = JBTextField()

        mainPanel = FormBuilder.createFormBuilder()
            // General
            .addComponent(TitledSeparator("General"))
            .addComponent(enableCucumberPlus!!)
            .addComponent(showProgressionGuides!!)
            // PDF Export — Frontpage
            .addComponent(TitledSeparator("PDF Export — Frontpage"))
            .addLabeledComponent(JBLabel("Title:"), frontpageTitle!!)
            .addLabeledComponent(JBLabel("Description:"), frontpageDesc!!)
            // PDF Export — Summary
            .addComponent(TitledSeparator("PDF Export — Summary"))
            .addLabeledComponent(JBLabel("Summary depth:"), summaryDepth!!)
            .addLabeledComponent(JBLabel("Summary leader:"), summaryLeader!!)
            // PDF Export — Header/Footer
            .addComponent(TitledSeparator("PDF Export — Header"))
            .addLabeledComponent(JBLabel("Top left:"), topLeft!!)
            .addLabeledComponent(JBLabel("Top center:"), topCenter!!)
            .addLabeledComponent(JBLabel("Top right:"), topRight!!)
            .addComponent(TitledSeparator("PDF Export — Footer"))
            .addLabeledComponent(JBLabel("Bottom left:"), bottomLeft!!)
            .addLabeledComponent(JBLabel("Bottom center:"), bottomCenter!!)
            .addLabeledComponent(JBLabel("Bottom right:"), bottomRight!!)
            .addLabeledComponent(JBLabel("Date format:"), dateFormat!!)
            .addComponent(JBLabel("Note: These are default values. Per-project overrides via .cucumber+/ folder take precedence."))
            .addComponentFillVertically(JPanel(), 0)
            .panel

        reset()
        return mainPanel!!
    }

    override fun isModified(): Boolean {
        val props = PropertiesComponent.getInstance()
        return enableCucumberPlus?.isSelected != props.getBoolean(ENABLED_KEY, true) ||
            showProgressionGuides?.isSelected != props.getBoolean(PROGRESSION_KEY, true) ||
            frontpageTitle?.text != props.getValue(FRONTPAGE_TITLE_KEY, DEF_TITLE) ||
            frontpageDesc?.text != props.getValue(FRONTPAGE_DESC_KEY, DEF_DESC) ||
            (summaryDepth?.selectedItem as? String) != props.getValue(SUMMARY_DEPTH_KEY, DEF_DEPTH) ||
            (summaryLeader?.selectedItem as? String) != props.getValue(SUMMARY_LEADER_KEY, DEF_LEADER) ||
            dateFormat?.text != props.getValue(DATE_FORMAT_KEY, DEF_DATE_FORMAT) ||
            topLeft?.text != props.getValue(TOP_LEFT_KEY, DEF_TOP_LEFT) ||
            topCenter?.text != props.getValue(TOP_CENTER_KEY, DEF_TOP_CENTER) ||
            topRight?.text != props.getValue(TOP_RIGHT_KEY, DEF_TOP_RIGHT) ||
            bottomLeft?.text != props.getValue(BOTTOM_LEFT_KEY, DEF_BOTTOM_LEFT) ||
            bottomCenter?.text != props.getValue(BOTTOM_CENTER_KEY, DEF_BOTTOM_CENTER) ||
            bottomRight?.text != props.getValue(BOTTOM_RIGHT_KEY, DEF_BOTTOM_RIGHT)
    }

    override fun apply() {
        val props = PropertiesComponent.getInstance()

        // General
        val enabled = enableCucumberPlus?.isSelected ?: true
        val progression = showProgressionGuides?.isSelected ?: true
        props.setValue(ENABLED_KEY, enabled, true)
        props.setValue(PROGRESSION_KEY, progression, true)
        TOGGLE_CUCUMBER_PL = enabled

        // PDF Export
        props.setValue(FRONTPAGE_TITLE_KEY, frontpageTitle?.text ?: DEF_TITLE)
        props.setValue(FRONTPAGE_DESC_KEY, frontpageDesc?.text ?: DEF_DESC)
        props.setValue(SUMMARY_DEPTH_KEY, summaryDepth?.selectedItem as? String ?: DEF_DEPTH)
        props.setValue(SUMMARY_LEADER_KEY, summaryLeader?.selectedItem as? String ?: DEF_LEADER)
        props.setValue(DATE_FORMAT_KEY, dateFormat?.text ?: DEF_DATE_FORMAT)
        props.setValue(TOP_LEFT_KEY, topLeft?.text ?: DEF_TOP_LEFT)
        props.setValue(TOP_CENTER_KEY, topCenter?.text ?: DEF_TOP_CENTER)
        props.setValue(TOP_RIGHT_KEY, topRight?.text ?: DEF_TOP_RIGHT)
        props.setValue(BOTTOM_LEFT_KEY, bottomLeft?.text ?: DEF_BOTTOM_LEFT)
        props.setValue(BOTTOM_CENTER_KEY, bottomCenter?.text ?: DEF_BOTTOM_CENTER)
        props.setValue(BOTTOM_RIGHT_KEY, bottomRight?.text ?: DEF_BOTTOM_RIGHT)
    }

    override fun reset() {
        val props = PropertiesComponent.getInstance()

        // General
        enableCucumberPlus?.isSelected = props.getBoolean(ENABLED_KEY, true)
        showProgressionGuides?.isSelected = props.getBoolean(PROGRESSION_KEY, true)

        // PDF Export
        frontpageTitle?.text = props.getValue(FRONTPAGE_TITLE_KEY, DEF_TITLE)
        frontpageDesc?.text = props.getValue(FRONTPAGE_DESC_KEY, DEF_DESC)
        summaryDepth?.selectedItem = props.getValue(SUMMARY_DEPTH_KEY, DEF_DEPTH)
        summaryLeader?.selectedItem = props.getValue(SUMMARY_LEADER_KEY, DEF_LEADER)
        dateFormat?.text = props.getValue(DATE_FORMAT_KEY, DEF_DATE_FORMAT)
        topLeft?.text = props.getValue(TOP_LEFT_KEY, DEF_TOP_LEFT)
        topCenter?.text = props.getValue(TOP_CENTER_KEY, DEF_TOP_CENTER)
        topRight?.text = props.getValue(TOP_RIGHT_KEY, DEF_TOP_RIGHT)
        bottomLeft?.text = props.getValue(BOTTOM_LEFT_KEY, DEF_BOTTOM_LEFT)
        bottomCenter?.text = props.getValue(BOTTOM_CENTER_KEY, DEF_BOTTOM_CENTER)
        bottomRight?.text = props.getValue(BOTTOM_RIGHT_KEY, DEF_BOTTOM_RIGHT)
    }

    companion object {
        /** IDE-level export settings accessible from Config loading */
        private val EXPORT_KEYS = mapOf(
            "export.frontpage.title" to (FRONTPAGE_TITLE_KEY to DEF_TITLE),
            "export.frontpage.description" to (FRONTPAGE_DESC_KEY to DEF_DESC),
            "export.summary.depth" to (SUMMARY_DEPTH_KEY to DEF_DEPTH),
            "export.summary.leader" to (SUMMARY_LEADER_KEY to DEF_LEADER),
            "export.dateFormat" to (DATE_FORMAT_KEY to DEF_DATE_FORMAT),
            "export.topLeft" to (TOP_LEFT_KEY to DEF_TOP_LEFT),
            "export.topCenter" to (TOP_CENTER_KEY to DEF_TOP_CENTER),
            "export.topRight" to (TOP_RIGHT_KEY to DEF_TOP_RIGHT),
            "export.bottomLeft" to (BOTTOM_LEFT_KEY to DEF_BOTTOM_LEFT),
            "export.bottomCenter" to (BOTTOM_CENTER_KEY to DEF_BOTTOM_CENTER),
            "export.bottomRight" to (BOTTOM_RIGHT_KEY to DEF_BOTTOM_RIGHT),
        )

        /**
         * Returns the IDE-level default value for a given export property key,
         * or null if no IDE override is stored for this key.
         */
        fun getIdeDefault(propertyKey: String): String? {
            val (ideKey, defValue) = EXPORT_KEYS[propertyKey] ?: return null
            val stored = PropertiesComponent.getInstance().getValue(ideKey) ?: return null
            return if (stored == defValue) null else stored
        }
    }
}
